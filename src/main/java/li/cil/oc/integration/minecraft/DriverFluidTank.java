package li.cil.oc.integration.minecraft;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.util.TankProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.IFluidTank;

public final class DriverFluidTank extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return IFluidTank.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level level, final BlockPos pos, final Direction side) {
        return new Environment((IFluidTank) level.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<IFluidTank> {
        public Environment(final IFluidTank tileEntity) {
            super(tileEntity, "fluid_tank");
        }

        @Callback(doc = "function():table -- Get some information about this tank.")
        public Object[] getInfo(final Context context, final Arguments args) {
            return new Object[]{new TankProperties(blockEntity.getCapacity(), blockEntity.getFluid())};
        }
    }
}