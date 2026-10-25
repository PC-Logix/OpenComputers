package li.cil.oc.common.block;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.item.data.RaidData;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.server.loot.LootFunctions;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.material.FluidState;

import java.util.List;

public class Raid extends SimpleBlock {
    public Raid(Properties properties) {
        super(properties);
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing);
    }

    @Override
    public void tooltipTail(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.tooltipTail(stack, context, tooltip, flag);
        if (Tooltip.showExtendedTooltip(flag)) {
            RaidData data = new RaidData(stack);
            for (ItemStack disk : data.disks) {
                if (!disk.isEmpty()) {
                    tooltip.add(Component.literal("- " + disk.getHoverName().getString()).setStyle(Tooltip.DefaultStyle));
                }
            }
        }
    }

    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Raid raid) {
            MenuTypes.openRaidGui(player, raid);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Raid(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Raid raid) {
            for (boolean present : raid.presence()) if (!present) return 0;
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
                                         ItemStack heldItem, net.minecraft.core.Direction side,
                                         float hitX, float hitY, float hitZ) {
        if (!player.isCrouching()) {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) {
                openGui(serverPlayer, world, pos);
            }
            return true;
        }
        return super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (world.isClientSide() || !(world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Raid raid)) return;

        RaidData data = new RaidData(stack);
        ItemStack[] disks = data.disks;
        for (int i = 0; i < Math.min(disks.length, raid.getContainerSize()); i++) {
            raid.setItem(i, disks[i]);
        }
        if (data.label.isDefined()) raid.label().setLabel(data.label.get());

        Object address = stack.get(OCComponentsJava.ADDRESS.get());
        if (address instanceof String raidAddress) {
            raid.tryCreateRaid(raidAddress);
            if (raid.filesystem().isDefined()) {
                try {
                    raid.filesystem().get().loadData(stack);
                } catch (li.cil.oc.api.UnrecoverablePersistanceException error) {
                    Raid.<RuntimeException>sneakyThrow(error);
                }
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        LootParams.Builder updatedContext = context.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, consumer -> {
            if (context.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                    instanceof li.cil.oc.common.blockentity.Raid raid) {
                ItemStack stack = createItemStack(1);
                boolean hasDisk = false;
                for (ItemStack disk : raid.items()) if (!disk.isEmpty()) { hasDisk = true; break; }
                if (hasDisk) {
                    RaidData data = new RaidData();
                    data.disks = raid.items().clone();
                    HolderLookup.Provider registries = raid.getLevel().registryAccess();
                    if (raid.filesystem().isDefined()) raid.filesystem().get().saveData(stack);
                    data.label = scala.Option$.MODULE$.apply(raid.label().getLabel(registries));
                    data.saveData(stack);
                }
                consumer.accept(stack);
            }
        });
        return super.getDrops(state, updatedContext);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && player.isCreative()
                && world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Raid raid) {
            for (ItemStack disk : raid.items()) {
                if (!disk.isEmpty()) {
                    Block.dropResources(state, world, pos, raid, player, player.getMainHandItem());
                    break;
                }
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable throwable) throws E {
        throw (E) throwable;
    }
}
