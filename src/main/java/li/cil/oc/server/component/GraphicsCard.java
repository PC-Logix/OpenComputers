package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.Tier;
import li.cil.oc.common.component.GpuTextBuffer;
import li.cil.oc.common.component.traits.VideoRamDevice;
import li.cil.oc.common.component.traits.VideoRamRasterizer;
import li.cil.oc.common.datacomponents.CompoundStorage;
import li.cil.oc.common.datacomponents.GraphicsCardState;
import li.cil.oc.server.machine.Machine;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedUnicodeHelper;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.ResultWrapper;
import li.cil.oc.util.TextBuffer;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IMPORTANT: usually methods with side effects should not be direct callbacks
 * to avoid the synchronization problems that can cause during world saving.
 * Screen callbacks are an exception because they would otherwise be painfully
 * sluggish. Buffer saving waits for computers in the same network to finish
 * their current execution and then pauses them, so saved screen state matches
 * the computer state saved afterward.
 */
public class GraphicsCard extends AbstractManagedEnvironment implements DeviceInfo, VideoRamDevice {
    private static final int RESERVED_SCREEN_INDEX = 0;

    @FunctionalInterface
    private interface ScreenOperation {
        Object[] apply(li.cil.oc.api.internal.TextBuffer screen) throws Exception;
    }

    private final int tier;
    private final Option<Double> vramScreens;
    private final Visibility visibility;
    private final HashMap<Integer, GpuTextBuffer> videoRamBufferMap = new HashMap<>();
    private final Tuple2<Object, Object> maxResolution;
    private final Tuple2<Object, Object> defaultResolution;
    private final li.cil.oc.api.internal.TextBuffer.ColorDepth maxDepth;
    private Option<String> screenAddress = Option.empty();
    private Option<li.cil.oc.api.internal.TextBuffer> screenInstance = Option.empty();
    private int bufferIndex = RESERVED_SCREEN_INDEX;

    private final double[] setBackgroundCosts = {1.0 / 32, 1.0 / 64, 1.0 / 128, 1.0 / 256};
    private final double[] setForegroundCosts = {1.0 / 32, 1.0 / 64, 1.0 / 128, 1.0 / 256};
    private final double[] setPaletteColorCosts = {1.0 / 2, 1.0 / 8, 1.0 / 16, 1.0 / 32};
    private final double[] setCosts = {1.0 / 64, 1.0 / 128, 1.0 / 256, 1.0 / 512};
    private final double[] copyCosts = {1.0 / 16, 1.0 / 32, 1.0 / 64, 1.0 / 128};
    private final double[] fillCosts = {1.0 / 32, 1.0 / 64, 1.0 / 128, 1.0 / 256};
    private final double bitbltCost;
    private final double totalVRAM;
    private boolean budgetExhausted;
    private Map<String, String> deviceInfo;
    private Map<String, String> deviceInfoT4;

    public static Visibility $lessinit$greater$default$3() {
        return Visibility.Neighbors;
    }

    public static Option<Double> $lessinit$greater$default$2() {
        return Option.empty();
    }

    public GraphicsCard(int tier) {
        this(tier, Option.empty(), Visibility.Neighbors);
    }

    public GraphicsCard(int tier, Option<Double> vramScreens) {
        this(tier, vramScreens, Visibility.Neighbors);
    }

    public GraphicsCard(int tier, Option<Double> vramScreens, Visibility visibility) {
        this.tier = tier;
        this.vramScreens = vramScreens;
        this.visibility = visibility;
        this.maxResolution = Settings.get().screenResolutionsByTier()[tier];
        Tuple2<Object, Object> configured = Settings.get().defaultResolution();
        int maxWidth = first(maxResolution);
        int maxHeight = second(maxResolution);
        int configuredWidth = first(configured);
        int configuredHeight = second(configured);
        this.defaultResolution = new Tuple2<>(
            configuredWidth > 0 ? Math.min(maxWidth, configuredWidth) : maxWidth,
            configuredHeight > 0 ? Math.min(maxHeight, configuredHeight) : maxHeight);
        this.maxDepth = Settings.get().screenDepthsByTier()[tier];
        this.bitbltCost = Settings.get().bitbltCost() * Math.pow(2, tier);
        int vramIndex = Math.max(0, Math.min(tier, Settings.get().vramSizes().length - 1));
        double screenCount = vramScreens.isDefined() ? vramScreens.get() : Settings.get().vramSizes()[vramIndex];
        this.totalVRAM = (double) maxWidth * maxHeight * screenCount;

        setNode(Network.newNode(this, visibility)
            .withComponent("gpu")
            .withConnector()
            .create());
    }

