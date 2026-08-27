package li.cil.oc.client.gui

import li.cil.oc.api
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.gui.BufferRenderer
import net.minecraft.client.gui.{screens, GuiGraphics}
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.components.events.ContainerEventHandler
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class Screen(initialBuffer: api.internal.TextBuffer, val hasMouse: Boolean, val hasKeyboardCallback: () => Boolean, val hasPower: () => Boolean)
  extends screens.Screen(Component.empty()) with traits.InputBuffer with ContainerEventHandler {

  override def buffer: api.internal.TextBuffer = initialBuffer

  override protected def hasKeyboard = hasKeyboardCallback()

  override protected def bufferX = 8 + x

  override protected def bufferY = 8 + y

  private val bufferMargin = BufferRenderer.margin + BufferRenderer.innerMargin

  private var didClick = false

  private var x, y = 0

  private var innerWidth, innerHeight = 0

  private var mx, my = -1

  protected def topPadding: Int = 0

  override def mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean = {
    if (hasMouse) {
      toBufferCoordinates(mouseX, mouseY) match {
        case Some((bx, by)) =>
          buffer.mouseScroll(bx, by, math.signum(scrollY.toInt), null)
          return true
        case _ => // Ignore when out of bounds.
      }
    }
    super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
  }

  override def mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    if (hasMouse) {
      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
        clickOrDrag(mouseX, mouseY, button)
        return true
      }
    }
    super.mouseClicked(mouseX, mouseY, button)
  }

  override def mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = {
    if (hasMouse) {
      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
        clickOrDrag(mouseX, mouseY, button)
        return true
      }
    }
    super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
  }

  override def mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    if (hasMouse) {
      if (didClick) {
        toBufferCoordinates(mouseX, mouseY) match {
          case Some((bx, by)) => buffer.mouseUp(bx, by, button, null)
          case _ => buffer.mouseUp(-1.0, -1.0, button, null)
        }
      }
      val hasClicked = didClick
      didClick = false
      mx = -1
      my = -1
      if (hasClicked) return true
    }
    super.mouseReleased(mouseX, mouseY, button)
  }

  private def clickOrDrag(mouseX: Double, mouseY: Double, button: Int) = {
    toBufferCoordinates(mouseX, mouseY) match {
      case Some((bx, by)) if bx.toInt != mx || (by*2).toInt != my =>
        if (mx >= 0 && my >= 0) buffer.mouseDrag(bx, by, button, null)
        else buffer.mouseDown(bx, by, button, null)
        didClick = true
        mx = bx.toInt
        my = (by*2).toInt
      case _ =>
    }
  }

  private def toBufferCoordinates(mouseX: Double, mouseY: Double): Option[(Double, Double)] = {
    val bx = (mouseX - x - bufferMargin) / scale / TextBufferRenderCache.renderer.charRenderWidth
    val by = (mouseY - y - bufferMargin) / scale / TextBufferRenderCache.renderer.charRenderHeight
    val bw = buffer.getViewportWidth
    val bh = buffer.getViewportHeight
    if (bx >= 0 && by >= 0 && bx < bw && by < bh) Some((bx, by))
    else None
  }

  override protected def init(): Unit = {
    super.init()
    minecraft.mouseHandler.releaseMouse()
    KeyMapping.releaseAll()
  }

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.render(graphics, mouseX, mouseY, dt)
    drawBufferLayer(graphics)
  }

  override def drawBuffer(graphics: GuiGraphics) = {
    val stack = graphics.pose()
    stack.translate(x.toFloat, y.toFloat, 0f)
    BufferRenderer.drawBackground(stack, innerWidth, innerHeight)
    if (hasPower()) {
      stack.translate(bufferMargin.toFloat, bufferMargin.toFloat, 0f)
      stack.scale(scale.toFloat, scale.toFloat, 1)
      BufferRenderer.drawText(stack, buffer)
    }
  }

  override protected def changeSize(w: Double, h: Double) = {
    val bw = buffer.renderWidth
    val bh = buffer.renderHeight
    val scaleX = math.min(width / (bw + bufferMargin * 2.0), 1)
    val availableHeight = height - topPadding
    val scaleY = math.min(availableHeight / (bh + bufferMargin * 2.0), 1)
    val scale = math.min(scaleX, scaleY)
    innerWidth = (bw * scale).toInt
    innerHeight = (bh * scale).toInt
    x = (width - (innerWidth + bufferMargin * 2)) / 2
    y = topPadding + (availableHeight - (innerHeight + bufferMargin * 2)) / 2
    scale
  }
}
