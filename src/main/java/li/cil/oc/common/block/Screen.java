package li.cil.oc.common.block;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.block.traits.Tickable;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class Screen extends RedstoneAware implements Tickable {
    private final int tier;

    public Screen(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw);
    }

    @Override
    public void tooltipBody(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        scala.Tuple2<Object, Object> resolution = Settings.get().screenResolutionsByTier()[tier];
        int width = ((Number) resolution._1()).intValue();
        int height = ((Number) resolution._2()).intValue();
        int depth = PackedColor.Depth.bits(Settings.get().screenDepthsByTier()[tier]);
        Tooltip.add(tooltip, flag, getClass().getSimpleName().toLowerCase(), width, height, depth);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Screen(pos, state, tier);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Screen screen) {
            screen.delayUntilCheckForMultiBlock_$eq(0);
        }
    }

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand,
                                         ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        return rightClick(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ, false);
    }

    public boolean rightClick(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem,
                              Direction side, float hitX, float hitY, float hitZ, boolean force) {
        if (Wrench.holdsApplicableWrench(player, pos) && contains(getValidRotations(world, pos), side) && !force) {
            return false;
        }
        if (li.cil.oc.api.Items.get(heldItem) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.Analyzer())) {
            return false;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof li.cil.oc.common.blockentity.Screen screen
                && screen.hasKeyboard()
                && (force || player.isCrouching() == screen.origin().invertTouchMode())) {
            // The screen GUI stays client-side; screens do not have a container menu.
            if (world.isClientSide()) showGui(screen);
            return true;
        }
        if (blockEntity instanceof li.cil.oc.common.blockentity.Screen screen
                && screen.tier() > 0 && side == screen.facing()) {
            if (world.isClientSide() && player == Minecraft.getInstance().player) {
                screen.click(hitX, hitY, hitZ);
            }
            return true;
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private void showGui(li.cil.oc.common.blockentity.Screen screen) {
        Minecraft.getInstance().pushGuiLayer(new li.cil.oc.client.gui.Screen(
                screen.origin().buffer(), screen.tier() > 0,
                () -> screen.origin().hasKeyboard(), () -> screen.origin().buffer().isRenderingEnabled()));
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide()) {
            if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Screen screen
                    && screen.tier() > 0 && screen.facing() == Direction.UP) {
                screen.walk(entity);
            } else {
                super.stepOn(world, pos, state, entity);
            }
        }
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (world.isClientSide() && entity instanceof Arrow arrow
                && world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Screen screen
                && screen.tier() > 0) {
            double hitX = Math.max(0, Math.min(1, arrow.getX() - pos.getX()));
            double hitY = Math.max(0, Math.min(1, arrow.getY() - pos.getY()));
            double hitZ = Math.max(0, Math.min(1, arrow.getZ() - pos.getZ()));
            double absX = Math.abs(hitX - 0.5);
            double absY = Math.abs(hitY - 0.5);
            double absZ = Math.abs(hitZ - 0.5);
            Direction side;
            if (absX > absY && absX > absZ) side = hitX < 0.5 ? Direction.WEST : Direction.EAST;
            else if (absY > absZ) side = hitY < 0.5 ? Direction.DOWN : Direction.UP;
            else side = hitZ < 0.5 ? Direction.NORTH : Direction.SOUTH;
            if (side == screen.facing()) screen.shot(arrow);
        }
    }

    @Override
    public Direction[] getValidRotations(Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Screen screen) {
            Direction facing = screen.facing();
            if (facing == Direction.UP || facing == Direction.DOWN) return Direction.values();
            return java.util.Arrays.stream(Direction.values())
                    .filter(direction -> direction != facing && direction != facing.getOpposite())
                    .toArray(Direction[]::new);
        }
        return super.getValidRotations(world, pos);
    }

    private static boolean contains(Direction[] directions, Direction target) {
        for (Direction direction : directions) if (direction == target) return true;
        return false;
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return BlockEntityTypes.SCREEN.get();
    }
}
