package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.{ImageProvider, ImageRenderer}
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.{Items, ItemStack}

object BlockImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val item = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(data.toLowerCase)).asItem()
    if (item != Items.AIR) new ItemStackImageRenderer(Array(new ItemStack(item)))
    else SpriteImageRenderer.missing("oc:gui.Manual.Warning.BlockMissing")
  }
}
