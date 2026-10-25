package li.cil.oc.common.component;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.client.TextBufferClientPacketSender;
import li.cil.oc.client.renderer.TextBufferRenderCache;
import li.cil.oc.client.renderer.font.TextBufferRenderData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Client-side rendering and input proxy for a text buffer. */
public final class TextBufferClientProxy extends TextBufferProxyBase {
    private final TextBufferRenderData renderer = new TextBufferRenderData() {
        @Override public boolean dirty() { return TextBufferClientProxy.this.dirty(); }
        @Override public void dirty_$eq(boolean value) { TextBufferClientProxy.this.dirty_$eq(value); }
        @Override public li.cil.oc.util.TextBuffer data() { return owner().data(); }
        @Override public scala.Tuple2<Object, Object> viewport() { return owner().viewport(); }
    };

    private ItemInfo debugger;

    public TextBufferClientProxy(li.cil.oc.common.component.TextBuffer owner) {
        super(owner);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean render(PoseStack stack) {
        boolean wasDirty = dirty();
        TextBufferRenderCache.render(stack, renderer);
        return wasDirty;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean render(PoseStack stack, MultiBufferSource renderBuffer) {
        TextBufferRenderCache.renderImmediate(stack, renderBuffer, renderer);
        return dirty();
    }

    @Override public void onBufferColorChange() { setChanged(); }

    @Override
    public void onBufferCopy(int col, int row, int width, int height, int tx, int ty) {
        super.onBufferCopy(col, row, width, height, tx, ty);
        setChanged();
    }

    @Override public void onBufferDepthChange(TextBuffer.ColorDepth depth) { setChanged(); }

    @Override
    public void onBufferFill(int col, int row, int width, int height, int color) {
        super.onBufferFill(col, row, width, height, color);
        setChanged();
    }

    @Override public void onBufferPaletteChange(int index) { setChanged(); }

    @Override
    public void onBufferResolutionChange(int width, int height) {
        super.onBufferResolutionChange(width, height);
        setChanged();
    }

    @Override
    public void onBufferViewportResolutionChange(int width, int height) {
        super.onBufferViewportResolutionChange(width, height);
        setChanged();
    }

    @Override
    public void onBufferSet(int col, int row, String value, boolean vertical) {
        super.onBufferSet(col, row, value, vertical);
        setChanged();
    }

    @Override
    public void onBufferBitBlt(int col, int row, int width, int height, GpuTextBuffer ram, int fromCol, int fromRow) {
        super.onBufferBitBlt(col, row, width, height, ram, fromCol, fromRow);
        setChanged();
    }

    @Override public void onBufferRamInit(GpuTextBuffer ram) { super.onBufferRamInit(ram); }
    @Override public void onBufferRamDestroy(GpuTextBuffer ram) { super.onBufferRamDestroy(ram); }

    @Override
    public void keyDown(char character, int code, Player player) {
        debug("{type = keyDown, char = " + character + ", code = " + code + "}");
        TextBufferClientPacketSender.sendKeyDown(nodeAddress(), character, code);
    }

    @Override
    public void keyUp(char character, int code, Player player) {
        debug("{type = keyUp, char = " + character + ", code = " + code + "}");
        TextBufferClientPacketSender.sendKeyUp(nodeAddress(), character, code);
    }

    @Override
    public void textInput(int codePoint, Player player) {
        debug("{type = textInput, codePt = " + codePoint + "}");
        TextBufferClientPacketSender.sendTextInput(nodeAddress(), codePoint);
    }

    @Override
    public void clipboard(String value, Player player) {
        debug("{type = clipboard}");
        TextBufferClientPacketSender.sendClipboard(nodeAddress(), value);
    }

    @Override
    public void mouseDown(double x, double y, int button, Player player) {
        debug("{type = mouseDown, x = " + x + ", y = " + y + ", button = " + button + "}");
        TextBufferClientPacketSender.sendMouseClick(nodeAddress(), x, y, false, button);
    }

    @Override
    public void mouseDrag(double x, double y, int button, Player player) {
        debug("{type = mouseDrag, x = " + x + ", y = " + y + ", button = " + button + "}");
        TextBufferClientPacketSender.sendMouseClick(nodeAddress(), x, y, true, button);
    }

    @Override
    public void mouseUp(double x, double y, int button, Player player) {
        debug("{type = mouseUp, x = " + x + ", y = " + y + ", button = " + button + "}");
        TextBufferClientPacketSender.sendMouseUp(nodeAddress(), x, y, button);
    }

    @Override
    public void mouseScroll(double x, double y, int delta, Player player) {
        debug("{type = mouseScroll, x = " + x + ", y = " + y + ", delta = " + delta + "}");
        TextBufferClientPacketSender.sendMouseScroll(nodeAddress(), x, y, delta);
    }

    @Override
    public void copyToAnalyzer(int line, Player player) {
        TextBufferClientPacketSender.sendCopyToAnalyzer(nodeAddress(), line);
    }

    private void debug(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.player != null) {
            if (debugger == null) debugger = li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.Debugger());
            ItemStack held = minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
            if (li.cil.oc.api.Items.get(held) == debugger) {
                OpenComputers.log().info("[NETWORK DEBUGGER] Sending packet to node " + nodeAddress() + ": " + message);
            }
        }
    }
}
