package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

public final class DriverFurnace extends DriverSidedBlockEntity {
    public static final DriverFurnace INSTANCE = new DriverFurnace();
    public static final EnvironmentProvider PROVIDER = stack ->
            !stack.isEmpty() && Block.byItem(stack.getItem()) == Blocks.FURNACE ? EnvironmentFurnace.class : null;

    private DriverFurnace() {}

    @Override
    public Class<?> getBlockEntityClass() { return FurnaceBlockEntity.class; }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new EnvironmentFurnace((AbstractFurnaceBlockEntity) world.getBlockEntity(pos), "furnace");
    }
}
