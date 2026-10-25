package li.cil.oc.server.agent;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.event.RobotAttackEntityEvent;
import li.cil.oc.api.event.RobotPlaceBlockEvent;
import li.cil.oc.api.internal.Agent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import scala.Function0;
import scala.Option;

/** Simulates player interaction events for the fake player representing an agent. */
public final class PlayerInteractionEvents {
    private PlayerInteractionEvents() {}

    public static PlayerInteractEvent.RightClickBlock fireRightClickBlock(Player player, BlockPos pos, Direction side) {
        Vec3 hit = new Vec3(0.5 + side.getStepX() * 0.5, 0.5 + side.getStepY() * 0.5, 0.5 + side.getStepZ() * 0.5);
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.OFF_HAND, pos,
                new BlockHitResult(hit, side, pos, false));
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    public static PlayerInteractEvent.LeftClickBlock fireLeftClickBlock(Player player, BlockPos pos, Direction side) {
        return CommonHooks.onLeftClickBlock(player, pos, side, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
    }

    public static PlayerInteractEvent.RightClickItem fireRightClickAir(Player player) {
        var event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.OFF_HAND);
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    public static InteractionResult interactOn(Player player, Agent agent, Entity entity, InteractionHand hand) {
        boolean cancel;
        try {
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, hand, entity));
            cancel = true;
        } catch (Throwable error) {
            if (java.util.Arrays.stream(error.getStackTrace())
                    .noneMatch(frame -> frame.getClassName().startsWith("mods.battlegear2."))) {
                OpenComputers.log().warn("Some event handler screwed up!", error);
            }
            cancel = false;
        }

