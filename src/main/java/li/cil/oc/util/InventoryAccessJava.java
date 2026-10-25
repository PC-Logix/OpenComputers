package li.cil.oc.util;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import scala.Option;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntSupplier;
import java.util.function.BiConsumer;

import scala.runtime.BoxedUnit;
import scala.Function2;

/** Java Optional entry points for optional world-inventory lookups. */
public final class InventoryAccessJava {
    private InventoryAccessJava() { }

    public static Optional<InventorySource> inventorySourceAt(BlockPosition position, Direction side) {
        return InventoryUtils.inventorySourceAtOptional(position, side);
    }

    public static Optional<IItemHandler> inventoryAt(BlockPosition position, Direction side) {
        return InventoryUtils.inventoryAtOptional(position, side);
    }

    public static Optional<IItemHandler> anyInventoryAt(BlockPosition position) {
        return InventoryUtils.anyInventoryAtOptional(position);
    }

    public static int extractFromInventorySlot(BiConsumer<ItemStack, Boolean> consumer, IItemHandler inventory,
                                               int slot, int limit) {
        return InventoryUtils.extractFromInventorySlot(toScalaConsumer(consumer), inventory, slot, limit);
    }

    public static int extractFromInventorySlot(BiConsumer<ItemStack, Boolean> consumer, Container inventory,
                                               Direction side, int slot, int limit) {
        return InventoryUtils.extractFromInventorySlot(toScalaConsumer(consumer), inventory, side, slot, limit);
    }

    public static int extractAnyFromInventory(BiConsumer<ItemStack, Boolean> consumer, IItemHandler inventory,
                                              int limit) {
        return InventoryUtils.extractAnyFromInventory(toScalaConsumer(consumer), inventory, limit);
    }

    private static Function2<ItemStack, Object, BoxedUnit> toScalaConsumer(BiConsumer<ItemStack, Boolean> consumer) {
        return (stack, simulated) -> {
            consumer.accept(stack, (Boolean) simulated);
            return BoxedUnit.UNIT;
        };
    }

    public static IntSupplier transferBetweenInventoriesAt(BlockPosition source, Direction sourceSide,
                                                             BlockPosition sink, Direction sinkSide, int limit) {
        return toIntSupplier(InventoryUtils.getTransferBetweenInventoriesAt(source, sourceSide, sink,
                Option.apply(sinkSide), limit));
    }

    public static IntSupplier transferBetweenInventoriesSlotsAt(BlockPosition source, Direction sourceSide,
                                                                  int sourceSlot, BlockPosition sink,
                                                                  Direction sinkSide, OptionalInt sinkSlot,
                                                                  int limit) {
        Option<Integer> optionalSinkSlot = sinkSlot.isPresent()
                ? Option.apply(sinkSlot.getAsInt()) : Option.empty();
        return toIntSupplier(InventoryUtils.getTransferBetweenInventoriesSlotsAt(source, sourceSide, sourceSlot,
                sink, Option.apply(sinkSide), optionalSinkSlot, limit));
    }

    static IntSupplier toIntSupplier(scala.Function0<Object> callback) {
        return callback == null ? null : () -> ((Number) callback.apply()).intValue();
    }

    static <T> Optional<T> toOptional(Option<T> value) {
        return value.isDefined() ? Optional.ofNullable(value.get()) : Optional.empty();
    }

}
