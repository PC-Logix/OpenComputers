package li.cil.oc.common.blockentity;

import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.server.HologramPacketSender;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import scala.collection.Iterator;
import scala.collection.mutable.Set;
import scala.collection.mutable.Set$;

import java.io.IOException;
import java.util.Map;

/** Three-dimensional voxel hologram with tiered palette and network display controls. */
public class Hologram extends HologramBlockEntity {
    private final li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava rotationState =
        new li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava();
    @Override public li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava rotationState() { return rotationState; }
    private int tier;
    private final ComponentConnector node;
    private final int width = 3 * 16;
    private final int height = 2 * 16;
    private final int[] volume = new int[width * width * 2];
    private double scale = 1.0;
    private Vec3 translation = Vec3.ZERO;
    private double litRatio = -1.0;
    private boolean needsRendering;
    private int visibleQuads;
    private Set<Object> dirty = Set$.MODULE$.empty();
    private int dirtyFromX = Integer.MAX_VALUE;
    private int dirtyUntilX = -1;
    private int dirtyFromZ = Integer.MAX_VALUE;
    private int dirtyUntilZ = -1;
    private boolean hasPower = true;
    private float rotationAngle, rotationX, rotationY, rotationZ;
    private float rotationSpeed, rotationSpeedX, rotationSpeedY, rotationSpeedZ;
    private final int[][] colorsByTier = {{0x00FF00}, {0x0000FF, 0x00FF00, 0xFF0000}, {0x0000FF, 0x00FF00, 0xFF0000}};
    private Map<String, String> deviceInfo;

    private final String TierTag = Settings.namespace() + "tier";
    private final String VolumeTag = "volume";
    private final String ColorsTag = "colors";
    private final String ScaleTag = Settings.namespace() + "scale";
    private final String OffsetXTag = Settings.namespace() + "offsetX";
    private final String OffsetYTag = Settings.namespace() + "offsetY";
    private final String OffsetZTag = Settings.namespace() + "offsetZ";
    private final String RotationAngleTag = Settings.namespace() + "rotationAngle";
    private final String RotationXTag = Settings.namespace() + "rotationX";
    private final String RotationYTag = Settings.namespace() + "rotationY";
    private final String RotationZTag = Settings.namespace() + "rotationZ";
    private final String RotationSpeedTag = Settings.namespace() + "rotationSpeed";
    private final String RotationSpeedXTag = Settings.namespace() + "rotationSpeedX";
    private final String RotationSpeedYTag = Settings.namespace() + "rotationSpeedY";
    private final String RotationSpeedZTag = Settings.namespace() + "rotationSpeedZ";
    private final String HasPowerTag = Settings.namespace() + "hasPower";

    public Hologram(BlockPos pos, BlockState state, int tier) {
        super(pos, state);
        this.tier = tier;
        node = li.cil.oc.api.Network.newNode(this, Visibility.Network).withComponent("hologram").withConnector().create();
    }

    public Hologram(BlockPos pos, BlockState state) { this(pos, state, 0); }

