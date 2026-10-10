package li.cil.oc.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.InteractiveImageRenderer;
import li.cil.oc.client.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public class SpriteImageRenderer implements ImageRenderer {
    private final TextureAtlasSprite sprite;

    public SpriteImageRenderer(ResourceLocation location) {
        sprite = Minecraft.getInstance().getGuiSprites().getSprite(location);
    }

    @Override
    public int getWidth() {
        return sprite.contents().width();
    }

    @Override
    public int getHeight() {
        return sprite.contents().height();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.blit(0, 0, 0, getWidth(), getHeight(), sprite);
    }

    public static SpriteImageRenderer missing(String tooltip) {
        return new MissingSpriteImageRenderer(tooltip);
    }

    private static final class MissingSpriteImageRenderer extends SpriteImageRenderer implements InteractiveImageRenderer {
        private final String tooltip;

        private MissingSpriteImageRenderer(String tooltip) {
            super(Textures.GUISprites$.MODULE$.ManualMissingItem());
            this.tooltip = tooltip;
        }

        @Override
        public String getTooltip(String oldTooltip) {
            return tooltip;
        }

        @Override
        public boolean onMouseClick(int mouseX, int mouseY) {
            return false;
        }
    }
}
