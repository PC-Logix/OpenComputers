package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.{Textures, PacketSender => ClientPacketSender}
import li.cil.oc.common.item.data.DriveData
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.{screens, GuiGraphics}
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

class Drive(playerInventory: Inventory, val driveStack: () => ItemStack) extends screens.Screen(Component.empty()) with traits.Window {
  override val windowHeight = 120

  override def backgroundImage = Textures.GUI.Drive

  private var managedButton: ImageButton = _
  private var unmanagedButton: ImageButton = _
  private var lockedButton: ImageButton = _

  private def updateButtonStates(): Unit = {
    val data = new DriveData(driveStack())
    unmanagedButton.active = !data.isUnmanaged
    managedButton.active = data.isUnmanaged
    lockedButton.active = data.isLocked
    lockedButton.active = !data.isLocked
  }

  override protected def init(): Unit = {
    super.init()
    minecraft.mouseHandler.releaseMouse()
    KeyMapping.releaseAll()

    managedButton = addRenderableWidget(new ImageButton(leftPos + 11, topPos + 11, 74, 18, (_: Button) => {
      ClientPacketSender.sendDriveMode(unmanaged = false)
      DriveData.setUnmanaged(driveStack(), unmanaged = false)
      updateButtonStates()
    }, Textures.GUISprites.ButtonDriveMode, text = Component.literal(Localization.Drive.Managed), textColor = 0x608060))

    unmanagedButton = addRenderableWidget(new ImageButton(leftPos + 91, topPos + 11, 74, 18, (_: Button) => {
      ClientPacketSender.sendDriveMode(unmanaged = true)
      DriveData.setUnmanaged(driveStack(), unmanaged = true)
      updateButtonStates()
    }, Textures.GUISprites.ButtonDriveMode, text = Component.literal(Localization.Drive.Unmanaged), textColor = 0x608060))

    lockedButton = addRenderableWidget(new ImageButton(leftPos + 11, topPos + windowHeight - 42, 44, 18, (_: Button) => {
      ClientPacketSender.sendDriveLock()
      DriveData.lock(driveStack(), playerInventory.player)
      updateButtonStates()
    }, Textures.GUISprites.ButtonDriveMode, text = Component.literal(Localization.Drive.ReadOnlyLock), textColor = 0x608060))

    updateButtonStates()
  }

  override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.render(graphics, mouseX, mouseY, dt)
    graphics.drawWordWrap(font, Component.literal(Localization.Drive.Warning), leftPos + 11, topPos + 37, imageWidth - 20, 0x404040)
    graphics.drawWordWrap(font, Component.literal(Localization.Drive.LockWarning), leftPos + 61, topPos + windowHeight - 48, imageWidth - 68, 0x404040)
  }
}
