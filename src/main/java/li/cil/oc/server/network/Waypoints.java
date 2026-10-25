package li.cil.oc.server.network;

import li.cil.oc.Settings;
import li.cil.oc.common.blockentity.Waypoint;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.RTree;
import li.cil.oc.util.SableCompat;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import scala.collection.Iterable;
import scala.collection.mutable.Map;
import scala.jdk.javaapi.CollectionConverters;

import java.util.HashMap;
import java.util.List;

public final class Waypoints {
    public static final Waypoints INSTANCE = new Waypoints();
    private static final java.util.Map<ResourceKey<Level>, RTree<Waypoint>> DIMENSIONS = new HashMap<>();
    public static final Map<ResourceKey<Level>, RTree<Waypoint>> dimensions = CollectionConverters.asScala(DIMENSIONS);

    private Waypoints() {
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
            if (event.getChunk().getBlockEntity(pos) instanceof Waypoint waypoint) remove(waypoint);
        }
    }

    public static void add(Waypoint waypoint) {
        Level level = waypoint.getEnvironmentLevel();
        if (!waypoint.isRemoved() && level != null && !level.isClientSide()) {
            DIMENSIONS.computeIfAbsent(level.dimension(), ignored ->
                new RTree<>(Settings.get().rTreeMaxEntries(), value -> point(coordinate(value)))).add(waypoint);
        }
    }

    public static void remove(Waypoint waypoint) {
        Level level = waypoint.getEnvironmentLevel();
        if (level != null && !level.isClientSide()) {
            RTree<Waypoint> tree = DIMENSIONS.get(level.dimension());
            if (tree != null) tree.remove(waypoint);
        }
    }

    public static Iterable<Waypoint> findWaypoints(BlockPosition pos, double range) {
        Level level = pos.worldOptional().orElseThrow();
        RTree<Waypoint> tree = DIMENSIONS.get(level.dimension());
        if (tree == null) return CollectionConverters.asScala(List.<Waypoint>of());

        Vec3 physical = SableCompat.physicalPosition(level, pos.toVec3());
        AABB bounds = new AABB(physical.x, physical.y, physical.z,
            physical.x + 1, physical.y + 1, physical.z + 1).inflate(range * 0.5, range * 0.5, range * 0.5);
        return CollectionConverters.asScala(tree.query(
            new RTree.Point(bounds.minX, bounds.minY, bounds.minZ),
            new RTree.Point(bounds.maxX, bounds.maxY, bounds.maxZ)));
    }

    private static Vec3 coordinate(Waypoint waypoint) {
        return SableCompat.physicalPosition(waypoint.getEnvironmentLevel(),
            new Vec3(waypoint.x() + 0.5, waypoint.y() + 0.5, waypoint.z() + 0.5));
    }

    private static RTree.Point point(Vec3 position) {
        return new RTree.Point(position.x, position.y, position.z);
    }
}