        boolean interacted = !cancel && PlayerInventoryActions.callUsingItemInSlot(player, agent,
                agent.equipmentInventory(), 0, stack -> {
                    boolean result = isItemUseAllowed(player, stack)
                            && (entity.interact(player, hand).consumesAction()
                            || entity instanceof LivingEntity living && !player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
                            && player.getItemInHand(InteractionHand.MAIN_HAND).interactLivingEntity(player, living, hand).consumesAction());
                    ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                    if (!mainHand.isEmpty()) {
                        if (mainHand.getCount() <= 0) {
                            player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
                        } else {
                            // Some integrations move the in-hand result to the offhand buffer.
                            player.getInventory().offhand.set(0, mainHand);
                            player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
                        }
                    }
                    return result;
                }, true);
        return interacted ? InteractionResult.sidedSuccess(player.level().isClientSide()) : InteractionResult.PASS;
    }

    public static void attack(Player player, Agent agent, Entity entity, Runnable vanillaAttack) {
        PlayerInventoryActions.<Void>callUsingItemInSlot(player, agent, agent.equipmentInventory(), 0, stack -> {
            if (entity instanceof net.minecraft.world.entity.player.Player target && !player.canHarmPlayer(target)) {
                return null;
            }
            var event = new RobotAttackEntityEvent.Pre(agent, entity);
            NeoForge.EVENT_BUS.post(event);
            if (!event.isCanceled()) {
                vanillaAttack.run();
                NeoForge.EVENT_BUS.post(new RobotAttackEntityEvent.Post(agent, entity));
            }
            return null;
        }, true);
    }

    public static boolean trySetActiveHand(Player player, double duration) {
        player.releaseUsingItem();
        Object durationHandler = new Object() {
            @SubscribeEvent(priority = EventPriority.LOWEST)
            public void onItemUseStart(LivingEntityUseItemEvent.Start event) {
                if (event.getEntity() == player && !event.isCanceled()) event.setDuration((int) duration);
            }
        };
        NeoForge.EVENT_BUS.register(durationHandler);
        try {
            player.startUsingItem(InteractionHand.OFF_HAND);
            return player.isUsingItem();
        } catch (Exception ignored) {
            return false;
        } finally {
            NeoForge.EVENT_BUS.unregister(durationHandler);
        }
    }

    public static boolean useItemWithHand(Player player, Agent agent, double duration, ItemStack stack) {
        if (!trySetActiveHand(player, duration) && duration > 0) return false;

        ItemStack oldStack = stack.copy();
        if (!isItemUseAllowed(player, stack)) return false;

        int maxDuration = stack.getUseDuration(player);
        int heldTicks = Math.max(0, Math.min(maxDuration, (int) (duration * 20)));
        agent.machine().pause(heldTicks / 20.0);

        // Setting the active hand also sets its initial duration.
        var useItemResult = stack.use(player.level(), player, InteractionHand.OFF_HAND);
        player.releaseUsingItem();
        if (!useItemResult.getResult().consumesAction()) return false;

        ItemStack newStack = useItemResult.getObject();
        boolean stackChanged = !ItemStack.matches(oldStack, newStack) || !ItemStack.matches(oldStack, stack);
        if (stackChanged) player.getInventory().offhand.set(0, newStack);
        return stackChanged;
    }

    public static boolean isItemUseAllowed(Player player, ItemStack stack) {
        return stack.isEmpty() || (Settings.get().allowUseItemsWithDuration() || stack.getUseDuration(player) <= 0)
                && !ItemStack.isSameItem(stack, new ItemStack(Items.LEAD));
    }

    public static boolean tryPlaceBlockWhileHandlingFunnySpecialCases(Player player, Agent agent, ItemStack stack,
                                                                       BlockPos pos, Direction side,
                                                                       float hitX, float hitY, float hitZ) {
        if (stack.isEmpty() || stack.getCount() <= 0) return false;
        var event = new RobotPlaceBlockEvent.Pre(agent, stack, player.level(), pos);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return false;

        double fakeEyeHeight = player.getXRot() < 0 && isSomeKindOfPiston(stack) ? 1.82 : 0;
        player.setPos(player.getX(), player.getY() - fakeEyeHeight, player.getZ());
        PlayerSupport.setPlayerInventoryItems(player);
        var state = player.level().getBlockState(pos);
        Vec3 traceEnd = new Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ);
        BlockHitResult trace = state.isAir()
                ? BlockHitResult.miss(traceEnd, side, pos)
                : new BlockHitResult(traceEnd, side, pos, false);
        var result = stack.useOn(new UseOnContext(player.level(), player, InteractionHand.OFF_HAND, stack, trace));
        PlayerSupport.detectPlayerInventoryChanges(player);
        player.setPos(player.getX(), player.getY() + fakeEyeHeight, player.getZ());
        if (result.consumesAction()) NeoForge.EVENT_BUS.post(new RobotPlaceBlockEvent.Post(agent, stack, player.level(), pos));
        return result.consumesAction();
    }

    public static ActivationType activateBlockOrUseItem(Player player, Agent agent, BlockPos pos, Direction side,
                                                          float hitX, float hitY, float hitZ, double duration) {
        return PlayerInventoryActions.callUsingItemInSlot(player, agent, agent.equipmentInventory(), 0, stack -> {
            if (shouldCancel(() -> fireRightClickBlock(player, pos, side))) return ActivationType.None;

            Item item = stack.isEmpty() ? null : stack.getItem();
            var state = player.level().getBlockState(pos);
            Vec3 traceEnd = new Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ);
            BlockHitResult trace = state.isAir()
                    ? BlockHitResult.miss(traceEnd, side, pos)
                    : new BlockHitResult(traceEnd, side, pos, false);
            if (item != null && item.onItemUseFirst(stack,
                    new UseOnContext(player.level(), player, InteractionHand.OFF_HAND, stack, trace)).consumesAction()) {
                return ActivationType.ItemUsed;
            }

            boolean canActivate = !state.isAir() && Settings.get().allowActivateBlocks();
            boolean shouldActivate = canActivate && (!player.isCrouching()
                    || item == null || item.doesSneakBypassUse(stack, player.level(), pos, player));
            if (shouldActivate && state.useItemOn(stack, player.level(), player, InteractionHand.OFF_HAND,
                    new BlockHitResult(new Vec3(hitX, hitY, hitZ), side, pos, false)).consumesAction()) {
                return ActivationType.BlockActivated;
            }
            if (duration <= Double.MIN_VALUE && isItemUseAllowed(player, stack)
                    && tryPlaceBlockWhileHandlingFunnySpecialCases(player, agent, stack, pos, side, hitX, hitY, hitZ)) {
                return ActivationType.ItemPlaced;
            }
            return useEquippedItem(player, agent, duration, Option.apply(stack))
                    ? ActivationType.ItemUsed : ActivationType.None;
        }, true);
    }

    public static boolean placeBlock(Player player, Agent agent, int slot, BlockPos pos, Direction side,
                                     float hitX, float hitY, float hitZ) {
        return PlayerInventoryActions.callUsingItemInSlot(player, agent, agent.mainInventory(), slot,
                stack -> !shouldCancel(() -> fireRightClickBlock(player, pos, side))
                        && tryPlaceBlockWhileHandlingFunnySpecialCases(player, agent, stack, pos, side, hitX, hitY, hitZ),
                false);
    }

    public static boolean useEquippedItem(Player player, Agent agent, double duration, Option<ItemStack> stackOption) {
        if (stackOption.isEmpty()) {
            return PlayerInventoryActions.callUsingItemInSlot(player, agent, agent.equipmentInventory(), 0,
                    item -> item != null && useEquippedItem(player, agent, duration, Option.apply(item)), true);
        }

        if (shouldCancel(() -> fireRightClickAir(player))) return false;

        Direction facing = player.facing();
        player.setPos(player.getX() + facing.getStepX() / 2.0, player.getY(), player.getZ() + facing.getStepZ() / 2.0);
        try {
            return useItemWithHand(player, agent, duration, stackOption.get());
        } finally {
            player.setPos(player.getX() - facing.getStepX() / 2.0, player.getY(), player.getZ() - facing.getStepZ() / 2.0);
        }
    }

    private static boolean isSomeKindOfPiston(ItemStack stack) {
        return stack.getItem() instanceof BlockItem itemBlock
                && itemBlock.getBlock() instanceof PistonBaseBlock;
    }

    public static boolean shouldCancel(Function0<PlayerInteractEvent> createEvent) {
        try {
            PlayerInteractEvent event = createEvent.apply();
            if (event instanceof ICancellableEvent cancellable && cancellable.isCanceled()) return true;
            if (event instanceof PlayerInteractEvent.RightClickBlock rightClick) {
                return rightClick.getUseBlock() == TriState.FALSE || rightClick.getUseItem() == TriState.FALSE;
            }
            if (event instanceof PlayerInteractEvent.LeftClickBlock leftClick) {
                return leftClick.getUseBlock() == TriState.FALSE || leftClick.getUseItem() == TriState.FALSE;
            }
            return event instanceof PlayerInteractEvent.RightClickItem rightClick
                    && rightClick.getCancellationResult() == InteractionResult.FAIL;
        } catch (Throwable error) {
            if (java.util.Arrays.stream(error.getStackTrace())
                    .noneMatch(frame -> frame.getClassName().startsWith("mods.battlegear2."))) {
                OpenComputers.log().warn("Some event handler screwed up!", error);
            }
            return false;
        }
    }
}
