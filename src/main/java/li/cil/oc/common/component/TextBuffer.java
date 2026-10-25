package li.cil.oc.common.component;

import com.google.common.base.Strings;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.PacketBuilder;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.Tier;
import li.cil.oc.common.component.traits.TextBufferProxy;
import li.cil.oc.common.component.traits.VideoRamDevice;
import li.cil.oc.common.component.traits.VideoRamRasterizer;
import li.cil.oc.common.datacomponents.MaximumVideoMode;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.SideTracker;
import li.cil.oc.SettingsAccessJava;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Option$;
import scala.Tuple2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Managed screen component; callback metadata and public device APIs remain stable. */
public class TextBuffer extends AbstractManagedEnvironment implements TextBufferProxy, VideoRamRasterizer, DeviceInfo {
    private final EnvironmentHost host;
    private final Map<String, VideoRamDevice> videoRamDeviceMap = new HashMap<>();
    private final Connector node;
    private Tuple2<Object, Object> maxResolution;
    private li.cil.oc.api.internal.TextBuffer.ColorDepth maxDepth;
    private Tuple2<Object, Object> aspectRatio = new Tuple2<>(1.0, 1.0);
    private double powerConsumptionPerTick;
    private boolean precisionMode;
    private boolean isRendering = true;
    private boolean isDisplaying = true;
    private boolean hasPower = true;
    private double relativeLitArea = -1.0;
    private Option<PacketBuilder> pendingCommands = Option$.MODULE$.empty();
    private final int syncInterval = 100;
    private int syncCooldown = syncInterval;
    private volatile Map<String, String> deviceInfo;
    private double fullyLitCost;
    private Tuple2<Object, Object> viewport;
    private final TextBufferProxyBase proxy;
    private final li.cil.oc.util.TextBuffer data;
    private boolean pendingExternalLoad;

    public TextBuffer(EnvironmentHost host) {
        this.host = host;
        this.node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
            .withComponent("screen").withConnector().create();
        this.maxResolution = new Tuple2<>(SettingsAccessJava.screenWidthAtTier(Tier.One), SettingsAccessJava.screenHeightAtTier(Tier.One));
        this.maxDepth = SettingsAccessJava.screenDepthAtTier(Tier.One);
        this.powerConsumptionPerTick = SettingsAccessJava.get().screenCost();
        this.fullyLitCost = computeFullyLitCost();
        this.proxy = SideTracker.isClient()
            ? new TextBufferClientProxy(this)
            : new TextBufferServerProxy(this);
        this.data = new li.cil.oc.util.TextBuffer(((Number) maxResolution._1()).intValue(),
            ((Number) maxResolution._2()).intValue(), PackedColor.Depth.format(maxDepth));
        this.viewport = new Tuple2<>(data.width(), data.height());
    }

    public EnvironmentHost host() { return host; }
    @Override public Connector node() { return node; }
    @Override public Map<String, VideoRamDevice> videoRamDevices() { return videoRamDeviceMap; }
    public TextBufferProxyBase proxy() { return proxy; }
    @Override public li.cil.oc.util.TextBuffer data() { return data; }
    public Tuple2<Object, Object> viewport() { return viewport; }
    public void viewport_$eq(Tuple2<?, ?> value) { viewport = new Tuple2<>(value._1(), value._2()); }
    public double fullyLitCost() { return fullyLitCost; }
    public void fullyLitCost_$eq(double value) { fullyLitCost = value; }

    public boolean precisionModeForProxy() { return precisionMode; }
    public boolean precisionModeForCallbacks() { return precisionMode; }
    public void setPrecisionModeForCallbacks(boolean value) { precisionMode = value; }
    public boolean precisionSupportedForCallbacks() {
        return maxDepth.compareTo(SettingsAccessJava.screenDepthAtTier(Tier.Three)) >= 0;
    }
    public Tuple2<Object, Object> aspectRatioForCallbacks() { return aspectRatio; }
    public Tuple2<Object, Object> maximumResolutionForCallbacks() { return maxResolution; }
    public li.cil.oc.api.internal.TextBuffer.ColorDepth maximumDepthForCallbacks() { return maxDepth; }
    public Tuple2<Object, Object> aspectRatioForVideoMode() { return aspectRatio; }
    public void setAspectRatioForVideoMode(double width, double height) { aspectRatio = new Tuple2<>(width, height); }
    public void setMaximumResolutionForVideoMode(int width, int height) { maxResolution = new Tuple2<>(width, height); }
    public void setMaximumDepthForVideoMode(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) { maxDepth = depth; }

