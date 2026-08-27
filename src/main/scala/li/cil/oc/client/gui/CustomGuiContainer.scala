package li.cil.oc.client.gui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu

abstract class CustomGuiContainer[C <: AbstractContainerMenu](val inventoryContainer: C, inv: Inventory, title: Component)
  extends AbstractContainerScreen[C](inventoryContainer, inv, title) {

  override def isPauseScreen: Boolean = false

  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    super.render(guiGraphics, mouseX, mouseY, partialTicks)
    this.renderTooltip(guiGraphics, mouseX, mouseY)
  }
}
