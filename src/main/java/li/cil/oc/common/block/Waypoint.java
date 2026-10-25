package li.cil.oc.common.block;

import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.block.traits.Tickable;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class Waypoint extends RedstoneAware implements Tickable {
    public Waypoint(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw);
    }

    @Override
    public li.cil.oc.common.blockentity.Waypoint newBlockEntity(final BlockPos pos, final BlockState state) {
        return new li.cil.oc.common.blockentity.Waypoint(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                            final Player player, final BlockHitResult hit) {
        if (player.isCrouching()) return super.useWithoutItem(state, level, pos, player, hit);
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Waypoint waypoint) {
            showGui(waypoint);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private void showGui(final li.cil.oc.common.blockentity.Waypoint waypoint) {
        Minecraft.getInstance().pushGuiLayer(new li.cil.oc.client.gui.Waypoint(waypoint));
    }

    @Override
    public Direction[] getValidRotations(final Level level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Waypoint waypoint) {
            return java.util.Arrays.stream(Direction.values())
                .filter(direction -> direction != waypoint.facing() && direction != waypoint.facing().getOpposite())
                .toArray(Direction[]::new);
        }
        return super.getValidRotations(level, pos);
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return BlockEntityTypes.WAYPOINT.get();
    }
}
