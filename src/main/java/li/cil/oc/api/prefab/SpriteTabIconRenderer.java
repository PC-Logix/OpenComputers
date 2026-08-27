package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Simple implementation of a tab icon renderer using a sprite on the default texture atlas as its graphic.
 */
public class SpriteTabIconRenderer implements TabIconRenderer {
    private final ResourceLocation location;

    public SpriteTabIconRenderer(ResourceLocation location) {
        this.location = location;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics) {
        graphics.blitSprite(location, 0, 0, 16, 16);
    }
}
