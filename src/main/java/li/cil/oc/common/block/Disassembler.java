package li.cil.oc.common.block;

import li.cil.oc.Settings;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.List;

public class Disassembler extends SimpleBlock implements Tickable {
    public Disassembler(Properties properties) {
        super(properties);
    }

    @Override
    public void tooltipBody(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        double chance = Settings.get().disassemblerBreakChance();
        if (chance > 0) {
            Tooltip.add(tooltip, flag, "disassembler.loss", Integer.toString((int) (chance * 100)));
        } else {
            Tooltip.add(tooltip, flag, "disassembler");
        }
    }

    public double energyThroughput() {
        return Settings.get().disassemblerRate();
    }

    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Disassembler disassembler) {
            MenuTypes.openDisassemblerGui(player, disassembler);
        }
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return null;
    }

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand,
                                         ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (!player.isCrouching()) {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) {
                openGui(serverPlayer, world, pos);
            }
            return true;
        }
        return super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ);
    }

    @Override
    public void tooltipTail(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.tooltipTail(stack, context, tooltip, flag);
        Tooltip.addExtended(tooltip, flag, "poweracceptor", (int) energyThroughput());
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Disassembler(pos, state);
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return BlockEntityTypes.DISASSEMBLER.get();
    }
}
