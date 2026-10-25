package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.RobotFlags;
import li.cil.oc.common.ToolDurabilityProviders;
import li.cil.oc.common.blockentity.RobotProxy;
import li.cil.oc.server.ParticlePacketSender;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Map;

public class Robot extends AbstractManagedEnvironment implements Agent, DeviceInfo {
    private final li.cil.oc.common.blockentity.Robot agent;
    private final ComponentConnector node;
    private final li.cil.oc.api.network.ManagedEnvironment romRobot;

    public Robot(li.cil.oc.common.blockentity.Robot agent) {
        this.agent = agent;
        this.node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
                .withComponent("robot")
                .withConnector(Settings.get().bufferRobot())
                .create();
        ResourceLocation romLocation = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), "lua/component/robot");
        this.romRobot = FileSystem.asManagedEnvironment(FileSystem.fromResource(romLocation), "robot");
    }

    @Override
    public li.cil.oc.api.internal.Agent agent() { return agent; }

    @Override
    public ComponentConnector node() { return node; }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceAttribute.Class, DeviceClass.System.toString(),
                DeviceAttribute.Description, "Robot",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "Caterpillar",
                DeviceAttribute.Capacity, Integer.toString(agent.getContainerSize()));
    }

    @Override
    public Direction checkSideForAction(Arguments args, int n) {
        return agent.toGlobal(new ExtendedArgumentsJava(args).checkSideForAction(n));
    }

    @Override
    public void onWorldInteraction(Context context, double duration) {
        Agent.super.onWorldInteraction(context, duration);
        agent.animateSwing(duration);
    }

    @Callback(doc = "function():number -- Get the current color of the activity light as an integer encoded RGB value (0xRRGGBB).")
    public Object[] getLightColor(Context context, Arguments args) {
        return ResultWrapper.result(agent.info().lightColor());
    }

    @Callback(doc = "function(value:number):number -- Set the color of the activity light to the specified integer encoded RGB value (0xRRGGBB).")
    public Object[] setLightColor(Context context, Arguments args) {
        agent.setLightColor(args.checkInteger(0));
        context.pause(0.1);
        return ResultWrapper.result(agent.info().lightColor());
    }

    @Callback(direct = true, doc = "function():string or nil -- Gets the currently displayed pride flag name, if any.")
    public Object[] getFlag(Context context, Arguments args) {
        scala.Option<ResourceLocation> current = agent.info().flag();
        if (current.isDefined()) {
            var flag = RobotFlags.byIdOptional(current.get());
            if (flag.isPresent()) return ResultWrapper.result(flag.get().name());
        }
        return ResultWrapper.result((Object) null);
    }

    @Callback(doc = "function(name:string):string -- Sets the displayed pride flag and returns its canonical name.")
    public Object[] setFlag(Context context, Arguments args) {
        scala.Option<RobotFlags.Flag> flag = RobotFlags.byName(args.checkString(0));
        if (flag.isDefined()) {
            agent.setFlag(scala.Option.apply(flag.get().id()));
            context.pause(0.1);
            return ResultWrapper.result(flag.get().name());
        }
        return ResultWrapper.result(null, "unknown pride flag");
    }

    @Callback(doc = "function() -- Hides the currently displayed pride flag.")
    public Object[] clearFlag(Context context, Arguments args) {
        agent.setFlag(scala.Option.empty());
        context.pause(0.1);
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function():number -- Get the durability of the currently equipped tool.")
    public Object[] durability(Context context, Arguments args) {
        ItemStack item = agent.equipmentInventory().getItem(0);
        if (item.isEmpty()) return ResultWrapper.result(null, "no tool equipped");
        scala.Option<Object> durability = ToolDurabilityProviders.getDurability(item);
        return durability.isDefined()
                ? ResultWrapper.result(durability.get())
                : ResultWrapper.result(null, "tool cannot be damaged");
    }

    @Callback(doc = "function(direction:number):boolean -- Move in the specified direction.")
    public Object[] move(Context context, Arguments args) {
        Direction direction = agent.toGlobal(new ExtendedArgumentsJava(args).checkSideForMovement(0));
        if (agent.isAnimatingMove()) return ResultWrapper.result(null, "already moving");

        var blockContent = blockContent(direction);
        if (Boolean.TRUE.equals(blockContent.detected())) {
            context.pause(0.4);
            ParticlePacketSender.sendUnchecked(BlockPositionsJava.apply(agent), ParticleTypes.CRIT,
                    8, 0.25, java.util.Optional.of(direction), Settings.get().maxNetworkClientEffectPacketDistance() / 2.0D);
            return ResultWrapper.result(null, blockContent.kind());
        }

        if (!node.tryChangeBuffer(-Settings.get().robotMoveCost())) {
            return ResultWrapper.result(null, "not enough energy");
        }
        if (agent.move(direction)) {
            context.pause(Settings.get().moveDelay());
            return ResultWrapper.result(true);
        }

        node.changeBuffer(Settings.get().robotMoveCost());
        context.pause(0.4);
        ParticlePacketSender.sendUnchecked(BlockPositionsJava.apply(agent), ParticleTypes.CRIT,
                8, 0.25, java.util.Optional.of(direction), Settings.get().maxNetworkClientEffectPacketDistance() / 2.0D);
        return ResultWrapper.result(null, "impossible move");
    }

    @Callback(doc = "function(clockwise:boolean):boolean -- Rotate in the specified direction.")
    public Object[] turn(Context context, Arguments args) {
        boolean clockwise = args.checkBoolean(0);
        if (!node.tryChangeBuffer(-Settings.get().robotTurnCost())) {
            return ResultWrapper.result(null, "not enough energy");
        }

        agent.rotate(clockwise ? Direction.UP : Direction.DOWN);
        agent.animateTurn(clockwise, Settings.get().turnDelay());
        context.pause(Settings.get().turnDelay());
        return ResultWrapper.result(true);
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node) {
            if (romRobot != null) {
                ((Component) romRobot.node()).setVisibility(Visibility.Network);
                node.connect(romRobot.node());
            }
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("network.message".equals(message.name()) && message.source() != agent.node()) {
            Object[] data = message.data();
            if (data.length == 1 && data[0] instanceof Packet packet) {
                agent.proxy().node().sendToReachable(message.name(), packet);
            }
        }
    }

    @Override
    public void loadData(DataComponentHolder holder) throws li.cil.oc.api.UnrecoverablePersistanceException {
        super.loadData(holder);
        if (romRobot instanceof li.cil.oc.server.component.FileSystem fileSystem) {
            DataComponentType<CompoundTag> dataType = OCComponentsJava.ROBOT_ROM_FILESYSTEM_DATA.get();
            CompoundTag tag = holder.get(dataType);
            if (tag != null) fileSystem.fileSystem().loadData(tag);
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        if (romRobot instanceof li.cil.oc.server.component.FileSystem fileSystem) {
            CompoundTag tag = new CompoundTag();
            fileSystem.fileSystem().saveData(tag);
            holder.set(OCComponentsJava.ROBOT_ROM_FILESYSTEM_DATA.get(), tag);
        }
    }
}

