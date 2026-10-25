package li.cil.oc.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.Settings;
import li.cil.oc.client.renderer.font.DynamicFontRenderer;
import li.cil.oc.client.renderer.font.StaticFontRenderer;
import li.cil.oc.client.renderer.font.TextBufferRenderData;
import li.cil.oc.client.renderer.font.TextureFontRenderer;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;

public final class TextBufferRenderCache {
    public static final TextureFontRenderer renderer = Settings.get().fontRenderer().equals("texture")
            ? new StaticFontRenderer() : new DynamicFontRenderer();

    private TextBufferRenderCache() {}

    public static TextureFontRenderer renderer() {
        return renderer;
    }

    public static void render(PoseStack stack, TextBufferRenderData buffer) {
        RenderState.checkError(TextBufferRenderCache.class.getName() + ".render: entering");
        renderDirect(stack, null, buffer);
        RenderState.checkError(TextBufferRenderCache.class.getName() + ".render: leaving");
    }

    public static void renderImmediate(PoseStack stack, MultiBufferSource renderBuffer, TextBufferRenderData buffer) {
        RenderState.checkError(TextBufferRenderCache.class.getName() + ".renderImmediate: entering");
        renderDirect(stack, renderBuffer, buffer);
        RenderState.checkError(TextBufferRenderCache.class.getName() + ".renderImmediate: leaving");
    }

    private static void renderDirect(PoseStack stack, MultiBufferSource renderBuffer, TextBufferRenderData buffer) {
        for (int[] line : buffer.data().buffer) renderer.generateChars(line);

        if (renderBuffer == null) {
            // Keep the pre-1.13 immediate-mode rendering path for GUI text buffers.
            ByteBufferBuilder byteBuffer = new ByteBufferBuilder(786432);
            try {
                MultiBufferSource.BufferSource source = MultiBufferSource.immediate(byteBuffer);
                renderer.drawBuffer(stack, source, buffer.data(), ((Number) buffer.viewport()._1()).intValue(),
                        ((Number) buffer.viewport()._2()).intValue());
                source.endBatch();
            } finally {
                byteBuffer.close();
            }
        } else {
            renderer.drawBuffer(stack, renderBuffer, buffer.data(), ((Number) buffer.viewport()._1()).intValue(),
                    ((Number) buffer.viewport()._2()).intValue());
        }
        buffer.dirty_$eq(false);
    }
}
