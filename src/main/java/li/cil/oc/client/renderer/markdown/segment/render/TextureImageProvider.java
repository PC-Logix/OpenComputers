package li.cil.oc.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.resources.ResourceLocation;

public final class TextureImageProvider implements ImageProvider {
    public static final TextureImageProvider INSTANCE = new TextureImageProvider();

    private TextureImageProvider() {}

    @Override
    public ImageRenderer getImage(String data) {
        var location = ResourceLocation.tryParse(data.toLowerCase());
        if (location != null) {
            var renderer = TextureImageRenderer.load(location);
            if (renderer.isPresent()) return renderer.get();
        }
        return SpriteImageRenderer.missing("oc:gui.Manual.Warning.ImageMissing");
    }
}
