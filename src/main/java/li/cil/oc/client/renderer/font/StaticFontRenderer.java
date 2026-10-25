package li.cil.oc.client.renderer.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class StaticFontRenderer extends TextureFontRenderer {
    private final String chars;
    private final int charWidth;
    private final int charHeight;
    private final int cols;
    private final float uStep;
    private final float vStep;
    private final float vSize;
    private final float scale;
    private final float dw;
    private final float dh;

    public StaticFontRenderer() {
        FontMetadata metadata = readMetadata();
        chars = metadata.chars();
        charWidth = metadata.width();
        charHeight = metadata.height();
        cols = 256 / charWidth;
        uStep = charWidth / 256f;
        vStep = (charHeight + 1) / 256f;
        vSize = charHeight / 256f;
        scale = (float) Settings.get().fontCharScale();
        dw = charWidth * scale - charWidth;
        dh = charHeight * scale - charHeight;
    }

    private FontMetadata readMetadata() {
        try {
            var location = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), "textures/font/chars.txt");
            var resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                try (var reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
                    String characters = reader.readLine();
                    if (characters == null) throw new IllegalStateException("Font metadata is empty");
                    String dimensions = reader.readLine();
                    if (dimensions != null) {
                        String[] size = dimensions.split(" ", 2);
                        return new FontMetadata(characters, Integer.parseInt(size[0]), Integer.parseInt(size[1]));
                    }
                    return new FontMetadata(characters, 10, 18);
                }
            }
        } catch (Throwable error) {
            OpenComputers.log().warn("Failed reading font metadata, using defaults.", error);
        }
        return new FontMetadata(basicChars, 10, 18);
    }

    @Override public int charWidth() { return charWidth; }
    @Override public int charHeight() { return charHeight; }
    @Override public int textureCount() { return 1; }

    @Override
    public RenderType selectType(int index) {
        boolean antialias = Settings.get().textAntiAlias();
        var location = antialias ? Textures.Font.AntiAliased : Textures.Font.Aliased;
        return RenderTypes.createFontTex(location.getPath(), location, antialias);
    }

    @Override public void generateChar(int codePoint) {}

    @Override
    public void drawChar(VertexConsumer builder, Matrix4f matrix, int color, float tx, float ty, int codePoint) {
        int index = chars.indexOf(codePoint);
        if (index < 0) index = chars.indexOf('?');
        int x = index % cols;
        int y = index / cols;
        float u = x * uStep;
        float v = y * vStep;
        float red = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >> 8) & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;
        builder.addVertex(matrix, tx - dw, ty + charHeight * scale, 0).setColor(red, green, blue, 1f).setUv(u, v + vSize);
        builder.addVertex(matrix, tx + charWidth * scale, ty + charHeight * scale, 0).setColor(red, green, blue, 1f).setUv(u + uStep, v + vSize);
        builder.addVertex(matrix, tx + charWidth * scale, ty - dh, 0).setColor(red, green, blue, 1f).setUv(u + uStep, v);
        builder.addVertex(matrix, tx - dw, ty - dh, 0).setColor(red, green, blue, 1f).setUv(u, v);
    }

    private record FontMetadata(String chars, int width, int height) {}
}
