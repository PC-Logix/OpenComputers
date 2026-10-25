package li.cil.oc.common.block;

import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.integration.Mods;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import java.util.List;

public class DiskDrive extends SimpleBlock {
    public DiskDrive(Properties properties) {
        super(properties);
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing);
    }

    @Override
    public void tooltipTail(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.tooltipTail(stack, context, tooltip, flag);
        if (Mods.ComputerCraft().isModAvailable()) {
            Tooltip.add(tooltip, flag, getClass().getSimpleName() + ".CC");
        }
    }

    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.DiskDrive drive) {
            MenuTypes.openDiskDriveGui(player, drive);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.DiskDrive(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.DiskDrive drive
                && !drive.getItem(0).isEmpty()) {
            return 15;
        }
        return 0;
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return null;
    }

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand,
                                         ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        // Behavior: sneaking -> Insert[+Eject], not sneaking -> GUI.
        if (player.isCrouching()) {
            if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.DiskDrive drive) {
                boolean isDiskInDrive = drive.getItem(0) != null;
                boolean isHoldingDisk = drive.canPlaceItem(0, heldItem);
                if (isDiskInDrive && !world.isClientSide()) {
                    drive.dropSlot(0, 1, java.util.Optional.of(drive.facing()));
                }
                if (isHoldingDisk) {
                    drive.setItem(0, heldItem.split(1));
                }
                return isDiskInDrive || isHoldingDisk;
            }
            return false;
        }

        if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) {
            openGui(serverPlayer, world, pos);
        }
        return true;
    }
}
