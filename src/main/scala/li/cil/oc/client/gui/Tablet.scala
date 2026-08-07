package li.cil.oc.client.gui

import li.cil.oc.common.menu
import li.cil.oc.Localization
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.client.Textures
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.player.Player
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

import scala.jdk.CollectionConverters._
import scala.collection.IterableOnce.iterableOnceExtensionMethods

class Tablet(state: menu.Tablet, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name)
  with traits.LockedHotbar[menu.Tablet] {

  protected var powerButton: ImageButton = _

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    // Issue: don't know how to get to the machine state from this class
    powerButton.toggled = ???
    super.render(graphics, mouseX, mouseY, dt)
  }

  override protected def init(): Unit = {
    super.init()
    //val machine = inventoryContainer.machine
    //powerButton = new ImageButton(leftPos + 70, topPos + 34, 18, 18, (_: Button) => ClientPacketSender.sendComputerPower(machine, !machine.isRunning), Textures.GUI.ButtonPower, canToggle = true)
    powerButton = new ImageButton(leftPos + 68, topPos + 34, 18, 18, (_: Button) => false, Textures.GUI.ButtonPower, canToggle = true)
    addRenderableWidget(powerButton)
  }

  override protected def drawSecondaryForegroundLayer(graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {
    super.drawSecondaryForegroundLayer(graphics, mouseX, mouseY)
    //val machine = inventoryContainer.machine
    if (powerButton.isMouseOver(mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[Component]
      tooltip.addAll(
        // (if (machine.isRunning) Localization.Computer.TurnOff
        // else Localization.Computer.TurnOn)
        Localization.Computer.TurnOn
          .linesIterator
          .map(Component.literal)
          .toSeq
          .asJava
      )
      graphics.renderComponentTooltip(font, tooltip, mouseX - leftPos, mouseY - topPos)
    }
  }

  override def lockedStack = inventoryContainer.stack
}
