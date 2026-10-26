package li.cil.oc.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.FluidAccessJava;
import li.cil.oc.util.ResultWrapper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public interface TankLevelControl extends TankAware, LevelAware, SideRestricted {
    @Callback(doc = "function(side:number [, tank:number]):boolean -- Compare the fluid in the selected tank with the fluid in the specified tank on the specified side. Returns true if equal.")
    default Object[] compareFluid(Context context, Arguments args) {
        var side = checkSideForAction(args, 0);
        var selected = fluidInTank(selectedTank());
        if (selected.isEmpty()) return ResultWrapper.result(false);
        var handler = FluidAccessJava.fluidHandlerAt(position().offset(side), side.getOpposite());
        if (handler.isEmpty()) return ResultWrapper.result(false);
        var properties = new ExtendedArgumentsJava(args).optTankProperties(handler.get(), 1, null);
        if (properties != null) return ResultWrapper.result(selected.get().is(properties.contents().getFluid()));
        for (int index = 0; index < handler.get().getTanks(); index++) {
            if (FluidStack.isSameFluidSameComponents(selected.get(), handler.get().getFluidInTank(index))) return ResultWrapper.result(true);
        }
        return ResultWrapper.result(false);
    }

    @Callback(doc = "function(side:boolean[, amount:number=1000]):boolean, number or string -- Drains the specified amount of fluid from the specified side. Returns the amount drained, or an error message.")
    default Object[] drain(Context context, Arguments args) {
        var facing = checkSideForAction(args, 0);
        var extended = new ExtendedArgumentsJava(args);
        int count = Math.max(0, extended.optFluidCount(1, extended.optFluidCount$default$2()));
        var selected = getTank(selectedTank());
        if (selected.isEmpty()) return ResultWrapper.result(ResultWrapper.unit, "no tank selected");
        var tank = selected.get();
        int amount = Math.min(count, tank.getCapacity() - tank.getFluidAmount());
        if (count >= 1 && amount <= 0) return ResultWrapper.result(ResultWrapper.unit, "tank is full");
        var source = FluidAccessJava.fluidHandlerAt(position().offset(facing), facing.getOpposite());
        if (source.isEmpty()) return ResultWrapper.result(ResultWrapper.unit, "incompatible or no fluid");
        FluidStack stack = tank.getFluid();
        if (stack != null) {
            FluidStack drained = source.get().drain(new FluidStack(stack.getFluid(), amount), FluidAction.EXECUTE);
            if ((drained != null && drained.getAmount() > 0) || amount == 0) {
                int filled = tank.fill(drained, FluidAction.EXECUTE);
                return ResultWrapper.result(true, filled);
            }
            return ResultWrapper.result(ResultWrapper.unit, "incompatible or no fluid");
        }
        int transferred = tank.fill(source.get().drain(amount, FluidAction.EXECUTE), FluidAction.EXECUTE);
        return ResultWrapper.result(transferred > 0, transferred);
    }

    @Callback(doc = "function(side:number[, amount:number=1000]):boolean, number of string -- Eject the specified amount of fluid to the specified side. Returns the amount ejected or an error message.")
    default Object[] fill(Context context, Arguments args) {
        var facing = checkSideForAction(args, 0);
        var extended = new ExtendedArgumentsJava(args);
        int count = Math.max(0, extended.optFluidCount(1, extended.optFluidCount$default$2()));
        var selected = getTank(selectedTank());
        if (selected.isEmpty()) return ResultWrapper.result(ResultWrapper.unit, "no tank selected");
        var tank = selected.get();
        int amount = Math.min(count, tank.getFluidAmount());
        if (count >= 1 && amount <= 0) return ResultWrapper.result(ResultWrapper.unit, "tank is empty");
        var target = FluidAccessJava.fluidHandlerAt(position().offset(facing), facing.getOpposite());
        if (target.isEmpty()) return ResultWrapper.result(ResultWrapper.unit, "no space");
        FluidStack stack = tank.getFluid();
        if (stack == null) return ResultWrapper.result(ResultWrapper.unit, "tank is empty");
        int filled = target.get().fill(new FluidStack(stack.getFluid(), amount), FluidAction.EXECUTE);
        if (filled > 0 || amount == 0) {
            tank.drain(filled, FluidAction.EXECUTE);
            return ResultWrapper.result(true, filled);
        }
        return ResultWrapper.result(ResultWrapper.unit, "incompatible or no fluid");
    }
}
