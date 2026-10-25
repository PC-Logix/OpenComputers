package li.cil.oc.common.component;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.common.component.traits.TextBufferProxy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Shared proxy state and invalidation defaults for client and server text buffers. */
public abstract class TextBufferProxyBase {
    private final li.cil.oc.common.component.TextBuffer owner;
    private boolean dirty;
    private String nodeAddress = "";

    protected TextBufferProxyBase(li.cil.oc.common.component.TextBuffer owner) {
        this.owner = owner;
    }

    public final li.cil.oc.common.component.TextBuffer owner() { return owner; }
    public final boolean dirty() { return dirty; }
    public final void dirty_$eq(boolean value) { dirty = value; }
    public final String nodeAddress() { return nodeAddress; }
    public final void nodeAddress_$eq(String value) { nodeAddress = value; }

    public void setChanged() { dirty = true; }

    @OnlyIn(Dist.CLIENT)
    public boolean render(PoseStack stack) { return false; }

    public abstract void onBufferColorChange();

    public void onBufferCopy(int col, int row, int width, int height, int tx, int ty) {
        owner.invalidateLitAreaForProxy();
    }

    public abstract void onBufferDepthChange(TextBuffer.ColorDepth depth);

    public void onBufferFill(int col, int row, int width, int height, int color) {
        owner.invalidateLitAreaForProxy();
    }

    public abstract void onBufferPaletteChange(int index);

    public void onBufferResolutionChange(int width, int height) {
        owner.invalidateLitAreaForProxy();
    }

    public void onBufferViewportResolutionChange(int width, int height) {
        owner.invalidateLitAreaForProxy();
    }

    public void onBufferMaxResolutionChange(int width, int height) { }

    public void onBufferSet(int col, int row, String value, boolean vertical) {
        owner.invalidateLitAreaForProxy();
    }

    public void onBufferBitBlt(int col, int row, int width, int height, GpuTextBuffer ram, int fromCol, int fromRow) {
        owner.invalidateLitAreaForProxy();
    }

    public void onBufferRamInit(GpuTextBuffer ram) {
        owner.invalidateLitAreaForProxy();
    }

    public void onBufferRamDestroy(GpuTextBuffer ram) {
        owner.invalidateLitAreaForProxy();
    }

    public void onBufferRawSetText(int col, int row, int[][] text) {
        owner.invalidateLitAreaForProxy();
    }

    public void onBufferRawSetBackground(int col, int row, int[][] color) {
        owner.invalidateLitAreaForProxy();
    }

    public void onBufferRawSetForeground(int col, int row, int[][] color) {
        owner.invalidateLitAreaForProxy();
    }

    public abstract void keyDown(char character, int code, Player player);
    public abstract void keyUp(char character, int code, Player player);
    public abstract void textInput(int codePoint, Player player);
    public abstract void clipboard(String value, Player player);
    public abstract void mouseDown(double x, double y, int button, Player player);
    public abstract void mouseDrag(double x, double y, int button, Player player);
    public abstract void mouseUp(double x, double y, int button, Player player);
    public abstract void mouseScroll(double x, double y, int delta, Player player);
    public abstract void copyToAnalyzer(int line, Player player);
}
