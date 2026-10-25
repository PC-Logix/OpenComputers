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
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

public final class DriverBeacon extends DriverSidedBlockEntity {
    public static final DriverBeacon INSTANCE = new DriverBeacon();
    public static final EnvironmentProvider PROVIDER = stack ->
            !stack.isEmpty() && Block.byItem(stack.getItem()) == Blocks.BEACON ? Environment.class : null;

    private DriverBeacon() {}

    @Override
    public Class<?> getBlockEntityClass() { return BeaconBlockEntity.class; }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((BeaconBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<BeaconBlockEntity> implements NamedBlock {
        public Environment(BeaconBlockEntity blockEntity) { super(blockEntity, "beacon"); }

        @Override
        public String preferredName() { return "beacon"; }

        @Override
        public int priority() { return 0; }

        @Callback(doc = "function():number -- Get the number of levels for this beacon.")
        public Object[] getLevels(Context context, Arguments args) {
            return ResultWrapper.result(blockEntity.levels);
        }

        @Callback(doc = "function():string -- Get the name of the active primary effect.")
        public Object[] getPrimaryEffect(Context context, Arguments args) {
            return ResultWrapper.result(effectName(blockEntity.primaryPower));
        }

        @Callback(doc = "function():string -- Get the name of the active secondary effect.")
        public Object[] getSecondaryEffect(Context context, Arguments args) {
            return ResultWrapper.result(effectName(blockEntity.secondaryPower));
        }

        private static String effectName(Holder<MobEffect> effect) {
            return effect == null ? null : effect.getKey().toString();
        }
    }
}
