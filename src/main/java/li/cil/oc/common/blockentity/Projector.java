package li.cil.oc.common.blockentity;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.datacomponents.MutableNbtComponentHolder;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.component.TextBuffer;
import li.cil.oc.server.ProjectorPacketSender;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Tuple2;
import java.util.Iterator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/** The projector framebuffer and its optional network-backed screen component. */
public class Projector extends ProjectorBlockEntity {
    public static final int Width = 320;
    public static final int Height = 200;

    private final int width = Width;
    private final int height = Height;
    private final int[] pixels = new int[width * height];
    private final ComponentConnector node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
            .withComponent("projector").withConnector().create();
    private final TextBuffer screenBuffer = new TextBuffer(this);
    private static final int ScreenWidth = 160;
    private static final int ScreenHeight = 50;
    private static final li.cil.oc.api.internal.TextBuffer.ColorDepth ScreenDepth =
            li.cil.oc.api.internal.TextBuffer.ColorDepth.EightBit;
    private final double PowerPerTick = Settings.get().screenCost();
    private static final String PixelsTag = Settings.namespace() + "projectorPixels";
    private static final String OnTag = Settings.namespace() + "projectorOn";
    private static final String ScreenModeTag = Settings.namespace() + "projectorScreenMode";
    private static final String ScreenNodeAddressTag = Settings.namespace() + "projectorScreenNodeAddress";

    private String pendingScreenNodeAddress;
    private Object modeNetwork;
    private boolean isOn = true;
    private boolean hasPower = true;
    private boolean clientPixelsDirty = true;
    private boolean screenMode = getBlockState().getValue(li.cil.oc.common.block.Projector.ScreenMode);
    private int dirtyFromX = width;
    private int dirtyFromY = height;
    private int dirtyUntilX;
    private int dirtyUntilY;
    private Map<String, String> deviceInfo;

    public Projector(BlockPos pos, BlockState state) {
        super(pos, state);
        screenBuffer.setMaximumResolution(ScreenWidth, ScreenHeight);
        screenBuffer.setMaximumColorDepth(ScreenDepth);
        screenBuffer.data().size_$eq(new Tuple2<>(ScreenWidth, ScreenHeight));
        screenBuffer.data().format_$eq(PackedColor.Depth.format(ScreenDepth));
        screenBuffer.viewport_$eq(new Tuple2<>(ScreenWidth, ScreenHeight));
        if (isServer()) setModeComponentVisibility();
    }

