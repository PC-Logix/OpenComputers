package li.cil.oc.common.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc.Settings;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.block.property.PropertyRunning;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.common.block.traits.Tickable;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;

import java.util.EnumSet;
import java.util.List;

public class Case extends RedstoneAware implements Tickable {
    public static final MapCodec<Case> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            BlockBehaviour.propertiesCodec(),
            Codec.INT.fieldOf("tier").forGetter(Case::tier)
    ).apply(builder, Case::new));

    private final int tier;

    public Case(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public MapCodec<Case> codec() {
        return CODEC;
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing, PropertyRunning.Running);
    }

    @Override
    public void tooltipBody(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Tooltip.add(tooltip, flag, getClass().getSimpleName().toLowerCase(), slots());
    }

    private String slots() {
        return switch (tier) {
            case 0 -> "2/1/1";
            case 1 -> "2/2/2";
            case 2, 3 -> "3/2/3";
            default -> "0/0/0";
        };
    }

    public double energyThroughput() {
        return Settings.get().caseRate()[tier];
    }

    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Case computer
                && computer.stillValid(player)) {
            MenuTypes.openCaseGui(player, computer);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Case(pos, state, tier);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return null;
    }

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand,
                                         ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (player.isCrouching()) {
            if (!world.isClientSide() && world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Case computer
                    && !computer.machine().isRunning() && computer.stillValid(player)) {
                computer.machine().start();
            }
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) {
            openGui(serverPlayer, world, pos);
        }
        return true;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.api.util.StateAware stateful) {
            EnumSet<li.cil.oc.api.util.StateAware.State> currentState = stateful.getCurrentState();
            if (currentState.contains(li.cil.oc.api.util.StateAware.State.IsWorking)) return 15;
            if (currentState.contains(li.cil.oc.api.util.StateAware.State.CanWork)) return 10;
        }
        return 0;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player,
                                       boolean willHarvest, FluidState fluid) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Case computer) {
            String playerName = player.getName().getString();
            if (computer.isCreative() && (!player.isCreative() || !computer.canInteract(playerName))) {
                return false;
            }
            return computer.canInteract(playerName)
                    && super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
        }
        return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
    }

    @Override
    public void tooltipTail(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.tooltipTail(stack, context, tooltip, flag);
        Tooltip.addExtended(tooltip, flag, "poweracceptor", (int) energyThroughput());
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return BlockEntityTypes.CASE.get();
    }
}