    private static int first(Tuple2<Object, Object> tuple) {
        return ((Number) tuple._1()).intValue();
    }

    private static int second(Tuple2<Object, Object> tuple) {
        return ((Number) tuple._2()).intValue();
    }

    public int tier() { return tier; }
    public Option<Double> vramScreens() { return vramScreens; }
    public Visibility visibility() { return visibility; }

    @Override
    public ComponentConnector node() {
        return (ComponentConnector) super.node();
    }

    @Override
    public Map<Integer, GpuTextBuffer> videoRamBuffers() {
        return videoRamBufferMap;
    }

    public final double[] setBackgroundCosts() { return setBackgroundCosts; }
    public final double[] setForegroundCosts() { return setForegroundCosts; }
    public final double[] setPaletteColorCosts() { return setPaletteColorCosts; }
    public final double[] setCosts() { return setCosts; }
    public final double[] copyCosts() { return copyCosts; }
    public final double[] fillCosts() { return fillCosts; }
    public final double bitbltCost() { return bitbltCost; }
    public final double totalVRAM() { return totalVRAM; }
    public boolean budgetExhausted() { return budgetExhausted; }
    public void budgetExhausted_$eq(boolean value) { budgetExhausted = value; }

    private Object[] screen(int index, ScreenOperation operation) {
        if (index == RESERVED_SCREEN_INDEX) {
            if (screenInstance.isDefined()) {
                li.cil.oc.api.internal.TextBuffer screen = screenInstance.get();
                synchronized (screen) {
                    try {
                        return operation.apply(screen);
                    }
                    catch (Exception error) {
                        GraphicsCard.<RuntimeException>throwUnchecked(error);
                    }
                }
            }
            return new Object[]{null, "no screen"};
        }
        Optional<GpuTextBuffer> buffer = getBufferOptional(index);
        if (buffer.isPresent()) {
            try {
                return operation.apply(buffer.get());
            }
            catch (Exception error) {
                GraphicsCard.<RuntimeException>throwUnchecked(error);
            }
        }
        return new Object[]{null, "invalid buffer index"};
    }

    private Object[] screen(ScreenOperation operation) {
        return screen(bufferIndex, operation);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable error) throws T {
        throw (T) error;
    }

    private Map<String, String> deviceInfo() {
        if (deviceInfo == null) {
            synchronized (this) {
                if (deviceInfo == null) {
                    deviceInfo = Map.of(
                        DeviceAttribute.Class, DeviceClass.Display,
                        DeviceAttribute.Description, "Graphics controller",
                        DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                        DeviceAttribute.Product, "MPG" + ((tier + 1) * 1000) + " GTZ",
                        DeviceAttribute.Capacity, capacityInfo(),
                        DeviceAttribute.Width, widthInfo(),
                        DeviceAttribute.Clock, clockInfo());
                }
            }
        }
        return deviceInfo;
    }

    private Map<String, String> deviceInfoT4() {
        if (deviceInfoT4 == null) {
            synchronized (this) {
                if (deviceInfoT4 == null) {
                    deviceInfoT4 = Map.of(
                        DeviceAttribute.Class, DeviceClass.Display,
                        DeviceAttribute.Description, "Graphics controller",
                        DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.ViridiaComputronics(),
                        DeviceAttribute.Product, "VC VR4000XT",
                        DeviceAttribute.Capacity, capacityInfo(),
                        DeviceAttribute.Width, widthInfo(),
                        DeviceAttribute.Clock, clockInfo());
                }
            }
        }
        return deviceInfoT4;
    }

    public String capacityInfo() {
        return Integer.toString(first(maxResolution) * second(maxResolution));
    }

    public String widthInfo() {
        return new String[]{"1", "4", "8", "16"}[maxDepth.ordinal()];
    }

    public String clockInfo() {
        return clockRate(setBackgroundCosts) + "/" + clockRate(setForegroundCosts) + "/"
            + clockRate(setPaletteColorCosts) + "/" + clockRate(setCosts) + "/"
            + clockRate(copyCosts) + "/" + clockRate(fillCosts);
    }

    private int clockRate(double[] costs) {
        return (int) (2000 / costs[tier]) / 100;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return tier == Tier.Four ? deviceInfoT4() : deviceInfo();
    }

    private boolean resolveInvokeCosts(int index, Context context, double budgetCost, int units, double factor) {
        if (index == RESERVED_SCREEN_INDEX) {
            context.consumeCallBudget(budgetCost);
            return consumePower(units, factor);
        }
        return true;
    }

