package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.{Textures, PacketSender => ClientPacketSender}
import li.cil.oc.common.menu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.{Button, Tooltip}
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class Server(state: menu.Server, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name)
    with traits.LockedHotbar[menu.Server] {

  protected var powerButton: ImageButton = _

  override def lockedStack = inventoryContainer.stack

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float) = {
    powerButton.visible = !inventoryContainer.isItem
    powerButton.toggled = inventoryContainer.isRunning
    powerButton.setTooltip(Tooltip.create(Component.literal(
      if (inventoryContainer.isRunning) Localization.Computer.TurnOff else Localization.Computer.TurnOn
    )))
    super.render(graphics, mouseX, mouseY, dt)
  }

  override protected def init() = {
    super.init()
    powerButton = addRenderableWidget(new ImageButton(leftPos + 48, topPos + 33, 18, 18, (_: Button) => if (inventoryContainer.rackSlot >= 0) {
      ClientPacketSender.sendServerPower(inventoryContainer, inventoryContainer.rackSlot, !inventoryContainer.isRunning)
    }, Textures.GUISprites.ButtonPower))
  }

  override def drawSecondaryBackgroundLayer(graphics: GuiGraphics) = {
    graphics.blit(Textures.GUI.Server, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }
}
