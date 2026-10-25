package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.Tier;
import li.cil.oc.common.blockentity.Waypoint;
import li.cil.oc.common.item.data.NavigationUpgradeData;
import li.cil.oc.server.network.Waypoints;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.SableCompat;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpgradeNavigation extends AbstractManagedEnvironment implements DeviceInfo {
    private final EnvironmentHost host;
    private final Rotatable rotatable;
    private final boolean absolutePosition;
    private final ComponentConnector node;
    private final NavigationUpgradeData data = new NavigationUpgradeData();

    public UpgradeNavigation(EnvironmentHost host, boolean absolutePosition) {
        this.host = host;
        this.rotatable = (Rotatable) host;
        this.absolutePosition = absolutePosition;
        this.node = Network.newNode(this, Visibility.Network)
                .withComponent("navigation", Visibility.Neighbors)
                .withConnector()
                .create();
    }

    public UpgradeNavigation(EnvironmentHost host) {
        this(host, false);
    }

    public static boolean $lessinit$greater$default$2() { return false; }

    public EnvironmentHost host() { return host; }
    public boolean absolutePosition() { return absolutePosition; }
    public NavigationUpgradeData data() { return data; }
    @Override public ComponentConnector node() { return node; }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceAttribute.Class, DeviceClass.Generic.toString(),
                DeviceAttribute.Description, absolutePosition ? "Navigation card" : "Navigation upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "PathFinder v3",
                DeviceAttribute.Capacity, absolutePosition ? "physical"
                        : Integer.toString(getSize(host.getEnvironmentLevel())));
    }

    @Callback(doc = "function():number, number, number -- Get the current relative position of the robot.")
    public Object[] getPosition(Context context, Arguments args) {
        Vec3 physical = SableCompat.physicalPosition(host);
        if (absolutePosition) return ResultWrapper.result(physical.x, physical.y, physical.z);
        var info = getMapData(host.getEnvironmentLevel());
        int size = getSize(host.getEnvironmentLevel());
        double relativeX = physical.x - info.centerX;
        double relativeZ = physical.z - info.centerZ;
        if (Math.abs(relativeX) <= size / 2.0 && Math.abs(relativeZ) <= size / 2.0) {
            return ResultWrapper.result(relativeX, physical.y, relativeZ);
        }
        return ResultWrapper.result(null, "out of range");
    }

    @Callback(doc = "function():number -- Get the current orientation of the robot.")
    public Object[] getFacing(Context context, Arguments args) {
        return ResultWrapper.result(SableCompat.physicalFacing(host.getEnvironmentLevel(),
                new Vec3(host.xPosition(), host.yPosition(), host.zPosition()), rotatable.facing()).ordinal());
    }

    @Callback(doc = "function():number -- Get the current world-space heading in degrees (0=north, 90=east, 180=south, 270=west).")
    public Object[] getHeading(Context context, Arguments args) {
        return ResultWrapper.result(SableCompat.physicalHeading(host.getEnvironmentLevel(),
                new Vec3(host.xPosition(), host.yPosition(), host.zPosition()), rotatable.facing()));
    }

    @Callback(doc = "function():number -- Get the current world-space pitch in degrees above the horizontal plane.")
    public Object[] getPitch(Context context, Arguments args) {
        return ResultWrapper.result(SableCompat.physicalPitch(host.getEnvironmentLevel(),
                new Vec3(host.xPosition(), host.yPosition(), host.zPosition()), rotatable.facing()));
    }

    @Callback(doc = "function():number -- Get the operational range of the navigation upgrade.")
    public Object[] getRange(Context context, Arguments args) {
        return ResultWrapper.result(absolutePosition ? Double.POSITIVE_INFINITY : getSize(host.getEnvironmentLevel()) / 2);
    }

    @Callback(doc = "function(range:number):table -- Find waypoints in the specified range.")
    public Object[] findWaypoints(Context context, Arguments args) {
        double range = Math.max(0, Math.min(args.checkDouble(0),
                li.cil.oc.Settings.get().maxWirelessRange()[Tier.Two]));
        if (range <= 0) return ResultWrapper.result(new Object[0]);
        if (!node.tryChangeBuffer(-range * li.cil.oc.Settings.get().wirelessCostPerRange()[Tier.Two] * 0.25)) {
            return ResultWrapper.result(null, "not enough energy");
        }
        context.pause(0.5);
        BlockPosition position = BlockPositionsJava.apply(host);
        Level level = host.getEnvironmentLevel();
        Vec3 physicalPosition = SableCompat.physicalPosition(level, position.toVec3());
        double rangeSquared = range * range;
        List<Object> result = new ArrayList<>();
        for (Waypoint waypoint : scala.jdk.javaapi.CollectionConverters.asJava(
                Waypoints.findWaypoints(position, range))) {
            Vec3 waypointPosition = SableCompat.physicalPosition(waypoint.getEnvironmentLevel(),
                    new Vec3(waypoint.x() + 0.5, waypoint.y() + 0.5, waypoint.z() + 0.5));
            if (SableCompat.distanceSquared(level, physicalPosition, waypointPosition) <= rangeSquared) {
                Vec3 waypointCenter = SableCompat.physicalPosition(waypoint.getEnvironmentLevel(),
                        waypoint.position().toVec3());
                Vec3 delta = waypointCenter.subtract(physicalPosition);
                result.add(Map.of(
                        "position", new double[]{delta.x, delta.y, delta.z},
                        "redstone", waypoint.maxInput(),
                        "label", waypoint.label(),
                        "address", waypoint.node().address()));
            }
        }
        return ResultWrapper.result(result.toArray());
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (!"tablet.use".equals(message.name())) return;
        if (!(message.source().host() instanceof Machine machine)) return;
        if (!(machine.host() instanceof li.cil.oc.api.internal.Tablet)) return;

        Object[] data = message.data();
        if (data.length != 8 || !(data[0] instanceof CompoundTag nbt)
                || !(data[1] instanceof ItemStack) || !(data[2] instanceof Player)
                || !(data[3] instanceof BlockPosition blockPosition) || !(data[4] instanceof Direction)
                || !(data[5] instanceof Float) || !(data[6] instanceof Float) || !(data[7] instanceof Float)) {
            return;
        }
        if (!absolutePosition) {
            var info = this.getMapData(host.getEnvironmentLevel());
            Level world = blockPosition.worldOptional().isPresent() ? blockPosition.worldOptional().orElseThrow() : null;
            Vec3 physical = SableCompat.physicalPosition(world, blockPosition.toVec3());
            nbt.putInt("posX", (int) Math.floor(physical.x) - info.centerX);
            nbt.putInt("posY", (int) Math.floor(physical.y));
            nbt.putInt("posZ", (int) Math.floor(physical.z) - info.centerZ);
        }
    }

    private int getSize(Level level) {
        try { return data.getSize(level); }
        catch (Exception exception) { return UpgradeNavigation.<RuntimeException, Integer>rethrow(exception); }
    }

    private net.minecraft.world.level.saveddata.maps.MapItemSavedData getMapData(Level level) {
        try { return data.mapData(level); }
        catch (Exception exception) { return UpgradeNavigation.<RuntimeException, net.minecraft.world.level.saveddata.maps.MapItemSavedData>rethrow(exception); }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable, R> R rethrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
    @Override
    public void loadData(DataComponentHolder holder) throws li.cil.oc.api.UnrecoverablePersistanceException {
        super.loadData(holder);
        data.loadData(holder);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        data.saveData(holder);
    }
}
