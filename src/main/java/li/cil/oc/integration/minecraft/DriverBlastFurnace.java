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
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;

public final class DriverBlastFurnace extends DriverSidedBlockEntity {
    public static final DriverBlastFurnace INSTANCE = new DriverBlastFurnace();
    public static final EnvironmentProvider PROVIDER = stack ->
            !stack.isEmpty() && Block.byItem(stack.getItem()) == Blocks.BLAST_FURNACE ? EnvironmentFurnace.class : null;

    private DriverBlastFurnace() {}

    @Override
    public Class<?> getBlockEntityClass() { return BlastFurnaceBlockEntity.class; }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new EnvironmentFurnace((AbstractFurnaceBlockEntity) world.getBlockEntity(pos), "blast_furnace");
    }
}
