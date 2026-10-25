package li.cil.oc.common.menu;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.Tier;
import li.cil.oc.server.GeneralPacketSender;
import li.cil.oc.util.SideTracker;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import scala.Function0;
import scala.Function1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractMenu extends AbstractContainerMenu {
    private final Inventory playerInventory;
    private final Container otherInventory;
    private final int playerInventorySizeX;
    private final int playerInventorySizeY;
    private final int slotSize = 18;
    private long lastSync = System.currentTimeMillis();
    protected final SynchronizedData synchronizedData = new SynchronizedData();

    protected AbstractMenu(final MenuType<? extends AbstractMenu> type, final int id,
                           final Inventory playerInventory, final Container otherInventory) {
        super(type, id);
        this.playerInventory = playerInventory;
        this.otherInventory = otherInventory;
        playerInventorySizeX = Math.min(9, Inventory.getSelectionSize());
        playerInventorySizeY = Math.min(4, playerInventory.getContainerSize() / playerInventorySizeX);
    }

    public Inventory playerInventory() { return playerInventory; }
    public Container otherInventory() { return otherInventory; }
    public int playerInventorySizeX() { return playerInventorySizeX; }
    public int playerInventorySizeY() { return playerInventorySizeY; }
    public int slotSize() { return slotSize; }
    public SynchronizedData synchronizedData() { return synchronizedData; }

    @Override
    public boolean stillValid(final Player player) {
        return otherInventory.stillValid(player);
    }

    @Override
    public void clicked(final int slot, final int dragType, final ClickType clickType, final Player player) {
        super.clicked(slot, dragType, clickType, player);
        if (SideTracker.isServer()) {
            // Enforce synchronization because inventory changes may assign stack addresses.
            broadcastChanges();
        }
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            tryTransferStackInSlot(slot, slot.container == otherInventory);
            if (SideTracker.isServer()) broadcastChanges();
        }
        return ItemStack.EMPTY;
    }

    protected boolean tryMoveAllSlotToSlot(final Slot from, final Slot to) {
        if (to == null) return false;
        if (from == null || !from.hasItem() || from.getItem().isEmpty()) return true;
        if (to.container == from.container) return false;

        final ItemStack fromStack = from.getItem();
        final ItemStack toStack = to.hasItem() ? to.getItem() : ItemStack.EMPTY;
        final int toStackSize = toStack.isEmpty() ? 0 : toStack.getCount();
        final int maxStackSize = Math.min(fromStack.getMaxStackSize(), to.getMaxStackSize());
        final int itemsMoved = Math.min(maxStackSize - toStackSize, fromStack.getCount());

        if (!toStack.isEmpty()) {
            if (toStackSize < maxStackSize && ItemStack.isSameItemSameComponents(fromStack, toStack) && itemsMoved > 0) {
                toStack.grow(from.remove(itemsMoved).getCount());
            } else return false;
        } else if (to.mayPlace(fromStack)) {
            to.set(from.remove(itemsMoved));
            if (maxStackSize == 0) return true;
        } else return false;

        to.setChanged();
        from.setChanged();
        return false;
    }

    protected List<Integer> fillOrder(final boolean backFill) {
        final var order = new ArrayList<Integer>(slots.size());
        if (backFill) {
            for (int index = slots.size() - 1; index >= 0; index--) order.add(index);
        } else {
            for (int index = 0; index < slots.size(); index++) order.add(index);
        }
        order.sort(Comparator.comparingInt(index -> {
            final Slot slot = slots.get(index);
            if (slot != null && slot.hasItem()) return -1;
            if (slot instanceof ComponentSlot componentSlot) return componentSlot.tier();
            return 99;
        }));
        return order;
    }

    public void tryTransferStackInSlot(final Slot from, final boolean intoPlayerInventory) {
        for (final int index : fillOrder(intoPlayerInventory)) {
            final Slot target = slots.get(index);
            if (target != null && tryMoveAllSlotToSlot(from, target)) return;
        }
    }

    public abstract Class<? extends EnvironmentHost> getHostClass();

    public void addSlotToContainer(final int x, final int y) {
        addSlotToContainer(x, y, li.cil.oc.common.Slot.Any, Tier.Any);
    }

    public void addSlotToContainer(final int x, final int y, final String slot) {
        addSlotToContainer(x, y, slot, Tier.Any);
    }

    public void addSlotToContainer(final int x, final int y, final String slot, final int tier) {
        final int index = slots.size();
        addSlot(new StaticComponentSlot(this, otherInventory, index, x, y, getHostClass(), slot, tier));
    }

    public void addSlotToContainer(final int x, final int y, final InventorySlots.InventorySlot[][] info,
                                   final Function0<?> containerTierGetter) {
        final int index = slots.size();
        addSlot(new DynamicComponentSlot(this, otherInventory, index, x, y, getHostClass(),
            slot -> info[((Number) slot.containerTierGetter.apply()).intValue()][slot.getSlotIndex()], containerTierGetter));
    }

    public void addSlotToContainer(final int x, final int y,
                                   final Function1<DynamicComponentSlot, InventorySlots.InventorySlot> info) {
        final int index = slots.size();
        addSlot(new DynamicComponentSlot(this, otherInventory, index, x, y, getHostClass(), info, () -> Tier.One));
    }

    protected void addPlayerInventorySlots(final int left, final int top) {
        for (int slotY = 1; slotY < playerInventorySizeY; slotY++) {
            for (int slotX = 0; slotX < playerInventorySizeX; slotX++) {
                final int index = slotX + slotY * playerInventorySizeX;
                final int x = left + slotX * slotSize;
                final int y = top + (slotY - 1) * slotSize;
                addSlot(new Slot(playerInventory, index, x, y));
            }
        }

        final int quickBarSpacing = 4;
        for (int index = 0; index < playerInventorySizeX; index++) {
            final int x = left + index * slotSize;
            final int y = top + slotSize * (playerInventorySizeY - 1) + quickBarSpacing;
            addSlot(new Slot(playerInventory, index, x, y));
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (SideTracker.isServer()) {
            final CompoundTag nbt = new CompoundTag();
            detectCustomDataChanges(nbt);
            if (playerInventory.player instanceof ServerPlayer player) {
                GeneralPacketSender.sendContainerUpdateUnchecked(this, nbt, player);
            }
        }
    }

    protected void detectCustomDataChanges(final CompoundTag nbt) {
        final CompoundTag delta = synchronizedData.getDelta();
        if (delta != null && !delta.isEmpty()) {
            nbt.put("delta", delta);
        } else if (System.currentTimeMillis() - lastSync > 250) {
            nbt.put("delta", synchronizedData);
            lastSync = Long.MAX_VALUE;
        }
    }

    public void updateCustomData(final CompoundTag nbt) {
        if (nbt.contains("delta")) {
            final CompoundTag delta = nbt.getCompound("delta");
            for (final String key : delta.getAllKeys()) synchronizedData.put(key, delta.get(key));
        }
    }

    protected class SynchronizedData extends CompoundTag {
        private CompoundTag delta = new CompoundTag();

        public synchronized CompoundTag getDelta() {
            if (delta.isEmpty()) return null;
            final CompoundTag result = delta;
            delta = new CompoundTag();
            return result;
        }

        @Override
        public synchronized Tag put(final String key, final Tag value) {
            if (!value.equals(get(key))) delta.put(key, value);
            return super.put(key, value);
        }

        @Override public synchronized void putByte(final String key, final byte value) {
            if (value != getByte(key)) delta.putByte(key, value);
            super.putByte(key, value);
        }
        @Override public synchronized void putShort(final String key, final short value) {
            if (value != getShort(key)) delta.putShort(key, value);
            super.putShort(key, value);
        }
        @Override public synchronized void putInt(final String key, final int value) {
            if (value != getInt(key)) delta.putInt(key, value);
            super.putInt(key, value);
        }
        @Override public synchronized void putLong(final String key, final long value) {
            if (value != getLong(key)) delta.putLong(key, value);
            super.putLong(key, value);
        }
        @Override public synchronized void putFloat(final String key, final float value) {
            if (value != getFloat(key)) delta.putFloat(key, value);
            super.putFloat(key, value);
        }
        @Override public synchronized void putDouble(final String key, final double value) {
            if (value != getDouble(key)) delta.putDouble(key, value);
            super.putDouble(key, value);
        }
        @Override public synchronized void putString(final String key, final String value) {
            if (!value.equals(getString(key))) delta.putString(key, value);
            super.putString(key, value);
        }
        @Override public synchronized void putByteArray(final String key, final byte[] value) {
            final Tag current = get(key);
            if (current instanceof ByteArrayTag array && !Arrays.equals(value, array.getAsByteArray())) {
                delta.putByteArray(key, value);
            }
            super.putByteArray(key, value);
        }
        @Override public synchronized void putIntArray(final String key, final int[] value) {
            final Tag current = get(key);
            if (current instanceof IntArrayTag array && !Arrays.equals(value, array.getAsIntArray())) {
                delta.putIntArray(key, value);
            }
            super.putIntArray(key, value);
        }
        @Override public synchronized void putBoolean(final String key, final boolean value) {
            if (value != getBoolean(key)) delta.putBoolean(key, value);
            super.putBoolean(key, value);
        }
    }
}
