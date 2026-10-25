package li.cil.oc.client.renderer.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.util.ExtendedUnicodeHelper;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.TextBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

public abstract class TextureFontRenderer {
    protected final String basicChars = "☺☻♥♦♣♠•◘○◙♂♀♪♫☼►◄↕‼¶§▬↨↑↓→←∟↔▲▼ !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~⌂ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿⌐¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αßΓπΣσµτΦΘΩδ∞φε∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■";

    public int charRenderWidth() {
        return charWidth() / 2;
    }

    public int charRenderHeight() {
        return charHeight() / 2;
    }

    public void generateChars(char[] chars) {
        for (char value : chars) generateChar(value);
    }

    public void generateChars(int[] chars) {
        for (int value : chars) generateChar(value);
    }

    public void drawBuffer(PoseStack stack, MultiBufferSource renderBuffer, TextBuffer buffer, int viewportWidth, int viewportHeight) {
        PackedColor.ColorFormat format = buffer.format();
        stack.pushPose();
        stack.scale(0.5f, 0.5f, 1);

        VertexConsumer quadBuilder = null;
        for (int y = 0; y < Math.min(viewportHeight, buffer.height()); y++) {
            int[] colors = buffer.color[y];
            int background = 0;
            int x = 0;
            int width = 0;
            for (int packedColor : colors) {
                if (x + width >= viewportWidth) break;
                int color = PackedColor.unpackBackground(packedColor, format);
                if (color != background) {
                    if (background != 0 && width > 0) {
                        if (quadBuilder == null) quadBuilder = renderBuffer.getBuffer(RenderTypes.FONT_QUAD);
                        drawQuad(quadBuilder, stack.last().pose(), background, x, y, width);
                    }
                    background = color;
                    x += width;
                    width = 0;
                }
                width++;
            }
            if (background != 0 && width > 0) {
                if (quadBuilder == null) quadBuilder = renderBuffer.getBuffer(RenderTypes.FONT_QUAD);
                drawQuad(quadBuilder, stack.last().pose(), background, x, y, width);
            }
        }

        for (int texture = 0; texture < textureCount(); texture++) {
            VertexConsumer fontBuilder = null;
            for (int y = 0; y < Math.min(viewportHeight, buffer.height()); y++) {
                int[] line = buffer.buffer[y];
                int[] colors = buffer.color[y];
                int ty = y * charHeight();
                float tx = 0;
                for (int x = 0; x < viewportWidth; x++) {
                    int character = line[x];
                    if (character != ' ') {
                        if (fontBuilder == null) fontBuilder = renderBuffer.getBuffer(selectType(texture));
                        int color = PackedColor.unpackForeground(colors[x], format);
                        drawChar(fontBuilder, stack.last().pose(), color, tx, ty, character);
                    }
                    tx += charWidth();
                }
            }
        }
        stack.popPose();
    }

    public void drawString(PoseStack stack, String value, int x, int y) {
        int length = ExtendedUnicodeHelper.length(value);
        stack.pushPose();
        stack.translate((float) x, (float) y, 0f);
        stack.scale(0.5f, 0.5f, 1);
        RenderSystem.depthMask(false);

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(786432);
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(byteBuffer);
        for (int texture = 0; texture < textureCount(); texture++) {
            RenderType renderType = selectType(texture);
            VertexConsumer builder = bufferSource.getBuffer(renderType);
            float tx = 0;
            int charIndex = 0;
            for (int i = 0; i < length; i++) {
                int character = value.codePointAt(charIndex);
                if (character != ' ') drawChar(builder, stack.last().pose(), 0xFFFFFF, tx, 0f, character);
                tx += charWidth();
                charIndex = value.offsetByCodePoints(charIndex, 1);
            }
            bufferSource.endBatch(renderType);
        }

        byteBuffer.close();
        RenderSystem.depthMask(true);
        stack.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected abstract int charWidth();
    protected abstract int charHeight();
    protected abstract int textureCount();
    protected abstract RenderType selectType(int index);
    protected abstract void generateChar(int character);
    protected abstract void drawChar(VertexConsumer builder, Matrix4f matrix, int color, float tx, float ty, int character);

    private void drawQuad(VertexConsumer builder, Matrix4f matrix, int color, int x, int y, int width) {
        if (color == 0 || width <= 0) return;
        int x0 = x * charWidth();
        int x1 = (x + width) * charWidth();
        int y0 = y * charHeight();
        int y1 = (y + 1) * charHeight();
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        builder.addVertex(matrix, x0, y1, 0).setUv(0f, 1f).setColor(red, green, blue, 255);
        builder.addVertex(matrix, x1, y1, 0).setUv(1f, 1f).setColor(red, green, blue, 255);
        builder.addVertex(matrix, x1, y0, 0).setUv(1f, 0f).setColor(red, green, blue, 255);
        builder.addVertex(matrix, x0, y0, 0).setUv(0f, 0f).setColor(red, green, blue, 255);
    }
}
