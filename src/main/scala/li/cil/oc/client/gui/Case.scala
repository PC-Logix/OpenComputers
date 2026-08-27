package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.{Textures, PacketSender => ClientPacketSender}
import li.cil.oc.common.menu
import net.minecraft.client.gui.components.{Button, Tooltip}
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class Case(state: menu.Case, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name) {

  protected var powerButton: ImageButton = _

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    powerButton.toggled = inventoryContainer.isRunning
    powerButton.setTooltip(Tooltip.create(Component.literal(
      if (inventoryContainer.isRunning) Localization.Computer.TurnOff else Localization.Computer.TurnOn
    )))
    super.render(graphics, mouseX, mouseY, dt)
  }

  override protected def init(): Unit = {
    super.init()
    powerButton = addRenderableWidget(new ImageButton(leftPos + 70, topPos + 33, 18, 18, (_: Button) => ClientPacketSender.sendComputerPower(inventoryContainer, !inventoryContainer.isRunning), Textures.GUISprites.ButtonPower))
  }

  override def drawSecondaryBackgroundLayer(graphics: GuiGraphics): Unit = {
    graphics.blit(Textures.GUI.Computer, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }
}