    public void setRenderingEnabledForClient(boolean enabled) { isRendering = enabled; }
    public boolean isRenderingEnabledForClient() { return isRendering; }
    public void restoreDisplaying(boolean value) { isDisplaying = value; }
    public void restoreHasPower(boolean value) { hasPower = value; }
    public void restorePrecision(boolean value) { precisionMode = value; }
    public void restoreViewport(int width, int height) { viewport = new Tuple2<>(width, height); }
    public boolean hasPowerForPersistence() { return hasPower; }
    public boolean isPreciseForPersistence() { return precisionMode; }
    public void restoreMaximumVideoMode(MaximumVideoMode mode) {
        maxResolution = new Tuple2<>(mode.width(), mode.height());
        var depths = li.cil.oc.api.internal.TextBuffer.ColorDepth.values();
        int ordinal = Math.max(0, Math.min(mode.depth(), depths.length - 1));
        maxDepth = depths[ordinal];
    }

    public boolean isDisplayingForPowerAccounting() { return isDisplaying; }
    public void setDisplayingForPowerAccounting(boolean value) { isDisplaying = value; }
    public boolean hasPowerForPowerAccounting() { return hasPower; }
    public void hasPowerForPowerAccounting_$eq(boolean value) { hasPower = value; }
    public double relativeLitAreaForPowerAccounting() { return relativeLitArea; }
    public void relativeLitAreaForPowerAccounting_$eq(double value) { relativeLitArea = value; }
    public void setPowerConsumptionPerTickForProxy(double value) { powerConsumptionPerTick = value; }
    public void setFullyLitCostForProxy(double value) { fullyLitCost = value; }
    public void invalidateLitAreaForProxy() { relativeLitArea = -1.0; }
    public Option<PacketBuilder> pendingCommandBufferForUpdate() { return pendingCommands; }
    public void clearPendingCommandBufferForUpdate() { pendingCommands = Option$.MODULE$.empty(); }
    public int syncCooldownForUpdate() { return syncCooldown; }
    public void syncCooldownForUpdate_$eq(int value) { syncCooldown = value; }
    public PacketBuilder pendingCommandsForProxy() { return pendingCommands(); }
    private PacketBuilder pendingCommands() {
        if (pendingCommands.isDefined()) return pendingCommands.get();
        PacketBuilder packet = new li.cil.oc.common.CompressedPacketBuilder(PacketType.TextBufferMulti);
        try {
            packet.writeUTF(node.address());
        } catch (IOException error) {
            throw sneaky(error);
        }
        pendingCommands = Option$.MODULE$.apply(packet);
        return packet;
    }

    public double computeFullyLitCost() {
        return TextBufferPowerJava.fullyLitCost(powerConsumptionPerTick, getMaximumWidth(), getMaximumHeight(),
            SettingsAccessJava.screenWidthAtTier(Tier.One), SettingsAccessJava.screenHeightAtTier(Tier.One));
    }
    public void markInitialized() { TextBufferUpdateJava.markInitialized(this); }
    public void requestSynchronization() { TextBufferUpdateJava.requestSynchronization(this, SideTracker.isClient()); }
    public boolean registerClientBufferOnLevel(Level level) { return TextBufferClientRegistry.registerClientBuffer(this, level); }
    public void unregisterClientBufferOnLevel(Level level) { TextBufferClientRegistry.unregisterClientBuffer(this, level); }
    public boolean isInitialized() { return TextBufferUpdateJava.isInitialized(this); }

    public Map<String, String> deviceInfo() {
        Map<String, String> result = deviceInfo;
        if (result == null) synchronized (this) {
            result = deviceInfo;
            if (result == null) {
                result = TextBufferDeviceInfo.create(((Number) maxResolution._1()).intValue() *
                    ((Number) maxResolution._2()).intValue(), maxDepth.ordinal());
                deviceInfo = result;
            }
        }
        return result;
    }
    @Override public Map<String, String> getDeviceInfo() { return deviceInfo(); }
    @Override public boolean canUpdate() { return true; }

