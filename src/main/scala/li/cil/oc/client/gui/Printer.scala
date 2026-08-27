package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.common.menu
import li.cil.oc.common.menu.ComponentSlot
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class Printer(state: menu.Printer, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name) {

  imageWidth = 176
  imageHeight = 166

  private var materialBar: ProgressBar = _
  private var inkBar: ProgressBar = _
  private var progressBar: ProgressBar = _

  override protected def init(): Unit = {
    super.init()
    materialBar = addRenderableWidget(new ProgressBar(leftPos + 40, topPos + 21, width = 62, height = 12, sprite = Textures.GUISprites.PrinterMaterial))
    inkBar = addRenderableWidget(new ProgressBar(leftPos + 40, topPos + 53, width = 62, height = 12, sprite = Textures.GUISprites.PrinterInk))
    progressBar = addRenderableOnly(new ProgressBar(leftPos + 105, topPos + 20, width = 46, height = 46, sprite = Textures.GUISprites.PrinterProgress))
  }

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    materialBar.level = inventoryContainer.amountMaterial / inventoryContainer.maxAmountMaterial.toDouble
    materialBar.setTooltip(Tooltip.create(Component.literal(s"${inventoryContainer.amountMaterial}/${inventoryContainer.maxAmountMaterial}")))

    inkBar.level = inventoryContainer.amountInk / inventoryContainer.maxAmountInk.toDouble
    inkBar.setTooltip(Tooltip.create(Component.literal(s"${inventoryContainer.amountInk}/${inventoryContainer.maxAmountInk}")))

    progressBar.level = inventoryContainer.progress

    super.render(graphics, mouseX, mouseY, dt)
  }

  override def renderBg(graphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int): Unit = {
    graphics.blit(Textures.GUI.Printer, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    drawInventorySlots(graphics)
  }

  override protected def drawDisabledSlot(graphics: GuiGraphics, slot: ComponentSlot): Unit = {}
}
