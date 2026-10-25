package li.cil.oc.server.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.FluidAccessJava;
import li.cil.oc.util.ResultWrapper;
import li.cil.oc.util.TankProperties;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Optional;

public interface LevelTankAnalytics extends LevelAware, SideRestricted {
    @Callback(doc = "function(side:number [, tank:number]):number -- Get the amount of fluid in the tank on the specified side.")
    default Object[] getTankLevel(Context context, Arguments args) {
        Optional<IFluidHandler> found = handler(checkSideForAction(args, 0));
        if (found.isEmpty()) return ResultWrapper.result(null, "no tank");
        IFluidHandler fluid = found.get();
        TankProperties properties = properties(args, fluid);
        if (properties != null) {
            FluidStack contents = properties.contents();
            return ResultWrapper.result(contents == null ? 0 : contents.getAmount());
        }
        int amount = 0;
        for (int index = 0; index < fluid.getTanks(); index++) {
            FluidStack contents = fluid.getFluidInTank(index);
            if (contents != null) amount += contents.getAmount();
        }
        return ResultWrapper.result(amount);
    }

    @Callback(doc = "function(side:number [, tank:number]):number -- Get the capacity of the tank on the specified side.")
    default Object[] getTankCapacity(Context context, Arguments args) {
        Optional<IFluidHandler> found = handler(checkSideForAction(args, 0));
        if (found.isEmpty()) return ResultWrapper.result(null, "no tank");
        IFluidHandler fluid = found.get();
        TankProperties properties = properties(args, fluid);
        if (properties != null) return ResultWrapper.result(properties.capacity());
        int capacity = 0;
        for (int index = 0; index < fluid.getTanks(); index++) capacity = Math.max(capacity, fluid.getTankCapacity(index));
        return ResultWrapper.result(capacity);
    }

    @Callback(doc = "function(side:number [, tank:number]):table -- Get a description of the fluid in the the tank on the specified side.")
    default Object[] getFluidInTank(Context context, Arguments args) {
        if (!Settings.get().allowItemStackInspection()) return ResultWrapper.result(null, "not enabled in config");
        Optional<IFluidHandler> found = handler(checkSideForAction(args, 0));
        if (found.isEmpty()) return ResultWrapper.result(null, "no tank");
        IFluidHandler fluid = found.get();
        TankProperties properties = properties(args, fluid);
        if (properties != null) return ResultWrapper.result(properties);
        TankProperties[] values = new TankProperties[fluid.getTanks()];
        for (int index = 0; index < values.length; index++) {
            values[index] = new TankProperties(fluid.getTankCapacity(index), fluid.getFluidInTank(index));
        }
        return ResultWrapper.result((Object) values);
    }

    @Callback(doc = "function(side:number):number -- Get the number of tanks available on the specified side.")
    default Object[] getTankCount(Context context, Arguments args) {
        Optional<IFluidHandler> found = handler(checkSideForAction(args, 0));
        return found.isPresent() && found.get().getTanks() > 0
            ? ResultWrapper.result(found.get().getTanks()) : ResultWrapper.result(null, "no tank");
    }

    private Optional<IFluidHandler> handler(Direction side) {
        return FluidAccessJava.fluidHandlerAt(position().offset(side), side.getOpposite());
    }

    private TankProperties properties(Arguments args, IFluidHandler handler) {
        return new ExtendedArgumentsJava(args).optTankProperties(handler, 1, null);
    }
}
