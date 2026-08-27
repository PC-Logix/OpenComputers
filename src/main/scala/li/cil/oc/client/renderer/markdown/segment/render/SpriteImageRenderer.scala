package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.{ImageRenderer, InteractiveImageRenderer}
import li.cil.oc.client.Textures
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.ResourceLocation

/**
 * An [[ImageRenderer]] that renders a GUI sprite.
 *
 * @param location The name of the GUI sprite.
 */
class SpriteImageRenderer(location: ResourceLocation) extends ImageRenderer {
  private val sprite: TextureAtlasSprite = Minecraft.getInstance.getGuiSprites.getSprite(location)

  override def getWidth: Int = sprite.contents.width

  override def getHeight: Int = sprite.contents.height

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = graphics.blit(0, 0, 0, getWidth, getHeight, sprite)
}

object SpriteImageRenderer {
  /**
   * A custom [[SpriteImageRenderer]] that is used when the original image is missing.
   *
   * @param tooltip The tooltip describing why the image is missing.
   * @return The image renderer instance.
   */
  def missing(tooltip: String): SpriteImageRenderer =
    new SpriteImageRenderer(Textures.GUISprites.ManualMissingItem) with InteractiveImageRenderer {
      override def getTooltip(oldTooltip: String): String = tooltip

      override def onMouseClick(mouseX: Int, mouseY: Int) = false
    }
}
