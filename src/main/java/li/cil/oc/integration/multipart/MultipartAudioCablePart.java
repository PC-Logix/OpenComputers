package li.cil.oc.integration.multipart;

import codechicken.multipart.api.MultipartType;
import codechicken.multipart.api.part.MultiPart;
import codechicken.multipart.minecraft.McStatePart;
import codechicken.multipart.util.PartRayTraceResult;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.block.CableHelper;
import li.cil.oc.common.block.property.PropertyCableConnection;
import li.cil.oc.common.init.OCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Audio cable part. Loaded only by the optional CB Multipart integration. */
public final class MultipartAudioCablePart extends McStatePart {
    public MultipartAudioCablePart(BlockState initialState) { super(initialState); }

    @Override public MultipartType<?> getType() { return MultipartIntegration.audioCableMultipartType(); }
    @Override public BlockState defaultBlockState() { return OCBlocks.AudioCable().get().defaultBlockState(); }
    @Override public BlockState getCurrentState() {
        updateConnections();
        return state;
    }
    @Override public ItemStack getDropStack() { return new ItemStack(OCBlocks.AudioCable().get()); }
    @Override public ItemStack getCloneStack(PartRayTraceResult hit, Player player) { return getDropStack(); }
    @Override public ItemStack getCloneStack(PartRayTraceResult hit) { return getDropStack(); }
    @Override public VoxelShape getShape(CollisionContext context) { return state.getShape(level(), pos(), context); }
    @Override public VoxelShape getCollisionShape(CollisionContext context) { return state.getCollisionShape(level(), pos(), context); }
    // AudioCable uses its actual connection-dependent geometry for multipart occlusion.
    @Override public VoxelShape getRenderOcclusionShape() { return state.getShape(level(), pos(), CollisionContext.empty()); }

    @Override public void onAdded() {
        super.onAdded();
        updateConnections();
        refreshAdjacentBlocks();
    }
    @Override public void onPartChanged(MultiPart other) {
        super.onPartChanged(other);
        updateConnections();
        refreshAdjacentBlocks();
    }
    @Override public void onNeighborBlockChanged(BlockPos changedPos) {
        super.onNeighborBlockChanged(changedPos);
        updateConnections();
    }
    @Override public void onChunkLoad(net.minecraft.world.level.chunk.LevelChunk chunk) {
        super.onChunkLoad(chunk);
        refreshAdjacentBlocks();
    }

    private void scheduleServerTask(Runnable task) {
        EventHandler.scheduleServer(new scala.runtime.AbstractFunction0<scala.runtime.BoxedUnit>() {
            @Override public scala.runtime.BoxedUnit apply() {
                task.run();
                return scala.runtime.BoxedUnit.UNIT;
            }
        });
    }

    private void refreshAdjacentBlocks() {
        if (hasLevel() && !level().isClientSide()) scheduleServerTask(() -> {
            if (hasLevel() && MultipartColorLookup.isAudioCable(level(), pos())) {
                level().updateNeighborsAt(pos(), level().getBlockState(pos()).getBlock());
            }
        });
    }

    private void updateConnections() {
        if (!hasLevel()) return;
        BlockState nextState = OCBlocks.AudioCable().get().defaultBlockState();
        for (Direction side : Direction.values()) {
            BlockPos neighborPos = pos().relative(side);
            BlockEntity neighbor = level().getBlockEntity(neighborPos);
            boolean canPassMultipart = MultipartColorLookup.canAudioConnectFromSide(level(), pos(), side) &&
                    MultipartColorLookup.canAudioConnectFromSide(level(), neighborPos, side.getOpposite());
            PropertyCableConnection.Shape shape;
            if (canPassMultipart && (neighbor instanceof li.cil.oc.common.blockentity.AudioCable || MultipartColorLookup.isAudioCable(level(), neighborPos))) {
                shape = PropertyCableConnection.Shape.CABLE;
            } else if (canPassMultipart && (neighbor instanceof li.cil.oc.common.blockentity.Speaker || neighbor instanceof li.cil.oc.common.blockentity.TapeDrive)) {
                shape = PropertyCableConnection.Shape.DEVICE;
            } else {
                shape = PropertyCableConnection.Shape.NONE;
            }
            nextState = CableHelper.helperSetCableShapeState(nextState, side, shape);
        }

        if (!state.equals(nextState)) {
            state = nextState;
            if (hasTile()) tile().notifyShapeChange();
            if (hasLevel() && !level().isClientSide()) sendUpdate(this::writeDesc);
        }
    }
}
