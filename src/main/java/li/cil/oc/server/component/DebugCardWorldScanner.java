package li.cil.oc.server.component;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedBlockJava;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import scala.Option;

/** World and entity scan behavior used by the debug card. */
final class DebugCardWorldScanner {
    private DebugCardWorldScanner() { }

    static Object[] scan(EnvironmentHost host, Arguments args) {
        int x = args.checkInteger(0);
        int y = args.checkInteger(1);
        int z = args.checkInteger(2);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Level world = args.count() > 3 ? worldById(server, args.checkInteger(3)) : host.getEnvironmentLevel();
        BlockPosition position = new BlockPosition(x, y, z, Option.apply(world));
        FakePlayer fakePlayer = FakePlayerFactory.get((ServerLevel) world, Settings.get().fakePlayerProfile());
        fakePlayer.setPos(position.x() + 0.5, position.y() + 0.5, position.z() + 0.5);

        Entity closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (Entity candidate : world.getEntitiesOfClass(Entity.class, position.bounds(), null)) {
            double distance = fakePlayer.distanceToSqr(candidate);
            if (closest == null || distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        if (closest instanceof LivingEntity living) return ResultWrapper.result(true, "EntityLiving", living);
        if (closest instanceof Minecart minecart) return ResultWrapper.result(true, "EntityMinecart", minecart);

        BlockPos blockPos = position.toBlockPos();
        BlockState state = world.getBlockState(blockPos);
        var block = state.getBlock();
        if (state.isAir()) return ResultWrapper.result(false, "air", block);
        if (block instanceof LiquidBlock || world.isFluidAtPosition(blockPos, fluid -> !fluid.isEmpty())) {
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, blockPos, state, fakePlayer);
            NeoForge.EVENT_BUS.post(event);
            return ResultWrapper.result(event.isCanceled(), "liquid", block);
        }
        if (new ExtendedBlockJava(block).isReplaceable(position)) {
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, blockPos, state, fakePlayer);
            NeoForge.EVENT_BUS.post(event);
            return ResultWrapper.result(event.isCanceled(), "replaceable", block);
        }
        if (state.getCollisionShape(world, blockPos, CollisionContext.empty()).isEmpty()) {
            return ResultWrapper.result(true, "passable", block);
        }
        return ResultWrapper.result(true, "solid", block);
    }

    private static Level worldById(MinecraftServer server, int worldId) {
        return switch (worldId) {
            case 0 -> server.overworld();
            case -1 -> server.getLevel(Level.NETHER);
            case 1 -> server.getLevel(Level.END);
            default -> null;
        };
    }
}
