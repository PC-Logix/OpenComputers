package li.cil.oc.server.component.traits;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.util.BlockInventorySource;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.EntityInventorySource;
import li.cil.oc.util.InventorySource;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import java.util.List;
import java.util.Optional;

public interface LevelAware {
    BlockPosition position();

    default Level world() {
        return position().worldOptional().orElseThrow();
    }

    default Player fakePlayer() {
        Player player = FakePlayerFactory.get((ServerLevel) world(), Settings.get().fakePlayerProfile());
        player.setPos(position().x() + 0.5, position().y() + 0.5, position().z() + 0.5);
        return player;
    }

    private boolean mayInteract(BlockPosition blockPos, Direction face) {
        try {
            Player player = fakePlayer();
            BlockHitResult trace = new BlockHitResult(player.position(), face, blockPos.toBlockPos(), false);
            PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(
                player, InteractionHand.MAIN_HAND, blockPos.toBlockPos(), trace);
            NeoForge.EVENT_BUS.post(event);
            return !event.isCanceled() && event.getUseBlock() != TriState.FALSE;
        }
        catch (Throwable error) {
            OpenComputers.log().warn("Some event handler threw up while checking for permission to access a block.", error);
            return true;
        }
    }

    private boolean mayInteract(Entity entity) {
        try {
            PlayerInteractEvent.EntityInteract event = new PlayerInteractEvent.EntityInteract(
                fakePlayer(), InteractionHand.MAIN_HAND, entity);
            NeoForge.EVENT_BUS.post(event);
            return !event.isCanceled();
        }
        catch (Throwable error) {
            OpenComputers.log().warn("Some event handler threw up while checking for permission to access an entity.", error);
            return true;
        }
    }

    default boolean mayInteract(InventorySource source) {
        if (source.inventory() instanceof InvWrapper inventory && inventory.getInv() != null
                && !inventory.getInv().stillValid(fakePlayer())) return false;
        if (source instanceof BlockInventorySource block) return mayInteract(block.position(), block.side());
        if (source instanceof EntityInventorySource entity) return mayInteract(entity.entity());
        return true;
    }

    default <T extends Entity> List<T> entitiesInBounds(Class<T> type, AABB bounds) {
        return world().getEntitiesOfClass(type, bounds);
    }

    default <T extends Entity> List<T> entitiesInBlock(Class<T> type, BlockPosition blockPos) {
        return entitiesInBounds(type, blockPos.bounds());
    }

    default <T extends Entity> List<T> entitiesOnSide(Class<T> type, Direction side) {
        return entitiesInBlock(type, position().offset(side));
    }

    default <T extends Entity> Optional<T> closestEntity(Class<T> type, Direction side) {
        List<T> candidates = world().getEntitiesOfClass(type, position().offset(side).bounds(), null);
        T closest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (T candidate : candidates) {
            double current = fakePlayer().distanceToSqr(candidate);
            if (current < distance) {
                closest = candidate;
                distance = current;
            }
        }
        return Optional.ofNullable(closest);
    }

    default BlockContent blockContent(Direction side) {
        Optional<Entity> entity = closestEntity(Entity.class, side);
        if (entity.isPresent() && (entity.get() instanceof LivingEntity || entity.get() instanceof Minecart)) {
            return new BlockContent(true, "entity");
        }

        BlockPosition blockPos = position().offset(side);
        BlockState state = world().getBlockState(blockPos.toBlockPos());
        var block = state.getBlock();
        if (state.isAir()) return new BlockContent(false, "air");
        if (block instanceof LiquidBlock || world().isFluidAtPosition(blockPos.toBlockPos(), fluid -> !fluid.isEmpty())) {
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world(), blockPos.toBlockPos(), state, fakePlayer());
            NeoForge.EVENT_BUS.post(event);
            return new BlockContent(event.isCanceled(), "liquid");
        }
        if (block.defaultBlockState().is(BlockTags.REPLACEABLE)) {
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world(), blockPos.toBlockPos(), state, fakePlayer());
            NeoForge.EVENT_BUS.post(event);
            return new BlockContent(event.isCanceled(), "replaceable");
        }
        if (state.getCollisionShape(world(), blockPos.toBlockPos(), CollisionContext.empty()).isEmpty()) {
            return new BlockContent(true, "passable");
        }
        return new BlockContent(true, "solid");
    }
}
