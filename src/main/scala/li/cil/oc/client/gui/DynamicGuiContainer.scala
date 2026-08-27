package li.cil.oc.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.menu.{AbstractMenu, ComponentSlot}
import li.cil.oc.integration.util.ItemSearch
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.{AbstractContainerMenu, Slot}

abstract class DynamicGuiContainer[C <: AbstractContainerMenu](container: C, inv: Inventory, title: Component)
  extends CustomGuiContainer(container, inv, title) {

  protected var hoveredStackNEI: StackOption = EmptyStack

  override protected def init(): Unit = {
    super.init()
    inventoryLabelY = imageHeight - 96 + 2
  }

  protected def drawSecondaryForegroundLayer(graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {}

  override protected def renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {
    super.renderLabels(graphics, mouseX, mouseY)
    drawSecondaryForegroundLayer(graphics, mouseX, mouseY)
    for (slot <- 0 until menu.slots.size()) {
      drawSlotHighlight(graphics, menu.getSlot(slot))
    }
  }

  protected def drawSecondaryBackgroundLayer(graphics: GuiGraphics): Unit = {}

  override protected def renderBg(graphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int): Unit = {
    graphics.blit(Textures.GUI.Background, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    drawSecondaryBackgroundLayer(graphics)
    drawInventorySlots(graphics)
  }

  protected def drawInventorySlots(graphics: GuiGraphics): Unit = {
    val stack = graphics.pose()
    stack.pushPose()
    stack.translate(leftPos.toFloat, topPos.toFloat, 0f)
    RenderSystem.disableDepthTest()
    RenderSystem.enableBlend()
    RenderSystem.defaultBlendFunc()
    for (slot <- 0 until menu.slots.size()) {
      drawSlotInventory(graphics, menu.getSlot(slot))
    }
    RenderSystem.disableBlend()
    RenderSystem.enableDepthTest()
    stack.popPose()
  }

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    hoveredStackNEI = ItemSearch.hoveredStack(this, mouseX, mouseY)
    super.render(graphics, mouseX, mouseY, dt)
  }

  protected def drawSlotInventory(graphics: GuiGraphics, slot: Slot): Unit = {
    slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None =>
        if (!slot.hasItem && slot.x >= 0 && slot.y >= 0 && component.tierIcon != null) {
          drawDisabledSlot(graphics, component)
        }
      case _ =>
        graphics.pose().pushPose()
        graphics.pose().translate(0, 0, 1)
        if (!isInPlayerInventory(slot)) {
          drawSlotBackground(graphics, slot.x - 1, slot.y - 1)
        }
        slot match {
          case component: ComponentSlot if !slot.hasItem =>
            if (component.tierIcon != null)
              graphics.blit(component.tierIcon, slot.x, slot.y, 0, 0, 16, 16, 16, 16)
            if (component.hasBackground)
              graphics.blit(component.getBackgroundLocation, slot.x, slot.y, 0, 0, 16, 16, 16, 16)
          case _ =>
        }
        graphics.pose().popPose()
    }
  }

  protected def drawSlotHighlight(graphics: GuiGraphics, slot: Slot): Unit = {
    if (minecraft.player.containerMenu.getCarried.isEmpty) slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None => // Ignore.
      case _ =>
        val currentIsInPlayerInventory = isInPlayerInventory(slot)
        val drawHighlight = hoveredSlot match {
          case hovered: Slot =>
            val hoveredIsInPlayerInventory = isInPlayerInventory(hovered)
            (currentIsInPlayerInventory != hoveredIsInPlayerInventory) &&
              ((currentIsInPlayerInventory && slot.hasItem && isSelectiveSlot(hovered) && hovered.mayPlace(slot.getItem)) ||
                (hoveredIsInPlayerInventory && hovered.hasItem && isSelectiveSlot(slot) && slot.mayPlace(hovered.getItem)))
          case _ => hoveredStackNEI match {
            case SomeStack(s) => !currentIsInPlayerInventory && isSelectiveSlot(slot) && slot.mayPlace(s)
            case _ => false
          }
        }
        if (drawHighlight) {
          graphics.pose().pushPose()
          graphics.pose().translate(0, 0, 100)
          graphics.fillGradient(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80FFFFFF, 0x80FFFFFF)
          graphics.pose().popPose()
        }
    }
  }

  private def isSelectiveSlot(slot: Slot): Boolean = slot match {
    case component: ComponentSlot => component.slot != common.Slot.Any && component.slot != common.Slot.Tool
    case _ => false
  }

  protected def drawDisabledSlot(graphics: GuiGraphics, slot: ComponentSlot): Unit = {
    graphics.blit(slot.tierIcon, slot.x, slot.y, 0, 0, 16, 16, 16, 16)
  }

  protected def drawSlotBackground(graphics: GuiGraphics, x: Int, y: Int): Unit = {
    graphics.blit(Textures.GUI.Slot, x, y, 0, 0, 18, 18, 18, 18)
  }

  private def isInPlayerInventory(slot: Slot): Boolean = container match {
    case player: AbstractMenu => slot.container == player.playerInventory
    case _ => false
  }
}
