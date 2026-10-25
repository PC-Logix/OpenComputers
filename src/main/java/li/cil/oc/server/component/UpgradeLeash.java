package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Drone;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.EventHandler;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UpgradeLeash extends AbstractManagedEnvironment implements DeviceInfo {
    private static final int MAX_LEASHED_ENTITIES = 8;

    private final Entity host;
    private final Drone drone;
    private final Node node;
    private final Set<UUID> leashedEntities = new HashSet<>();

    public UpgradeLeash(Entity host) {
        this.host = host;
        this.drone = (Drone) host;
        this.node = Network.newNode(this, Visibility.Network).withComponent("leash").create();
    }

    public Entity host() { return host; }
    public Set<UUID> leashedEntities() { return leashedEntities; }
    @Override public Node node() { return node; }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceAttribute.Class, DeviceClass.Generic.toString(),
                DeviceAttribute.Description, "Leash",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "FlockControl (FC-3LS)",
                DeviceAttribute.Capacity, Integer.toString(MAX_LEASHED_ENTITIES));
    }

    public BlockPosition position() { return BlockPositionsJava.apply(host); }

    private List<Mob> entitiesInBounds(AABB bounds) {
        return drone.getEnvironmentLevel().getEntitiesOfClass(Mob.class, bounds);
    }

    @Callback(doc = "function(side:number):boolean -- Tries to put an entity on the specified side of the device onto a leash.")
    public Object[] leash(Context context, Arguments args) {
        if (leashedEntities.size() >= MAX_LEASHED_ENTITIES) {
            return ResultWrapper.result(null, "too many leashed entities");
        }
        var side = new ExtendedArgumentsJava(args).checkSideAny(0);
        AABB nearBounds = position().bounds();
        AABB farBounds = nearBounds.move(side.getStepX() * 2.0, side.getStepY() * 2.0, side.getStepZ() * 2.0);
        AABB bounds = nearBounds.minmax(farBounds);
        for (Mob entity : entitiesInBounds(bounds)) {
            if (!entity.canBeLeashed()) continue;
            if (shrinkLeash()) {
                entity.setLeashedTo(host, false);
                leashedEntities.add(entity.getUUID());
                context.pause(0.1);
                return ResultWrapper.result(true);
            }
            return ResultWrapper.result(false, "no lead in inventory");
        }
        return ResultWrapper.result(false, "no unleashed entity");
    }

    @Callback(doc = "function() -- Unleashes all currently leashed entities.")
    public Object[] unleash(Context context, Arguments args) {
        unleashAll();
        return null;
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node) unleashAll();
    }

    private void unleashAll() {
        for (Mob entity : entitiesInBounds(position().bounds().inflate(5, 5, 5))) {
            if (leashedEntities.contains(entity.getUUID()) && entity.getLeashHolder() == host && returnLeash()) {
                entity.dropLeash(true, false);
                leashedEntities.remove(entity.getUUID());
            }
        }
        leashedEntities.clear();
    }

    private boolean shrinkLeash() {
        var inventory = drone.mainInventory();
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            ItemStack stack = inventory.getItem(index);
            if (!stack.isEmpty() && stack.getItem() == Items.LEAD) {
                stack.shrink(1);
                if (stack.isEmpty()) inventory.setItem(index, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    private boolean returnLeash() {
        var inventory = drone.mainInventory();
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            ItemStack stack = inventory.getItem(index);
            if (!stack.isEmpty() && stack.getItem() == Items.LEAD && stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
                return true;
            }
        }
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            if (inventory.getItem(index).isEmpty()) {
                inventory.setItem(index, new ItemStack(Items.LEAD));
                return true;
            }
        }
        return false;
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        var loaded = holder.get(OCComponentsJava.LEASHED_ENTITIES.get());
        if (loaded != null) leashedEntities.addAll(scala.jdk.javaapi.CollectionConverters.asJava(loaded));
        EventHandler.scheduleServer(new scala.Function0<scala.runtime.BoxedUnit>() {
            @Override
            public scala.runtime.BoxedUnit apply() {
                Set<UUID> foundEntities = new HashSet<>();
                for (Mob entity : entitiesInBounds(position().bounds().inflate(5, 5, 5))) {
                    if (leashedEntities.contains(entity.getUUID())) {
                        entity.setLeashedTo(host, false);
                        foundEntities.add(entity.getUUID());
                    }
                }
                Set<UUID> missing = new HashSet<>(leashedEntities);
                missing.removeAll(foundEntities);
                if (!missing.isEmpty()) {
                    li.cil.oc.OpenComputers$.MODULE$.log().info("Could not find {} leashed entities after loading!", missing.size());
                    leashedEntities.removeAll(missing);
                }
                return scala.runtime.BoxedUnit.UNIT;
            }
        });;
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        holder.set(OCComponentsJava.LEASHED_ENTITIES.get(), scala.jdk.javaapi.CollectionConverters.asScala(leashedEntities).toList());
    }
}
