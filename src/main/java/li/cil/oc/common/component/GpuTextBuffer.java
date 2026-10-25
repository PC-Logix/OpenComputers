package li.cil.oc.common.component;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.internal.TextBuffer.ColorDepth;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.component.traits.TextBufferProxy;
import li.cil.oc.common.component.traits.VideoRamRasterizer;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.minecraft.world.entity.player.Player;

import java.io.InvalidObjectException;

public class GpuTextBuffer implements TextBufferProxy {
    private final String owner;
    private final int id;
    private final li.cil.oc.util.TextBuffer data;
    private boolean dirty = true;

    public GpuTextBuffer(String owner, int id, li.cil.oc.util.TextBuffer data) {
        this.owner = owner;
        this.id = id;
        this.data = data;
    }

    public String owner() { return owner; }
    public int id() { return id; }
    @Override public li.cil.oc.util.TextBuffer data() { return data; }
    public boolean dirty() { return dirty; }
    public void dirty_$eq(boolean value) { dirty = value; }

    @Override public Node node() {
        GpuTextBuffer.<RuntimeException>throwUnchecked(new InvalidObjectException("GpuTextBuffers do not have nodes"));
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable throwable) throws T {
        throw (T) throwable;
    }

    @Override public int getMaximumWidth() { return data.width(); }
    @Override public int getMaximumHeight() { return data.height(); }
    @Override public int getViewportWidth() { return data.height(); }
    @Override public int getViewportHeight() { return data.width(); }

    @Override public void onBufferSet(int col, int row, String value, boolean vertical) { dirty = true; }
    @Override public void onBufferColorChange() { dirty = true; }
    @Override public void onBufferCopy(int col, int row, int width, int height, int tx, int ty) { dirty = true; }
    @Override public void onBufferFill(int col, int row, int width, int height, int color) { dirty = true; }

    @Override public void loadData(DataComponentHolder holder) {
        data.loadData(holder);
        dirty = true;
    }

    @Override public void saveData(MutableDataComponentHolder holder) {
        data.saveData(holder);
        dirty = false;
    }

    @Override public void setEnergyCostPerTick(double value) { }
    @Override public double getEnergyCostPerTick() { return 0; }
    @Override public void setPowerState(boolean value) { }
    @Override public boolean getPowerState() { return false; }
    @Override public void setMaximumResolution(int width, int height) { }
    @Override public void setAspectRatio(double width, double height) { }
    @Override public double getAspectRatio() { return 1; }
    @Override public boolean setResolution(int width, int height) { return false; }
    @Override public boolean setViewport(int width, int height) { return false; }
    @Override public void setMaximumColorDepth(ColorDepth depth) { }
    @Override public ColorDepth getMaximumColorDepth() { return data.format().depth(); }
    @OnlyIn(Dist.CLIENT)
    @Override public boolean renderText(PoseStack stack) { return false; }
    @Override public int renderWidth() { return 0; }
    @Override public int renderHeight() { return 0; }
    @Override public void setRenderingEnabled(boolean enabled) { }
    @Override public boolean isRenderingEnabled() { return false; }
    @Override public void keyDown(char character, int code, Player player) { }
    @Override public void keyUp(char character, int code, Player player) { }
    @Override public void textInput(int codePoint, Player player) { }
    @Override public void clipboard(String value, Player player) { }
    @Override public void mouseDown(double x, double y, int button, Player player) { }
    @Override public void mouseDrag(double x, double y, int button, Player player) { }
    @Override public void mouseUp(double x, double y, int button, Player player) { }
    @Override public void mouseScroll(double x, double y, int delta, Player player) { }
    @Override public boolean canUpdate() { return false; }
    @Override public void update() { }
    @Override public void onConnect(Node node) { }
    @Override public void onDisconnect(Node node) { }
    @Override public void onMessage(Message message) { }

    public static GpuTextBuffer wrap(String owner, int id, li.cil.oc.util.TextBuffer data) {
        return new GpuTextBuffer(owner, id, data);
    }

    public static void bitblt(li.cil.oc.api.internal.TextBuffer dst, int col, int row, int width, int height,
                              li.cil.oc.api.internal.TextBuffer src, int fromCol, int fromRow) {
        int x = col - 1;
        int y = row - 1;
        int fx = fromCol - 1;
        int fy = fromRow - 1;
        int adjustedDstX = x;
        int adjustedDstY = y;
        int adjustedWidth = width;
        int adjustedHeight = height;
        int adjustedSourceX = fx;
        int adjustedSourceY = fy;

        if (x < 0) {
            adjustedWidth += x;
            adjustedSourceX -= x;
            adjustedDstX = 0;
        }
        if (y < 0) {
            adjustedHeight += y;
            adjustedSourceY -= y;
            adjustedDstY = 0;
        }
        if (adjustedSourceX < 0) {
            adjustedWidth += adjustedSourceX;
            adjustedDstX -= adjustedSourceX;
            adjustedSourceX = 0;
        }
        if (adjustedSourceY < 0) {
            adjustedHeight += adjustedSourceY;
            adjustedDstY -= adjustedSourceY;
            adjustedSourceY = 0;
        }

        adjustedWidth -= Math.max((adjustedDstX + adjustedWidth) - dst.getWidth(), 0);
        adjustedWidth -= Math.max((adjustedSourceX + adjustedWidth) - src.getWidth(), 0);
        adjustedHeight -= Math.max((adjustedDstY + adjustedHeight) - dst.getHeight(), 0);
        adjustedHeight -= Math.max((adjustedSourceY + adjustedHeight) - src.getHeight(), 0);
        if (adjustedWidth <= 0 || adjustedHeight <= 0) return;

        if (dst instanceof TextBuffer dstScreen) {
            if (src instanceof GpuTextBuffer srcRam) {
                write_vram_to_screen(dstScreen, adjustedDstX, adjustedDstY, adjustedWidth, adjustedHeight, srcRam, adjustedSourceX, adjustedSourceY);
                return;
            }
            throw new UnsupportedOperationException("Source buffer does not support bitblt operations to a screen");
        }
        if (dst instanceof GpuTextBuffer dstRam) {
            if (src instanceof TextBufferProxy srcProxy) {
                write_to_vram(dstRam, adjustedDstX, adjustedDstY, adjustedWidth, adjustedHeight, srcProxy, adjustedSourceX, adjustedSourceY);
                return;
            }
            throw new UnsupportedOperationException("Source buffer does not support bitblt operations");
        }
        throw new UnsupportedOperationException("Destination buffer does not support bitblt operations");
    }

    public static boolean write_vram_to_screen(TextBuffer dstScreen, int x, int y, int width, int height,
                                                GpuTextBuffer srcRam, int fromX, int fromY) {
        if (dstScreen.data().rawcopy(x + 1, y + 1, width, height, srcRam.data, fromX + 1, fromY + 1)) {
            dstScreen.addBuffer(srcRam);
            dstScreen.onBufferBitBlt(x + 1, y + 1, width, height, srcRam, fromX + 1, fromY + 1);
            return true;
        }
        return false;
    }

    public static boolean write_to_vram(GpuTextBuffer dstRam, int x, int y, int width, int height,
                                        TextBufferProxy src, int fromX, int fromY) {
        if (dstRam.data.rawcopy(x + 1, y + 1, width, height, src.data(), fromX + 1, fromY + 1)) {
            dstRam.dirty = true;
            return true;
        }
        return false;
    }
}