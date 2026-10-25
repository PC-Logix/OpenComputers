package li.cil.oc.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public final class TagImageProvider implements ImageProvider {
    public static final TagImageProvider INSTANCE = new TagImageProvider();

    private TagImageProvider() {}

    @Override
    public ImageRenderer getImage(String data) {
        var desired = ResourceLocation.tryParse(data.toLowerCase());
        var stacks = new ArrayList<ItemStack>();

        var itemTag = TagKey.create(BuiltInRegistries.ITEM.key(), desired);
        BuiltInRegistries.ITEM.getTag(itemTag).ifPresent(holders -> {
            for (var holder : holders) stacks.add(new ItemStack(holder.value()));
        });

        if (stacks.isEmpty()) {
            var blockTag = TagKey.create(BuiltInRegistries.BLOCK.key(), desired);
            BuiltInRegistries.BLOCK.getTag(blockTag).ifPresent(holders -> {
                for (var holder : holders) {
                    var item = holder.value().asItem();
                    if (item != null) stacks.add(new ItemStack(item));
                }
            });
        }

        return stacks.isEmpty()
                ? SpriteImageRenderer.missing("oc:gui.Manual.Warning.OreDictMissing")
                : new ItemStackImageRenderer(stacks.toArray(ItemStack[]::new));
    }
}
