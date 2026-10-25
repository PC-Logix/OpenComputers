package li.cil.oc.integration.appeng;

import appeng.api.networking.security.IActionHost;
import appeng.blockentity.misc.InterfaceBlockEntity;
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

public final class DriverBlockInterface extends DriverSidedBlockEntity {
    public static final DriverBlockInterface INSTANCE = new DriverBlockInterface();

    private DriverBlockInterface() {
    }

    @Override
    public Class<?> getBlockEntityClass() {
        return InterfaceBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment((InterfaceBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<InterfaceBlockEntity>
            implements NamedBlock, NetworkControl, ConfigEnvironment {
        public Environment(final InterfaceBlockEntity tile) {
            super(tile, "me_interface");
        }

        @Override
        public String preferredName() {
            return "me_interface";
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

        @Callback(doc = "function([slot:number]):table -- Get the configuration of the interface.")
        public Object[] getInterfaceConfiguration(final Context context, final Arguments args) {
            return getConfig(blockEntity.getInterfaceLogic().getConfig(), args, 0);
        }

        @Callback(doc = "function([slot:number][, database:address, entry:number[, size:number]]):boolean -- Configure the interface.")
        public Object[] setInterfaceConfiguration(final Context context, final Arguments args) {
            return setConfig(blockEntity.getInterfaceLogic().getConfig(), context, args, 0, 1, 2, 3);
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(final ItemStack stack) {
            return AEUtil.isBlockInterface(stack) ? Environment.class : null;
        }
    }
}
