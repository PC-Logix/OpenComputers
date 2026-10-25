package li.cil.oc.integration.appeng;

import appeng.api.parts.IPartHost;
import appeng.parts.misc.InterfacePart;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.util.ExtendedArgumentsJava;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DriverPartInterface implements DriverBlock {
    public static final DriverPartInterface INSTANCE = new DriverPartInterface();

    private DriverPartInterface() {
    }

    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        final var blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof IPartHost)) {
            return false;
        }
        final IPartHost host = (IPartHost) blockEntity;
        for (final Direction direction : Direction.values()) {
            final var part = host.getPart(direction);
            if (part != null && AEUtil.isPartInterface(part.getPartItem().asItem().getDefaultInstance())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment((IPartHost) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<IPartHost>
            implements NamedBlock, PartEnvironmentBase {
        public Environment(final IPartHost host) {
            super(host, "me_interface");
        }

        @Override
        public String preferredName() {
            return "me_interface";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public IPartHost host() {
            return blockEntity;
        }

        @Callback(doc = "function(side:number[, slot:number]):table -- Get the configuration of the interface pointing in the specified direction.")
        public Object[] getInterfaceConfiguration(final Context context, final Arguments args) {
            final Direction side = new ExtendedArgumentsJava(args).checkSideAny(0);
            final var part = blockEntity.getPart(side);
            if (part instanceof InterfacePart) {
                return getConfig(((InterfacePart) part).getInterfaceLogic().getConfig(), args, 1);
            }
            return new Object[]{null, "no matching part"};
        }

        @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number[, size:number]]):boolean -- Configure the interface pointing in the specified direction.")
        public Object[] setInterfaceConfiguration(final Context context, final Arguments args) {
            final Direction side = new ExtendedArgumentsJava(args).checkSideAny(0);
            final var part = blockEntity.getPart(side);
            if (part instanceof InterfacePart) {
                return setConfig(((InterfacePart) part).getInterfaceLogic().getConfig(), context, args, 1, 2, 3, 4);
            }
            return new Object[]{null, "no matching part"};
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(final ItemStack stack) {
            return AEUtil.isPartInterface(stack) ? Environment.class : null;
        }
    }
}
