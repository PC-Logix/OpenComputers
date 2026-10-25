package li.cil.oc.integration.appeng;

import appeng.api.parts.IPartHost;
import appeng.parts.automation.ImportBusPart;
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

public final class DriverImportBus implements DriverBlock {
    public static final DriverImportBus INSTANCE = new DriverImportBus();

    private DriverImportBus() {
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
            if (part != null && AEUtil.isImportBus(part.getPartItem().asItem().getDefaultInstance())) {
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
            super(host, "me_importbus");
        }

        @Override
        public String preferredName() {
            return "me_importbus";
        }

        @Override
        public int priority() {
            return 1;
        }

        @Override
        public IPartHost host() {
            return blockEntity;
        }

        @Callback(doc = "function(side:number[, slot:number]):table -- Get the configuration of the import bus pointing in the specified direction.")
        public Object[] getImportConfiguration(final Context context, final Arguments args) {
            final Direction side = new ExtendedArgumentsJava(args).checkSideAny(0);
            final var part = blockEntity.getPart(side);
            if (part instanceof ImportBusPart) {
                return getConfig(((ImportBusPart) part).getConfig(), args, 1);
            }
            return new Object[]{null, "no matching part"};
        }

        @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number]):boolean -- Configure the import bus.")
        public Object[] setImportConfiguration(final Context context, final Arguments args) {
            final Direction side = new ExtendedArgumentsJava(args).checkSideAny(0);
            final var part = blockEntity.getPart(side);
            if (part instanceof ImportBusPart) {
                return setConfig(((ImportBusPart) part).getConfig(), context, args, 1, 2, 3, 4);
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
            return AEUtil.isImportBus(stack) ? Environment.class : null;
        }
    }
}
