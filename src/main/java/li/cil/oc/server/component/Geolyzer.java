package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.event.GeolyzerEvent;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.blockentity.Microcontroller;
import li.cil.oc.common.blockentity.Robot;
import li.cil.oc.common.entity.Drone;
import li.cil.oc.common.item.TabletWrapper;
import li.cil.oc.server.component.traits.LevelControl;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.DatabaseAccess;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.ResultWrapper;
import li.cil.oc.util.SableCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Map;

public class Geolyzer extends AbstractManagedEnvironment implements LevelControl, DeviceInfo {
    private final EnvironmentHost host;

    public Geolyzer(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).withComponent("geolyzer").withConnector().create());
    }

    public EnvironmentHost host() { return host; }

    @Override
    public Connector node() { return (Connector) super.node(); }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Generic,
            DeviceAttribute.Description, "Geolyzer",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Terrain Analyzer MkII",
            DeviceAttribute.Capacity, Integer.toString(Settings.get().geolyzerRange()));
    }

    @Override
    public Direction checkSideForAction(Arguments args, int index) {
        Direction side = new ExtendedArgumentsJava(args).checkSideAny(index);
        if (host instanceof Robot robot) return robot.proxy().toGlobal(side);
        if (host instanceof Drone drone) return drone.toGlobal(side);
        if (host instanceof Microcontroller microcontroller) return microcontroller.toLocal(side);
        if (host instanceof TabletWrapper tablet) return tablet.toGlobal(side);
        return side;
    }

    @Override
    public BlockPosition position() {
        if (host instanceof Robot robot) return robot.proxy().position();
        if (host instanceof Drone drone) return BlockPositionsJava.apply(drone.blockPosition(), drone.getEnvironmentLevel());
        if (host instanceof Microcontroller microcontroller) return microcontroller.position();
        if (host instanceof TabletWrapper tablet) {
            return BlockPositionsJava.apply(tablet.xPosition(), tablet.yPosition(), tablet.zPosition(), tablet.getEnvironmentLevel());
        }
        return BlockPositionsJava.apply(host);
    }

    // Callback discovery scans class methods, so retain the Scala trait's callback explicitly.
    @Override
    @Callback(doc = "function(side:number):boolean, string -- Checks the contents of the block on the specified sides and returns the findings.")
    public Object[] detect(Context context, Arguments args) {
        return LevelControl.super.detect(context, args);
    }

    private boolean canSeeSky() {
        BlockPosition above = position().offset(Direction.UP);
        BlockPos physical = BlockPos.containing(SableCompat.physicalPosition(host.getEnvironmentLevel(), above.toVec3()));
        return !host.getEnvironmentLevel().dimension().equals(Level.NETHER) &&
            host.getEnvironmentLevel().canSeeSkyFromBelowWater(physical);
    }

    @Callback(doc = "function():boolean -- Returns whether there is a clear line of sight to the sky directly above.")
    public Object[] canSeeSky(Context context, Arguments args) {
        return ResultWrapper.result(canSeeSky());
    }

    @Callback(doc = "function():boolean -- Return whether the sun is currently visible directly above.")
    public Object[] isSunVisible(Context context, Arguments args) {
        BlockPosition above = BlockPositionsJava.apply(host).offset(Direction.UP);
        BlockPos physical = BlockPos.containing(SableCompat.physicalPosition(host.getEnvironmentLevel(), above.toVec3()));
        Level level = host.getEnvironmentLevel();
        return ResultWrapper.result(level.isDay() && canSeeSky() &&
            (level.getBiome(physical).value().getPrecipitationAt(physical) == Precipitation.NONE ||
                (!level.isRaining() && !level.isThundering())));
    }

    @Callback(doc = "function(x:number, z:number[, y:number, w:number, d:number, h:number][, ignoreReplaceable:boolean|options:table]):table -- Analyzes the density of the column at the specified relative coordinates.")
    public Object[] scan(Context context, Arguments args) {
        ScanBounds bounds = getScanArgs(args);
        int volume = (bounds.maxX() - bounds.minX() + 1) * (bounds.maxZ() - bounds.minZ() + 1) *
            (bounds.maxY() - bounds.minY() + 1);
        if (volume > 64) throw new IllegalArgumentException("volume too large (maximum is 64)");
        Map<?, ?> options = args.isBoolean(bounds.optionIndex())
            ? Map.of("includeReplaceable", !args.checkBoolean(bounds.optionIndex()))
            : args.optTable(bounds.optionIndex(), Map.of());
        int range = Settings.get().geolyzerRange();
        if (Math.abs(bounds.minX()) > range || Math.abs(bounds.maxX()) > range ||
            Math.abs(bounds.minY()) > range || Math.abs(bounds.maxY()) > range ||
            Math.abs(bounds.minZ()) > range || Math.abs(bounds.maxZ()) > range) {
            throw new IllegalArgumentException("location out of bounds");
        }
        if (!node().tryChangeBuffer(-Settings.get().geolyzerScanCost())) return ResultWrapper.result(null, "not enough energy");
        GeolyzerEvent.Scan event = new GeolyzerEvent.Scan(host, options, bounds.minX(), bounds.minY(), bounds.minZ(),
            bounds.maxX(), bounds.maxY(), bounds.maxZ());
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled() ? ResultWrapper.result(null, "scan was canceled") : ResultWrapper.result(event.data);
    }

    private ScanBounds getScanArgs(Arguments args) {
        int minX = args.checkInteger(0);
        int minZ = args.checkInteger(1);
        if (args.isInteger(2) && args.isInteger(3) && args.isInteger(4) && args.isInteger(5)) {
            int minY = args.checkInteger(2);
            int maxX = minX + args.checkInteger(3) - 1;
            int maxZ = minZ + args.checkInteger(4) - 1;
            int maxY = minY + args.checkInteger(5) - 1;
            return new ScanBounds(Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ),
                Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ), 6);
        }
        return new ScanBounds(minX, -32, minZ, minX, 31, minZ, 2);
    }

    @Callback(doc = "function(side:number[,options:table]):table -- Get some information on a directly adjacent block.")
    public Object[] analyze(Context context, Arguments args) {
        if (!Settings.get().allowItemStackInspection()) return ResultWrapper.result(null, "not enabled in config");
        Direction side = new ExtendedArgumentsJava(args).checkSideAny(0);
        Direction globalSide = host instanceof Rotatable rotatable ? rotatable.toGlobal(side) : side;
        Map<?, ?> options = args.optTable(1, Map.of());
        if (!node().tryChangeBuffer(-Settings.get().geolyzerScanCost())) return ResultWrapper.result(null, "not enough energy");
        GeolyzerEvent.Analyze event = new GeolyzerEvent.Analyze(host, options, BlockPositionsJava.apply(host).offset(globalSide).toBlockPos());
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled() ? ResultWrapper.result(null, "scan was canceled") : ResultWrapper.result(event.data);
    }

    @Callback(doc = "function(side:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack representation of the block on the specified side in a database component.")
    public Object[] store(Context context, Arguments args) {
        Direction side = new ExtendedArgumentsJava(args).checkSideAny(0);
        Direction globalSide = host instanceof Rotatable rotatable ? rotatable.toGlobal(side) : side;
        if (!node().tryChangeBuffer(-Settings.get().geolyzerScanCost())) return ResultWrapper.result(null, "not enough energy");
        BlockPos pos = BlockPositionsJava.apply(host).offset(globalSide).toBlockPos();
        var state = host.getEnvironmentLevel().getBlockState(pos);
        Item item = state.getBlock().asItem();
        if (item == null) return ResultWrapper.result(null, "block has no registered item representation");
        List<ItemStack> drops = Block.getDrops(state, (ServerLevel) host.getEnvironmentLevel(), pos,
            host.getEnvironmentLevel().getBlockEntity(pos));
        ItemStack stack;
        if (!drops.isEmpty()) {
            stack = drops.getFirst();
            for (ItemStack candidate : drops) {
                if (candidate.getItem() == item) { stack = candidate; break; }
            }
            stack.setCount(1);
        }
        else stack = new ItemStack(item, 1);
        ItemStack selected = stack;
        return DatabaseAccess.withDatabase(node(), args.checkString(1), database -> {
            int slot = new ExtendedArgumentsJava(args).checkSlot(database.data(), 2);
            boolean nonEmpty = database.getStackInSlot(slot) != ItemStack.EMPTY;
            database.setStackInSlot(slot, selected);
            return ResultWrapper.result(nonEmpty);
        });
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (!message.name().equals("tablet.use") || !(message.source().host() instanceof Machine machine) ||
            !(machine.host() instanceof Tablet)) return;
        Object[] data = message.data();
        if (data != null && data.length == 8 && data[0] instanceof CompoundTag nbt && data[1] instanceof ItemStack &&
            data[2] instanceof Player && data[3] instanceof BlockPosition position && data[4] instanceof Direction &&
            data[5] instanceof Float && data[6] instanceof Float && data[7] instanceof Float &&
            node().tryChangeBuffer(-Settings.get().geolyzerScanCost())) {
            GeolyzerEvent.Analyze event = new GeolyzerEvent.Analyze(host, Map.of(), position.toBlockPos());
            NeoForge.EVENT_BUS.post(event);
            if (!event.isCanceled()) {
                for (Map.Entry<String, Object> entry : event.data.entrySet()) {
                    if (entry.getValue() instanceof Number number) nbt.putDouble(entry.getKey(), number.doubleValue());
                    else if (entry.getValue() instanceof String value && !value.isEmpty()) nbt.putString(entry.getKey(), value);
                }
            }
        }
    }

    private record ScanBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int optionIndex) {}
}