    @Override public void update() {
        super.update();
        try { TextBufferPowerJava.updateAndSend(this, SettingsAccessJava.get().tickFrequency()); }
        catch (IOException error) { throw sneaky(error); }
        double wirelessRange = SettingsAccessJava.get().maxWirelessRange()[Tier.Two];
        TextBufferUpdateJava.flushPendingCommands(this, wirelessRange * wirelessRange);
        TextBufferUpdateJava.tickClientSync(this, SideTracker.isClient(), syncInterval);
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the screen is currently on.")
    public Object[] isOn(Context computer, Arguments args) { return TextBufferCallbacksJava.isOn(this); }
    @Callback(doc = "function():boolean -- Turns the screen on. Returns whether the state changed, and whether it is now on.")
    public Object[] turnOn(Context computer, Arguments args) { return TextBufferCallbacksJava.turnOn(this); }
    @Callback(doc = "function():boolean -- Turns off the screen. Returns whether the state changed, and whether it is now on.")
    public Object[] turnOff(Context computer, Arguments args) { return TextBufferCallbacksJava.turnOff(this); }
    @Callback(direct = true, doc = "function():number, number -- The aspect ratio of the screen. For multi-block screens this is the number of blocks, horizontal and vertical.")
    public Object[] getAspectRatio(Context context, Arguments args) { return TextBufferCallbacksJava.aspectRatio(this); }
    @Callback(doc = "function():table -- The list of keyboards attached to the screen.")
    public Object[] getKeyboards(Context context, Arguments args) { return TextBufferCallbacksJava.keyboards(this, context); }
    @Callback(direct = true, doc = "function():boolean -- Returns whether the screen is in high precision mode (sub-pixel mouse event positions).")
    public Object[] isPrecise(Context computer, Arguments args) { return TextBufferCallbacksJava.isPrecise(this); }
    @Callback(doc = "function(enabled:boolean):boolean -- Set whether to use high precision mode (sub-pixel mouse event positions).")
    public Object[] setPrecise(Context computer, Arguments args) { return TextBufferCallbacksJava.setPrecise(this, args); }
    @Callback(doc = "function():number -- Get the maximum resolution supported by the screen.")
    public Object[] hardwareResolution(Context context, Arguments args) { return TextBufferCallbacksJava.hardwareResolution(this); }
    @Callback(direct = true, doc = "function():number -- Get the maximum color depth supported by the screen.")
    public Object[] hardwareDepth(Context context, Arguments args) { return TextBufferCallbacksJava.hardwareDepth(this); }

    @Override public void setEnergyCostPerTick(double value) { TextBufferPowerJava.setEnergyCostPerTick(this, value); }
    @Override public double getEnergyCostPerTick() { return powerConsumptionPerTick; }
    @Override public void setPowerState(boolean value) {
        try { TextBufferPowerJava.setPowerStateAndSend(this, value, SettingsAccessJava.get().tickFrequency()); }
        catch (IOException error) { throw sneaky(error); }
    }
    @Override public boolean getPowerState() { return isDisplaying; }
    @Override public void setMaximumResolution(int width, int height) { TextBufferVideoModeJava.setMaximumResolution(this, width, height); }
    @Override public int getMaximumWidth() { return ((Number) maxResolution._1()).intValue(); }
    @Override public int getMaximumHeight() { return ((Number) maxResolution._2()).intValue(); }
    @Override public void setAspectRatio(double width, double height) { TextBufferVideoModeJava.setAspectRatio(this, width, height); }
    @Override public double getAspectRatio() { return TextBufferVideoModeJava.getAspectRatio(this); }
    @Override public boolean setResolution(int width, int height) { return TextBufferVideoModeJava.setResolution(this, width, height); }
    @Override public boolean setViewport(int width, int height) { return TextBufferVideoModeJava.setViewport(this, width, height); }
    @Override public int getViewportWidth() { return ((Number) viewport._1()).intValue(); }
    @Override public int getViewportHeight() { return ((Number) viewport._2()).intValue(); }
    @Override public void setMaximumColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) { TextBufferVideoModeJava.setMaximumColorDepth(this, depth); }
    @Override public li.cil.oc.api.internal.TextBuffer.ColorDepth getMaximumColorDepth() { return maxDepth; }
    @Override public boolean setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {
        return TextBufferVideoModeJava.afterColorDepthChange(this, depth, TextBufferProxy.super.setColorDepth(depth));
    }
    @Override public void onBufferPaletteChange(int index) { proxy.onBufferPaletteChange(index); }
    @Override public void onBufferColorChange() { proxy.onBufferColorChange(); }
    @Override public void onBufferCopy(int col, int row, int width, int height, int tx, int ty) { proxy.onBufferCopy(col, row, width, height, tx, ty); }
    @Override public void onBufferFill(int col, int row, int width, int height, int color) { proxy.onBufferFill(col, row, width, height, color); }
    @Override public void onBufferSet(int col, int row, String value, boolean vertical) { proxy.onBufferSet(col, row, value, vertical); }
    @Override public void onBufferBitBlt(int col, int row, int width, int height, GpuTextBuffer ram, int fromCol, int fromRow) { proxy.onBufferBitBlt(col, row, width, height, ram, fromCol, fromRow); }
    @Override public void onBufferRamInit(GpuTextBuffer ram) { proxy.onBufferRamInit(ram); }
    @Override public void onBufferRamDestroy(GpuTextBuffer ram) { proxy.onBufferRamDestroy(ram); }
    @Override public void rawSetText(int col, int row, int[][] text) {
        TextBufferProxy.super.rawSetText(col, row, text);
        proxy.onBufferRawSetText(col, row, text);
    }
    @Override public void rawSetBackground(int col, int row, int[][] colors) {
        TextBufferProxy.super.rawSetBackground(col, row, colors);
        proxy.onBufferRawSetBackground(col, row, colors);
    }
    @Override public void rawSetForeground(int col, int row, int[][] colors) {
        TextBufferProxy.super.rawSetForeground(col, row, colors);
        proxy.onBufferRawSetForeground(col, row, colors);
    }

