package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Drone extends AbstractManagedEnvironment implements Agent, DeviceInfo {
    private final li.cil.oc.common.entity.Drone agent;

    public Drone(li.cil.oc.common.entity.Drone agent) {
        this.agent = agent;
    }

    @Override
    public li.cil.oc.api.network.ComponentConnector node() {
        return li.cil.oc.api.Network.newNode(this, Visibility.Network)
                .withComponent("drone")
                .withConnector(Settings.get().bufferDrone())
                .create();
    }

    @Override
    public li.cil.oc.api.internal.Agent agent() {
        return agent;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceAttribute.Class, DeviceClass.System.toString(),
                DeviceAttribute.Description, "Drone",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "Overwatcher",
                DeviceAttribute.Capacity, Integer.toString(agent.inventorySize()));
    }

    @Override
    public Direction checkSideForAction(Arguments args, int n) {
        return new ExtendedArgumentsJava(args).checkSideAny(n);
    }

    @Override
    public List<ItemEntity> suckableItems(Direction side) {
        List<ItemEntity> items = new ArrayList<>(Agent.super.entitiesInBlock(ItemEntity.class, position()));
        items.addAll(Agent.super.suckableItems(side));
        return items;
    }

    @Override
    public void onSuckCollect(ItemEntity entity) {
        IItemHandler inventoryHandler = InventoryUtils.asItemHandler(inventory());
        if (InventoryUtils.insertIntoInventory(entity.getItem(), inventoryHandler, 64, false,
                java.util.Optional.of(Agent.super.insertionSlots()))) {
            var level = agent.level();
            float pitch = ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7f + 1.0f) * 2.0f;
            level.playSound(agent.player(), agent.getX(), agent.getY(), agent.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.2f, pitch);
        }
    }

    @Override
    public void onWorldInteraction(Context context, double duration) {
        Agent.super.onWorldInteraction(context, duration * 2.0);
    }

    @Callback(doc = "function():string -- Get the status text currently being displayed in the GUI.")
    public Object[] getStatusText(Context context, Arguments args) {
        return ResultWrapper.result(agent.statusText().getString());
    }

    @Callback(doc = "function(value:string):string -- Set the status text to display in the GUI, returns new value.")
    public Object[] setStatusText(Context context, Arguments args) {
        agent.statusText_$eq(Component.literal(args.checkString(0)));
        context.pause(0.1);
        return ResultWrapper.result(agent.statusText().getString());
    }

    @Callback(doc = "function():number -- Get the current color of the flap lights as an integer encoded RGB value (0xRRGGBB).")
    public Object[] getLightColor(Context context, Arguments args) {
        return ResultWrapper.result(agent.lightColor());
    }

    @Callback(doc = "function(value:number):number -- Set the color of the flap lights to the specified integer encoded RGB value (0xRRGGBB).")
    public Object[] setLightColor(Context context, Arguments args) {
        agent.lightColor_$eq(args.checkInteger(0));
        context.pause(0.1);
        return ResultWrapper.result(agent.lightColor());
    }

    @Callback(doc = "function(dx:number, dy:number, dz:number) -- Change the target position by the specified offset.")
    public Object[] move(Context context, Arguments args) {
        float dx = (float) args.checkDouble(0);
        float dy = (float) args.checkDouble(1);
        float dz = (float) args.checkDouble(2);
        agent.targetX_$eq(agent.targetX() + dx);
        agent.targetY_$eq(agent.targetY() + dy);
        agent.targetZ_$eq(agent.targetZ() + dz);
        return null;
    }

    @Callback(doc = "function():number -- Get the current distance to the target position.")
    public Object[] getOffset(Context context, Arguments args) {
        var target = agent.getTarget();
        return ResultWrapper.result(agent.position().distanceTo(new Vec3(target.x, target.y, target.z)));
    }

    @Callback(doc = "function():number -- Get the current velocity in m/s.")
    public Object[] getVelocity(Context context, Arguments args) {
        return ResultWrapper.result(agent.getDeltaMovement().length() * 20.0);
    }

    @Callback(doc = "function():number -- Get the maximum velocity, in m/s.")
    public Object[] getMaxVelocity(Context context, Arguments args) {
        return ResultWrapper.result(agent.maxVelocity() * 20.0);
    }

    // Compatibility alias for builds that accidentally exposed the typo'd callback name.
    @Callback(doc = "function():number -- Alias for getMaxVelocity().")
    public Object[] getV1elocity(Context context, Arguments args) {
        return getMaxVelocity(context, args);
    }

    @Callback(doc = "function():number -- Get the currently set acceleration.")
    public Object[] getAcceleration(Context context, Arguments args) {
        return ResultWrapper.result(agent.targetAcceleration() * 20.0);
    }

    @Callback(doc = "function(value:number):number -- Try to set the acceleration to the specified value and return the new acceleration.")
    public Object[] setAcceleration(Context context, Arguments args) {
        agent.targetAcceleration_$eq((float) (args.checkDouble(0) / 20.0));
        return ResultWrapper.result(agent.targetAcceleration() * 20.0);
    }
}


