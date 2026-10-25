package li.cil.oc.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ItemImageProvider implements ImageProvider {
    public static final ItemImageProvider INSTANCE = new ItemImageProvider();

    private ItemImageProvider() {}

    @Override
    public ImageRenderer getImage(String data) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(data.toLowerCase()));
        return item != Items.AIR
                ? new ItemStackImageRenderer(new ItemStack[]{new ItemStack(item)})
                : SpriteImageRenderer.missing("oc:gui.Manual.Warning.ItemMissing");
    }
}
