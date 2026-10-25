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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public final class DriverMobSpawner extends DriverSidedBlockEntity {
    public static final DriverMobSpawner INSTANCE = new DriverMobSpawner();
    public static final EnvironmentProvider PROVIDER = stack ->
            !stack.isEmpty() && Block.byItem(stack.getItem()) == Blocks.SPAWNER ? Environment.class : null;

    private DriverMobSpawner() {}

    @Override
    public Class<?> getBlockEntityClass() { return SpawnerBlockEntity.class; }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((SpawnerBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<SpawnerBlockEntity> implements NamedBlock {
        public Environment(SpawnerBlockEntity blockEntity) { super(blockEntity, "mob_spawner"); }

        @Override
        public String preferredName() { return "mob_spawner"; }

        @Override
        public int priority() { return 0; }

        @Callback(doc = "function():string -- Get the name of the entity that is being spawned by this spawner.")
        public Object[] getSpawningMobName(Context context, Arguments args) {
            var tag = new CompoundTag();
            blockEntity.getSpawner().save(tag);
            return ResultWrapper.result(tag.getCompound("SpawnData").getCompound("entity").getString("id"));
        }
    }
}
