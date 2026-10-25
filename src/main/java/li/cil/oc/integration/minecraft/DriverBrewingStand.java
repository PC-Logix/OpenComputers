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
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

public final class DriverBrewingStand extends DriverSidedBlockEntity {
    public static final DriverBrewingStand INSTANCE = new DriverBrewingStand();
    public static final EnvironmentProvider PROVIDER = stack ->
            !stack.isEmpty() && stack.getItem() == Items.BREWING_STAND ? Environment.class : null;

    private DriverBrewingStand() {}

    @Override
    public Class<?> getBlockEntityClass() { return BrewingStandBlockEntity.class; }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((BrewingStandBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<BrewingStandBlockEntity> implements NamedBlock {
        public Environment(BrewingStandBlockEntity blockEntity) { super(blockEntity, "brewing_stand"); }

        @Override
        public String preferredName() { return "brewing_stand"; }

        @Override
        public int priority() { return 0; }

        @Callback(doc = "function():number -- Get the number of ticks remaining of the current brewing operation.")
        public Object[] getBrewTime(Context context, Arguments args) {
            return ResultWrapper.result(blockEntity.brewTime);
        }
    }
}
