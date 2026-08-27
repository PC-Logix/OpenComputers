package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.{ImageProvider, ImageRenderer}
import net.minecraft.resources.ResourceLocation

object TextureImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    Option(ResourceLocation.tryParse(data.toLowerCase))
      .flatMap(TextureImageRenderer.load)
      .getOrElse(SpriteImageRenderer.missing("oc:gui.Manual.Warning.ImageMissing"))
  }
}
