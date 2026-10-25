package li.cil.oc.common.blockentity.traits;

import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;
import java.util.function.Consumer;

/** Native Java block-inventory capability, including loot and moving-host interaction. */
public interface BlockInventory extends BaseBlockEntity, li.cil.oc.common.container.Inventory {
    InventoryRuntimeJava inventoryRuntime();
    Vec3 inventoryMovingPosition();
    @Override default ItemStack[] items() { return inventoryRuntime().items(getContainerSize(), ItemStack.EMPTY); }
    @Override default boolean stillValid(Player player) {
        return InventoryRuntimeJava.stillValid(player.position(), isMoving(), inventoryMovingPosition(), getBlockPos())
            && ComputerLifecycleJava.canInteract(this, player);
    }
    default void forAllLoot(Consumer<ItemStack> destination) { InventoryUtils.forAllSlots(this, destination); }
    default boolean dropSlot(int slot, int count, Optional<Direction> direction) {
        return InventoryUtils.dropSlot(BlockPositionsJava.apply(x(), y(), z(), getLevel()), this, slot, count, direction);
    }
    default boolean dropSlot(int slot, int count, scala.Option<Direction> direction) { return dropSlot(slot, count, scala.jdk.javaapi.OptionConverters.toJava(direction)); }
    default boolean dropSlot(int slot) { return dropSlot(slot, getMaxStackSize(), Optional.empty()); }
    default boolean dropSlot(int slot, int count) { return dropSlot(slot, count, Optional.empty()); }
    default int dropSlot$default$2() { return getMaxStackSize(); }
    default scala.Option<Direction> dropSlot$default$3() { return scala.Option.empty(); }
    default void dropAllSlots() { InventoryUtils.dropAllSlots(BlockPositionsJava.apply(x(), y(), z(), getLevel()), this); }
    default net.minecraft.world.entity.item.ItemEntity spawnStackInWorld(ItemStack stack, Optional<Direction> direction) { return InventoryUtils.spawnStackInWorld(BlockPositionsJava.apply(x(), y(), z(), getLevel()), stack, direction); }
    default net.minecraft.world.entity.item.ItemEntity spawnStackInWorld(ItemStack stack, scala.Option<Direction> direction) { return spawnStackInWorld(stack, scala.jdk.javaapi.OptionConverters.toJava(direction)); }
    default net.minecraft.world.entity.item.ItemEntity spawnStackInWorld(ItemStack stack) { return spawnStackInWorld(stack, Optional.empty()); }
    default scala.Option<Direction> spawnStackInWorld$default$2() { return scala.Option.empty(); }
}


