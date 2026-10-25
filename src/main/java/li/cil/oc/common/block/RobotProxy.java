package li.cil.oc.common.block;

import li.cil.oc.Settings;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.entity.TrainRobot;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.server.RobotPacketSender;
import li.cil.oc.server.loot.LootFunctions;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.List;
import java.util.Set;

public class RobotProxy extends RedstoneAware implements li.cil.oc.common.block.traits.Tickable {
    private final VoxelShape shape = Shapes.box(0.1, 0.1, 0.1, 0.9, 0.9, 0.9);
    public final ThreadLocal<scala.Option<li.cil.oc.common.blockentity.Robot>> moving = new ThreadLocal<>() {
        @Override protected scala.Option<li.cil.oc.common.blockentity.Robot> initialValue() {
            return scala.Option$.MODULE$.empty();
        }
    };
    private static final Set<String> REAL_DROP_CALLERS = Set.of(
            "appeng.parts.automation.PartAnnihilationPlane.EatBlock");

    public RobotProxy(Properties properties) {
        super(properties);
    }

    @Override
    public String getDescriptionId() {
        return "robot";
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.RobotProxy proxy) {
            return proxy.robot().info().copyItemStack(world.registryAccess());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter world, BlockPos pos, CollisionContext context) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.RobotProxy proxy) {
            li.cil.oc.common.blockentity.Robot robot = proxy.robot();
            if (robot.isAnimatingMove()) {
                double remaining = (double) robot.animationTicksLeft() / robot.animationTicksTotal();
                BlockPos from = robot.moveFrom().get();
                BlockPos current = robot.getBlockPos();
                BlockPos delta = new BlockPos(from.getX() - current.getX(), from.getY() - current.getY(), from.getZ() - current.getZ());
                return shape.move(delta.getX() * remaining, delta.getY() * remaining, delta.getZ() * remaining);
            }
            return shape;
        }
        return super.getShape(state, world, pos, context);
    }

    @Override
    public void tooltipHead(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.tooltipHead(stack, context, tooltip, flag);
        addLines(stack, tooltip, flag);
    }

    @Override
    public void tooltipBody(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Tooltip.add(tooltip, flag, "robot");
    }

    @Override
    public void tooltipTail(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.tooltipTail(stack, context, tooltip, flag);
        if (Tooltip.showExtendedTooltip(flag)) {
            RobotData info = new RobotData(stack);
            ItemStack[] containers = info.containers;
            ItemStack[] components = info.components;
            if (containers.length + components.length > 0) {
                Tooltip.add(tooltip, flag, "server.Components");
                for (ItemStack component : containers) addComponentLine(component, tooltip);
                for (ItemStack component : components) addComponentLine(component, tooltip);
            }
        }
    }

    private void addComponentLine(ItemStack stack, List<Component> tooltip) {
        if (!stack.isEmpty()) {
            tooltip.add(Component.literal("- " + stack.getHoverName().getString()).setStyle(Tooltip.DefaultStyle));
        }
    }

    private void addLines(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            var customData = stack.get(DataComponents.CUSTOM_DATA);
            String xpKey = Settings.namespace() + "xp";
            if (customData.contains(xpKey)) {
                double xp = customData.getUnsafe().getDouble(xpKey);
                int level = Math.min((int) (Math.pow(xp - Settings.get().baseXpToLevel(),
                        1 / Settings.get().exponentialXpGrowth()) / Settings.get().constantXpGrowth()), 30);
                if (level > 0) Tooltip.add(tooltip, flag, getDescriptionId() + "_level", level);
            }
            String energyKey = Settings.namespace() + "storedEnergy";
            if (customData.contains(energyKey)) {
                int energy = customData.copyTag().getInt(energyKey);
                if (energy > 0) Tooltip.add(tooltip, flag, getDescriptionId() + "_storedenergy", energy);
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        scala.Option<li.cil.oc.common.blockentity.Robot> robot = moving.get();
        return robot.isDefined()
                ? new li.cil.oc.common.blockentity.RobotProxy(pos, state, robot.get())
                : new li.cil.oc.common.blockentity.RobotProxy(pos, state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        LootParams.Builder updatedContext = context.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, consumer -> {
            if (context.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                    instanceof li.cil.oc.common.blockentity.RobotProxy proxy) {
                li.cil.oc.common.blockentity.Robot robot = proxy.robot();
                if (robot.node() != null) {
                    if (gettingDropsForActualDrop()) {
                        robot.node().remove();
                        robot.saveComponents();
                    }
                    consumer.accept(robot.info().createItemStack());
                }
            }
        });
        return super.getDrops(state, updatedContext);
    }

    private boolean gettingDropsForActualDrop() {
        for (StackTraceElement element : new Exception().getStackTrace()) {
            if (REAL_DROP_CALLERS.contains(element.getClassName() + "." + element.getMethodName())) return true;
        }
        return false;
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
        if (!player.isCrouching() && TrainRobot.isHat(heldItem)) {
            if (!world.isClientSide() && world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.RobotProxy proxy
                    && TrainRobot.replaceRobot(world, pos, player.getYRot(), proxy) && !player.isCreative()) {
                heldItem.shrink(1);
            }
            return true;
        }
        if (!player.isCrouching()) {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer
                    && world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.RobotProxy proxy
                    && proxy.robot().node().network() != null) {
                RobotPacketSender.sendSelectedSlotChangeUnchecked(proxy.robot(), Settings.get().maxNetworkClientEffectPacketDistance() / 4.0D);
                if (proxy.stillValid(player)) MenuTypes.openRobotGui(serverPlayer, proxy.robot());
            }
            return true;
        }
        if (heldItem.isEmpty()) {
            if (!world.isClientSide() && world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.RobotProxy proxy
                    && !proxy.machine().isRunning() && proxy.stillValid(player)) {
                proxy.machine().start();
            }
            return true;
        }
        return false;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);
        if (world.isClientSide() || !(world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.RobotProxy proxy)) return;

        li.cil.oc.common.blockentity.Robot robot = proxy.robot();
        String owner;
        java.util.UUID uuid;
        if (entity instanceof li.cil.oc.server.agent.Player agentPlayer) {
            owner = agentPlayer.agent().ownerName();
            uuid = agentPlayer.agent().ownerUUID();
        } else if (entity instanceof Player player) {
            owner = player.getName().getString();
            uuid = player.getGameProfile().getId();
        } else {
            return;
        }

        robot.ownerName_$eq(owner);
        robot.ownerUUID_$eq(li.cil.oc.server.agent.PlayerSupport.determineUUID(scala.Option$.MODULE$.apply(uuid)));
        // The entity may have connected using its empty default component layout before placement data arrived.
        robot.disconnectComponents();
        robot.info().loadData(stack);
        robot.bot().node().changeBuffer(robot.info().robotEnergy - robot.bot().node().localBuffer());
        robot.updateInventorySize();
        robot.connectComponents();
        robot.machine().onHostChanged();
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player,
                                       boolean willHarvest, net.minecraft.world.level.material.FluidState fluid) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.RobotProxy proxy) {
            li.cil.oc.common.blockentity.Robot robot = proxy.robot();
            String playerName = player.getName().getString();
            if (robot.isCreative() && (!player.isCreative() || !robot.canInteract(playerName))) return false;

            if (!world.isClientSide()) {
                if (robot.player() == player) return false;
                robot.node().remove();
                robot.saveComponents();
                if (player.isCreative()) {
                    InventoryUtils.spawnStackInWorld(BlockPositionsJava.apply(pos, world), robot.info().createItemStack(),
                            java.util.Optional.empty(), java.util.Optional.empty());
                }
            }

            scala.Option<BlockPos> moveFrom = robot.moveFrom();
            if (moveFrom.isDefined()) {
                BlockPos fromPos = moveFrom.get();
                BlockState targetState = world.getBlockState(fromPos);
                if (targetState.is(OCBlocks.RobotAfterimage().get())) {
                    world.setBlock(fromPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return BlockEntityTypes.ROBOT.get();
    }
}
