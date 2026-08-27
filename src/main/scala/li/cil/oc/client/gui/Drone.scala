package li.cil.oc.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.client.{Textures, PacketSender => ClientPacketSender}
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.font.TextBufferRenderData
import li.cil.oc.common.menu
import li.cil.oc.util.{PackedColor, RenderState, TextBuffer}
import net.minecraft.client.gui.components.{Button, Tooltip}
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class Drone(state: menu.Drone, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name)
    with traits.DisplayBuffer {

  imageWidth = 176
  imageHeight = 148

  protected var powerButton: ImageButton = _

  private val buffer = new TextBuffer(20, 2, new PackedColor.SingleBitFormat(0x33FF33))
  private val bufferRenderer = new TextBufferRenderData {
    private var _dirty = true

    override def dirty = _dirty

    override def dirty_=(value: Boolean): Unit = _dirty = value

    override def data = buffer

    override def viewport: (Int, Int) = buffer.size
  }

  override protected val bufferX = 9
  override protected val bufferY = 9
  override protected val bufferColumns = 80
  override protected val bufferRows = 16

  private val inventoryX = 97
  private val inventoryY = 7

  private var power: ProgressBar = _

  private val selectionSize = 20

  override protected def init(): Unit = {
    super.init()
    powerButton = addRenderableWidget(new ImageButton(leftPos + 7, topPos + 45, 18, 18, (_: Button) =>
      ClientPacketSender.sendDronePower(inventoryContainer, !inventoryContainer.isRunning), Textures.GUISprites.ButtonPower))
    power = addRenderableWidget(new ProgressBar(leftPos + 28, topPos + 48))
  }

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    val format = Localization.Computer.Power + ": %d%% (%d/%d)"
    power.level = inventoryContainer.globalBuffer.toFloat / math.max(inventoryContainer.globalBufferSize.toFloat, 1.0f)
    power.setTooltip(Tooltip.create(Component.literal(format.format(
      inventoryContainer.globalBuffer * 100 / math.max(inventoryContainer.globalBufferSize, 1),
      inventoryContainer.globalBuffer, inventoryContainer.globalBufferSize))))

    powerButton.toggled = inventoryContainer.isRunning
    powerButton.setTooltip(Tooltip.create(Component.literal(
      if (inventoryContainer.isRunning) Localization.Computer.TurnOff else Localization.Computer.TurnOn
    )))

    bufferRenderer.dirty = inventoryContainer.statusText.getString.linesIterator.zipWithIndex.exists {
      case (line, i) => buffer.set(0, i, line, vertical = false)
    }

    super.render(graphics, mouseX, mouseY, dt)
  }

  override protected def drawBuffer(graphics: GuiGraphics): Unit = {
    val stack = graphics.pose()
    stack.translate(bufferX.toFloat, bufferY.toFloat, 0f)
    RenderState.makeItBlend()
    stack.scale(scale.toFloat, scale.toFloat, 1)
    RenderSystem.depthMask(false)
    RenderSystem.setShaderColor(0.5f, 0.5f, 1f, 1f)
    TextBufferRenderCache.render(stack, bufferRenderer)
    RenderSystem.setShaderColor(1, 1, 1, 1)
  }

  override protected def changeSize(w: Double, h: Double) = 2.0

  override protected def renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit =
    drawSecondaryForegroundLayer(graphics, mouseX, mouseY)

  override protected def drawSecondaryForegroundLayer(graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {
    drawBufferLayer(graphics)
  }

  override protected def renderBg(graphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int): Unit = {
    graphics.blit(Textures.GUI.Drone, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    if (inventoryContainer.otherInventory.getContainerSize > 0) {
      drawSelection(graphics)
    }

    drawInventorySlots(graphics)
  }

  // No custom slots, we just extend DynamicGuiContainer for the highlighting.
  override protected def drawSlotBackground(graphics: GuiGraphics, x: Int, y: Int): Unit = {}

  private def drawSelection(graphics: GuiGraphics): Unit = {
    val slot = inventoryContainer.selectedSlot
    if (slot >= 0 && slot < 16) {
      val x = leftPos + inventoryX - 1 + (slot % 4) * (selectionSize - 2)
      val y = topPos + inventoryY - 1 + (slot / 4) * (selectionSize - 2)
      RenderSystem.enableBlend()
      graphics.blitSprite(Textures.GUISprites.RobotSelection, x, y, selectionSize, selectionSize)
      RenderSystem.disableBlend()
    }
  }
}
