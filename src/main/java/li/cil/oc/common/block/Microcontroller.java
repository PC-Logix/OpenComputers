package li.cil.oc.common.block;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.common.Tier;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.block.traits.Tickable;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.server.loot.LootFunctions;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.StackOption;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public class Microcontroller extends RedstoneAware implements Tickable {
    public Microcontroller(Properties properties) {
        super(properties);
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Microcontroller microcontroller) {
            return microcontroller.info().copyItemStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void tooltipTail(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.tooltipTail(stack, context, tooltip, flag);
        Tooltip.addExtended(tooltip, flag, "poweracceptor", (int) energyThroughput());
        if (Tooltip.showExtendedTooltip(flag)) {
            MicrocontrollerData info = new MicrocontrollerData(stack);
            for (ItemStack component : info.components) {
                if (!component.isEmpty()) {
                    tooltip.add(Component.literal("- " + component.getHoverName().getString()).setStyle(Tooltip.DefaultStyle));
                }
            }
        }
        Tooltip.addExtended(tooltip, flag, "poweracceptor", (int) energyThroughput());
    }

    public double energyThroughput() {
        return Settings.get().caseRate()[Tier.One];
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Microcontroller(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.api.util.StateAware stateful) {
            java.util.EnumSet<li.cil.oc.api.util.StateAware.State> currentState = stateful.getCurrentState();
            if (currentState.contains(li.cil.oc.api.util.StateAware.State.IsWorking)) return 15;
            if (currentState.contains(li.cil.oc.api.util.StateAware.State.CanWork)) return 10;
        }
        return 0;
    }

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand,
                                         ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (Wrench.holdsApplicableWrench(player, pos)) return false;

        if (!player.isCrouching()) {
            if (!world.isClientSide() && world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Microcontroller microcontroller) {
                if (microcontroller.machine().isRunning()) microcontroller.machine().stop();
                else microcontroller.machine().start();
            }
            return true;
        }

        if (Items.get(heldItem) == Items.get(Constants.ItemName$.MODULE$.EEPROM())) {
            if (!world.isClientSide() && world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Microcontroller microcontroller) {
                ItemStack newEeprom = player.getInventory().removeItem(player.getInventory().selected, 1);
                StackOption oldEeprom = microcontroller.changeEEPROM(newEeprom);
                if (oldEeprom.isDefined()) InventoryUtils.addToPlayerInventory(oldEeprom.get(), player, true);
            }
            return true;
        }
        return false;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Microcontroller microcontroller) {
            microcontroller.info().loadData(stack);
            microcontroller.snooperNode().changeBuffer(
                    microcontroller.info().storedEnergy - microcontroller.snooperNode().localBuffer());
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        LootParams.Builder updatedContext = context.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, consumer -> {
            if (context.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                    instanceof li.cil.oc.common.blockentity.Microcontroller microcontroller) {
                microcontroller.saveComponents();
                microcontroller.info().storedEnergy = (int) microcontroller.snooperNode().localBuffer();
                consumer.accept(microcontroller.info().createItemStack());
            }
        });
        return super.getDrops(state, updatedContext);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && player.isCreative()
                && world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Microcontroller microcontroller) {
            Block.dropResources(state, world, pos, microcontroller, player, player.getMainHandItem());
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return BlockEntityTypes.MICROCONTROLLER.get();
    }
}
