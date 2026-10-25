package li.cil.oc.integration.appeng;

import appeng.api.networking.security.IActionHost;
import appeng.blockentity.networking.ControllerBlockEntity;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DriverController extends DriverSidedBlockEntity {
    public static final DriverController INSTANCE = new DriverController();

    private DriverController() {
    }

    @Override
    public Class<?> getBlockEntityClass() {
        return ControllerBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment((ControllerBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<ControllerBlockEntity>
            implements NamedBlock, NetworkControl {
        public Environment(final ControllerBlockEntity tile) {
            super(tile, "me_controller");
        }

        @Override
        public String preferredName() {
            return "me_controller";
        }

        @Override
        public int priority() {
            return 5;
        }

        @Override
        public appeng.api.networking.IManagedGridNode gridNode() {
            return blockEntity.getMainNode();
        }

        @Override
        public IActionHost actionHost() {
            return blockEntity;
        }

        @Callback(doc = "function():number -- Get the current AE energy stored in the network.")
        public Object[] getEnergyStored(final Context context, final Arguments args) {
            return getStoredPower(context, args);
        }

        @Callback(doc = "function():number -- Get the maximum AE energy stored in the network.")
        public Object[] getMaxEnergyStored(final Context context, final Arguments args) {
            return getMaxStoredPower(context, args);
        }

        @Callback(doc = "function():boolean -- Whether the network can provide energy.")
        public Object[] canExtract(final Context context, final Arguments args) {
            return new Object[]{true};
        }

        @Callback(doc = "function():boolean -- Whether the network can receive energy.")
        public Object[] canReceive(final Context context, final Arguments args) {
            return new Object[]{true};
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(final ItemStack stack) {
            return AEUtil.isController(stack) ? Environment.class : null;
        }
    }
}