    @Callback(direct = true, doc = "function(): number -- returns the index of the currently selected buffer. 0 is reserved for the screen. Can return 0 even when there is no screen")
    public Object[] getActiveBuffer(Context context, Arguments args) {
        return ResultWrapper.result(bufferIndex);
    }

    @Callback(direct = true, doc = "function(index: number): number -- Sets the active buffer to `index`. 1 is the first vram buffer and 0 is reserved for the screen. returns nil for invalid index (0 is always valid)")
    public Object[] setActiveBuffer(Context context, Arguments args) {
        int previousIndex = bufferIndex;
        int newIndex = args.checkInteger(0);
        if (newIndex != RESERVED_SCREEN_INDEX && getBufferOptional(newIndex).isEmpty()) {
            return ResultWrapper.result(null, "invalid buffer index");
        }
        bufferIndex = newIndex;
        if (bufferIndex == RESERVED_SCREEN_INDEX) screen(s -> ResultWrapper.result(true));
        return ResultWrapper.result(previousIndex);
    }

    @Callback(direct = true, doc = "function(): number -- Returns an array of indexes of the allocated buffers")
    public Object[] buffers(Context context, Arguments args) {
        return ResultWrapper.result(bufferIndexes());
    }

    @Callback(direct = true, doc = "function([width: number, height: number]): number -- allocates a new buffer with dimensions width*height (defaults to max resolution) and appends it to the buffer list. Returns the index of the new buffer and returns nil with an error message on failure. A buffer can be allocated even when there is no screen bound to this gpu. Index 0 is always reserved for the screen and thus the lowest index of an allocated buffer is always 1.")
    public Object[] allocateBuffer(Context context, Arguments args) {
        int width = args.optInteger(0, first(maxResolution));
        int height = args.optInteger(1, second(maxResolution));
        int size = width * height;
        if (width <= 0 || height <= 0) return ResultWrapper.result(null, "invalid page dimensions: must be greater than zero");
        if (size > totalVRAM - calculateUsedMemory()) return ResultWrapper.result(null, "not enough video memory");
        if (node() == null) return ResultWrapper.result(null, "graphics card appears disconnected");
        PackedColor.ColorFormat format = PackedColor.Depth.format(Settings.get().screenDepthsByTier()[tier]);
        TextBuffer buffer = new TextBuffer(width, height, format);
        GpuTextBuffer page = GpuTextBuffer.wrap(node().address(), nextAvailableBufferIndex(), buffer);
        addBuffer(page);
        return ResultWrapper.result(page.id());
    }

    @Override
    public void onBufferRamDestroy(int id) {
        if (id != RESERVED_SCREEN_INDEX) {
            screen(RESERVED_SCREEN_INDEX, screen -> {
                if (screen instanceof VideoRamRasterizer rasterizer) {
                    return ResultWrapper.result(rasterizer.removeBuffer(node().address(), id));
                }
                return ResultWrapper.result(true);
            });
        }
        if (id == bufferIndex) bufferIndex = RESERVED_SCREEN_INDEX;
    }

    @Callback(direct = true, doc = "function(index: number): boolean -- Closes buffer at `index`. Returns true if a buffer closed. If the current buffer is closed, index moves to 0")
    public Object[] freeBuffer(Context context, Arguments args) {
        int index = args.optInteger(0, bufferIndex);
        if (removeBuffers(new int[]{index}) == 1) return ResultWrapper.result(true);
        return ResultWrapper.result(null, "no buffer at index");
    }

    @Callback(direct = true, doc = "function(): number -- Closes all buffers and returns the count. If the active buffer is closed, index moves to 0")
    public Object[] freeAllBuffers(Context context, Arguments args) {
        return ResultWrapper.result(removeAllBuffers());
    }

    @Callback(direct = true, doc = "function(): number -- returns the total memory size of the gpu vram. This does not include the screen.")
    public Object[] totalMemory(Context context, Arguments args) {
        return ResultWrapper.result(totalVRAM);
    }

    @Callback(direct = true, doc = "function(): number -- returns the total free memory not allocated to buffers. This does not include the screen.")
    public Object[] freeMemory(Context context, Arguments args) {
        return ResultWrapper.result(totalVRAM - calculateUsedMemory());
    }

