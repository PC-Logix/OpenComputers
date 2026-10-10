package li.cil.oc.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import scala.Option;
import scala.Some;

import java.io.IOException;
import java.io.UncheckedIOException;

public class TextureImageRenderer implements ImageRenderer {
    private final ResourceLocation location;
    private final int width;
    private final int height;

    private TextureImageRenderer(ResourceLocation location, int width, int height) {
        this.location = location;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.blit(location, 0, 0, 0, 0, width, height, width, height);
    }

    public static Option<TextureImageRenderer> load(ResourceLocation location) {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        AbstractTexture current = manager.getTexture(location, MissingTextureAtlasSprite.getTexture());
        ImageTexture image;
        if (current instanceof ImageTexture existing) {
            image = existing;
        } else {
            image = new ImageTexture(location);
            manager.register(location, image);
            if (manager.getTexture(location) != image) return Option.empty();
        }
        return new Some<>(new TextureImageRenderer(location, image.width, image.height));
    }

    private static final class ImageTexture extends SimpleTexture {
        private int width;
        private int height;

        private ImageTexture(ResourceLocation location) {
            super(location);
        }

        @Override
        public TextureImage getTextureImage(ResourceManager resourceManager) {
            try {
                TextureImage texture = super.getTextureImage(resourceManager);
                width = texture.getImage().getWidth();
                height = texture.getImage().getHeight();
                return texture;
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
    }
}
