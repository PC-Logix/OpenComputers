package li.cil.oc.common.item;

import li.cil.oc.common.block.SimpleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;

public final class Wrench extends SimpleItemBase implements li.cil.oc.api.internal.Wrench{
    private String unlocalizedName;

    public Wrench(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player != null && world.isLoaded(pos) && world.mayInteract(player, pos)) {
            var state = world.getBlockState(pos);
            if (state.getBlock() instanceof SimpleBlock block && block.rotateBlock(world, pos, context.getClickedFace())) {
                state.onNeighborChange(world, pos, pos);
                return InteractionResult.sidedSuccess(world.isClientSide());
            }
            var updated = state.rotate(world, pos, Rotation.CLOCKWISE_90);
            if (updated != state) {
                world.setBlock(pos, updated, Block.UPDATE_ALL);
                return InteractionResult.sidedSuccess(world.isClientSide());
            }
        }
        return super.onItemUseFirst(stack, context);
    }

    @Override
    public boolean useWrenchOnBlock(Player player, Level world, BlockPos pos, boolean simulate) {
        if (!simulate) player.swing(InteractionHand.MAIN_HAND);
        return true;
    }
}
