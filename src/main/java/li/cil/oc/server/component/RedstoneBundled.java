package li.cil.oc.server.component;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.common.blockentity.traits.BundledRedstoneAware;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;

import java.util.Map;

/** Bundled redstone callbacks layered on vanilla redstone. */
public interface RedstoneBundled extends RedstoneVanilla {
    private BundledRedstoneAware bundledRedstone() { return (BundledRedstoneAware) redstone(); }

    @Override default Map<String, String> getDeviceInfo() { return RedstoneDeviceInfo.bundled(); }

    @Callback(direct = true, doc = "function([side:number[, color:number]]):number or table -- Fewer params returns set of inputs")
    default Object[] getBundledInput(Context context, Arguments args) {
        if (args.count() == 2) {
            Direction side = checkSide(args, 0);
            int color = RedstoneBundledJava.checkColor(args, 1);
            return ResultWrapper.result(bundledRedstone().getBundledInput(side, color));
        }
        if (args.count() == 1) {
            Direction side = checkSide(args, 0);
            return ResultWrapper.result(RedstoneBundledJava.colorsToMap(bundledRedstone().getBundledInput(side)));
        }
        if (args.count() == 0) {
            return ResultWrapper.result(RedstoneBundledJava.sidesToMap(bundledRedstone().getBundledInput()));
        }
        throw new IllegalArgumentException("too many arguments, expected 0, 1, or 2");
    }

    @Callback(direct = true, doc = "function([side:number[, color:number]]):number or table -- Fewer params returns set of outputs")
    default Object[] getBundledOutput(Context context, Arguments args) {
        if (args.count() == 2) {
            Direction side = checkSide(args, 0);
            int color = RedstoneBundledJava.checkColor(args, 1);
            return ResultWrapper.result(bundledRedstone().getBundledOutput(side, color));
        }
        if (args.count() == 1) {
            Direction side = checkSide(args, 0);
            return ResultWrapper.result(RedstoneBundledJava.colorsToMap(bundledRedstone().getBundledOutput(side)));
        }
        if (args.count() == 0) {
            return ResultWrapper.result(RedstoneBundledJava.sidesToMap(bundledRedstone().getBundledOutput()));
        }
        throw new IllegalArgumentException("too many arguments, expected 0, 1, or 2");
    }

    @Callback(doc = "function([side:number[, color:number,]] value:number or table):number or table -- Fewer params to assign set of outputs. Returns previous values")
    default Object[] setBundledOutput(Context context, Arguments args) {
        Object[] result;
        boolean changed;
        if (args.count() == 3) {
            Direction side = checkSide(args, 0);
            int color = RedstoneBundledJava.checkColor(args, 1);
            int value = args.checkInteger(2);
            result = ResultWrapper.result(bundledRedstone().getBundledOutput(side, color));
            changed = bundledRedstone().setBundledOutput(side, color, value);
        } else if (args.count() == 2) {
            Direction side = checkSide(args, 0);
            Map<?, ?> values = args.checkTable(1);
            result = ResultWrapper.result(RedstoneBundledJava.colorsToMap(bundledRedstone().getBundledOutput(side)));
            changed = bundledRedstone().setBundledOutput(side, values);
        } else if (args.count() == 1) {
            Map<?, ?> values = args.checkTable(0);
            result = ResultWrapper.result(RedstoneBundledJava.sidesToMap(bundledRedstone().getBundledOutput()));
            changed = bundledRedstone().setBundledOutput(values);
        } else {
            throw new IllegalArgumentException("invalid number of arguments, expected 1, 2, or 3");
        }
        if (changed && Settings.get().redstoneDelay() > 0) context.pause(Settings.get().redstoneDelay());
        return result;
    }
}
