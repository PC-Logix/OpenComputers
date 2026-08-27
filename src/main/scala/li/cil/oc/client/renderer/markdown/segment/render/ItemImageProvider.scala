package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.{ImageProvider, ImageRenderer}
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.{Items, ItemStack}

object ItemImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(data.toLowerCase))
    if (item != Items.AIR) new ItemStackImageRenderer(Array(new ItemStack(item)))
    else SpriteImageRenderer.missing("oc:gui.Manual.Warning.ItemMissing")
  }
}
