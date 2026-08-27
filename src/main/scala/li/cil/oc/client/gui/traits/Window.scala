package li.cil.oc.client.gui.traits

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation

trait Window extends Screen {
  var leftPos = 0
  var topPos = 0
  var imageWidth = 0
  var imageHeight = 0

  val windowWidth = 176
  val windowHeight = 166

  def backgroundImage: ResourceLocation

  override def isPauseScreen = false

  override protected def init(): Unit = {
    super.init()

    imageWidth = windowWidth
    imageHeight = windowHeight
    leftPos = (width - imageWidth) / 2
    topPos = (height - imageHeight) / 2
  }

  override def renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.renderBackground(guiGraphics, mouseX, mouseY, dt)
    guiGraphics.blit(backgroundImage, leftPos, topPos, 0, 0, imageWidth, imageHeight, windowWidth, windowHeight)
  }
}
