package li.cil.oc.common.block;

import li.cil.oc.integration.util.Wrench;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class NetSplitter extends RedstoneAware {
    public NetSplitter(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public li.cil.oc.common.blockentity.NetSplitter newBlockEntity(final BlockPos pos, final BlockState state) {
        return new li.cil.oc.common.blockentity.NetSplitter(pos, state);
    }

    // Keep this overridable for Immibis Microblocks compatibility.
    @Override
    public InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                            final Player player, final BlockHitResult hit) {
        if (!Wrench.holdsApplicableWrench(player, pos)) return super.useWithoutItem(state, level, pos, player, hit);

        final var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof li.cil.oc.common.blockentity.NetSplitter splitter) {
            final var side = hit.getDirection();
            final var sideToToggle = player.isCrouching() ? side.getOpposite() : side;
            if (!level.isClientSide()) {
                final boolean oldValue = splitter.openSides()[sideToToggle.ordinal()];
                splitter.setSideOpen(sideToToggle, !oldValue);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }
}
