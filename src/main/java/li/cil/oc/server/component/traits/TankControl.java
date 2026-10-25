package li.cil.oc.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.ResultWrapper;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public interface TankControl extends TankAware {
    @Callback(doc = "function():number -- The number of tanks installed in the device.")
    default Object[] tankCount(Context context, Arguments args) {
        return ResultWrapper.result(tank().tankCount());
    }

    @Callback(doc = "function([index:number]):number -- Select a tank and/or get the number of the currently selected tank.")
    default Object[] selectTank(Context context, Arguments args) {
        if (args.count() > 0 && args.checkAny(0) != null) {
            selectedTank_$eq(new ExtendedArgumentsJava(args).checkTank(tank(), 0));
        }
        return ResultWrapper.result(selectedTank() + 1);
    }

    @Callback(direct = true, doc = "function([index:number]):number -- Get the fluid amount in the specified or selected tank.")
    default Object[] tankLevel(Context context, Arguments args) {
        int index = args.count() > 0 && args.checkAny(0) != null
            ? new ExtendedArgumentsJava(args).checkTank(tank(), 0) : selectedTank();
        var fluid = fluidInTank(index);
        return ResultWrapper.result(fluid.isPresent() ? fluid.get().getAmount() : 0);
    }

    @Callback(direct = true, doc = "function([index:number]):number -- Get the remaining fluid capacity in the specified or selected tank.")
    default Object[] tankSpace(Context context, Arguments args) {
        int index = args.count() > 0 && args.checkAny(0) != null
            ? new ExtendedArgumentsJava(args).checkTank(tank(), 0) : selectedTank();
        var selected = getTank(index);
        return ResultWrapper.result(selected.isPresent() ? selected.get().getCapacity() - selected.get().getFluidAmount() : 0);
    }

    @Callback(doc = "function(index:number):boolean -- Compares the fluids in the selected and the specified tank. Returns true if equal.")
    default Object[] compareFluidTo(Context context, Arguments args) {
        int index = new ExtendedArgumentsJava(args).checkTank(tank(), 0);
        var first = fluidInTank(selectedTank());
        var second = fluidInTank(index);
        return ResultWrapper.result(first.isPresent() && second.isPresent()
            ? haveSameFluidType(first.get(), second.get()) : first.isEmpty() && second.isEmpty());
    }

    @Callback(doc = "function(index:number[, count:number=1000]):boolean -- Move the specified amount of fluid from the selected tank into the specified tank.")
    default Object[] transferFluidTo(Context context, Arguments args) {
        var extended = new ExtendedArgumentsJava(args);
        int index = extended.checkTank(tank(), 0);
        int count = extended.optFluidCount$default$2();
        count = extended.optFluidCount(1, count);
        if (index == selectedTank() || count == 0) return ResultWrapper.result(true);

        var source = getTank(selectedTank());
        var target = getTank(index);
        if (source.isEmpty() || target.isEmpty()) return ResultWrapper.result(ResultWrapper.unit, "invalid index");
        var from = source.get();
        var to = target.get();
        var drained = from.drain(count, FluidAction.SIMULATE);
        int transferred = to.fill(drained, FluidAction.EXECUTE);
        if (transferred > 0) {
            from.drain(transferred, FluidAction.EXECUTE);
            return ResultWrapper.result(true);
        }
        if (count >= from.getFluidAmount() && to.getCapacity() >= from.getFluidAmount()
            && from.getCapacity() >= to.getFluidAmount()) {
            var previous = to.drain(to.getFluidAmount(), FluidAction.EXECUTE);
            to.fill(from.drain(from.getFluidAmount(), FluidAction.EXECUTE), FluidAction.EXECUTE);
            from.fill(previous, FluidAction.EXECUTE);
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(ResultWrapper.unit, "incompatible or no fluid");
    }
}
