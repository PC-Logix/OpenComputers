package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;

public final class DriverSmoker extends DriverSidedBlockEntity {
    public static final DriverSmoker INSTANCE = new DriverSmoker();
    public static final EnvironmentProvider PROVIDER = stack ->
            !stack.isEmpty() && Block.byItem(stack.getItem()) == Blocks.SMOKER ? EnvironmentFurnace.class : null;

    private DriverSmoker() {}

    @Override
    public Class<?> getBlockEntityClass() { return SmokerBlockEntity.class; }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new EnvironmentFurnace((AbstractFurnaceBlockEntity) world.getBlockEntity(pos), "smoker");
    }
}
