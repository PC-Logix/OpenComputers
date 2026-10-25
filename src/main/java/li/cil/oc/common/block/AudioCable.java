package li.cil.oc.common.block;

import li.cil.oc.common.block.property.PropertyCableConnection;
import li.cil.oc.integration.multipart.ModCBMultipart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AudioCable extends SimpleBlock {
    public AudioCable(Properties properties) {
        super(properties);
        registerDefaultState(CableHelper.helperRegisterDefaultState(stateDefinition));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyCableConnection.DOWN, PropertyCableConnection.UP, PropertyCableConnection.NORTH,
            PropertyCableConnection.SOUTH, PropertyCableConnection.WEST, PropertyCableConnection.EAST);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.AudioCable(pos, state);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.box(.375, .375, .375, .625, .625, .625);
        for (Direction side : Direction.values()) {
            if (CableHelper.getCableShape(state, side) != PropertyCableConnection.Shape.NONE) {
                var normal = side.getNormal();
                shape = Shapes.or(shape, Shapes.box(
                    normal.getX() < 0 ? 0 : normal.getX() > 0 ? .625 : .375,
                    normal.getY() < 0 ? 0 : normal.getY() > 0 ? .625 : .375,
                    normal.getZ() < 0 ? 0 : normal.getZ() > 0 ? .625 : .375,
                    normal.getX() < 0 ? .375 : normal.getX() > 0 ? 1 : .625,
                    normal.getY() < 0 ? .375 : normal.getY() > 0 ? 1 : .625,
                    normal.getZ() < 0 ? .375 : normal.getZ() > 0 ? 1 : .625));
            }
        }
        return shape;
    }

    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction side : Direction.values())
            state = updateConnection(state, context.getLevel(), context.getClickedPos().relative(side), side);
        return state;
    }

    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (!level.isClientSide) {
            BlockState next = state;
            for (Direction side : Direction.values()) next = updateConnection(next, level, pos.relative(side), side);
            if (next != state) level.setBlock(pos, next, 0x13);
        }
        super.neighborChanged(state, level, pos, block, fromPos, moving);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState fromState, LevelAccessor level, BlockPos pos, BlockPos fromPos) {
        return updateConnection(state, level, fromPos, side);
    }

    private BlockState updateConnection(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        var entity = level.getBlockEntity(pos);
        boolean canPassMultipart = ModCBMultipart.canAudioConnectFromSide(level, pos, side.getOpposite());
        PropertyCableConnection.Shape shape = canPassMultipart && (entity instanceof li.cil.oc.common.blockentity.AudioCable || ModCBMultipart.isAudioCable(level, pos)) ? PropertyCableConnection.Shape.CABLE
            : canPassMultipart && (entity instanceof li.cil.oc.common.blockentity.Speaker || entity instanceof li.cil.oc.common.blockentity.TapeDrive) ? PropertyCableConnection.Shape.DEVICE
            : PropertyCableConnection.Shape.NONE;
        return CableHelper.helperSetCableShapeState(state, side, shape);
    }
}
