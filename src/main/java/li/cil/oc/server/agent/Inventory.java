package li.cil.oc.server.agent;

import li.cil.oc.api.internal.Agent;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import scala.Option;
import scala.collection.Iterable;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class Inventory extends net.minecraft.world.entity.player.Inventory {
    private final Agent agent;

    public Inventory(Player player, Agent agent) {
        super(player);
        this.agent = agent;
    }

    private ItemStack selectedItemStack() {
        return agent.mainInventory().getItem(agent.selectedSlot());
    }

    private List<Integer> inventorySlots() {
        List<Integer> slots = new ArrayList<>();
        for (int slot = agent.selectedSlot(); slot < getContainerSize(); slot++) slots.add(slot);
        for (int slot = 0; slot < agent.selectedSlot(); slot++) slots.add(slot);
        return slots;
    }

    @Override
    public ItemStack getSelected() {
        return agent.equipmentInventory().getItem(0);
    }

    @Override
    public int getFreeSlot() {
        if (selectedItemStack().isEmpty()) return agent.selectedSlot();
        for (int slot : inventorySlots()) {
            if (getItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    @Override
    public void pickSlot(int direction) {
    }

    @Override
    public int clearOrCountMatchingItems(Predicate<ItemStack> predicate, int count, Container container) {
        return 0;
    }

    @Override
    public void tick() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack != null && !stack.isEmpty()) {
                try {
                    stack.inventoryTick(agent.getEnvironmentLevel(),
                        agent.getEnvironmentLevel().isClientSide() ? null : agent.player(), slot, slot == 0);
                } catch (NullPointerException ignored) {
                    // Client side item updates that need a player instance.
                }
            }
        }
    }

    @Override
    public boolean add(ItemStack stack) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = agent.selectedSlot(); slot < getContainerSize(); slot++) slots.add(slot);
        for (int slot = 0; slot < agent.selectedSlot(); slot++) slots.add(slot);
        Iterable<Object> scalaSlots = (Iterable<Object>) (Object) CollectionConverters.asScala(slots);
        Option<Iterable<Object>> selectedSlots = Option.apply(scalaSlots);
        IItemHandlerModifiable handler = InventoryUtils.asItemHandler(this);
        return InventoryUtils.insertIntoInventory(stack, handler, 64, false, selectedSlots);
    }

    @Override
    public float getDestroySpeed(BlockState state) {
        ItemStack selected = getSelected();
        return selected.isEmpty() ? 1.0f : selected.getDestroySpeed(state);
    }

    @Override
    public ListTag save(ListTag nbt) {
        return nbt;
    }

    @Override
    public void load(ListTag nbt) {
    }

    @Override
    public ItemStack getArmor(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void dropAll() {
    }

    @Override
    public boolean contains(ItemStack stack) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack containerStack = getItem(slot);
            if (!containerStack.isEmpty() && ItemStack.isSameItem(stack, containerStack)) return true;
        }
        return false;
    }

    @Override
    public void replaceWith(net.minecraft.world.entity.player.Inventory from) {
    }

    @Override
    public int getContainerSize() {
        return agent.mainInventory().getContainerSize();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot < 0 ? agent.equipmentInventory().getItem(~slot) : agent.mainInventory().getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return slot < 0 ? agent.equipmentInventory().removeItem(~slot, amount) : agent.mainInventory().removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot < 0 ? agent.equipmentInventory().removeItemNoUpdate(~slot) : agent.mainInventory().removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0) agent.equipmentInventory().setItem(~slot, stack);
        else agent.mainInventory().setItem(slot, stack);
    }

    @Override
    public Component getName() {
        return Component.literal(agent.name());
    }

    @Override
    public int getMaxStackSize() {
        return agent.mainInventory().getMaxStackSize();
    }

    @Override
    public void setChanged() {
        agent.mainInventory().setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return agent.mainInventory().stillValid(player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < 0 ? agent.equipmentInventory().canPlaceItem(~slot, stack) : agent.mainInventory().canPlaceItem(slot, stack);
    }
}
