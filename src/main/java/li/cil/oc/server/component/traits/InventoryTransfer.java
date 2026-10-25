package li.cil.oc.server.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.FluidUtils;
import li.cil.oc.util.InventoryAccessJava;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntSupplier;

public interface InventoryTransfer extends LevelAware, SideRestricted {
    // Empty on success, otherwise the reason the transfer was denied.
    Optional<String> onTransferContents();

    @Callback(doc = "function(sourceSide:number, sinkSide:number[, count:number[, sourceSlot:number[, sinkSlot:number]]]):boolean -- Transfer some items between two inventories.")
    default Object[] transferItem(Context context, Arguments args) {
        Direction sourceSide = checkSideForAction(args, 0);
        BlockPosition sourcePos = position().offset(sourceSide);
        Direction sinkSide = checkSideForAction(args, 1);
        BlockPosition sinkPos = position().offset(sinkSide);
        ExtendedArgumentsJava extended = new ExtendedArgumentsJava(args);
        int count = extended.optItemCount(2, 64);

        Optional<String> blocked = onTransferContents();
        if (blocked.isPresent()) return ResultWrapper.result(null, blocked.get());

        IntSupplier extractor;
        if (args.count() > 3) {
            Optional<IItemHandler> source = InventoryAccessJava.inventoryAt(sourcePos, sourceSide.getOpposite());
            if (source.isEmpty()) throw new IllegalArgumentException("no inventory");
            int sourceSlot = extended.checkSlot(source.get(), 3);
            Optional<IItemHandler> sink = InventoryAccessJava.inventoryAt(sinkPos, sinkSide.getOpposite());
            if (sink.isEmpty()) throw new IllegalArgumentException("no inventory");
            int sinkSlot = extended.optSlot(sink.get(), 4, -1);
            extractor = InventoryAccessJava.transferBetweenInventoriesSlotsAt(
                sourcePos, sourceSide.getOpposite(), sourceSlot, sinkPos, sinkSide.getOpposite(),
                sinkSlot < 0 ? OptionalInt.empty() : OptionalInt.of(sinkSlot), count);
        }
        else {
            extractor = InventoryAccessJava.transferBetweenInventoriesAt(
                sourcePos, sourceSide.getOpposite(), sinkPos, sinkSide.getOpposite(), count);
        }
        return extractor == null ? ResultWrapper.result(null, "no inventory") : ResultWrapper.result(extractor.getAsInt());
    }

    @Callback(doc = "function(sourceSide:number, sinkSide:number[, count:number [, sourceTank:number]]):boolean, number -- Transfer some fluid between two tanks. Returns operation result and filled amount")
    default Object[] transferFluid(Context context, Arguments args) {
        Direction sourceSide = checkSideForAction(args, 0);
        BlockPosition sourcePos = position().offset(sourceSide);
        Direction sinkSide = checkSideForAction(args, 1);
        BlockPosition sinkPos = position().offset(sinkSide);
        ExtendedArgumentsJava extended = new ExtendedArgumentsJava(args);
        int count = extended.optFluidCount(2, FluidType.BUCKET_VOLUME);
        int sourceTank = args.optInteger(3, -1);

        Optional<String> blocked = onTransferContents();
        if (blocked.isPresent()) return ResultWrapper.result(null, blocked.get());

        int moved = FluidUtils.transferBetweenFluidHandlersAt(
            sourcePos, sourceSide.getOpposite(), sinkPos, sinkSide.getOpposite(), count, sourceTank);
        if (moved > 0) context.pause(moved / Settings.get().transposerFluidTransferRate());
        return ResultWrapper.result(moved > 0, moved);
    }
}
