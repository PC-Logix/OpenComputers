package li.cil.oc.client.renderer.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.Settings;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.util.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DynamicFontRenderer extends TextureFontRenderer implements ResourceManagerReloadListener {
    private final IGlyphProvider glyphProvider = new FontParserHex();
    private final ArrayList<CharTexture> textures = new ArrayList<>();
    private final Map<Integer, CharIcon> charMap = new HashMap<>();
    private int activeTextureIndex;

    public DynamicFontRenderer() {
        initialize();
        if (Minecraft.getInstance().getResourceManager() instanceof ReloadableResourceManager reloadable) {
            reloadable.registerReloadListener(this);
        }
    }

    public void initialize() {
        textures.forEach(CharTexture::delete);
        textures.clear();
        charMap.clear();
        glyphProvider.initialize();
        textures.add(new CharTexture(this));
        generateChars(basicChars.toCharArray());
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        initialize();
    }

    @Override protected int charWidth() { return glyphProvider.getGlyphWidth(); }
    @Override protected int charHeight() { return glyphProvider.getGlyphHeight(); }
    @Override protected int textureCount() { return textures.size(); }

    @Override
    protected RenderType selectType(int index) {
        activeTextureIndex = index;
        return textures.get(index).getType();
    }

    @Override
    protected void generateChar(int character) {
        if (!charMap.containsKey(character)) charMap.put(character, createCharIcon(character));
    }

    @Override
    protected void drawChar(VertexConsumer builder, Matrix4f matrix, int color, float tx, float ty, int character) {
        CharIcon icon = charMap.get(character);
        if (icon != null && icon.texture == textures.get(activeTextureIndex)) {
            icon.draw(builder, matrix, color, tx, ty);
        }
    }

    private CharIcon createCharIcon(int character) {
        if (FontUtils.wcwidth(character) < 1 || glyphProvider.getGlyph(character) == null) {
            if (character == '?') return null;
            if (!charMap.containsKey((int) '?')) charMap.put((int) '?', createCharIcon('?'));
            return charMap.get((int) '?');
        }
        CharTexture texture = textures.get(textures.size() - 1);
        if (texture.isFull(character)) {
            texture = new CharTexture(this);
            textures.add(texture);
        }
        return texture.add(character);
    }

    public static final class CharTexture {
        private static final int SIZE = 256;
        private final DynamicFontRenderer owner;
        private final DynamicTexture texture;
        private final net.minecraft.resources.ResourceLocation location;
        private final RenderType renderType;
        private final int cellWidth;
        private final int cellHeight;
        private final int cols;
        private final int rows;
        private final float uStep;
        private final float vStep;
        private final float pad = 1f / SIZE;
        private final int capacity;
        private int chars;

        private CharTexture(DynamicFontRenderer owner) {
            this.owner = owner;
            texture = new DynamicTexture(SIZE, SIZE, false);
            String name = "oc_font_cache_" + System.nanoTime();
            location = Minecraft.getInstance().getTextureManager().register(name, texture);
            renderType = RenderTypes.createFontTex(name, location, Settings.get().textAntiAlias());
            cellWidth = owner.charWidth() + 2;
            cellHeight = owner.charHeight() + 2;
            cols = SIZE / cellWidth;
            rows = SIZE / cellHeight;
            uStep = cellWidth / (float) SIZE;
            vStep = cellHeight / (float) SIZE;
            capacity = cols * rows;
        }

        public void delete() {
            texture.close();
            Minecraft.getInstance().getTextureManager().release(location);
        }

        public RenderType getType() { return renderType; }

        public boolean isFull(int character) {
            return chars + FontUtils.wcwidth(character) > capacity;
        }

        public CharIcon add(int character) {
            int glyphWidth = FontUtils.wcwidth(character);
            int width = owner.charWidth() * glyphWidth;
            int height = owner.charHeight();
            if (chars % cols + glyphWidth > cols) chars += cols - (chars % cols);
            int x = chars % cols;
            int y = chars / cols;

            ByteBuffer glyphData = owner.glyphProvider.getGlyph(character);
            NativeImage pixels = texture.getPixels();
            for (int glyphY = 0; glyphY < height; glyphY++) {
                for (int glyphX = 0; glyphX < width; glyphX++) {
                    int index = (glyphX + glyphY * width) * 4;
                    int red = glyphData.get(index) & 0xFF;
                    int green = glyphData.get(index + 1) & 0xFF;
                    int blue = glyphData.get(index + 2) & 0xFF;
                    int alpha = glyphData.get(index + 3) & 0xFF;
                    int abgr = (alpha << 24) | (blue << 16) | (green << 8) | red;
                    pixels.setPixelRGBA(1 + x * cellWidth + glyphX, 1 + y * cellHeight + glyphY, abgr);
                }
            }
            texture.upload();

            CharIcon icon = new CharIcon(this, width, height, pad + x * uStep, pad + y * vStep,
                (x + glyphWidth) * uStep - pad, (y + 1) * vStep - pad);
            chars += glyphWidth;
            return icon;
        }
    }

    public static final class CharIcon {
        private final CharTexture texture;
        private final int width;
        private final int height;
        private final float u1, v1, u2, v2;

        private CharIcon(CharTexture texture, int width, int height, float u1, float v1, float u2, float v2) {
            this.texture = texture;
            this.width = width;
            this.height = height;
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
        }

        private void draw(VertexConsumer builder, Matrix4f matrix, int color, float tx, float ty) {
            int red = (color >> 16) & 0xFF;
            int green = (color >> 8) & 0xFF;
            int blue = color & 0xFF;
            builder.addVertex(matrix, tx, ty + height, 0).setColor(red, green, blue, 255).setUv(u1, v2);
            builder.addVertex(matrix, tx + width, ty + height, 0).setColor(red, green, blue, 255).setUv(u2, v2);
            builder.addVertex(matrix, tx + width, ty, 0).setColor(red, green, blue, 255).setUv(u2, v1);
            builder.addVertex(matrix, tx, ty, 0).setColor(red, green, blue, 255).setUv(u1, v1);
        }
    }
}
