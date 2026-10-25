package li.cil.oc.server.component;

import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.common.blockentity.traits.RedstoneAware;
import li.cil.oc.common.blockentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedBlockJava;
import li.cil.oc.util.ExtendedLevelJava;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;
import scala.collection.immutable.Map$;
import scala.jdk.javaapi.CollectionConverters;

/** Vanilla redstone input and output callbacks. */
public interface RedstoneVanilla extends RedstoneSignaller, DeviceInfo {
    EnvironmentHost redstone();

    private RedstoneAware redstoneAware() { return (RedstoneAware) redstone(); }

    @Override default Map<String, String> getDeviceInfo() { return RedstoneDeviceInfo.vanilla(); }

    @Callback(direct = true, doc = "function([side:number]):number or table -- Get the redstone input (all sides, or optionally on the specified side)")
    default Object[] getInput(Context context, Arguments args) {
        Integer side = getOptionalSide(args);
        return side != null ? ResultWrapper.result(redstoneAware().getInput(Direction.from3DDataValue(side)))
            : ResultWrapper.result(valuesToMap(redstoneAware().getInput()));
    }

    @Callback(direct = true, doc = "function([side:number]):number or table -- Get the redstone output (all sides, or optionally on the specified side)")
    default Object[] getOutput(Context context, Arguments args) {
        Integer side = getOptionalSide(args);
        return side != null ? ResultWrapper.result(redstoneAware().getOutput(Direction.from3DDataValue(side)))
            : ResultWrapper.result(valuesToMap(redstoneAware().getOutput()));
    }

    @Callback(doc = "function([side:number, ]value:number or table):number or table -- Set the redstone output (all sides, or optionally on the specified side). Returns previous values")
    default Object[] setOutput(Context context, Arguments args) {
        Object[] result;
        boolean changed;
        if (args.count() == 2) {
            Direction side = checkSide(args, 0);
            int value = args.checkInteger(1);
            result = ResultWrapper.result(redstoneAware().getOutput(side));
            changed = redstoneAware().setOutput(side, value);
        } else if (args.count() == 1) {
            Map<?, ?> values = args.checkTable(0);
            result = ResultWrapper.result(valuesToMap(redstoneAware().getOutput()));
            changed = redstoneAware().setOutput(values);
        } else {
            throw new IllegalArgumentException("invalid number of arguments, expected 1 or 2");
        }
        if (changed && Settings.get().redstoneDelay() > 0) context.pause(Settings.get().redstoneDelay());
        return result;
    }

    @Callback(direct = true, doc = "function(side:number):number -- Get the comparator input on the specified side.")
    default Object[] getComparatorInput(Context context, Arguments args) {
        Direction side = checkSide(args, 0);
        var position = BlockPositionsJava.apply(redstone()).offset(side);
        var level = new ExtendedLevelJava(redstone().getEnvironmentLevel());
        if (level.blockExists(position)) {
            var block = new ExtendedBlockJava(level.getBlock(position));
            BlockPos blockPos = position.toBlockPos();
            if (redstone().getEnvironmentLevel().getBlockState(blockPos).hasAnalogOutputSignal()) {
                int value = block.getComparatorInputOverride(position, side.getOpposite());
                return ResultWrapper.result(value);
            }
        }
        return ResultWrapper.result(0);
    }

    @Override default void onMessage(Message message) {
        RedstoneSignaller.super.onMessage(message);
        if ("redstone.changed".equals(message.name())) {
            Object[] data = message.data();
            if (data.length == 1 && data[0] instanceof RedstoneChangedEventArgs args) onRedstoneChanged(args);
        }
    }

    private Integer getOptionalSide(Arguments args) {
        return args.count() == 1 ? checkSide(args, 0).ordinal() : null;
    }

    default Direction checkSide(Arguments args, int index) {
        int side = args.checkInteger(index);
        if (side < 0 || side > 5) throw new IllegalArgumentException("invalid side");
        return ((RedstoneAware) redstone()).toGlobal(Direction.from3DDataValue(side));
    }

    private scala.collection.immutable.Map<Integer, Integer> valuesToMap(int[] values) {
        var result = new LinkedHashMap<Integer, Integer>();
        for (Direction side : Direction.values()) {
            int index = side.ordinal();
            if (index < values.length) result.put(index, values[index]);
        }
        return Map$.MODULE$.from(CollectionConverters.asScala(result));
    }
}


