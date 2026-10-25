package li.cil.oc.util;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Optional;

/** Java Optional entry points for optional world-fluid lookups. */
public final class FluidAccessJava {
    private FluidAccessJava() { }

    public static Optional<IFluidHandler> fluidHandlerAt(BlockPosition position, Direction side) {
        return FluidUtils.fluidHandlerAtOptional(position, side);
    }
}
