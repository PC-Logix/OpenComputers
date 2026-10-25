package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;

public final class DriverComparator extends DriverSidedBlockEntity {
    public static final DriverComparator INSTANCE = new DriverComparator();
    public static final EnvironmentProvider PROVIDER = stack ->
            !stack.isEmpty() && stack.getItem() == Items.COMPARATOR ? Environment.class : null;

    private DriverComparator() {}

    @Override
    public Class<?> getBlockEntityClass() { return ComparatorBlockEntity.class; }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((ComparatorBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<ComparatorBlockEntity> implements NamedBlock {
        public Environment(ComparatorBlockEntity blockEntity) { super(blockEntity, "comparator"); }

        @Override
        public String preferredName() { return "comparator"; }

        @Override
        public int priority() { return 0; }

        @Callback(doc = "function():number -- Get the strength of the comparators output signal.")
        public Object[] getOutputSignal(Context context, Arguments args) {
            return ResultWrapper.result(blockEntity.getOutputSignal());
        }
    }
}
