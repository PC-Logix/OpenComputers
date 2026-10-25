package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.Sound;
import li.cil.oc.common.container.ItemStackInventory;

import li.cil.oc.common.menu.DiskDrive;

import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedDataComponentHolderSupportJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.MenuProvider;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;

import java.util.Optional;
import java.util.EnumSet;
import java.util.Map;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;

public final class DiskDriveMountable extends AbstractManagedEnvironment implements ItemStackInventory, RackMountable, Analyzable, DeviceInfo, MenuProvider {
    private final Rack rack;
    private final int slot;
    private final ItemStack[] items = ItemStackInventory.newItems(this);
    private final DiskDriveMountableInventory components = new DiskDriveMountableInventory(this);
    private long lastAccess;
    private Map<String, String> deviceInfo;

    public DiskDriveMountable(Rack rack, int slot) {
        this.rack = rack;
        this.slot = slot;
        setNode(Network.newNode(this, Visibility.Network).withComponent("disk_drive").create());
        if (container() != null && !container().isEmpty()) reinitialize();
    }

    public Rack rack() { return rack; }
    public int slot() { return slot; }
    public void lastAccess_$eq(long value) { lastAccess = value; }
    @Override public ItemStack[] items() { return items; }
    @Override public ItemStack container() { return rack.getItem(slot); }
    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && Driver.driverFor(stack) != null && li.cil.oc.common.Slot.Floppy.equals(Driver.driverFor(stack).slot(stack));
    }
    @Override public boolean stillValid(Player player) { return rack.stillValid(player); }

    public Option<Node> filesystemNode() {
        Optional<ManagedEnvironment>[] slots = ComponentInventorySlotAccessJava.snapshot(components);
        if (slots.length == 0 || slots[0].isEmpty()) return Option.empty();
        Node node = slots[0].get().node();
        return Option.apply(node);
    }

    @Override public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = li.cil.oc.server.component.ServerComponentDeviceInfo.diskDriveMountable();
        return deviceInfo;
    }

    @Callback(doc = "function():boolean -- Checks whether some medium is currently in the drive.")
    public Object[] isEmpty(Context context, Arguments args) { return ResultWrapper.result(filesystemNode().isEmpty()); }

    @Callback(doc = "function([velocity:number]):boolean -- Eject the currently present medium from the drive.")
    public Object[] eject(Context context, Arguments args) {
        double velocity = Math.max(0, Math.min(1, args.optDouble(0, 0)));
        ItemStack ejected = removeItem(0, 1);
        if (ejected.isEmpty()) return ResultWrapper.result(false);
        var entity = InventoryUtils.spawnStackInWorld(BlockPositionsJava.apply(rack), ejected, java.util.Optional.of(rack.facing()));
        if (entity != null) entity.push(rack.facing().getStepX() * velocity,
                rack.facing().getStepY() * velocity, rack.facing().getStepZ() * velocity);
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(): string -- Return the internal floppy disk address")
    public Object[] media(Context context, Arguments args) {
        Option<Node> node = filesystemNode();
        return node.isEmpty() ? ResultWrapper.result(null, "drive is empty") : ResultWrapper.result(node.get().address());
    }

    @Override public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return filesystemNode().isEmpty() ? null : new Node[]{filesystemNode().get()};
    }

    @Override public void onItemAdded(int slot, ItemStack stack) {
        components.onItemAdded(slot, stack);
        Optional<ManagedEnvironment>[] componentSlots = ComponentInventorySlotAccessJava.snapshot(components);
        if (slot >= 0 && slot < componentSlots.length && componentSlots[slot].isPresent()) {
            Node node = componentSlots[slot].get().node();
            if (node instanceof li.cil.oc.api.network.Component component) component.setVisibility(Visibility.Network);
        }
        if (!rack.getEnvironmentLevel().isClientSide()) {
            rack.markChanged(this.slot);
            Sound.playDiskInsert(rack);
        }
    }

    @Override public void onItemRemoved(int slot, ItemStack stack) {
        components.onItemRemoved(slot, stack);
        if (!rack.getEnvironmentLevel().isClientSide()) {
            rack.markChanged(this.slot);
            Sound.playDiskEject(rack);
        }
    }
    @Override public void loadData(DataComponentHolder holder) {
        try { super.loadData(holder); }
        catch (UnrecoverablePersistanceException exception) { DiskDriveMountable.<RuntimeException>throwUnchecked(exception); }
        components.loadData(holder);
        components.connectComponents();
        lastAccess = ExtendedDataComponentHolderSupportJava.convert(holder)
                .getComponentOptional(li.cil.oc.common.datacomponents.OCComponentsJava.Network.LAST_ACCESS.get())
                .map(Number::longValue).orElse(0L);
    }

    @Override public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        components.saveData(holder);
    }

    @Override public void describeForClient(MutableDataComponentHolder holder) {
        holder.set(li.cil.oc.common.datacomponents.OCComponentsJava.Network.LAST_ACCESS.get(), lastAccess);
        holder.set(li.cil.oc.common.datacomponents.OCComponentsJava.Network.DISK_ITEM.get(), ImmutableItemStack.copyOf(getItem(0)));
    }

    @Override public int getConnectableCount() { return 0; }
    @Override public RackBusConnectable getConnectableAt(int index) { return null; }

    @Override public boolean onActivate(Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY) {
        if (player.isCrouching()) {
            boolean diskInDrive = !getItem(0).isEmpty();
            boolean holdingDisk = canPlaceItem(0, heldItem);
            if (diskInDrive && !rack.getEnvironmentLevel().isClientSide())
                InventoryUtils.dropSlot(BlockPositionsJava.apply(rack), this, 0, 1, java.util.Optional.of(rack.facing()));
            if (holdingDisk) setItem(0, player.getInventory().removeItem(player.getInventory().selected, 1));
            return diskInDrive || holdingDisk;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this);
            return true;
        }
        return false;
    }

    @Override public Component getDisplayName() { return Component.empty(); }
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new DiskDrive(id, playerInventory, this);
    }
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable error) throws T { throw (T) error; }

    @Override public EnumSet<StateAware.State> getCurrentState() { return EnumSet.noneOf(StateAware.State.class); }
}