    public int tier() { return tier; }
    public void tier_$eq(int value) { tier = value; }
    public ComponentConnector node() { return node; }
    public int width() { return width; }
    public int height() { return height; }
    public int[] volume() { return volume; }
    public double scale() { return scale; }
    public void scale_$eq(double value) { scale = value; }
    public Vec3 translation() { return translation; }
    public void translation_$eq(Vec3 value) { translation = value; }
    public double litRatio() { return litRatio; }
    public void litRatio_$eq(double value) { litRatio = value; }
    public boolean needsRendering() { return needsRendering; }
    public void needsRendering_$eq(boolean value) { needsRendering = value; }
    public int visibleQuads() { return visibleQuads; }
    public void visibleQuads_$eq(int value) { visibleQuads = value; }
    public Set<Object> dirty() { return dirty; }
    public int dirtyFromX() { return dirtyFromX; }
    public int dirtyUntilX() { return dirtyUntilX; }
    public int dirtyFromZ() { return dirtyFromZ; }
    public int dirtyUntilZ() { return dirtyUntilZ; }
    public boolean hasPower() { return hasPower; }
    public void hasPower_$eq(boolean value) { hasPower = value; }
    public float rotationAngle() { return rotationAngle; }
    public void rotationAngle_$eq(float value) { rotationAngle = value; }
    public float rotationX() { return rotationX; }
    public void rotationX_$eq(float value) { rotationX = value; }
    public float rotationY() { return rotationY; }
    public void rotationY_$eq(float value) { rotationY = value; }
    public float rotationZ() { return rotationZ; }
    public void rotationZ_$eq(float value) { rotationZ = value; }
    public float rotationSpeed() { return rotationSpeed; }
    public void rotationSpeed_$eq(float value) { rotationSpeed = value; }
    public float rotationSpeedX() { return rotationSpeedX; }
    public void rotationSpeedX_$eq(float value) { rotationSpeedX = value; }
    public float rotationSpeedY() { return rotationSpeedY; }
    public void rotationSpeedY_$eq(float value) { rotationSpeedY = value; }
    public float rotationSpeedZ() { return rotationSpeedZ; }
    public void rotationSpeedZ_$eq(float value) { rotationSpeedZ = value; }
    public int[] colors() { return colorsByTier[tier]; }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = BlockEntityDeviceInfo.hologram(tier + 1, width, height, colors().length);
        return deviceInfo;
    }

    public int getColor(int x, int y, int z) {
        int index = x + z * width;
        int low = (volume[index] >>> y) & 1;
        int high = (volume[index + width * width] >>> y) & 1;
        return low | (high << 1);
    }

    public void setColor(int x, int y, int z, int value) {
        if ((value & 3) != getColor(x, y, z)) {
            int low = value & 1, high = (value >>> 1) & 1, index = x + z * width;
            volume[index] = (volume[index] & ~(1 << y)) | (low << y);
            volume[index + width * width] = (volume[index + width * width] & ~(1 << y)) | (high << y);
            setDirty(x, z);
        }
    }

    private void setDirty(int x, int z) {
        dirty.$plus$eq(Short.valueOf((short) (((byte) x << 8) | (byte) z)));
        dirtyFromX = Math.min(dirtyFromX, x); dirtyUntilX = Math.max(dirtyUntilX, x + 1);
        dirtyFromZ = Math.min(dirtyFromZ, z); dirtyUntilZ = Math.max(dirtyUntilZ, z + 1);
        litRatio = -1;
    }

    private void resetDirtyFlag() {
        dirty.clear();
        dirtyFromX = Integer.MAX_VALUE; dirtyUntilX = -1; dirtyFromZ = Integer.MAX_VALUE; dirtyUntilZ = -1;
    }

    @OnlyIn(Dist.CLIENT)
    @Override public boolean canConnect(Direction side) { return toLocal(side) == Direction.DOWN; }
    @Override public Node sidedNode(Direction side) { return toLocal(side) == Direction.DOWN ? node : null; }
    @Override public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) { return new Node[]{node}; }

    @Callback(doc = "function() -- Clears the hologram.")
    public synchronized Object[] clear(Context context, Arguments args) {
        java.util.Arrays.fill(volume, 0);
        sendClear();
        resetDirtyFlag();
        litRatio = 0;
        return null;
    }

    @Callback(direct = true, doc = "function(x:number, y:number, z:number):number -- Returns the value for the specified voxel.")
    public synchronized Object[] get(Context context, Arguments args) {
        int[] xyz = checkCoordinates(args, 0, 1, 2);
        return ResultWrapper.result(getColor(xyz[0], xyz[1], xyz[2]));
    }

    @Callback(direct = true, limit = 256, doc = "function(x:number, y:number, z:number, value:number or boolean) -- Set the value for the specified voxel.")
    public synchronized Object[] set(Context context, Arguments args) {
        int[] xyz = checkCoordinates(args, 0, 1, 2);
        setColor(xyz[0], xyz[1], xyz[2], checkColor(args, 3));
        return null;
    }

    @Callback(direct = true, limit = 128, doc = "function(x:number, z:number[, minY:number], maxY:number, value:number or boolean) -- Fills an interval of a column with the specified value.")
    public synchronized Object[] fill(Context context, Arguments args) {
        int[] xz = checkCoordinates(args, 0, -1, 1);
        int minY, maxY, value;
        if (args.count() > 4) {
            minY = Math.min(32, Math.max(1, args.checkInteger(2)));
            maxY = Math.min(32, Math.max(1, args.checkInteger(3)));
            value = checkColor(args, 4);
        } else {
            minY = 1; maxY = Math.min(32, Math.max(1, args.checkInteger(2))); value = checkColor(args, 3);
        }
        if (minY > maxY) throw new IllegalArgumentException("interval is empty");
        int mask = (0xFFFFFFFF >>> (31 - (maxY - minY))) << (minY - 1);
        int index = xz[0] + xz[2] * width;
        if ((value & 1) == 0) volume[index] &= ~mask; else volume[index] |= mask;
        if (((value >>> 1) & 1) == 0) volume[index + width * width] &= ~mask; else volume[index + width * width] |= mask;
        setDirty(xz[0], xz[2]);
        return null;
    }

    @Callback(doc = "function(data:string) -- Set the raw buffer to the specified byte array, where each byte represents a voxel color. Nesting is x,z,y.")
    public synchronized Object[] setRaw(Context context, Arguments args) {
        byte[] data = args.checkByteArray(0);
        for (int x = 0; x < width; x++) for (int z = 0; z < width; z++) {
            int offset = z * height + x * height * width;
            if (data.length >= offset + height) {
                int low = 0, high = 0;
                for (int y = height - 1; y >= 0; y--) {
                    int color = data[offset + y];
                    low |= (color & 1) << y;
                    high |= ((color & 3) >>> 1) << y;
                }
                int index = x + z * width;
                if (volume[index] != low || volume[index + width * width] != high) {
                    volume[index] = low; volume[index + width * width] = high; setDirty(x, z);
                }
            }
        }
        context.pause(Settings.get().hologramSetRawDelay());
        return null;
    }

    @Callback(doc = "function(x:number, z:number, sx:number, sz:number, tx:number, tz:number) -- Copies an area of columns by the specified translation.")
    public synchronized Object[] copy(Context context, Arguments args) {
        int[] origin = checkCoordinates(args, 0, -1, 1);
        int w = args.checkInteger(2), h = args.checkInteger(3), tx = args.checkInteger(4), tz = args.checkInteger(5);
        if (w <= 0 || h <= 0 || (tx == 0 && tz == 0)) return null;
        int dxA = clamp(origin[0] + tx + w - 1, 0, width - 1), dxB = clamp(origin[0] + tx, 0, width);
        int dx0 = tx > 0 ? dxA : dxB, dx1 = tx > 0 ? dxB : dxA;
        int dzA = clamp(origin[2] + tz + h - 1, 0, width - 1), dzB = clamp(origin[2] + tz, 0, width);
        int dz0 = tz > 0 ? dzA : dzB, dz1 = tz > 0 ? dzB : dzA;
        int sx = tx > 0 ? -1 : 1, sz = tz > 0 ? -1 : 1;
        for (int nz = dz0; sz > 0 ? nz <= dz1 : nz >= dz1; nz += sz) {
            int oz = nz - tz;
            if (oz < 0 || oz >= width) continue;
            for (int nx = dx0; sx > 0 ? nx <= dx1 : nx >= dx1; nx += sx) {
                int ox = nx - tx;
                if (ox < 0 || ox >= width) continue;
                int target = nz * width + nx, source = oz * width + ox;
                volume[target] = volume[source];
                volume[target + width * width] = volume[source + width * width];
                setDirty(nx, nz);
            }
        }
        int area = (Math.max(dx0, dx1) - Math.min(dx0, dx1)) * (Math.max(dz0, dz1) - Math.min(dz0, dz1));
        context.pause(Math.max(0, area / (float) (width * width) - 0.25));
        return null;
    }

    private static int clamp(int value, int minimum, int maximum) { return Math.max(minimum, Math.min(maximum, value)); }

    @Callback(direct = true, doc = "function():number -- Returns the render scale of the hologram.")
    public Object[] getScale(Context context, Arguments args) { return ResultWrapper.result(scale); }
    @Callback(doc = "function(value:number) -- Set the render scale. A larger scale consumes more energy.")
    public Object[] setScale(Context context, Arguments args) {
        scale = Math.max(0.333333, Math.min(Settings.get().hologramMaxScaleByTier()[tier], args.checkDouble(0)));
        sendScale(); return null;
    }
    @Callback(direct = true, doc = "function():number, number, number -- Returns the relative render projection offsets of the hologram.")
    public Object[] getTranslation(Context context, Arguments args) { return ResultWrapper.result(translation.x, translation.y, translation.z); }
    @Callback(doc = "function(tx:number, ty:number, tz:number) -- Sets the relative render projection offsets of the hologram.")
    public Object[] setTranslation(Context context, Arguments args) {
        double max = Settings.get().hologramMaxTranslationByTier()[tier];
        double tx = Math.max(-max, Math.min(max, args.checkDouble(0)));
        double ty = Math.max(0, Math.min(max * 2, args.checkDouble(1)));
        double tz = Math.max(-max, Math.min(max, args.checkDouble(2)));
        translation = new Vec3(tx, ty, tz);
        sendOffset(); return null;
    }
    @Callback(direct = true, doc = "function():number -- The color depth supported by the hologram.")
    public Object[] maxDepth(Context context, Arguments args) { return ResultWrapper.result(tier + 1); }
    @Callback(doc = "function(index:number):number -- Get the color defined for the specified value.")
    public Object[] getPaletteColor(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index < 1 || index > colors().length) throw new ArrayIndexOutOfBoundsException();
        return ResultWrapper.result(convertColor(colors()[index - 1]));
    }
    @Callback(doc = "function(index:number, value:number):number -- Set the color defined for the specified value.")
    public Object[] setPaletteColor(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index < 1 || index > colors().length) throw new ArrayIndexOutOfBoundsException();
        int value = args.checkInteger(1), old = colors()[index - 1];
        colors()[index - 1] = convertColor(value);
        sendColor(index - 1, colors()[index - 1]);
        return ResultWrapper.result(old);
    }
    @Callback(doc = "function(angle:number, x:number, y:number, z:number):boolean -- Set the base rotation of the displayed hologram.")
    public Object[] setRotation(Context context, Arguments args) {
        if (tier <= 0) return ResultWrapper.result(ResultWrapper.unit, "not supported");
        rotationAngle = (float) (args.checkDouble(0) % 360);
        rotationX = (float) args.checkDouble(1); rotationY = (float) args.checkDouble(2); rotationZ = (float) args.checkDouble(3);
        sendRotation(); return ResultWrapper.result(true);
    }
    @Callback(doc = "function(speed:number, x:number, y:number, z:number):boolean -- Set the rotation speed of the displayed hologram.")
    public Object[] setRotationSpeed(Context context, Arguments args) {
        if (tier <= 0) return ResultWrapper.result(ResultWrapper.unit, "not supported");
        rotationSpeed = (float) Math.max(-360 * 4, Math.min(args.checkDouble(0), 360 * 4));
        rotationSpeedX = (float) args.checkDouble(1); rotationSpeedY = (float) args.checkDouble(2); rotationSpeedZ = (float) args.checkDouble(3);
        sendRotationSpeed(); return ResultWrapper.result(true);
    }
    @Callback(direct = true, doc = "function():number, number, number -- Get the dimension of the x,y,z axes.")
    public Object[] getDimensions(Context context, Arguments args) { return ResultWrapper.result(width, height, width); }

    private int[] checkCoordinates(Arguments args, int idxX, int idxY, int idxZ) {
        int x = idxX >= 0 ? args.checkInteger(idxX) - 1 : 0;
        if (x < 0 || x >= width) throw new ArrayIndexOutOfBoundsException("x");
        int y = idxY >= 0 ? args.checkInteger(idxY) - 1 : 0;
        if (y < 0 || y >= height) throw new ArrayIndexOutOfBoundsException("y");
        int z = idxZ >= 0 ? args.checkInteger(idxZ) - 1 : 0;
        if (z < 0 || z >= width) throw new ArrayIndexOutOfBoundsException("z");
        return new int[]{x, y, z};
    }
    private int checkColor(Arguments args, int index) {
        int value = args.isBoolean(index) ? (args.checkBoolean(index) ? 1 : 0) : args.checkInteger(index);
        if (value < 0 || value > colors().length) throw new IllegalArgumentException("invalid value");
        return value;
    }
    private static int convertColor(int color) { return ((color & 0x0000FF) << 16) | (color & 0x00FF00) | ((color & 0xFF0000) >>> 16); }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!isServer()) return;
        if (dirty.nonEmpty()) synchronized (this) {
            int dirtySizeX = dirtyUntilX - dirtyFromX, dirtySizeZ = dirtyUntilZ - dirtyFromZ;
            if (dirty.size() > dirtySizeX * dirtySizeZ * 0.8) sendArea(); else sendValues();
            resetDirtyFlag();
        }
        if (getLevel().getGameTime() % Settings.get().tickFrequency() == 0) {
            if (litRatio < 0) synchronized (this) {
                litRatio = 0;
                for (int value : volume) if (value != 0) litRatio += 1;
                litRatio /= volume.length;
            }
            boolean hadPower = hasPower;
            double neededPower = Settings.get().hologramCost() * litRatio * scale * Settings.get().tickFrequency();
            hasPower = node.tryChangeBuffer(-neededPower);
            if (hasPower != hadPower) sendPowerChange();
        }
    }

    public double getViewDistance() { return scale / maxScale() * Settings.get().hologramRenderDistance(); }
    public double getFadeStartDistanceSquared() {
        double fade = Settings.get().hologramFadeStartDistance();
        return scale / maxScale() * fade * fade;
    }
    private double maxScale() { return java.util.Arrays.stream(Settings.get().hologramMaxScaleByTier()).max().orElse(1); }

    private String dataPath() { return node.address() + "_data"; }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        tier = Math.max(0, Math.min(nbt.getByte(TierTag), 2));
        super.loadForServer(nbt, provider);
        li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava.loadForServer(this, nbt);
        CompoundTag tag = SaveHandler.loadNBT(nbt, dataPath());
        System.arraycopy(tag.getIntArray(VolumeTag), 0, volume, 0, Math.min(tag.getIntArray(VolumeTag).length, volume.length));
        int[] savedColors = tag.getIntArray(ColorsTag);
        for (int i = 0; i < Math.min(savedColors.length, colors().length); i++) colors()[i] = convertColor(savedColors[i]);
        loadCommonState(nbt);
    }
    @Override
    public synchronized void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        nbt.putByte(TierTag, (byte) tier);
        super.saveForServer(nbt, provider);
        li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava.saveForServer(this, nbt);
        SaveHandler.scheduleSave(getLevel(), getBlockPos().getX(), getBlockPos().getZ(), nbt, dataPath(), tag -> {
            tag.putIntArray(VolumeTag, volume); tag.putIntArray(ColorsTag, java.util.Arrays.stream(colors()).map(Hologram::convertColor).toArray()); return scala.runtime.BoxedUnit.UNIT;
        });
        saveCommonState(nbt);
    }
    @Override
    public void loadForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForClient(nbt, provider);
        li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava.loadForClient(this, nbt);
        int[] stored = nbt.getIntArray(VolumeTag); System.arraycopy(stored, 0, volume, 0, Math.min(stored.length, volume.length));
        int[] storedColors = nbt.getIntArray(ColorsTag); System.arraycopy(storedColors, 0, colors(), 0, Math.min(storedColors.length, colors().length));
        scale = nbt.getDouble(ScaleTag); hasPower = nbt.getBoolean(HasPowerTag); loadCommonState(nbt);
    }
    @Override
    public void saveForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForClient(nbt, provider);
        li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava.saveForClient(this, nbt);
        nbt.putIntArray(VolumeTag, volume); nbt.putIntArray(ColorsTag, colors());
        nbt.putDouble(ScaleTag, scale); nbt.putBoolean(HasPowerTag, hasPower); saveCommonState(nbt);
    }
    private void loadCommonState(CompoundTag nbt) {
        translation = new Vec3(nbt.getDouble(OffsetXTag), nbt.getDouble(OffsetYTag), nbt.getDouble(OffsetZTag));
        rotationAngle = nbt.getFloat(RotationAngleTag); rotationX = nbt.getFloat(RotationXTag);
        rotationY = nbt.getFloat(RotationYTag); rotationZ = nbt.getFloat(RotationZTag);
        rotationSpeed = nbt.getFloat(RotationSpeedTag); rotationSpeedX = nbt.getFloat(RotationSpeedXTag);
        rotationSpeedY = nbt.getFloat(RotationSpeedYTag); rotationSpeedZ = nbt.getFloat(RotationSpeedZTag);
    }
    private void saveCommonState(CompoundTag nbt) {
        nbt.putDouble(ScaleTag, scale); nbt.putDouble(OffsetXTag, translation.x); nbt.putDouble(OffsetYTag, translation.y); nbt.putDouble(OffsetZTag, translation.z);
        nbt.putFloat(RotationAngleTag, rotationAngle); nbt.putFloat(RotationXTag, rotationX); nbt.putFloat(RotationYTag, rotationY); nbt.putFloat(RotationZTag, rotationZ);
        nbt.putFloat(RotationSpeedTag, rotationSpeed); nbt.putFloat(RotationSpeedXTag, rotationSpeedX); nbt.putFloat(RotationSpeedYTag, rotationSpeedY); nbt.putFloat(RotationSpeedZTag, rotationSpeedZ);
    }

    private void sendClear() { send(HologramPacketSender::sendClear); }
    private void sendScale() { send(HologramPacketSender::sendScale); }
    private void sendOffset() { send(HologramPacketSender::sendOffset); }
    private void sendRotation() { send(HologramPacketSender::sendRotation); }
    private void sendRotationSpeed() { send(HologramPacketSender::sendRotationSpeed); }
    private void sendPowerChange() { send(HologramPacketSender::sendPowerChange); }
    private void sendArea() { send(HologramPacketSender::sendArea); }
    private void sendValues() { send(HologramPacketSender::sendValues); }
    private void sendColor(int index, int value) {
        try { HologramPacketSender.sendColor(this, index, value); }
        catch (IOException exception) { Hologram.<RuntimeException>throwUnchecked(exception); }
    }
    private void send(PacketAction action) {
        try { action.send(this); }
        catch (IOException exception) { Hologram.<RuntimeException>throwUnchecked(exception); }
    }
    @FunctionalInterface private interface PacketAction { void send(Hologram hologram) throws IOException; }
    @SuppressWarnings("unchecked") private static <T extends Throwable> void throwUnchecked(Throwable exception) throws T { throw (T) exception; }
}

