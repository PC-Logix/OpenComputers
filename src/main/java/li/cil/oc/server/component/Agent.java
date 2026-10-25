package li.cil.oc.server.component;

import li.cil.oc.Settings;
import li.cil.oc.api.event.RobotPlaceInAirEvent;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.server.agent.ActivationType;
import li.cil.oc.server.agent.PlayerSupport;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.ExtendedLevelJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

public interface Agent extends li.cil.oc.server.component.traits.LevelControl,
        li.cil.oc.server.component.traits.ContainerControl,
        li.cil.oc.server.component.traits.ContainerLevelControl,
        li.cil.oc.server.component.traits.TankAware,
        li.cil.oc.server.component.traits.TankControl,
        li.cil.oc.server.component.traits.TankLevelControl {
    li.cil.oc.api.internal.Agent agent();

    @Override
    default BlockPosition position() { return BlockPositionsJava.apply(agent()); }

    @Override
    default net.minecraft.world.entity.player.Player fakePlayer() { return agent().player(); }

    default li.cil.oc.server.agent.Player rotatedPlayer(Direction facing, Direction side) {
        li.cil.oc.server.agent.Player player = (li.cil.oc.server.agent.Player) agent().player();
        PlayerSupport.updatePositionAndRotation(player, facing, side);
        return player;
    }

    default li.cil.oc.server.agent.Player rotatedPlayer() {
        return rotatedPlayer(agent().facing(), agent().facing());
    }

    default li.cil.oc.server.agent.Player rotatedPlayer(Direction facing) {
        return rotatedPlayer(facing, agent().facing());
    }

    @Override default Container inventory() { return agent().mainInventory(); }
    @Override default int selectedSlot() { return agent().selectedSlot(); }
    @Override default void selectedSlot_$eq(int value) { agent().setSelectedSlot(value); }
    @Override default MultiTank tank() { return agent().tank(); }
    @Override default int selectedTank() { return agent().selectedTank(); }
    @Override default void selectedTank_$eq(int value) { agent().setSelectedTank(value); }

    default boolean canPlaceInAir() {
        return NeoForge.EVENT_BUS.post(new RobotPlaceInAirEvent(agent())).isAllowed();
    }

    default void onWorldInteraction(Context context, double duration) { context.pause(duration); }

    @Callback(doc = "function():string -- Get the name of the agent.")
    default Object[] name(Context context, Arguments args) { return ResultWrapper.result(agent().name()); }

    default List<Direction> actionSides(Arguments args, Direction facing) {
        if (args.isInteger(1)) return List.of(checkSideForFace(args, 1, facing));
        List<Direction> sides = new ArrayList<>();
        sides.add(facing);
        for (Direction side : Direction.values()) {
            if (side != facing && side != facing.getOpposite()) sides.add(side);
        }
        return sides;
    }

    @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false]]):boolean, string -- Perform a 'left click' towards the specified side. The `face' allows a more precise click calibration, and is relative to the targeted blockspace.")
    default Object[] swing(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        List<Direction> sides = actionSides(args, facing);
        boolean sneaky = args.isBoolean(2) && args.checkBoolean(2);
        String reason = null;

        for (Direction side : sides) {
            li.cil.oc.server.agent.Player player = rotatedPlayer(facing, side);
            player.setPose(sneaky ? Pose.CROUCHING : Pose.STANDING);
            HitResult hit = pick(player, Settings.get().swingRange());
            boolean success = false;
            String what = null;
            if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                attack(player, ((EntityHitResult) hit).getEntity(), context);
                success = true;
                what = "entity";
            }
            else if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                double breakTime = player.clickBlock(blockHit.getBlockPos(), blockHit.getDirection());
                success = breakTime > 0;
                what = "block";
                if (success) onWorldInteraction(context, breakTime);
            }
            else {
                java.util.Optional<Entity> closest = player.closestEntityJava(LivingEntity.class, player.facing());
                if (closest.isPresent()) {
                    attack(player, closest.get(), context);
                    success = true;
                    what = "entity";
                }
                else if (new ExtendedLevelJava(world()).extinguishFire(player, position(), facing)) {
                    onWorldInteraction(context, Settings.get().swingDelay());
                    success = true;
                    what = "fire";
                }
                else what = "air";
            }
            player.setPose(Pose.STANDING);
            if (success) return ResultWrapper.result(true, what);
            if (reason == null) reason = what;
        }

        li.cil.oc.server.component.traits.BlockContent block = blockContent(facing);
        if (Boolean.TRUE.equals(block.detected())) {
            BlockPosition blockPosition = position().offset(facing);
            li.cil.oc.server.agent.Player player = rotatedPlayer(facing, facing);
            player.setPose(sneaky ? Pose.CROUCHING : Pose.STANDING);
            double breakTime = player.clickBlock(blockPosition.toBlockPos(), facing);
            boolean success = breakTime > 0;
            player.setPose(Pose.STANDING);
            if (success) onWorldInteraction(context, breakTime);
            return ResultWrapper.result(success, "block");
        }
        return ResultWrapper.result(false, reason);
    }

    private void attack(li.cil.oc.server.agent.Player player, Entity entity, Context context) {
        beginConsumeDrops(entity);
        player.attack(entity);
        if (entity instanceof Minecart) {
            for (int i = 0; i < 10 && entity.isAlive(); i++) player.attack(entity);
        }
        endConsumeDrops(player, entity);
        onWorldInteraction(context, Settings.get().swingDelay());
    }

    private boolean interact(li.cil.oc.server.agent.Player player, Entity entity) {
        beginConsumeDrops(entity);
        boolean consumesAction = player.interactOn(entity, InteractionHand.MAIN_HAND).consumesAction();
        endConsumeDrops(player, entity);
        return consumesAction;
    }

    @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false[, duration:number=0]]]):boolean, string -- Perform a 'right click' towards the specified side. The `face' allows a more precise click calibration, and is relative to the targeted blockspace.")
    default Object[] use(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        List<Direction> sides = actionSides(args, facing);
        boolean sneaky = args.isBoolean(2) && args.checkBoolean(2);
        double duration = args.isDouble(3) ? args.checkDouble(3) : 0.0;

        for (Direction side : sides) {
            li.cil.oc.server.agent.Player player = rotatedPlayer(facing, side);
            player.setPose(sneaky ? Pose.CROUCHING : Pose.STANDING);
            HitResult hit = pick(player, Settings.get().useAndPlaceRange());
            boolean success = false;
            String what = null;

            if (hit.getType() == HitResult.Type.ENTITY
                    && interact(player, ((EntityHitResult) hit).getEntity())) {
                onWorldInteraction(context, Settings.get().useDelay());
                success = true;
                what = "item_interacted";
            }
            else if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                ClickParams click = clickParamsFromHit(blockHit);
                var activation = player.activateBlockOrUseItem(click.position(), blockHit.getDirection(),
                        click.hitX(), click.hitY(), click.hitZ(), duration);
                if (activation == ActivationType.BlockActivated) {
                    onWorldInteraction(context, Settings.get().useDelay());
                    success = true;
                    what = "block_activated";
                }
                else if (activation == ActivationType.ItemPlaced) {
                    onWorldInteraction(context, Settings.get().useDelay());
                    success = true;
                    what = "item_placed";
                }
                else if (activation == ActivationType.ItemUsed) {
                    onWorldInteraction(context, Settings.get().useDelay());
                    success = true;
                    what = "item_used";
                }
            }
            else if (canPlaceInAir()) {
                ClickParams click = clickParamsForPlace(facing);
                if (player.placeBlock(0, click.position(), facing, click.hitX(), click.hitY(), click.hitZ())) {
                    onWorldInteraction(context, Settings.get().useDelay());
                    success = true;
                    what = "item_placed";
                }
                else {
                    click = clickParamsForItemUse(facing, side);
                    var activation = player.activateBlockOrUseItem(click.position(), side.getOpposite(),
                            click.hitX(), click.hitY(), click.hitZ(), duration);
                    if (activation == ActivationType.BlockActivated) {
                        onWorldInteraction(context, Settings.get().useDelay());
                        success = true;
                        what = "block_activated";
                    }
                    else if (activation == ActivationType.ItemPlaced) {
                        onWorldInteraction(context, Settings.get().useDelay());
                        success = true;
                        what = "item_placed";
                    }
                    else if (activation == ActivationType.ItemUsed) {
                        onWorldInteraction(context, Settings.get().useDelay());
                        success = true;
                        what = "item_used";
                    }
                }
            }
            else if (player.useEquippedItem(duration)) {
                onWorldInteraction(context, Settings.get().useDelay());
                success = true;
                what = "item_used";
            }
            else what = "air";

            player.setPose(Pose.STANDING);
            if (success) return ResultWrapper.result(true, what);
        }
        return ResultWrapper.result(false);
    }

    @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false]]):boolean -- Place a block towards the specified side. The `face' allows a more precise click calibration, and is relative to the targeted blockspace.")
    default Object[] place(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        List<Direction> sides = actionSides(args, facing);
        boolean sneaky = args.isBoolean(2) && args.checkBoolean(2);
        if (agent().mainInventory().getItem(agent().selectedSlot()).isEmpty()) {
            return ResultWrapper.result(null, "nothing selected");
        }

        for (Direction side : sides) {
            li.cil.oc.server.agent.Player player = rotatedPlayer(facing, side);
            player.setPose(sneaky ? Pose.CROUCHING : Pose.STANDING);
            HitResult hit = pick(player, Settings.get().useAndPlaceRange());
            boolean success = false;
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                ClickParams click = clickParamsFromHit(blockHit);
                success = player.placeBlock(agent().selectedSlot(), click.position(), blockHit.getDirection(),
                        click.hitX(), click.hitY(), click.hitZ());
            }
            else if (hit == null && canPlaceInAir()
                    && player.closestEntity(Entity.class, player.facing()).isEmpty()) {
                ClickParams click = clickParamsForPlace(facing);
                BlockPos adjustedPos = click.position().relative(facing);
                BlockState state = world().getBlockState(adjustedPos);
                if (state.is(BlockTags.REPLACEABLE)) {
                    success = player.placeBlock(agent().selectedSlot(), adjustedPos, facing,
                            click.hitX(), click.hitY(), click.hitZ());
                }
            }
            player.setPose(Pose.STANDING);
            if (success) {
                onWorldInteraction(context, Settings.get().placeDelay());
                return ResultWrapper.result(true);
            }
        }
        return ResultWrapper.result(false);
    }

    default void beginConsumeDrops(Entity entity) {
        entity.captureDrops(new ArrayList<ItemEntity>());
    }

    default void endConsumeDrops(li.cil.oc.server.agent.Player player, Entity entity) {
        java.util.Collection<ItemEntity> captured = entity.captureDrops(null);
        if (player.getInventory().getContainerSize() > 0) {
            for (ItemEntity drop : captured) {
                if (drop.isAlive()) InventoryUtils.addToPlayerInventory(drop.getItem(), player, false);
            }
        }
    }

    default Direction checkSideForFace(Arguments args, int n, Direction facing) {
        ExtendedArgumentsJava extended = new ExtendedArgumentsJava(args);
        return agent().toGlobal(extended.checkSideForFace(n, agent().toLocal(facing)));
    }

    default HitResult pick(li.cil.oc.server.agent.Player player, double range) {
        Direction facing = player.facing();
        Direction side = player.side();
        Vec3 origin = new Vec3(player.getX() + facing.getStepX() * 0.5,
                player.getY() + facing.getStepY() * 0.5,
                player.getZ() + facing.getStepZ() * 0.5);
        Vec3 blockCenter = origin.add(facing.getStepX() * 0.51, facing.getStepY() * 0.51, facing.getStepZ() * 0.51);
        Vec3 target = blockCenter.add(side.getStepX() * range, side.getStepY() * range, side.getStepZ() * range);
        HitResult hit = world().clip(new ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        java.util.Optional<Entity> closest = player.closestEntityJava(Entity.class, facing);
        if (closest.isPresent()) {
            Entity entity = closest.get();
            if ((entity instanceof LivingEntity || entity instanceof Minecart || entity instanceof li.cil.oc.common.entity.Drone)
                    && (hit.getType() == HitResult.Type.MISS
                    || player.distanceToSqr(hit.getLocation()) > player.distanceToSqr(entity))) {
                return new EntityHitResult(entity);
            }
        }
        return hit;
    }

    default ClickParams clickParamsFromHit(BlockHitResult hit) {
        BlockPos position = hit.getBlockPos();
        Vec3 location = hit.getLocation();
        return new ClickParams(position,
                (float) (location.x - position.getX()),
                (float) (location.y - position.getY()),
                (float) (location.z - position.getZ()));
    }

    default ClickParams clickParamsForItemUse(Direction facing, Direction side) {
        BlockPos position = position().offset(facing).offset(side).toBlockPos();
        return new ClickParams(position,
                0.5f - side.getStepX() * 0.5f,
                0.5f - side.getStepY() * 0.5f,
                0.5f - side.getStepZ() * 0.5f);
    }

    default ClickParams clickParamsForPlace(Direction facing) {
        return new ClickParams(position().toBlockPos(),
                0.5f + facing.getStepX() * 0.5f,
                0.5f + facing.getStepY() * 0.5f,
                0.5f + facing.getStepZ() * 0.5f);
    }

    record ClickParams(BlockPos position, float hitX, float hitY, float hitZ) {}
}


