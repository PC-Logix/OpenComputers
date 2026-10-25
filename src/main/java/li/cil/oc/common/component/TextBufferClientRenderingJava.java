package li.cil.oc.common.component;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.client.renderer.TextBufferRenderCache;
import li.cil.oc.client.renderer.font.TextureFontRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Client rendering entry points for text-buffer displays. */
final class TextBufferClientRenderingJava {
    private TextBufferClientRenderingJava() { }

    @OnlyIn(Dist.CLIENT)
    static boolean render(TextBuffer screen, PoseStack stack) {
        return screen.relativeLitAreaForPowerAccounting() != 0 && screen.proxy().render(stack);
    }

    @OnlyIn(Dist.CLIENT)
    static boolean render(TextBuffer screen, PoseStack stack, MultiBufferSource renderBuffer) {
        if (screen.relativeLitAreaForPowerAccounting() == 0) return false;
        if (screen.proxy() instanceof TextBufferClientProxy client) return client.render(stack, renderBuffer);
        return screen.proxy().render(stack);
    }

    @OnlyIn(Dist.CLIENT)
    static int renderWidth(TextBuffer screen) {
        return renderer().charRenderWidth() * screen.getViewportWidth();
    }

    @OnlyIn(Dist.CLIENT)
    static int renderHeight(TextBuffer screen) {
        return renderer().charRenderHeight() * screen.getViewportHeight();
    }

    @OnlyIn(Dist.CLIENT)
    static void setRenderingEnabled(TextBuffer screen, boolean enabled) {
        screen.setRenderingEnabledForClient(enabled);
    }

    @OnlyIn(Dist.CLIENT)
    static boolean isRenderingEnabled(TextBuffer screen) {
        return screen.isRenderingEnabledForClient();
    }

    @OnlyIn(Dist.CLIENT)
    private static TextureFontRenderer renderer() {
        return TextBufferRenderCache.renderer();
    }
}
