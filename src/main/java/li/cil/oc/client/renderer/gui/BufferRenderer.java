package li.cil.oc.client.renderer.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.client.Textures;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public final class BufferRenderer {
    public static final int margin = 7;
    public static final int innerMargin = 1;

    private BufferRenderer() {}

    public static void drawBackground(PoseStack pose, int bufferWidth, int bufferHeight) {
        drawBackground(pose, bufferWidth, bufferHeight, false);
    }

    public static void drawBackground(PoseStack pose, int bufferWidth, int bufferHeight, boolean forRobot) {
        RenderState.checkError(BufferRenderer.class.getName() + ".drawBackground: entering (aka: wasntme)");
        int innerWidth = innerMargin * 2 + bufferWidth;
        int innerHeight = innerMargin * 2 + bufferHeight;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        li.cil.oc.client.Textures.bind(Textures.GUI.Borders);

        var tessellator = Tesselator.getInstance();
        var vertices = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float border = forRobot ? 2f : 7f;
        float innerWidthF = innerWidth;
        float innerHeightF = innerHeight;
        float c0 = forRobot ? 5f : 0f;
        float c1 = forRobot ? 7f : 7f;
        float c2 = forRobot ? 9f : 9f;
        float c3 = forRobot ? 11f : 16f;

        Matrix4f matrix = pose.last().pose();
        drawQuad(matrix, vertices, 0, 0, border, border, c0, c0, c1, c1);
        drawQuad(matrix, vertices, border, 0, innerWidthF, border, c1 + 0.25f, c0, c2 - 0.25f, c1);
        drawQuad(matrix, vertices, border + innerWidthF, 0, border, border, c2, c0, c3, c1);
        drawQuad(matrix, vertices, 0, border, border, innerHeightF, c0, c1 + 0.25f, c1, c2 - 0.25f);
        drawQuad(matrix, vertices, border, border, innerWidthF, innerHeightF, c1 + 0.25f, c1 + 0.25f, c2 - 0.25f, c2 - 0.25f);
        drawQuad(matrix, vertices, border + innerWidthF, border, border, innerHeightF, c2, c1 + 0.25f, c3, c2 - 0.25f);
        drawQuad(matrix, vertices, 0, border + innerHeightF, border, border, c0, c2, c1, c3);
        drawQuad(matrix, vertices, border, border + innerHeightF, innerWidthF, border, c1 + 0.25f, c2, c2 - 0.25f, c3);
        drawQuad(matrix, vertices, border + innerWidthF, border + innerHeightF, border, border, c2, c2, c3, c3);
        BufferUploader.drawWithShader(vertices.buildOrThrow());
        RenderState.checkError(BufferRenderer.class.getName() + ".drawBackground: leaving");
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer vertices, float x, float y, float width, float height,
                                 float u1, float v1, float u2, float v2) {
        u1 /= 16f;
        u2 /= 16f;
        v1 /= 16f;
        v2 /= 16f;
        vertices.addVertex(matrix, x, y + height, 0).setUv(u1, v2);
        vertices.addVertex(matrix, x + width, y + height, 0).setUv(u2, v2);
        vertices.addVertex(matrix, x + width, y, 0).setUv(u2, v1);
        vertices.addVertex(matrix, x, y, 0).setUv(u1, v1);
    }

    public static void drawText(PoseStack pose, TextBuffer screen) {
        screen.renderText(pose);
    }
}
