package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.ImageRenderer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.texture.{MissingTextureAtlasSprite, SimpleTexture}
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager

class TextureImageRenderer private(private val location: ResourceLocation, private val width: Int, private val height: Int) extends ImageRenderer {
  override def getWidth: Int = width

  override def getHeight: Int = height

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {
    graphics.blit(location, 0, 0, 0, 0, width, height, width, height)
  }
}

object TextureImageRenderer {
  def load(location: ResourceLocation): Option[TextureImageRenderer] = {
    val manager = Minecraft.getInstance.getTextureManager
    val image = manager.getTexture(location, MissingTextureAtlasSprite.getTexture) match {
      case image: ImageTexture => image
      case _ =>
        val image = new ImageTexture(location)
        manager.register(location, image)
        // If the image fails to load then it won't be registered. Abort.
        if (manager.getTexture(location) != image) return None
        image
    }

    Some(new TextureImageRenderer(location, image.width, image.height))
  }

  private class ImageTexture(resLoc: ResourceLocation) extends SimpleTexture(resLoc) {
    var width: Int = 0
    var height: Int = 0

    override def getTextureImage(resourceManager: ResourceManager): SimpleTexture.TextureImage = {
      val texture = super.getTextureImage(resourceManager)
      width = texture.getImage.getWidth
      height = texture.getImage.getHeight
      texture
    }
  }
}
