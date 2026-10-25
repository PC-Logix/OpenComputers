package li.cil.oc.util;

import li.cil.oc.OpenComputers;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import scala.Option;
import scala.Option$;
import scala.Some;
import scala.Function0;
import scala.collection.Iterable;
import scala.collection.Iterator;
import scala.collection.immutable.List$;
import scala.runtime.BoxedUnit;
import scala.Function1;
import scala.Function2;
import scala.jdk.javaapi.OptionConverters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class InventoryUtils {
    private InventoryUtils() {}

    public static <T> T nullable(Option<T> option) { return option.isDefined() ? option.get() : null; }

    public static IItemHandlerModifiable asItemHandler(Container inventory, Direction side) {
        return inventory instanceof WorldlyContainer worldly && side != null
                ? new SidedInvWrapper(worldly, side) : new InvWrapper(inventory);
    }
    public static IItemHandlerModifiable asItemHandler(Container inventory) { return asItemHandler(inventory, null); }

    public static boolean haveSameItemType(ItemStack a, ItemStack b, boolean checkNbt) {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem()
                && a.getDamageValue() == b.getDamageValue()
                && (!checkNbt || ItemStack.isSameItemSameComponents(a, b));
    }
    public static boolean haveSameItemType(ItemStack a, ItemStack b) { return haveSameItemType(a, b, false); }

    /** Scala compatibility accessor; Java callers should use {@link #inventorySourceAtOptional(BlockPosition, Direction)}. */
    @Deprecated
    public static Option<InventorySource> inventorySourceAt(BlockPosition position, Direction side) {
        return OptionConverters.toScala(inventorySourceAtOptional(position, side));
    }

    public static Optional<InventorySource> inventorySourceAtOptional(BlockPosition position, Direction side) {
        Optional<Level> levelOption = position.worldOptional();
        if (levelOption.isEmpty()) return Optional.empty();
        Level level = levelOption.get();
        if (!new ExtendedLevelJava(level).blockExists(position)) return Optional.empty();
        var pos = position.toBlockPos();
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (handler != null) return Optional.of(new BlockInventorySource(position, side, handler));
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container container)
            return Optional.of(new BlockInventorySource(position, side, asItemHandler(container, side)));
        for (Entity entity : level.getEntitiesOfClass(Entity.class, position.bounds())) {
            if (!entity.isAlive()) continue;
            IItemHandler entityHandler = entity.getCapability(Capabilities.ItemHandler.ENTITY);
            if (entityHandler != null) return Optional.of(new EntityInventorySource(entity, side, entityHandler));
        }
        return Optional.empty();
    }

    public static Option<IItemHandler> inventoryAt(BlockPosition position, Direction side) {
        return OptionConverters.toScala(inventoryAtOptional(position, side));
    }

    public static Optional<IItemHandler> inventoryAtOptional(BlockPosition position, Direction side) {
        Optional<InventorySource> source = inventorySourceAtOptional(position, side);
        return source.map(InventorySource::inventory);
    }

    public static Option<InventorySource> anyInventorySourceAt(BlockPosition position) {
        return OptionConverters.toScala(anyInventorySourceAtOptional(position));
    }

    public static Optional<InventorySource> anyInventorySourceAtOptional(BlockPosition position) {
        Optional<InventorySource> found = inventorySourceAtOptional(position, null);
        if (found.isPresent()) return found;
        for (Direction side : Direction.values()) {
            found = inventorySourceAtOptional(position, side);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    public static Option<IItemHandler> anyInventoryAt(BlockPosition position) {
        return OptionConverters.toScala(anyInventoryAtOptional(position));
    }

    public static Optional<IItemHandler> anyInventoryAtOptional(BlockPosition position) {
        Optional<InventorySource> source = anyInventorySourceAtOptional(position);
        return source.map(InventorySource::inventory);
    }

    public static boolean insertIntoInventorySlot(ItemStack stack, IItemHandler inventory, int slot, int limit, boolean simulate) {
        if (stack.isEmpty() || limit <= 0 || stack.getCount() <= 0) return false;
        int amount = Math.min(stack.getCount(), limit);
        ItemStack toInsert = stack.split(amount);
        ItemStack remaining = inventory.insertItem(slot, toInsert, simulate);
        boolean inserted = remaining.getCount() < amount;
        stack.grow(remaining.getCount());
        return inserted;
    }
    public static boolean insertIntoInventorySlot(ItemStack s, IItemHandler i, int slot, int limit) { return insertIntoInventorySlot(s, i, slot, limit, false); }
    public static boolean insertIntoInventorySlot(ItemStack s, IItemHandler i, int slot) { return insertIntoInventorySlot(s, i, slot, 64, false); }
    public static boolean insertIntoInventorySlot(ItemStack stack, Container inventory, Option<Direction> side, int slot, int limit, boolean simulate) {
        return insertIntoInventorySlot(stack, asItemHandler(inventory, nullable(side)), slot, limit, simulate);
    }

    public static int extractFromInventorySlot(Function2<ItemStack, Object, BoxedUnit> consumer, IItemHandler inventory, int slot, int limit) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty() || limit <= 0 || stack.getCount() <= 0) return 0;
        int amount = Math.min(Math.min(stack.getMaxStackSize(), stack.getCount()), limit);
        ItemStack simulated = inventory.extractItem(slot, amount, true);
        if (simulated == null || simulated.isEmpty()) return 0;
        ItemStack extracted = simulated.copy();
        amount = extracted.getCount();
        consumer.apply(extracted, Boolean.TRUE);
        int count = Math.max(amount - extracted.getCount(), 0);
        if (count > 0) {
            ItemStack real = inventory.extractItem(slot, count, false);
            if (real != null && real.getCount() == count) consumer.apply(real, Boolean.FALSE);
            else {
                OpenComputers.log().warn("An IItemHandler instance acted differently between simulated and non-simulated extraction. Offender: " + inventory);
                if (real != null && !real.isEmpty()) consumer.apply(real, Boolean.FALSE);
            }
        }
        return count;
    }
    public static int extractFromInventorySlot(Function2<ItemStack, Object, BoxedUnit> c, IItemHandler i, int slot) { return extractFromInventorySlot(c, i, slot, 64); }
    public static int extractFromInventorySlot(Function2<ItemStack, Object, BoxedUnit> c, Container i, Direction side, int slot, int limit) {
        return extractFromInventorySlot(c, asItemHandler(i, side), slot, limit);
    }

    public static boolean insertIntoInventory(ItemStack stack, IItemHandler inventory, int limit, boolean simulate, Option<Iterable<Object>> slots) {
        if (stack.isEmpty() || limit <= 0 || stack.getCount() <= 0) return false;
        List<Integer> range = new ArrayList<>();
        if (slots.isDefined()) {
            Iterator<Object> iterator = slots.get().iterator();
            while (iterator.hasNext()) range.add((Integer) iterator.next());
        }
        else for (int slot = 0; slot < inventory.getSlots(); slot++) range.add(slot);
        return insertIntoInventory(stack, inventory, limit, simulate, range);
    }
    public static boolean insertIntoInventory(ItemStack stack, IItemHandler inventory, int limit, boolean simulate, Optional<? extends java.lang.Iterable<Integer>> slots) {
        if (stack.isEmpty() || limit <= 0 || stack.getCount() <= 0) return false;
        List<Integer> range = new ArrayList<>();
        if (slots.isPresent()) slots.get().forEach(range::add);
        else for (int slot = 0; slot < inventory.getSlots(); slot++) range.add(slot);
        return insertIntoInventory(stack, inventory, limit, simulate, range);
    }
    private static boolean insertIntoInventory(ItemStack stack, IItemHandler inventory, int limit, boolean simulate, List<Integer> range) {
        if (stack.isEmpty() || limit <= 0 || stack.getCount() <= 0) return false;
        int remaining = Math.min(limit, stack.getCount());
        boolean success = false;
        for (int slot : range) {
            int previous = stack.getCount();
            if (remaining > 0 && insertIntoInventorySlot(stack, inventory, slot, remaining, simulate)) {
                remaining -= previous - stack.getCount();
                success = true;
            }
            if (remaining <= 0) break;
        }
        return success;
    }
    public static boolean insertIntoInventory(ItemStack s, IItemHandler i, int limit, boolean simulate) { return insertIntoInventory(s, i, limit, simulate, Option$.MODULE$.empty()); }
    public static boolean insertIntoInventory(ItemStack s, IItemHandler i, int limit) { return insertIntoInventory(s, i, limit, false); }
    public static boolean insertIntoInventory(ItemStack s, IItemHandler i) { return insertIntoInventory(s, i, 64, false); }
    public static boolean insertIntoInventory(ItemStack s, Container i, Option<Direction> side, int limit, boolean simulate, Option<Iterable<Object>> slots) {
        return insertIntoInventory(s, asItemHandler(i, nullable(side)), limit, simulate, slots);
    }
    public static boolean insertIntoInventory(ItemStack s, Container i, Optional<Direction> side, int limit, boolean simulate, Optional<? extends java.lang.Iterable<Integer>> slots) {
        return insertIntoInventory(s, asItemHandler(i, side.orElse(null)), limit, simulate, slots);
    }

    public static int extractAnyFromInventory(Function2<ItemStack, Object, BoxedUnit> consumer, IItemHandler inventory, int limit) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            int extracted = extractFromInventorySlot(consumer, inventory, slot, limit);
            if (extracted > 0) return extracted;
        }
        return 0;
    }
    public static int extractAnyFromInventory(Function2<ItemStack, Object, BoxedUnit> c, IItemHandler i) { return extractAnyFromInventory(c, i, 64); }
    public static int extractAnyFromInventory(Function2<ItemStack, Object, BoxedUnit> c, Container i, Direction side, int limit) {
        return extractAnyFromInventory(c, asItemHandler(i, side), limit);
    }

    public static ItemStack extractFromInventory(ItemStack stack, IItemHandler inventory, boolean simulate, boolean exact) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < inventory.getSlots() && remaining.getCount() > 0; slot++) {
            final ItemStack remainder = remaining;
            extractFromInventorySlot((stackInInventory, simulateInsert) -> {
                if (stackInInventory != null && remainder.getItem() == stackInInventory.getItem()
                        && (!exact || haveSameItemType(remainder, stackInInventory, true))) {
                    int transferred = Math.min(stackInInventory.getCount(), remainder.getCount());
                    if (!((Boolean) simulateInsert)) remainder.shrink(transferred);
                    if (((Boolean) simulateInsert) || !simulate) stackInInventory.shrink(transferred);
                }
                return BoxedUnit.UNIT;
            }, inventory, slot, remaining.getCount());
        }
        return remaining;
    }
    public static ItemStack extractFromInventory(ItemStack s, IItemHandler i, boolean simulate) { return extractFromInventory(s, i, simulate, true); }
    public static ItemStack extractFromInventory(ItemStack s, IItemHandler i) { return extractFromInventory(s, i, false, true); }
    public static ItemStack extractFromInventory(ItemStack s, Container i, Direction side, boolean simulate, boolean exact) {
        return extractFromInventory(s, asItemHandler(i, side), simulate, exact);
    }

    public static boolean insertIntoInventoryAt(ItemStack stack, BlockPosition position, Option<Direction> side, int limit, boolean simulate) {
        Option<IItemHandler> inventory = inventoryAt(position, nullable(side));
        return inventory.isDefined() && insertIntoInventory(stack, inventory.get(), limit, simulate);
    }
    public static boolean insertIntoInventoryAt(ItemStack stack, BlockPosition position, Optional<Direction> side,
                                                int limit, boolean simulate) {
        Optional<IItemHandler> inventory = InventoryAccessJava.inventoryAt(position, side.orElse(null));
        return inventory.isPresent() && insertIntoInventory(stack, inventory.get(), limit, simulate);
    }
    public static boolean insertIntoInventoryAt(ItemStack stack, BlockPosition position, Optional<Direction> side, int limit) {
        return insertIntoInventoryAt(stack, position, side, limit, false);
    }
    public static boolean insertIntoInventoryAt(ItemStack stack, BlockPosition position, Optional<Direction> side) {
        return insertIntoInventoryAt(stack, position, side, 64, false);
    }
    public static boolean insertIntoInventoryAt(ItemStack s, BlockPosition p, Option<Direction> side, int limit) { return insertIntoInventoryAt(s, p, side, limit, false); }
    public static boolean insertIntoInventoryAt(ItemStack s, BlockPosition p, Option<Direction> side) { return insertIntoInventoryAt(s, p, side, 64, false); }
    public static boolean insertIntoInventoryAt(ItemStack s, BlockPosition p) { return insertIntoInventoryAt(s, p, Option$.MODULE$.empty()); }

    public static Function0<Object> getExtractorFromInventoryAt(Function2<ItemStack, Object, BoxedUnit> consumer, BlockPosition position, Direction side, int limit) {
        Option<IItemHandler> inventory = inventoryAt(position, side);
        return inventory.isDefined() ? () -> extractAnyFromInventory(consumer, inventory.get(), limit) : null;
    }
    public static Function0<Object> getExtractorFromInventoryAt(Function2<ItemStack, Object, BoxedUnit> c, BlockPosition p, Direction s) { return getExtractorFromInventoryAt(c, p, s, 64); }

    public static int transferBetweenInventories(IItemHandler source, IItemHandler sink, int limit) {
        return extractAnyFromInventory((stack, simulated) -> {
            insertIntoInventory(stack, sink, limit, (Boolean) simulated);
            return BoxedUnit.UNIT;
        }, source, limit);
    }
    public static int transferBetweenInventories(IItemHandler s, IItemHandler d) { return transferBetweenInventories(s, d, 64); }
    public static int transferBetweenInventories(Container s, Direction ss, Container d, Option<Direction> ds, int limit) {
        return transferBetweenInventories(asItemHandler(s, ss), asItemHandler(d, nullable(ds)), limit);
    }

    public static int transferBetweenInventoriesSlots(IItemHandler source, int sourceSlot, IItemHandler sink, Option<Integer> sinkSlot, int limit) {
        Function2<ItemStack, Object, BoxedUnit> callback = (stack, simulated) -> {
            if (sinkSlot.isDefined()) insertIntoInventorySlot(stack, sink, sinkSlot.get(), limit, (Boolean) simulated);
            else insertIntoInventory(stack, sink, limit, (Boolean) simulated);
            return BoxedUnit.UNIT;
        };
        return extractFromInventorySlot(callback, source, sourceSlot, limit);
    }
    public static int transferBetweenInventoriesSlots(IItemHandler s, int ss, IItemHandler d, Option<Integer> ds) { return transferBetweenInventoriesSlots(s, ss, d, ds, 64); }
    public static int transferBetweenInventoriesSlots(Container s, Direction ss, int slot, Container d, Option<Direction> ds, Option<Integer> dslot, int limit) {
        return transferBetweenInventoriesSlots(asItemHandler(s, ss), slot, asItemHandler(d, nullable(ds)), dslot, limit);
    }

    public static Function0<Object> getTransferBetweenInventoriesAt(BlockPosition source, Direction sourceSide, BlockPosition sink, Option<Direction> sinkSide, int limit) {
        Option<IItemHandler> src = inventoryAt(source, sourceSide), dst = inventoryAt(sink, nullable(sinkSide));
        return src.isDefined() && dst.isDefined() ? () -> transferBetweenInventories(src.get(), dst.get(), limit) : null;
    }
    public static Function0<Object> getTransferBetweenInventoriesAt(BlockPosition s, Direction ss, BlockPosition d, Option<Direction> ds) { return getTransferBetweenInventoriesAt(s, ss, d, ds, 64); }
    public static Function0<Object> getTransferBetweenInventoriesSlotsAt(BlockPosition source, Direction sourceSide, int sourceSlot, BlockPosition sink, Option<Direction> sinkSide, Option<Integer> sinkSlot, int limit) {
        Option<IItemHandler> src = inventoryAt(source, sourceSide), dst = inventoryAt(sink, nullable(sinkSide));
        return src.isDefined() && dst.isDefined() ? () -> transferBetweenInventoriesSlots(src.get(), sourceSlot, dst.get(), sinkSlot, limit) : null;
    }
    public static Function0<Object> getTransferBetweenInventoriesSlotsAt(BlockPosition s, Direction ss, int slot, BlockPosition d, Option<Direction> ds, Option<Integer> dslot) { return getTransferBetweenInventoriesSlotsAt(s, ss, slot, d, ds, dslot, 64); }

    public static void forAllSlots(Container inventory, Consumer<ItemStack> destination) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && !stack.isEmpty() && stack.getCount() > 0) destination.accept(stack);
        }
    }
    public static boolean dropSlot(BlockPosition position, Container inventory, int slot, int count, Option<Direction> direction) {
        ItemStack stack = inventory.removeItem(slot, count);
        if (stack == null || stack.isEmpty() || stack.getCount() <= 0) return false;
        spawnStackInWorld(position, stack, direction);
        return true;
    }
    public static boolean dropSlot(BlockPosition position, Container inventory, int slot, int count, Optional<Direction> direction) {
        return dropSlot(position, inventory, slot, count, toScalaOption(direction));
    }
    public static boolean dropSlot(BlockPosition p, Container i, int slot, int count) { return dropSlot(p, i, slot, count, Option$.MODULE$.empty()); }
    public static void dropAllSlots(BlockPosition position, Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && !stack.isEmpty() && stack.getCount() > 0) {
                inventory.setItem(slot, ItemStack.EMPTY);
                spawnStackInWorld(position, stack);
            }
        }
    }
    public static void addToPlayerInventory(ItemStack stack, Player player, boolean spawnInWorld) {
        if (stack.isEmpty()) return;
        if (player.getInventory().add(stack)) {
            player.getInventory().setChanged();
            if (player.containerMenu != null) player.containerMenu.broadcastChanges();
        }
        if (stack.getCount() > 0 && spawnInWorld) player.drop(stack, false, false);
    }
    public static void addToPlayerInventory(ItemStack s, Player p) { addToPlayerInventory(s, p, true); }

    public static ItemEntity spawnStackInWorld(BlockPosition position, ItemStack stack, Option<Direction> direction, Option<Function1<ItemEntity, Object>> validator) {
        Option<Level> levelOption = position.world();
        if (levelOption.isEmpty() || stack.isEmpty() || stack.getCount() <= 0) return null;
        Level level = levelOption.get();
        Direction dir = direction.isDefined() ? direction.get() : null;
        double ox = dir == null ? 0 : dir.getStepX(), oy = dir == null ? 0 : dir.getStepY(), oz = dir == null ? 0 : dir.getStepZ();
        var random = level.getRandom();
        double tx = 0.1 * (random.nextDouble() - 0.5) + ox * 0.65;
        double ty = 0.1 * (random.nextDouble() - 0.5) + oy * 0.75 + (ox + oz) * 0.25;
        double tz = 0.1 * (random.nextDouble() - 0.5) + oz * 0.65;
        var dropPos = position.offset(0.5 + tx, 0.5 + ty, 0.5 + tz);
        ItemEntity entity = new ItemEntity(level, dropPos.x(), dropPos.y(), dropPos.z(), stack.copy());
        entity.setDeltaMovement(new Vec3(0.0125 * (random.nextDouble() - 0.5) + ox * 0.03,
                0.0125 * (random.nextDouble() - 0.5) + oy * 0.08 + (ox + oz) * 0.03,
                0.0125 * (random.nextDouble() - 0.5) + oz * 0.03));
        if (validator.isDefined() && !((Boolean) validator.get().apply(entity))) return null;
        entity.setPickUpDelay(15);
        level.addFreshEntity(entity);
        return entity;
    }
    public static ItemEntity spawnStackInWorld(BlockPosition p, ItemStack s, Option<Direction> d) { return spawnStackInWorld(p, s, d, Option$.MODULE$.empty()); }
    public static ItemEntity spawnStackInWorld(BlockPosition p, ItemStack s, Optional<Direction> d) {
        return spawnStackInWorld(p, s, toScalaOption(d));
    }
    public static ItemEntity spawnStackInWorld(BlockPosition p, ItemStack s, Optional<Direction> d, Optional<Predicate<ItemEntity>> validator) {
        Option<Function1<ItemEntity, Object>> scalaValidator = validator.isPresent()
                ? Option$.MODULE$.apply((Function1<ItemEntity, Object>) item -> validator.get().test(item))
                : Option$.MODULE$.empty();
        return spawnStackInWorld(p, s, toScalaOption(d), scalaValidator);
    }
    public static ItemEntity spawnStackInWorld(BlockPosition p, ItemStack s) { return spawnStackInWorld(p, s, Option$.MODULE$.empty()); }

    static <T> Option<T> toScalaOption(Optional<T> value) {
        return value.isPresent() ? Option$.MODULE$.apply(value.get()) : Option$.MODULE$.empty();
    }
}
