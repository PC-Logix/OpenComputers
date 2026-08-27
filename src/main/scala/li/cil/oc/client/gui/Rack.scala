package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.{Textures, PacketSender => ClientPacketSender}
import li.cil.oc.common.menu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.{Button, Tooltip}
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

import scala.jdk.CollectionConverters._

class Rack(state: menu.Rack, playerInventory: Inventory, name: Component)
  extends DynamicGuiContainer(state, playerInventory, name) {

  imageHeight = 210

  final val busMasterBlankUVs  = (195, 14, 3, 5)
  final val busMasterPresentUVs = (194, 20, 5, 5)
  final val busSlaveBlankUVs   = (195, 1,  3, 4)
  final val busSlavePresentUVs  = (194, 6,  5, 4)

  final val connectorMasterUVs = (194, 26, 1, 3)
  final val connectorSlaveUVs  = (194, 11, 1, 2)

  final val hoverMasterSize = (3, 3)
  final val hoverSlaveSize  = (3, 2)

  final val wireMasterUVs = Array(
    (186, 16, 6, 3),
    (186, 20, 6, 3),
    (186, 24, 6, 3),
    (186, 28, 6, 3),
    (186, 32, 6, 3)
  )
  final val wireSlaveUVs = Array(
    (186, 1,  6, 2),
    (186, 4,  6, 2),
    (186, 7,  6, 2),
    (186, 10, 6, 2),
    (186, 13, 6, 2)
  )

  final val busStart = Array(
    (45, 22), (56, 22), (67, 22), (78, 22), (89, 22)
  )

  final val busGap = 3

  final val connectorStart = Array(
    (37, 23), (37, 43), (37, 63), (37, 83)
  )

  final val connectorGap = 2

  final val relayModeUVs = (195, 30, 4, 2)

  final val wireRelay = Array(
    (50, 104), (61, 104), (72, 104), (83, 104)
  )

  final val busToSide = Direction.values().filter(_ != Direction.SOUTH)
  final val sideToBus = busToSide.zipWithIndex.toMap

  var relayButton: ImageButton = _

  var wireButtons = Array.fill(inventoryContainer.otherInventory.getContainerSize)(Array.fill(4)(Array.fill(5)(null: ImageButton)))

  def sideName(side: Direction) = side match {
    case Direction.UP    => Localization.Rack.Top
    case Direction.DOWN  => Localization.Rack.Bottom
    case Direction.EAST  => Localization.Rack.Left
    case Direction.WEST  => Localization.Rack.Right
    case Direction.NORTH => Localization.Rack.Back
    case _               => Localization.Rack.None
  }

  protected def onRackButton(mountable: Int, connectable: Int, bus: Int): Unit = {
    if (inventoryContainer.nodeMapping(mountable)(connectable).contains(busToSide(bus)))
      ClientPacketSender.sendRackMountableMapping(inventoryContainer, mountable, connectable, None)
    else
      ClientPacketSender.sendRackMountableMapping(inventoryContainer, mountable, connectable, Option(busToSide(bus)))
  }

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    for (bus <- 0 until 5) {
      for (mountable <- 0 until inventoryContainer.otherInventory.getContainerSize) {
        val presence = inventoryContainer.nodePresence(mountable)
        for (connectable <- 0 until 4) {
          wireButtons(mountable)(connectable)(bus).visible = presence(connectable)
        }
      }
    }
    val relayMessage =
      if (inventoryContainer.isRelayEnabled) Localization.Rack.RelayEnabled
      else Localization.Rack.RelayDisabled
    relayButton.setMessage(Component.literal(relayMessage))
    super.render(graphics, mouseX, mouseY, dt)
  }

  override protected def init(): Unit = {
    super.init()

    relayButton = new ImageButton(
      leftPos + 101, topPos + 96, 65, 18,
      (_: Button) => ClientPacketSender.sendRackRelayState(inventoryContainer, !inventoryContainer.isRelayEnabled),
      Textures.GUISprites.ButtonRelay,
      Component.literal(Localization.Rack.RelayDisabled),
      textIndent = 18)
    relayButton.setTooltip(Tooltip.create(Component.literal(Localization.Rack.RelayModeTooltip)))
    addRenderableWidget(relayButton)

    val (mw, mh) = hoverMasterSize
    val (sw, sh) = hoverSlaveSize
    val (_, _, _, mbh) = busMasterBlankUVs
    val (_, _, _, sbh) = busSlaveBlankUVs
    for (bus <- 0 until 5) {
      for (mountable <- 0 until inventoryContainer.otherInventory.getContainerSize) {
        val offset = mountable * (mbh + sbh * 3 + busGap)
        val (bx, by) = busStart(bus)

        {
          val button = new ImageButton(leftPos + bx, topPos + by + offset + 1, mw, mh,
            (_: Button) => onRackButton(mountable, 0, bus))
          addRenderableWidget(button)
          wireButtons(mountable)(0)(bus) = button
        }

        for (connectable <- 0 until 3) {
          val button = new ImageButton(leftPos + bx, topPos + by + offset + 1 + mbh + sbh * connectable, sw, sh,
            (_: Button) => onRackButton(mountable, connectable + 1, bus))
          addRenderableWidget(button)
          wireButtons(mountable)(connectable + 1)(bus) = button
        }
      }
    }
  }

  override def drawSecondaryForegroundLayer(graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {
    super.drawSecondaryForegroundLayer(graphics, mouseX, mouseY)

    if (inventoryContainer.isRelayEnabled) {
      val (left, top, w, h) = relayModeUVs
      for ((x, y) <- wireRelay) drawRect(graphics, x, y, w, h, left, top)
    }

    val (mcx, mcy, mcw, mch) = connectorMasterUVs
    val (mbx, mby, mbw, mbh) = busMasterBlankUVs
    val (mpx, mpy, mpw, mph) = busMasterPresentUVs
    val (scx, scy, scw, sch) = connectorSlaveUVs
    val (sbx, sby, sbw, sbh) = busSlaveBlankUVs
    val (spx, spy, spw, sph) = busSlavePresentUVs

    for (mountable <- 0 until inventoryContainer.otherInventory.getContainerSize) {
      val presence = inventoryContainer.nodePresence(mountable)

      val (cx, cy) = connectorStart(mountable)
      if (presence(0)) {
        drawRect(graphics, cx, cy, mcw, mch, mcx, mcy)
        inventoryContainer.nodeMapping(mountable)(0) match {
          case Some(side) =>
            val bus = sideToBus(side)
            val (mwx, mwy, mww, mwh) = wireMasterUVs(bus)
            for (i <- 0 to bus) {
              val xOffset = mcw + i * (mpw + mww)
              drawRect(graphics, cx + xOffset, cy, mww, mwh, mwx, mwy)
            }
          case _ =>
        }
        for (connectable <- 1 until 4) {
          inventoryContainer.nodeMapping(mountable)(connectable) match {
            case Some(side) =>
              val bus = sideToBus(side)
              val (swx, swy, sww, swh) = wireSlaveUVs(bus)
              val yOffset = (mch + connectorGap) + (sch + connectorGap) * (connectable - 1)
              for (i <- 0 to bus) {
                val xOffset = scw + i * (spw + sww)
                drawRect(graphics, cx + xOffset, cy + yOffset, sww, swh, swx, swy)
              }
            case _ =>
          }
        }
      }
      for (connectable <- 1 until 4) {
        if (presence(connectable)) {
          val yOffset = (mch + connectorGap) + (sch + connectorGap) * (connectable - 1)
          drawRect(graphics, cx, cy + yOffset, scw, sch, scx, scy)
        }
      }

      val yOffset = mountable * (mbh + sbh * 3 + busGap)
      for (bus <- 0 until 5) {
        val (bx, by) = busStart(bus)
        if (presence(0))
          drawRect(graphics, bx - 1, by + yOffset, mpw, mph, mpx, mpy)
        else
          drawRect(graphics, bx, by + yOffset, mbw, mbh, mbx, mby)
        for (connectable <- 0 until 3) {
          if (presence(connectable + 1))
            drawRect(graphics, bx - 1, by + yOffset + mph + sph * connectable, spw, sph, spx, spy)
          else
            drawRect(graphics, bx, by + yOffset + mbh + sbh * connectable, sbw, sbh, sbx, sby)
        }
      }
    }

    for (bus <- 0 until 5) {
      val x = 122
      val y = 20 + bus * 11
      graphics.drawString(font, Localization.localizeImmediately(sideName(busToSide(bus))), x, y, 0x404040, false)
    }

    if (mouseX >= leftPos + 122 && mouseY >= topPos + 20 && mouseX < leftPos + 158 && mouseY < topPos + 20 + 5 * 11) {
      val tooltip = new java.util.ArrayList[Component]
      tooltip.addAll(Localization.Rack.OrientationTooltip.linesIterator.map(Component.literal).toList.asJava)
      graphics.renderComponentTooltip(font, tooltip, mouseX - leftPos, mouseY - topPos)
    }
  }

  override def drawSecondaryBackgroundLayer(graphics: GuiGraphics): Unit = {
    graphics.blit(Textures.GUI.Rack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }

  private def drawRect(graphics: GuiGraphics, x: Int, y: Int, w: Int, h: Int, u: Int, v: Int): Unit =
    graphics.blit(Textures.GUI.Rack, x, y, u.toFloat, v.toFloat, w, h, 256, 256)
}
