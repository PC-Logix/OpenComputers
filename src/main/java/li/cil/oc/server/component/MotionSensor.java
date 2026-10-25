package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.ResultWrapper;
import li.cil.oc.util.SideTracker;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MotionSensor extends AbstractManagedEnvironment implements DeviceInfo {
    private final EnvironmentHost host;
    private static final int RADIUS = 8;
    private double sensitivity = 0.4;
    private final Map<LivingEntity, Vec3> trackedEntities = new HashMap<>();
    private final String sensitivityTag = Settings.namespace() + "sensitivity";

    public MotionSensor(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).withComponent("motion_sensor").withConnector().create());
    }

    public EnvironmentHost host() { return host; }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Generic,
            DeviceAttribute.Description, "Motion sensor",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Blinker M1K0",
            DeviceAttribute.Capacity, Integer.toString(RADIUS));
    }

    @Override
    public boolean canUpdate() {
        return host.getEnvironmentLevel() != null ? !host.getEnvironmentLevel().isClientSide() : SideTracker.isServer();
    }

    @Override
    public void update() {
        super.update();
        var world = host.getEnvironmentLevel();
        if (world.getGameTime() % 10 == 0) {
            Set<LivingEntity> entities = new HashSet<>();
            for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, sensorBounds())) {
                if (entity.isAlive() && isInRange(entity) && isVisible(entity)) entities.add(entity);
            }
            trackedEntities.keySet().retainAll(entities);
            for (LivingEntity entity : entities) {
                Vec3 previous = trackedEntities.get(entity);
                if (previous == null || entity.distanceToSqr(previous) > sensitivity * sensitivity * 2) sendSignal(entity);
                trackedEntities.put(entity, entity.position());
            }
        }
    }

    private AABB sensorBounds() {
        double x = host.xPosition() + 0.5;
        double y = host.yPosition() + 0.5;
        double z = host.zPosition() + 0.5;
        return new AABB(x - RADIUS, y - RADIUS, z - RADIUS, x + RADIUS, y + RADIUS, z + RADIUS);
    }

    private boolean isInRange(LivingEntity entity) {
        return entity.distanceToSqr(host.xPosition() + 0.5, host.yPosition() + 0.5, host.zPosition() + 0.5) <= RADIUS * RADIUS;
    }

    private boolean isClearPath(Vec3 target) {
        Vec3 origin = new Vec3(host.xPosition(), host.yPosition(), host.zPosition());
        Vec3 path = target.subtract(origin).normalize();
        var trace = host.getEnvironmentLevel().clip(new ClipContext(origin.add(path), target,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, (CollisionContext) null));
        return trace.getType() == HitResult.Type.MISS;
    }

    private boolean isVisible(LivingEntity entity) {
        // Living entities are detected in darkness too; invisible entities remain hidden.
        return entity.getEffect(MobEffects.INVISIBILITY) == null &&
            (isClearPath(entity.position()) || isClearPath(entity.position().add(0, entity.getEyeHeight(), 0)));
    }

    private void sendSignal(LivingEntity entity) {
        double x = entity.getX() - (host.xPosition() + 0.5);
        double y = entity.getY() - (host.yPosition() + 0.5);
        double z = entity.getZ() - (host.zPosition() + 0.5);
        if (Settings.get().inputUsername()) {
            node().sendToReachable("computer.signal", "motion", x, y, z, entity.getName().getString());
        }
        else node().sendToReachable("computer.signal", "motion", x, y, z);
    }

    @Callback(direct = true, doc = "function():number -- Gets the current sensor sensitivity.")
    public Object[] getSensitivity(Context context, Arguments args) { return ResultWrapper.result(sensitivity); }

    @Callback(direct = true, doc = "function(value:number):number -- Sets the sensor's sensitivity. Returns the old value.")
    public Object[] setSensitivity(Context context, Arguments args) {
        double previous = sensitivity;
        sensitivity = Math.max(0.2, args.checkDouble(0));
        return ResultWrapper.result(previous);
    }

    @Override
    public void loadData(CompoundTag nbt, HolderLookup.Provider provider) throws UnrecoverablePersistanceException {
        super.loadData(nbt, provider);
        sensitivity = nbt.getDouble(sensitivityTag);
    }

    @Override
    public void saveData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveData(nbt, provider);
        nbt.putDouble(sensitivityTag, sensitivity);
    }
}