    @Callback(direct = true, doc = "function(index: number): number, number -- returns the buffer size at index. Returns the screen resolution for index 0. returns nil for invalid indexes")
    public Object[] getBufferSize(Context context, Arguments args) {
        int index = args.optInteger(0, bufferIndex);
        return screen(index, screen -> ResultWrapper.result(screen.getWidth(), screen.getHeight()));
    }

    private double determineBitbltBudgetCost(li.cil.oc.api.internal.TextBuffer dst,
                                              li.cil.oc.api.internal.TextBuffer src) {
        if (src instanceof GpuTextBuffer page) {
            if (dst instanceof GpuTextBuffer) return 0.0;
            if (page.dirty()) {
                return bitbltCost * (src.getWidth() * src.getHeight())
                    / (first(maxResolution) * second(maxResolution));
            }
            return .001;
        }
        return 0.0;
    }

    private double determineBitbltEnergyCost(li.cil.oc.api.internal.TextBuffer dst) {
        return dst instanceof GpuTextBuffer ? 0 : Settings.get().gpuCopyCost() / 15;
    }

    @Callback(direct = true, doc = "function([dst: number, col: number, row: number, width: number, height: number, src: number, fromCol: number, fromRow: number]):boolean -- bitblt from buffer to screen. All parameters are optional. Writes to `dst` page in rectangle `x, y, width, height`, defaults to the bound screen and its viewport. Reads data from `src` page at `fx, fy`, default is the active page from position 1, 1")
    public Object[] bitblt(Context context, Arguments args) throws LimitReachedException {
        int dstIndex = args.optInteger(0, RESERVED_SCREEN_INDEX);
        return screen(dstIndex, dst -> {
            int col = args.optInteger(1, 1);
            int row = args.optInteger(2, 1);
            int width = args.optInteger(3, dst.getWidth());
            int height = args.optInteger(4, dst.getHeight());
            int srcIndex = args.optInteger(5, bufferIndex);
            return screen(srcIndex, src -> {
                int fromCol = args.optInteger(6, 1);
                int fromRow = args.optInteger(7, 1);
                double budgetCost = determineBitbltBudgetCost(dst, src);
                double energyCost = determineBitbltEnergyCost(dst);
                double tierCredit = (tier + 1) * .5;
                double overBudget = budgetCost - tierCredit;
                if (overBudget > 0) {
                    if (budgetExhausted) {
                        if (overBudget > tierCredit) {
                            double pauseNeeded = overBudget - tierCredit;
                            context.pause((pauseNeeded / tierCredit) / 20);
                        }
                        budgetCost = 0;
                    }
                    else {
                        budgetExhausted = true;
                        throw new LimitReachedException();
                    }
                }
                budgetExhausted = false;
                if (!resolveInvokeCosts(dstIndex, context, budgetCost, width * height, energyCost)) {
                    return ResultWrapper.result(null, "not enough energy");
                }
                if (dstIndex == srcIndex) {
                    int tx = col - fromCol;
                    int ty = row - fromRow;
                    dst.copy(fromCol - 1, fromRow - 1, width, height, tx, ty);
                }
                else {
                    GpuTextBuffer.bitblt(dst, col, row, width, height, src, fromCol, fromRow);
                }
                return ResultWrapper.result(true);
            });
        });
    }

