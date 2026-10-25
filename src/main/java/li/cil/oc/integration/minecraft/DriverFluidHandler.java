package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.util.TankProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class DriverFluidHandler implements DriverBlock {
    @Override
    public boolean worksWith(final Level level, final BlockPos pos, final Direction side) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level level, final BlockPos pos, final Direction side) {
        return new Environment(level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<IFluidHandler> {
        public Environment(final IFluidHandler tileEntity) {
            super(tileEntity, "fluid_handler");
        }

        @Callback(doc = "function():table -- Get some information about the tank accessible from the specified side.")
        public Object[] getTankInfo(final Context context, final Arguments args) {
            TankProperties[] props = new TankProperties[blockEntity.getTanks()];
            for (int i = 0; i < props.length; i++) {
                props[i] = new TankProperties(blockEntity.getTankCapacity(i), blockEntity.getFluidInTank(i));
            }
            return props;
        }
    }
}
