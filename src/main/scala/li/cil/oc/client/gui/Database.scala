package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import li.cil.oc.common.{menu, Tier}
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class Database(state: menu.Database, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name)
  with traits.LockedHotbar[menu.Database] {

  imageHeight = 256

  override def lockedStack = inventoryContainer.container

  override protected def renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit =
    drawSecondaryForegroundLayer(graphics, mouseX, mouseY)

  override def drawSecondaryForegroundLayer(graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {}

  override protected def renderBg(graphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int): Unit = {
    graphics.blit(Textures.GUI.Database, leftPos, topPos, 0, 0, imageWidth, imageHeight)

    if (inventoryContainer.tier > Tier.One) {
      graphics.blit(Textures.GUI.Database1, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    }

    if (inventoryContainer.tier > Tier.Two) {
      graphics.blit(Textures.GUI.Database2, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    }
  }
}
