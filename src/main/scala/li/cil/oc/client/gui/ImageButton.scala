package li.cil.oc.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.{Button, WidgetSprites}
import net.minecraft.network.chat.Component
import net.minecraft.util.FastColor
import net.neoforged.api.distmarker.{Dist, OnlyIn}

@OnlyIn(Dist.CLIENT)
class ImageButton(xPos: Int, yPos: Int, w: Int, h: Int,
                  handler: Button.OnPress,
                  private val image: WidgetSprites = null,
                  text: Component = Component.empty(),
                  val canToggle: Boolean = false,
                  val textColor: Int = 0xE0E0E0,
                  val textDisabledColor: Int = 0xA0A0A0,
                  val textHoverColor: Int = 0xFFFFA0,
                  val textIndent: Int = -1,
                  val textureWidth: Int = -1,
                  val textureHeight: Int = -1)
  extends Button(xPos, yPos, w, h, text, handler, _ => Component.empty()) {

  var toggled = true
  var hoverOverride = false

  override def renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    val isHov = hoverOverride || (isHovered && active)

    if (image != null) {
      graphics.blitSprite(image.get(isActive && toggled, isHov), x, y, width, height)
    } else {
      if (isHov) graphics.fill(x, y, x + width, y + height, FastColor.ARGB32.colorFromFloat(0.4f, 1f, 1f, 1f))
    }

    if (getMessage.getString.nonEmpty) {
      val color =
        if (!active) textDisabledColor
        else if (isHov) textHoverColor
        else textColor
      val font = Minecraft.getInstance.font
      if (textIndent >= 0) {
        graphics.drawString(font, getMessage, textIndent + x, y + (height - 8) / 2, color, false)
      } else {
        // If no indent, then draw the string centred. We don't want to draw with a drop shadow, so can't use
        // drawCenteredString here.
        graphics.drawString(font, getMessage, x + width / 2 - font.width(getMessage) / 2, y + (height - 8) / 2, color, false)
      }
    }
  }
}
