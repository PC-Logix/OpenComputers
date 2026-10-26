package li.cil.oc.server.component.traits;

import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.util.ExtendedArgumentsJava;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;

import java.util.Optional;

public interface TankAware {
    MultiTank tank();

    int selectedTank();

    void selectedTank_$eq(int value);

    default int optTank(Arguments args, int index) {
        return args.count() > 0 && args.checkAny(0) != null
            ? new ExtendedArgumentsJava(args).checkTank(tank(), 0) : selectedTank();
    }

    default Optional<IFluidTank> getTank(int index) {
        return Optional.ofNullable(tank().getFluidTank(index));
    }

    default Optional<FluidStack> fluidInTank(int index) {
        return getTank(index).map(IFluidTank::getFluid);
    }

    default boolean haveSameFluidType(FluidStack first, FluidStack second) {
        return FluidStack.isSameFluidSameComponents(first, second);
    }
}
