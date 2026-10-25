package li.cil.oc.server.network;

import li.cil.oc.Settings;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.util.RTree;
import li.cil.oc.util.SableCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class WirelessNetwork {
    public static final WirelessNetwork INSTANCE = new WirelessNetwork();
    private static final java.util.Map<ResourceKey<Level>, RTree<WirelessEndpoint>> DIMENSIONS = new HashMap<>();
    public static final java.util.Map<ResourceKey<Level>, RTree<WirelessEndpoint>> dimensions = DIMENSIONS;

    private WirelessNetwork() {
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) DIMENSIONS.remove(level.dimension());
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) DIMENSIONS.remove(level.dimension());
    }

    // Safety cleanup for block entities that did not leave the network before their chunk unloaded.
    @SubscribeEvent
    public void onChunkUnloaded(ChunkEvent.Unload event) {
        for (var pos : event.getChunk().getBlockEntitiesPos()) {
            if (event.getChunk().getBlockEntity(pos) instanceof WirelessEndpoint endpoint) remove(endpoint);
        }
    }

    public static void add(WirelessEndpoint endpoint) {
        DIMENSIONS.computeIfAbsent(dimension(endpoint), ignored ->
            new RTree<>(Settings.get().rTreeMaxEntries(), value -> point(coordinate(value)))).add(endpoint);
    }

    public static void update(WirelessEndpoint endpoint) {
        RTree<WirelessEndpoint> tree = DIMENSIONS.get(dimension(endpoint));
        if (tree == null) return;
        Optional<RTree.Point> previous = tree.get(endpoint);
        if (previous.isPresent()) {
            RTree.Point old = previous.get();
            Vec3 current = coordinate(endpoint);
            if (Math.abs(current.x - old.x()) > 0.5 ||
                Math.abs(current.y - old.y()) > 0.5 ||
                Math.abs(current.z - old.z()) > 0.5) {
                tree.remove(endpoint);
                tree.add(endpoint);
            }
        }
    }

    public static boolean remove(WirelessEndpoint endpoint, ResourceKey<Level> dimension) {
        RTree<WirelessEndpoint> tree = DIMENSIONS.get(dimension);
        return tree != null && tree.remove(endpoint);
    }

    public static boolean remove(WirelessEndpoint endpoint) {
        return remove(endpoint, dimension(endpoint));
    }

    public static List<WirelessEndpoint> computeReachableFrom(WirelessEndpoint endpoint, double strength) {
        RTree<WirelessEndpoint> tree = DIMENSIONS.get(dimension(endpoint));
        if (tree == null || !(strength > 0)) return List.of();

        double range = strength + 1;
        List<WirelessEndpoint> reachable = new ArrayList<>();
        for (WirelessEndpoint candidate : tree.query(point(offset(endpoint, -range)), point(offset(endpoint, range)))) {
            if (Objects.equals(candidate, endpoint)) continue;
            double distanceSquared = SableCompat.distanceSquared(endpoint.getWirelessLevel(),
                coordinate(endpoint), coordinate(candidate));
            if (!(distanceSquared <= range * range)) continue;
            double distance = Math.sqrt(distanceSquared);
            if (isUnobstructed(endpoint, strength, candidate, distance)) reachable.add(candidate);
        }
        return reachable;
    }

    private static ResourceKey<Level> dimension(WirelessEndpoint endpoint) {
        return endpoint.getWirelessLevel().dimension();
    }

    private static Vec3 coordinate(WirelessEndpoint endpoint) {
        return SableCompat.physicalPosition(endpoint.getWirelessLevel(),
            new Vec3(endpoint.x() + 0.5, endpoint.y() + 0.5, endpoint.z() + 0.5));
    }

    private static Vec3 offset(WirelessEndpoint endpoint, double value) {
        Vec3 coordinate = coordinate(endpoint);
        return new Vec3(coordinate.x + value, coordinate.y + value, coordinate.z + value);
    }

    private static RTree.Point point(Vec3 position) {
        return new RTree.Point(position.x, position.y, position.z);
    }

    private static boolean isUnobstructed(WirelessEndpoint reference, double strength,
                                           WirelessEndpoint endpoint, double distance) {
        double gap = distance - 1;
        if (!(gap > 0)) return true;

        Level world = endpoint.getWirelessLevel();
        Vec3 localOrigin = new Vec3(reference.x(), reference.y(), reference.z());
        Vec3 localTarget = new Vec3(endpoint.x(), endpoint.y(), endpoint.z());
        boolean bothInPlotGrid = SableCompat.isInPlotGrid(world, localOrigin) &&
            SableCompat.isInPlotGrid(world, localTarget);
        Vec3 origin = bothInPlotGrid ? localOrigin : SableCompat.physicalPosition(world, localOrigin);
        Vec3 target = bothInPlotGrid ? localTarget : SableCompat.physicalPosition(world, localTarget);

        Vec3 direction = target.subtract(origin).normalize();
        Vec3 up;
        if (direction.x == 0 && direction.z == 0) {
            assert direction.y != 0;
            up = new Vec3(1, 0, 0);
        } else {
            up = new Vec3(0, 1, 0);
        }
        Vec3 side = direction.cross(up);
        Vec3 top = direction.cross(side);

        double hardness = 0;
        int samples = Math.max(1, (int) Math.sqrt(gap));
        for (int i = 0; i < samples; i++) {
            double rGap = world.random.nextDouble() * gap;
            int rSide = world.random.nextInt(3) - 1;
            int rTop = world.random.nextInt(3) - 1;
            int x = (int) (origin.x + direction.x * rGap + side.x * rSide + top.x * rTop);
            int y = (int) (origin.y + direction.y * rGap + side.y * rSide + top.y * rTop);
            int z = (int) (origin.z + direction.z * rGap + side.z * rSide + top.z * rTop);
            BlockPos blockPos = new BlockPos(x, y, z);
            if (world.isLoaded(blockPos)) {
                hardness += world.getBlockState(blockPos).getDestroySpeed(world, blockPos);
            }
        }

        hardness *= gap / samples;
        return strength - gap > hardness;
    }
}