    @Callback(doc = "function(address:string[, reset:boolean=true]):boolean -- Binds the GPU to the screen with the specified address and resets screen settings if `reset` is true.")
    public Object[] bind(Context context, Arguments args) {
        String address = args.checkString(0);
        boolean reset = args.optBoolean(1, true);
        Node connectedNode = node().network().node(address);
        if (connectedNode == null) return ResultWrapper.result(null, "invalid address");
        if (!(connectedNode.host() instanceof li.cil.oc.api.internal.TextBuffer connectedScreen)) {
            return ResultWrapper.result(null, "not a screen");
        }
        screenAddress = Option.apply(address);
        screenInstance = Option.apply(connectedScreen);
        return screen(screen -> {
            if (reset) {
                int gpuWidth = first(maxResolution);
                int gpuHeight = second(maxResolution);
                screen.setResolution(Math.min(gpuWidth, screen.getMaximumWidth()),
                    Math.min(gpuHeight, screen.getMaximumHeight()));
                screen.setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth.values()[
                    Math.min(maxDepth.ordinal(), screen.getMaximumColorDepth().ordinal())]);
                screen.setForegroundColor(0xFFFFFF);
                screen.setBackgroundColor(0x000000);
                if (screen instanceof VideoRamRasterizer rasterizer) rasterizer.removeAllBuffers();
            }
            else context.pause(0);
            return ResultWrapper.result(true);
        });
    }

    @Callback(direct = true, doc = "function():string -- Get the address of the screen the GPU is currently bound to.")
    public Object[] getScreen(Context context, Arguments args) {
        return screen(RESERVED_SCREEN_INDEX, screen -> ResultWrapper.result(screen.node().address()));
    }

    @Callback(direct = true, doc = "function():number, boolean -- Get the current background color and whether it's from the palette or not.")
    public Object[] getBackground(Context context, Arguments args) {
        return screen(screen -> ResultWrapper.result(screen.getBackgroundColor(), screen.isBackgroundFromPalette()));
    }

    @Callback(direct = true, doc = "function(value:number[, palette:boolean]):number, number or nil -- Sets the background color to the specified value. Optionally takes an explicit palette index. Returns the old value and if it was from the palette its palette index.")
    public Object[] setBackground(Context context, Arguments args) {
        int color = args.checkInteger(0);
        if (bufferIndex == RESERVED_SCREEN_INDEX) context.consumeCallBudget(setBackgroundCosts[tier]);
        return screen(screen -> {
            int oldValue = screen.getBackgroundColor();
            Object oldColor = screen.isBackgroundFromPalette() ? screen.getPaletteColor(oldValue) : oldValue;
            Object oldIndex = screen.isBackgroundFromPalette() ? oldValue : null;
            screen.setBackgroundColor(color, args.optBoolean(1, false));
            return ResultWrapper.result(oldColor, oldIndex);
        });
    }

    @Callback(direct = true, doc = "function():number, boolean -- Get the current foreground color and whether it's from the palette or not.")
    public Object[] getForeground(Context context, Arguments args) {
        return screen(screen -> ResultWrapper.result(screen.getForegroundColor(), screen.isForegroundFromPalette()));
    }

    @Callback(direct = true, doc = "function(value:number[, palette:boolean]):number, number or nil -- Sets the foreground color to the specified value. Optionally takes an explicit palette index. Returns the old value and if it was from the palette its palette index.")
    public Object[] setForeground(Context context, Arguments args) {
        int color = args.checkInteger(0);
        if (bufferIndex == RESERVED_SCREEN_INDEX) context.consumeCallBudget(setForegroundCosts[tier]);
        return screen(screen -> {
            int oldValue = screen.getForegroundColor();
            Object oldColor = screen.isForegroundFromPalette() ? screen.getPaletteColor(oldValue) : oldValue;
            Object oldIndex = screen.isForegroundFromPalette() ? oldValue : null;
            screen.setForegroundColor(color, args.optBoolean(1, false));
            return ResultWrapper.result(oldColor, oldIndex);
        });
    }

    @Callback(direct = true, doc = "function(index:number):number -- Get the palette color at the specified palette index.")
    public Object[] getPaletteColor(Context context, Arguments args) {
        int index = args.checkInteger(0);
        return screen(screen -> {
            try { return ResultWrapper.result(screen.getPaletteColor(index)); }
            catch (ArrayIndexOutOfBoundsException error) { throw new IllegalArgumentException("invalid palette index"); }
        });
    }

    @Callback(direct = true, doc = "function(index:number, color:number):number -- Set the palette color at the specified palette index. Returns the previous value.")
    public Object[] setPaletteColor(Context context, Arguments args) {
        int index = args.checkInteger(0);
        int color = args.checkInteger(1);
        if (bufferIndex == RESERVED_SCREEN_INDEX) {
            context.consumeCallBudget(setPaletteColorCosts[tier]);
            context.pause(0.1);
        }
        return screen(screen -> {
            try {
                int oldColor = screen.getPaletteColor(index);
                screen.setPaletteColor(index, color);
                return ResultWrapper.result(oldColor);
            }
            catch (ArrayIndexOutOfBoundsException error) {
                throw new IllegalArgumentException("invalid palette index");
            }
        });
    }

    @Callback(direct = true, doc = "function():number -- Returns the currently set color depth.")
    public Object[] getDepth(Context context, Arguments args) {
        return screen(screen -> ResultWrapper.result(PackedColor.Depth.bits(screen.getColorDepth())));
    }

    @Callback(doc = "function(depth:number):number -- Set the color depth. Returns the previous value.")
    public Object[] setDepth(Context context, Arguments args) {
        int depth = args.checkInteger(0);
        return screen(screen -> {
            li.cil.oc.api.internal.TextBuffer.ColorDepth oldDepth = screen.getColorDepth();
            switch (depth) {
                case 1 -> screen.setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth.OneBit);
                case 4 -> {
                    if (maxDepth.ordinal() >= li.cil.oc.api.internal.TextBuffer.ColorDepth.FourBit.ordinal())
                        screen.setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth.FourBit);
                    else throw new IllegalArgumentException("unsupported depth");
                }
                case 8 -> {
                    if (maxDepth.ordinal() >= li.cil.oc.api.internal.TextBuffer.ColorDepth.EightBit.ordinal())
                        screen.setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth.EightBit);
                    else throw new IllegalArgumentException("unsupported depth");
                }
                case 16 -> {
                    if (maxDepth.ordinal() >= li.cil.oc.api.internal.TextBuffer.ColorDepth.SixteenBit.ordinal())
                        screen.setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth.SixteenBit);
                    else throw new IllegalArgumentException("unsupported depth");
                }
                default -> throw new IllegalArgumentException("unsupported depth");
            }
            return ResultWrapper.result(oldDepth);
        });
    }

    @Callback(direct = true, doc = "function():number -- Get the maximum supported color depth by the current GPU+screen combo.")
    public Object[] maxDepth(Context context, Arguments args) {
        return screen(screen -> ResultWrapper.result(PackedColor.Depth.bits(
            li.cil.oc.api.internal.TextBuffer.ColorDepth.values()[
                Math.min(maxDepth.ordinal(), screen.getMaximumColorDepth().ordinal())])));
    }

    @Callback(direct = true, doc = "function():number -- Get the maximum color depth supported by the GPU.")
    public Object[] hardwareDepth(Context context, Arguments args) {
        return ResultWrapper.result(PackedColor.Depth.bits(maxDepth));
    }

    @Callback(direct = true, doc = "function():number, number -- Get the current screen resolution.")
    public Object[] getResolution(Context context, Arguments args) {
        return screen(screen -> ResultWrapper.result(screen.getWidth(), screen.getHeight()));
    }

    @Callback(doc = "function(width:number, height:number):boolean -- Set the screen resolution. Returns true if the resolution changed.")
    public Object[] setResolution(Context context, Arguments args) {
        int width = args.checkInteger(0);
        int height = args.checkInteger(1);
        int maxWidth = first(maxResolution);
        int maxHeight = second(maxResolution);
        if (width < 1 || height < 1 || width > maxWidth || height > maxWidth || height * width > maxWidth * maxHeight)
            throw new IllegalArgumentException("unsupported resolution");
        return screen(screen -> ResultWrapper.result(screen.setResolution(width, height)));
    }

    @Callback(direct = true, doc = "function():number, number -- Get the maximum screen resolution supported by the current GPU+screen combo.")
    public Object[] maxResolution(Context context, Arguments args) {
        return screen(screen -> ResultWrapper.result(
            Math.min(first(maxResolution), screen.getMaximumWidth()),
            Math.min(second(maxResolution), screen.getMaximumHeight())));
    }

    @Callback(direct = true, doc = "function():number, number -- Get the default screen resolution.")
    public Object[] getDefaultResolution(Context context, Arguments args) {
        return screen(screen -> ResultWrapper.result(
            Math.min(first(defaultResolution), screen.getMaximumWidth()),
            Math.min(second(defaultResolution), screen.getMaximumHeight())));
    }

    @Callback(direct = true, doc = "function():number, number -- Get the maximum screen resolution supported by the GPU.")
    public Object[] hardwareResolution(Context context, Arguments args) {
        return ResultWrapper.result(first(maxResolution), second(maxResolution));
    }

    @Callback(direct = true, doc = "function():number, number -- Get the current viewport resolution.")
    public Object[] getViewport(Context context, Arguments args) {
        return screen(screen -> ResultWrapper.result(screen.getViewportWidth(), screen.getViewportHeight()));
    }

    @Callback(doc = "function(width:number, height:number):boolean -- Set the viewport resolution. Cannot exceed the screen resolution. Returns true if the resolution changed.")
    public Object[] setViewport(Context context, Arguments args) {
        int width = args.checkInteger(0);
        int height = args.checkInteger(1);
        int maxWidth = first(maxResolution);
        int maxHeight = second(maxResolution);
        if (width < 1 || height < 1 || width > maxWidth || height > maxWidth || height * width > maxWidth * maxHeight)
            throw new IllegalArgumentException("unsupported viewport size");
        return screen(screen -> {
            if (width > screen.getWidth() || height > screen.getHeight())
                throw new IllegalArgumentException("unsupported viewport size");
            return ResultWrapper.result(screen.setViewport(width, height));
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number):string, number, number, number or nil, number or nil -- Get the value displayed on the screen at the specified index, as well as the foreground and background color. If the foreground or background is from the palette, returns the palette indices as fourth and fifth results, else nil, respectively.")
    public Object[] get(Context context, Arguments args) {
        int x = args.checkInteger(0) - 1;
        int y = args.checkInteger(1) - 1;
        return screen(screen -> {
            int fgValue = screen.getForegroundColor(x, y);
            Object fgColor = screen.isForegroundFromPalette(x, y) ? screen.getPaletteColor(fgValue) : fgValue;
            Object fgIndex = screen.isForegroundFromPalette(x, y) ? fgValue : null;
            int bgValue = screen.getBackgroundColor(x, y);
            Object bgColor = screen.isBackgroundFromPalette(x, y) ? screen.getPaletteColor(bgValue) : bgValue;
            Object bgIndex = screen.isBackgroundFromPalette(x, y) ? bgValue : null;
            String value = new StringBuilder().appendCodePoint(screen.getCodePoint(x, y)).toString();
            return ResultWrapper.result(value, fgColor, bgColor, fgIndex, bgIndex);
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, value:string[, vertical:boolean]):boolean -- Plots a string value to the screen at the specified position. Optionally writes the string vertically.")
    public Object[] set(Context context, Arguments args) {
        int x = args.checkInteger(0) - 1;
        int y = args.checkInteger(1) - 1;
        String value = args.checkString(2);
        boolean vertical = args.optBoolean(3, false);
        return screen(screen -> {
            if (!resolveInvokeCosts(bufferIndex, context, setCosts[tier], ExtendedUnicodeHelper.length(value), Settings.get().gpuSetCost()))
                return ResultWrapper.result(null, "not enough energy");
            screen.set(x, y, value, vertical);
            return ResultWrapper.result(true);
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, width:number, height:number, tx:number, ty:number):boolean -- Copies a portion of the screen from the specified location with the specified size by the specified translation.")
    public Object[] copy(Context context, Arguments args) {
        int x = args.checkInteger(0) - 1;
        int y = args.checkInteger(1) - 1;
        int width = Math.max(0, args.checkInteger(2));
        int height = Math.max(0, args.checkInteger(3));
        int tx = args.checkInteger(4);
        int ty = args.checkInteger(5);
        return screen(screen -> {
            if (!resolveInvokeCosts(bufferIndex, context, copyCosts[tier], width * height, Settings.get().gpuCopyCost()))
                return ResultWrapper.result(null, "not enough energy");
            screen.copy(x, y, width, height, tx, ty);
            return ResultWrapper.result(true);
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, width:number, height:number, char:string):boolean -- Fills a portion of the screen at the specified position with the specified size with the specified character.")
    public Object[] fill(Context context, Arguments args) throws Exception {
        int x = args.checkInteger(0) - 1;
        int y = args.checkInteger(1) - 1;
        int width = Math.max(0, args.checkInteger(2));
        int height = Math.max(0, args.checkInteger(3));
        String value = args.checkString(4);
        if (ExtendedUnicodeHelper.length(value) != 1) throw new Exception("invalid fill value");
        return screen(screen -> {
            int character = value.codePointAt(0);
            double cost = character == ' ' ? Settings.get().gpuClearCost() : Settings.get().gpuFillCost();
            if (!resolveInvokeCosts(bufferIndex, context, fillCosts[tier], width * height, cost))
                return ResultWrapper.result(null, "not enough energy");
            screen.fill(x, y, width, height, character);
            return ResultWrapper.result(true);
        });
    }

    private boolean consumePower(double units, double cost) {
        return node().tryChangeBuffer(-units * cost);
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (node().isNeighborOf(message.source())
            && (message.name().equals("computer.stopped") || message.name().equals("computer.started"))) {
            bufferIndex = RESERVED_SCREEN_INDEX;
            removeAllBuffers();
        }
        if (message.name().equals("computer.stopped") && node().isNeighborOf(message.source())) {
            screen(screen -> {
                int gpuWidth = first(maxResolution);
                int gpuHeight = second(maxResolution);
                screen.setResolution(Math.min(gpuWidth, screen.getMaximumWidth()),
                    Math.min(gpuHeight, screen.getMaximumHeight()));
                screen.setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth.values()[
                    Math.min(maxDepth.ordinal(), screen.getMaximumColorDepth().ordinal())]);
                screen.setForegroundColor(0xFFFFFF);
                int width = screen.getWidth();
                int height = screen.getHeight();
                Object host = message.source().host();
                if (host instanceof Machine machine && machine.lastError() != null) {
                    if (screen.getColorDepth().ordinal() > li.cil.oc.api.internal.TextBuffer.ColorDepth.OneBit.ordinal())
                        screen.setBackgroundColor(0x0000FF);
                    else screen.setBackgroundColor(0x000000);
                    screen.fill(0, 0, width, height, 0x20);
                    try {
                        Pattern wrap = Pattern.compile("(.{1," + Math.max(1, width - 2) + "})\\s");
                        Matcher matcher = wrap.matcher(Localization.localizeImmediately(machine.lastError()).replace("\t", "  ") + "\n");
                        StringBuffer wrapped = new StringBuffer();
                        while (matcher.find()) {
                            matcher.appendReplacement(wrapped, Matcher.quoteReplacement(matcher.group(1) + "\n"));
                        }
                        matcher.appendTail(wrapped);
                        String[] lines = wrapped.toString().lines().toArray(String[]::new);
                        int firstRow = Math.max((height - lines.length) / 2, 2);
                        String title = "Unrecoverable Error";
                        screen.set((width - title.length()) / 2, firstRow - 2, title, false);
                        int maxLineLength = Arrays.stream(lines).mapToInt(String::length).max().orElse(0);
                        int col = Math.max((width - maxLineLength) / 2, 0);
                        for (int index = 0; index < lines.length; index++) {
                            screen.set(col, firstRow + index, lines[index], false);
                        }
                    }
                    catch (Throwable error) {
                        error.printStackTrace();
                    }
                }
                else {
                    screen.setBackgroundColor(0x000000);
                    screen.fill(0, 0, width, height, 0x20);
                }
                return null;
            });
        }
    }

    @Override
    public void onConnect(Node connectedNode) {
        super.onConnect(connectedNode);
        if (screenInstance.isEmpty() && screenAddress.isDefined()
            && Objects.equals(screenAddress.get(), connectedNode.address())
            && connectedNode.host() instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            screenInstance = Option.apply(buffer);
        }
    }

    @Override
    public void onDisconnect(Node disconnectedNode) {
        super.onDisconnect(disconnectedNode);
        if (disconnectedNode == node() || (screenAddress.isDefined()
            && Objects.equals(screenAddress.get(), disconnectedNode.address()))) {
            screenAddress = Option.empty();
            screenInstance = Option.empty();
        }
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        DataComponentType<GraphicsCardState> graphicsCardType = OCComponentsJava.GRAPHICS_CARD.get();
        var state = new ExtendedDataComponentHolder(holder).getComponentOptional(graphicsCardType);
        if (state.isPresent()) {
            screenAddress = state.get().screen();
            screenInstance = Option.empty();
            bufferIndex = state.get().bufferIndex();
        }
        removeAllBuffers();
        DataComponentType<?> videoRamType = OCComponentsJava.VIDEO_RAM.get();
        var savedBuffers = new ExtendedDataComponentHolder(holder).getComponentOptional(videoRamType);
        if (savedBuffers.isPresent()) {
            scala.collection.Iterator<?> iterator = ((scala.collection.Iterable<?>) savedBuffers.get()).iterator();
            while (iterator.hasNext()) {
                Tuple2<?, ?> entry = (Tuple2<?, ?>) iterator.next();
                int index = ((Number) entry._1()).intValue();
                loadBuffer(node().address(), index, (CompoundStorage) entry._2());
            }
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        holder.set(OCComponentsJava.GRAPHICS_CARD.get(), new GraphicsCardState(screenAddress, bufferIndex));
        List<Tuple2<Object, CompoundStorage>> savedBuffers = new ArrayList<>();
        for (int index : bufferIndexes()) {
            Optional<GpuTextBuffer> page = getBufferOptional(index);
            if (page.isPresent()) {
                CompoundStorage storage = new CompoundStorage();
                page.get().data().saveData(storage);
                savedBuffers.add(new Tuple2<>(index, storage));
            }
        }
        scala.collection.immutable.List<?> rawBuffers =
            scala.jdk.javaapi.CollectionConverters.asScala(savedBuffers).toList();
        @SuppressWarnings("unchecked")
        scala.collection.immutable.List<Tuple2<Integer, CompoundStorage>> scalaBuffers =
            (scala.collection.immutable.List<Tuple2<Integer, CompoundStorage>>) (scala.collection.immutable.List<?>) rawBuffers;
        holder.set(OCComponentsJava.VIDEO_RAM.get(), scalaBuffers);
    }
}
