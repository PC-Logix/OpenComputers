package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.{Textures, PacketSender => ClientPacketSender}
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.common.menu
import li.cil.oc.common.menu.ComponentSlot
import li.cil.oc.common.template.AssemblerTemplates
import net.minecraft.client.gui.components.{Button, Tooltip}
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot

import scala.jdk.CollectionConverters._

class Assembler(val state: menu.Assembler, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name) {

  imageWidth = 176
  imageHeight = 192

  for (slot <- menu.slots.asScala) slot match {
    case component: ComponentSlot => component.changeListener = Option(onSlotChanged)
    case _ =>
  }

  private def onSlotChanged(slot: Slot): Unit = {
    runButton.active = canBuild
    info = validate
  }

  var info: Option[(Boolean, Component, Array[Component])] = None

  protected var runButton: ImageButton = _

  private var progress: ProgressBar = _

  private def validate = AssemblerTemplates.select(inventoryContainer.getSlot(0).getItem).map(_.validate(inventoryContainer.otherInventory))

  private def canBuild = !inventoryContainer.isAssembling && validate.exists(_._1)

  override protected def init(): Unit = {
    super.init()
    runButton = addRenderableWidget(new ImageButton(leftPos + 7, topPos + 89, 18, 18, (b: Button) => if (canBuild) ClientPacketSender.sendRobotAssemblerStart(inventoryContainer), Textures.GUISprites.ButtonRun))
    progress = addRenderableWidget(new ProgressBar(leftPos + 28, topPos + 92))
  }

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    if (inventoryContainer.isAssembling) {
      val timeRemaining = formatTime(inventoryContainer.assemblyRemainingTime)
      progress.level = inventoryContainer.assemblyProgress / 100.0
      progress.setTooltip(
        Tooltip.create(Component.literal(Localization.Assembler.Progress(inventoryContainer.assemblyProgress, timeRemaining)))
      )
    } else {
      progress.level = 0
      progress.setTooltip(null)
    }

    super.render(graphics, mouseX, mouseY, dt)
  }

  override protected def renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {
    drawSecondaryForegroundLayer(guiGraphics, mouseX, mouseY)

    for (slot <- 0 until menu.slots.size()) {
      drawSlotHighlight(guiGraphics, menu.getSlot(slot))
    }
  }

  override def drawSecondaryForegroundLayer(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {
    if (!inventoryContainer.isAssembling) {
      val message =
        if (!inventoryContainer.getSlot(0).hasItem) {
          Localization.Assembler.InsertTemplate
        }
        else info match {
          case Some((_, value, _)) if value != null => value.getString
          case _ if inventoryContainer.getSlot(0).hasItem => Localization.Assembler.CollectResult
          case _ => ""
        }
      guiGraphics.drawString(font, message, 30, 94, 0x404040, false)
      if (runButton.isMouseOver(mouseX, mouseY)) {
        val tooltip = new java.util.ArrayList[Component]
        tooltip.add(Component.literal(Localization.Assembler.Run))
        info.foreach {
          case (valid, _, warnings) => if (valid && warnings.length > 0) {
            warnings.foreach(w => tooltip.add(w))
          }
        }
        guiGraphics.renderComponentTooltip(font, tooltip, mouseX - leftPos, mouseY - topPos)
      }
    }
  }

  private def formatTime(seconds: Int) = {
    if (seconds < 60) f"0:$seconds%02d"
    else f"${seconds / 60}:${seconds % 60}%02d"
  }

  override protected def renderBg(guiGraphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int): Unit = {
    guiGraphics.blit(Textures.GUI.RobotAssembler, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    drawInventorySlots(guiGraphics)
  }

  override protected def drawDisabledSlot(guiGraphics: GuiGraphics, slot: ComponentSlot): Unit = {}
}
