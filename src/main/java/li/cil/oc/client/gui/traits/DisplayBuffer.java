package li.cil.oc.client.gui.traits;

import li.cil.oc.util.RenderState;
import net.minecraft.client.gui.GuiGraphics;

/** Draws a scaled text buffer in a GUI screen. */
public interface DisplayBuffer {
    int bufferX();

    int bufferY();

    int bufferColumns();

    int bufferRows();

    double scale();

    void scale_$eq(double value);

    void drawBuffer(GuiGraphics graphics);

    double changeSize(double width, double height);

    default void drawBufferLayer(GuiGraphics graphics) {
        scale_$eq(changeSize(bufferColumns(), bufferRows()));

        RenderState.checkError(getClass().getName() + ".drawBufferLayer: entering (aka: wasntme)");

        graphics.pose().pushPose();
        drawBuffer(graphics);
        graphics.pose().popPose();

        RenderState.checkError(getClass().getName() + ".drawBufferLayer: buffer layer");
    }
}
