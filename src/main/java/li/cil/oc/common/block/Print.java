package li.cil.oc.common.block;

import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.server.loot.LootFunctions;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class Print extends RedstoneAware {
    public Print(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Deprecated
    @Override
    public boolean propagatesSkylightDown(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return false;
    }

    @Override
    public void tooltipBody(final ItemStack stack, final TooltipContext context,
                            final List<Component> tooltip, final TooltipFlag flag) {
        super.tooltipBody(stack, context, tooltip, flag);
        final var text = new PrintData(stack).tooltipOptional();
        if (text.isPresent()) {
            final List<Component> lines = text.get().lines()
                .map(line -> (Component) Component.literal(line).setStyle(Tooltip.DefaultStyle))
                .toList();
            tooltip.addAll(lines);
        }
    }

    @Override
    public void tooltipTail(final ItemStack stack, final TooltipContext context,
                            final List<Component> tooltip, final TooltipFlag flag) {
        super.tooltipTail(stack, context, tooltip, flag);
        final PrintData data = new PrintData(stack);
        if (data.isBeaconBase()) {
            tooltip.add(Component.literal(Localization.Tooltip.PrintBeaconBase()).setStyle(Tooltip.DefaultStyle));
        }
        if (data.emitRedstone()) {
            tooltip.add(Component.literal(Localization.Tooltip.PrintRedstoneLevel(data.redstoneLevel()))
                .setStyle(Tooltip.DefaultStyle));
        }
        if (data.emitLight()) {
            tooltip.add(Component.literal(Localization.Tooltip.PrintLightValue(data.lightLevel()))
                .setStyle(Tooltip.DefaultStyle));
        }
    }

    @Override
    public int getLightEmission(final BlockState state, final BlockGetter level, final BlockPos pos) {
        if (level instanceof Level world && world.isLoaded(pos)
            && world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Print print) {
            return print.data().lightLevel();
        }
        return super.getLightEmission(state, level, pos);
    }

    @Deprecated
    @Override
    public int getLightBlock(final BlockState state, final BlockGetter level, final BlockPos pos) {
        if (level instanceof Level world && world.isLoaded(pos)
            && world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Print print
            && Settings.get().printsHaveOpacity()) {
            return (int) (print.data().opacity() * 4);
        }
        return super.getLightBlock(state, level, pos);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                               final CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Print print) return print.shape();
        return super.getShape(state, level, pos, context);
    }

    public int tickRate(final Level level) {
        return 20;
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Print print
            && print.state()) {
            print.toggleState();
        }
    }

    @Deprecated
    public boolean isBeaconBase(final BlockGetter level, final BlockPos pos, final BlockPos beacon) {
        return level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Print print && print.data().isBeaconBase();
    }

    @Override
    public li.cil.oc.common.blockentity.Print newBlockEntity(final BlockPos pos, final BlockState state) {
        return new li.cil.oc.common.blockentity.Print(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                            final Player player, final BlockHitResult hit) {
        final var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof li.cil.oc.common.blockentity.Print print) {
            return print.activate() ? InteractionResult.sidedSuccess(level.isClientSide()) : InteractionResult.PASS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos,
                         final BlockState newState, final boolean moved) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Print print
            && print.data().emitRedstone(print.state())) {
            level.updateNeighborsAt(pos, this);
            for (final Direction side : Direction.values()) level.updateNeighborsAt(pos.relative(side), this);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
                            final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Print print) {
            print.data().loadData(stack);
            print.updateShape();
            print.updateRedstone();
            print.getLevel().getLightEngine().checkBlock(print.getBlockPos());
        }
    }

    @Override
    public List<ItemStack> getDrops(final BlockState state, final LootParams.Builder context) {
        final LootParams.Builder updatedContext = context.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, consumer -> {
            if (context.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof li.cil.oc.common.blockentity.Print print) {
                consumer.accept(print.data().createItemStack());
            }
        });
        return super.getDrops(state, updatedContext);
    }
}
