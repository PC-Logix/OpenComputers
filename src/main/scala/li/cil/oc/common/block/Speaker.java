package li.cil.oc.common.block;

import li.cil.oc.common.block.property.PropertyRotatable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class Speaker extends SimpleBlock {
    public Speaker(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PropertyRotatable.Facing, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(PropertyRotatable.Facing, context.getHorizontalDirection());
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Speaker(pos, state);
    }
}
