package li.cil.oc.client

import com.mojang.blaze3d.platform.InputConstants
import li.cil.oc.OpenComputers
import li.cil.oc.client.gui.traits.InputBuffer
import net.minecraft.client.{KeyMapping, Minecraft}
import net.neoforged.neoforge.client.settings.IKeyConflictContext
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.client.settings.KeyModifier
import org.lwjgl.glfw.GLFW

import scala.collection.mutable

object KeyBindings {
  private def isActive(input: InputConstants.Key): Boolean = {
    val window = Minecraft.getInstance.getWindow.getWindow
    input.getType match {
      case InputConstants.Type.MOUSE => GLFW.glfwGetMouseButton(window, input.getValue) == GLFW.GLFW_PRESS
      case InputConstants.Type.SCANCODE => false // GLFW doesn't have a glfwGetScancode method to test these.
      case InputConstants.Type.KEYSYM => GLFW.glfwGetKey(window, input.getValue) == GLFW.GLFW_PRESS
    }
  }

  def showExtendedTooltips: Boolean = {
    if (extendedTooltip.isDown) return true
    // We have to know if the keybind is pressed even if the active screen doesn't pass events.
    if (!extendedTooltip.getKeyConflictContext.isActive) return false
    if (extendedTooltip.getKey == InputConstants.UNKNOWN) return false
    extendedTooltip.getKeyModifier match {
      // KeyModifier.NONE does not accept pure modifier keys by default, so check for that.
      case KeyModifier.NONE if KeyModifier.isKeyCodeModifier(extendedTooltip.getKey) => isActive(extendedTooltip.getKey)
      case mod if mod.isActive(extendedTooltip.getKeyConflictContext) => isActive(extendedTooltip.getKey)
      case _ => false
    }
  }

  def isAnalyzeCopyingAddress: Boolean = analyzeCopyAddr.isDown

  val textInputConflict = new IKeyConflictContext {
    override def isActive: Boolean = Minecraft.getInstance.screen.isInstanceOf[InputBuffer]

    override def conflicts(other: IKeyConflictContext): Boolean = this == other
  }

  val extendedTooltip = new KeyMapping("key.opencomputers.extendedTooltip", KeyConflictContext.GUI,
    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, OpenComputers.Name)

  val analyzeCopyAddr = new KeyMapping("key.opencomputers.analyzeCopyAddress", KeyConflictContext.IN_GAME,
    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, OpenComputers.Name)

  val clipboardPaste = new KeyMapping("key.opencomputers.clipboardPaste", textInputConflict,
    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_INSERT, OpenComputers.Name)
}