    @OnlyIn(Dist.CLIENT) @Override public boolean renderText(PoseStack stack) { return TextBufferClientRenderingJava.render(this, stack); }
    @OnlyIn(Dist.CLIENT) public boolean renderText(PoseStack stack, MultiBufferSource buffer) { return TextBufferClientRenderingJava.render(this, stack, buffer); }
    @OnlyIn(Dist.CLIENT) @Override public int renderWidth() { return TextBufferClientRenderingJava.renderWidth(this); }
    @OnlyIn(Dist.CLIENT) @Override public int renderHeight() { return TextBufferClientRenderingJava.renderHeight(this); }
    @OnlyIn(Dist.CLIENT) @Override public void setRenderingEnabled(boolean enabled) { TextBufferClientRenderingJava.setRenderingEnabled(this, enabled); }
    @OnlyIn(Dist.CLIENT) @Override public boolean isRenderingEnabled() { return TextBufferClientRenderingJava.isRenderingEnabled(this); }
    @Override public void keyDown(char character, int code, Player player) { proxy.keyDown(character, code, player); }
    @Override public void keyUp(char character, int code, Player player) { proxy.keyUp(character, code, player); }
    @Override public void textInput(int codePoint, Player player) { proxy.textInput(codePoint, player); }
    @Override public void clipboard(String value, Player player) { proxy.clipboard(value, player); }
    @Override public void mouseDown(double x, double y, int button, Player player) { proxy.mouseDown(x, y, button, player); }
    @Override public void mouseDrag(double x, double y, int button, Player player) { proxy.mouseDrag(x, y, button, player); }
    @Override public void mouseUp(double x, double y, int button, Player player) { proxy.mouseUp(x, y, button, player); }
    @Override public void mouseScroll(double x, double y, int delta, Player player) { proxy.mouseScroll(x, y, delta, player); }
    public void copyToAnalyzer(int line, Player player) { proxy.copyToAnalyzer(line, player); }

    @Override public void onConnect(Node connected) { super.onConnect(connected); TextBufferComponentStateJava.onConnect(this, connected); }
    @Override public void onDisconnect(Node disconnected) { super.onDisconnect(disconnected); TextBufferComponentStateJava.onDisconnect(this, disconnected); }
    private void loadExternalData() {
        Level level = host.getEnvironmentLevel();
        if (level == null) { pendingExternalLoad = true; return; }
        pendingExternalLoad = false;
        TextBufferComponentStateJava.loadExternalData(this, level);
    }
    public boolean hasPendingExternalLoadForPersistence() { return pendingExternalLoad; }
    public void loadExternalDataForPersistence() { loadExternalData(); }

    @Override public void loadData(DataComponentHolder holder) {
        try {
            super.loadData(holder);
        } catch (UnrecoverablePersistanceException error) {
            throw sneaky(error);
        }
        ExtendedDataComponentHolder components = new ExtendedDataComponentHolder(holder);
        var address = components.getComponentOptional(OCComponentsJava.ADDRESS.get());
        if (address.isPresent()) {
            String addressValue = address.get();
            if (SideTracker.isClient()) {
                if (!Strings.isNullOrEmpty(proxy.nodeAddress())) return;
                proxy.nodeAddress_$eq(addressValue);
                requestSynchronization();
            } else if (components.getComponentOptional(OCComponentsJava.TEXT_BUFFER.get()).isPresent()) data.loadData(holder);
            else loadExternalData();
        }
        TextBufferComponentStateJava.load(this, holder);
    }

    @Override public void saveData(MutableDataComponentHolder holder) {
        if (node == null) return;
        super.saveData(holder);
        TextBufferComponentStateJava.pauseConnectedComputers(this);
        if (TextBufferComponentStateJava.shouldSaveInline(host)) data.saveData(holder);
        else TextBufferComponentStateJava.scheduleExternalSave(this, host);
        TextBufferComponentStateJava.saveState(this, holder);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneaky(Throwable error) throws E { throw (E) error; }
}
