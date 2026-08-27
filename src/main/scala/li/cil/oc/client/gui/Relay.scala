package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common.menu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.Rect2i
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

import java.text.DecimalFormat

class Relay(state: menu.Relay, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name) {

  private val format = new DecimalFormat("#.##hz")

  val tabPosition = new Rect2i(imageWidth, 10, 23, 26)

  override protected def drawSecondaryBackgroundLayer(graphics: GuiGraphics): Unit = {
    super.drawSecondaryBackgroundLayer(graphics)

    graphics.blitSprite(
      Textures.GUISprites.UpgradeTab, leftPos + tabPosition.getX, topPos + tabPosition.getY, tabPosition.getWidth, tabPosition.getHeight
    )
  }

  override def mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    // So MC doesn't throw away the item in the upgrade slot when we're trying to pick it up...
    val originalWidth = imageWidth
    try {
      imageWidth += tabPosition.getWidth
      super.mouseClicked(mouseX, mouseY, button)
    }
    finally {
      imageWidth = originalWidth
    }
  }

  override def mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    // So MC doesn't throw away the item in the upgrade slot when we're trying to pick it up...
    val originalWidth = imageWidth
    try {
      imageWidth += tabPosition.getWidth
      super.mouseReleased(mouseX, mouseY, button)
    }
    finally {
      imageWidth = originalWidth
    }
  }

  override def drawSecondaryForegroundLayer(graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {
    super.drawSecondaryForegroundLayer(graphics, mouseX, mouseY)

    graphics.drawString(font,
      Localization.Switch.TransferRate,
      14, 20, 0x404040, false)
    graphics.drawString(font,
      Localization.Switch.PacketsPerCycle,
      14, 39, 0x404040, false)
    graphics.drawString(font,
      Localization.Switch.QueueSize,
      14, 58, 0x404040, false)

    graphics.drawString(font,
      format.format(20f / inventoryContainer.relayDelay),
      108, 20, 0x404040, false)
    graphics.drawString(font,
      s"${inventoryContainer.packetsPerCycleAvg} / ${inventoryContainer.relayAmount}",
      108, 39, thresholdBasedColor(inventoryContainer.packetsPerCycleAvg, math.ceil(inventoryContainer.relayAmount / 2f).toInt, inventoryContainer.relayAmount), false)
    graphics.drawString(font,
      s"${inventoryContainer.queueSize} / ${inventoryContainer.maxQueueSize}",
      108, 58, thresholdBasedColor(inventoryContainer.queueSize, inventoryContainer.maxQueueSize / 2, inventoryContainer.maxQueueSize), false)
  }

  private def thresholdBasedColor(value: Int, yellow: Int, red: Int) = {
    if (value < yellow) 0x009900
    else if (value < red) 0x999900
    else 0x990000
  }
}