    public int width() { return width; }
    public int height() { return height; }
    public int[] pixels() { return pixels; }
    public Component node() { return node; }
    public TextBuffer screenBuffer() { return screenBuffer; }
    public boolean isOn() { return isOn; }
    public void isOn_$eq(boolean value) { isOn = value; }
    public boolean hasPower() { return hasPower; }
    public void hasPower_$eq(boolean value) { hasPower = value; }
    public boolean isScreenMode() { return screenMode; }
    public boolean clientPixelsDirty() { return clientPixelsDirty; }
    public void clientPixelsDirty_$eq(boolean value) { clientPixelsDirty = value; }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = BlockEntityDeviceInfo.projector(width, height);
        return deviceInfo;
    }

    @Callback(direct = true, doc = "function():number, number -- Returns the framebuffer resolution.")
    public Object[] getResolution(Context context, Arguments args) { return ResultWrapper.result(width, height); }

    public void toggleMode() {
        screenMode = !screenMode;
        if (getLevel() != null && isServer()) {
            BlockState state = getBlockState();
            if (state.hasProperty(li.cil.oc.common.block.Projector.ScreenMode)) {
                getLevel().setBlock(getBlockPos(), state.setValue(li.cil.oc.common.block.Projector.ScreenMode, screenMode), 3);
            }
        }
        setModeComponentVisibility();
        screenBuffer.setPowerState(screenMode);
        setChanged();
        if (getLevel() != null && isServer()) getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    private void updateLightState() {
        if (getLevel() != null && isServer()) {
            BlockState state = getBlockState();
            boolean lit = state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
            if (lit != isOn) getLevel().setBlock(getBlockPos(), state.setValue(BlockStateProperties.LIT, isOn), 3);
        }
    }

    private void setModeComponentVisibility() {
        if (!isServer()) return;
        Component projectorComponent = node;
        Component screenComponent = (Component) screenBuffer.node();
        if (node.network() != null && modeNetwork != node.network()) {
            projectorComponent.setVisibility(Visibility.None);
            screenComponent.setVisibility(Visibility.None);
            modeNetwork = node.network();
        }
        projectorComponent.setVisibility(screenMode ? Visibility.None : Visibility.Network);
        screenComponent.setVisibility(screenMode ? Visibility.Network : Visibility.None);
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the projector is enabled and powered.")
    public Object[] isProjecting(Context context, Arguments args) { return ResultWrapper.result(isOn && hasPower); }

    @Callback(doc = "function():boolean -- Turns the projector on.")
    public Object[] turnOn(Context context, Arguments args) {
        boolean changed = !isOn;
        isOn = true;
        updateLightState();
        setChanged();
        if (isServer()) sendPowerChange();
        return ResultWrapper.result(changed, isOn);
    }

    @Callback(doc = "function():boolean -- Turns the projector off.")
    public Object[] turnOff(Context context, Arguments args) {
        boolean changed = isOn;
        isOn = false;
        updateLightState();
        setChanged();
        if (isServer()) sendPowerChange();
        return ResultWrapper.result(changed, isOn);
    }

    @Callback(direct = true, doc = "function(x:number, y:number):number -- Returns the RGB pixel at one-based coordinates.")
    public synchronized Object[] get(Context context, Arguments args) {
        int[] xy = checkCoordinates(args);
        return ResultWrapper.result(pixels[xy[0] + xy[1] * width] & 0xFFFFFF);
    }

    @Callback(limit = 256, doc = "function(x:number, y:number, color:number) -- Sets an ARGB pixel at one-based coordinates.")
    public synchronized Object[] set(Context context, Arguments args) {
        int[] xy = checkCoordinates(args);
        setPixel(xy[0], xy[1], args.checkInteger(2));
        return null;
    }

    @Callback(limit = 128, doc = "function(x:number, y:number, width:number, height:number, color:number) -- Fills a rectangle with an ARGB color.")
    public synchronized Object[] fill(Context context, Arguments args) {
        int x = args.checkInteger(0) - 1, y = args.checkInteger(1) - 1;
        int w = args.checkInteger(2), h = args.checkInteger(3);
        if (w <= 0 || h <= 0) throw new IllegalArgumentException("rectangle is empty");
        int color = normalizeColor(args.checkInteger(4));
        int x0 = Math.max(0, x), y0 = Math.max(0, y);
        int x1 = Math.min(width, x + w), y1 = Math.min(height, y + h);
        for (int py = y0; py < y1; py++) for (int px = x0; px < x1; px++) pixels[px + py * width] = color;
        if (x0 < x1 && y0 < y1) markDirty(x0, y0, x1, y1);
        return null;
    }

    @Callback(doc = "function():boolean -- Clears the framebuffer to transparent black.")
    public synchronized Object[] clear(Context context, Arguments args) {
        Arrays.fill(pixels, 0);
        markDirty(0, 0, width, height);
        return null;
    }

    @Callback(doc = "function(data:string) -- Sets row-major RGBA bytes for the framebuffer; incomplete data clears the remaining pixels.")
    public synchronized Object[] setRaw(Context context, Arguments args) {
        byte[] data = args.checkByteArray(0);
        Arrays.fill(pixels, 0);
        int count = Math.min(pixels.length, data.length / 4);
        for (int i = 0; i < count; i++) {
            int offset = i * 4;
            int r = data[offset] & 0xFF, g = data[offset + 1] & 0xFF;
            int b = data[offset + 2] & 0xFF, a = data[offset + 3] & 0xFF;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        markDirty(0, 0, width, height);
        context.pause(0.5);
        return null;
    }

    private int[] checkCoordinates(Arguments args) {
        int x = args.checkInteger(0) - 1, y = args.checkInteger(1) - 1;
        if (x < 0 || x >= width) throw new ArrayIndexOutOfBoundsException("x");
        if (y < 0 || y >= height) throw new ArrayIndexOutOfBoundsException("y");
        return new int[]{x, y};
    }

    private void setPixel(int x, int y, int color) {
        int normalized = normalizeColor(color), index = x + y * width;
        if (pixels[index] != normalized) {
            pixels[index] = normalized;
            markDirty(x, y, x + 1, y + 1);
        }
    }

    private static int normalizeColor(int color) { return color == 0 || (color & 0xFF000000) != 0 ? color : color | 0xFF000000; }

    private void markDirty(int x0, int y0, int x1, int y1) {
        dirtyFromX = Math.min(dirtyFromX, x0); dirtyFromY = Math.min(dirtyFromY, y0);
        dirtyUntilX = Math.max(dirtyUntilX, x1); dirtyUntilY = Math.max(dirtyUntilY, y1);
        clientPixelsDirty = true;
        setChanged();
    }

    private void resetDirty() { dirtyFromX = width; dirtyFromY = height; dirtyUntilX = 0; dirtyUntilY = 0; }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isServer()) {
            updateLightState();
            if (node.network() == null) li.cil.oc.api.Network.joinOrCreateNetwork(this);
            if (screenBuffer.node().network() != node.network() && node.network() != null) node.connect(screenBuffer.node());
            if (screenBuffer.node().network() == node.network() && node.network() != null) {
                java.util.HashSet<Node> screenNeighbors = new java.util.HashSet<>();
                Iterator<Node> screens = screenBuffer.node().neighbors().iterator();
                while (screens.hasNext()) screenNeighbors.add(screens.next());
                Iterator<Node> neighbors = node.neighbors().iterator();
                while (neighbors.hasNext()) {
                    Node neighbor = neighbors.next();
                    if (neighbor != screenBuffer.node() && !screenNeighbors.contains(neighbor)) screenBuffer.node().connect(neighbor);
                }
            }
            if (node.network() != null) setModeComponentVisibility();
        }
        if (isClient() || (isScreenMode() && screenBuffer.node().network() != null)) screenBuffer.update();
        if (isServer()) {
            if (!isScreenMode() && isOn) {
                boolean wasPowered = hasPower;
                hasPower = node.tryChangeBuffer(-PowerPerTick);
                if (hasPower != wasPowered) {
                    getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                    sendPowerChange();
                }
            }
            synchronized (this) {
                if (dirtyUntilX > dirtyFromX && dirtyUntilY > dirtyFromY) {
                    sendFrame(dirtyFromX, dirtyFromY, dirtyUntilX, dirtyUntilY);
                    resetDirty();
                }
            }
        }
    }

    public synchronized void clientFrame(int x, int y, int w, int h, int[] data) {
        for (int py = 0; py < h; py++) for (int px = 0; px < w; px++) {
            int dx = x + px, dy = y + py;
            if (dx >= 0 && dx < width && dy >= 0 && dy < height && px + py * w < data.length) pixels[dx + dy * width] = data[px + py * w];
        }
        clientPixelsDirty = true;
    }

    public Direction getProjectionDirection() { return getBlockState().getValue(PropertyRotatable.Facing); }
    public AABB projectionBounds() { return new AABB(getBlockPos()).inflate(16); }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        int[] stored = nbt.getIntArray(PixelsTag);
        if (stored.length == pixels.length) System.arraycopy(stored, 0, pixels, 0, stored.length);
        isOn = nbt.contains(OnTag) ? nbt.getBoolean(OnTag) : true;
        screenMode = nbt.contains(ScreenModeTag) ? nbt.getBoolean(ScreenModeTag) : getBlockState().getValue(li.cil.oc.common.block.Projector.ScreenMode);
        restoreScreenNodeAddress(nbt);
    }

    @Override
    public synchronized void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        nbt.putIntArray(PixelsTag, pixels); nbt.putBoolean(OnTag, isOn); nbt.putBoolean(ScreenModeTag, screenMode);
        saveScreenNodeAddress(nbt);
    }

    @Override
    public void loadForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForClient(nbt, provider);
        int[] stored = nbt.getIntArray(PixelsTag);
        if (stored.length == pixels.length) System.arraycopy(stored, 0, pixels, 0, stored.length);
        isOn = nbt.getBoolean(OnTag);
        screenMode = nbt.contains(ScreenModeTag) ? nbt.getBoolean(ScreenModeTag) : getBlockState().getValue(li.cil.oc.common.block.Projector.ScreenMode);
        hasPower = nbt.getBoolean(Settings.namespace() + "projectorPower");
        pendingScreenNodeAddress = nbt.getString(ScreenNodeAddressTag);
        if (pendingScreenNodeAddress != null && pendingScreenNodeAddress.isEmpty()) pendingScreenNodeAddress = null;
        clientPixelsDirty = true;
    }

    @Override
    public synchronized void saveForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForClient(nbt, provider);
        nbt.putBoolean(OnTag, isOn); nbt.putBoolean(ScreenModeTag, screenMode);
        nbt.putBoolean(Settings.namespace() + "projectorPower", hasPower);
        saveScreenNodeAddress(nbt);
    }

    @Override
    public void loadComponentsForServer(DataComponentHolder holder) {
        super.loadComponentsForServer(holder);
        screenBuffer.loadData(holder);
        setModeComponentVisibility();
    }

    @Override
    public void saveComponentsForServer(MutableDataComponentHolder holder) {
        super.saveComponentsForServer(holder);
        screenBuffer.saveData(holder);
    }

    @Override
    public void loadComponentsForClient(DataComponentHolder holder) {
        super.loadComponentsForClient(holder);
        screenBuffer.loadData(holder);
        if (screenBuffer.proxy().nodeAddress().isEmpty() && pendingScreenNodeAddress != null) {
            screenBuffer.proxy().nodeAddress_$eq(pendingScreenNodeAddress);
            screenBuffer.requestSynchronization();
        }
        pendingScreenNodeAddress = null;
    }

    @Override
    public void saveComponentsForClient(MutableDataComponentHolder holder) {
        super.saveComponentsForClient(holder);
        screenBuffer.saveData(holder);
    }

    @Override
    public void dispose() {
        if (isServer() && screenBuffer.node() != null) screenBuffer.node().remove();
        super.dispose();
    }

    private void restoreScreenNodeAddress(CompoundTag nbt) {
        String address = nbt.getString(ScreenNodeAddressTag);
        if (address != null && !address.isEmpty() && screenBuffer.node() != null) {
            MutableNbtComponentHolder holder = new MutableNbtComponentHolder();
            holder.set(OCComponentsJava.ADDRESS.get(), address);
            try {
                screenBuffer.node().loadData(holder);
            } catch (UnrecoverablePersistanceException exception) {
                Projector.<RuntimeException>throwUnchecked(exception);
            }
        }
    }

    private void saveScreenNodeAddress(CompoundTag nbt) {
        if (screenBuffer.node() != null && screenBuffer.node().address() != null) nbt.putString(ScreenNodeAddressTag, screenBuffer.node().address());
    }

    private void sendPowerChange() {
        try { ProjectorPacketSender.sendPowerChange(this); }
        catch (IOException exception) { Projector.<RuntimeException>throwUnchecked(exception); }
    }

    private void sendFrame(int x0, int y0, int x1, int y1) {
        try { ProjectorPacketSender.sendFrame(this, x0, y0, x1, y1); }
        catch (IOException exception) { Projector.<RuntimeException>throwUnchecked(exception); }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable exception) throws T { throw (T) exception; }
}
