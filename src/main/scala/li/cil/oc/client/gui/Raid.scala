package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import li.cil.oc.common.menu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class Raid(state: menu.Raid, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name) {

  override def renderBg(graphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int) = {
    graphics.blit(Textures.GUI.Raid, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }
}
