package li.cil.oc.client.gui

import li.cil.oc.common.menu
import li.cil.oc.Localization
import li.cil.oc.client.{Textures, PacketSender => ClientPacketSender}
import net.minecraft.client.gui.components.{Button, Tooltip}
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class Tablet(state: menu.Tablet, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name)
  with traits.LockedHotbar[menu.Tablet] {

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
    powerButton = addRenderableWidget(new ImageButton(leftPos + 68, topPos + 34, 18, 18, (_: Button) => ClientPacketSender.sendTabletPower(inventoryContainer, !inventoryContainer.isRunning), Textures.GUISprites.ButtonPower))
  }

  override def lockedStack = inventoryContainer.stack
}
