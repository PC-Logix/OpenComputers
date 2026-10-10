package li.cil.oc.common.item

import com.google.common.base.Strings
import li.cil.oc.{api, Localization}
import li.cil.oc.client.gui
import li.cil.oc.common.{blockentity, component}
import li.cil.oc.common.blockentity.traits.BaseBlockEntity
import li.cil.oc.common.datacomponents.{OCComponents, TerminalReference}
import li.cil.oc.util.ExtendedDataComponentHolder._
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.{InteractionHand, InteractionResultHolder}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.{Item, ItemStack, TooltipFlag}
import net.minecraft.world.item.Item.{Properties, TooltipContext}
import net.minecraft.world.level.{Level, LevelReader}
import net.neoforged.api.distmarker.{Dist, OnlyIn}
import net.neoforged.neoforge.common.extensions.IItemExtension

import java.util

class Terminal(props: Properties) extends Item(props) with traits.SimpleItem with IItemExtension {
  def hasServer(stack: ItemStack) = stack.has(OCComponents.TERMINAL_REFERENCE)

  override def doesSneakBypassUse(stack: ItemStack, level: LevelReader, pos: BlockPos, player: Player): Boolean = {
    level.getBlockEntity(pos) match {
      // Pairing is a sneak-use on the terminal server's rack face. Allow the
      // rack to receive that interaction instead of sending it to the item.
      case _: blockentity.Rack => true
      case _ => super.doesSneakBypassUse(stack, level, pos, player)
    }
  }

  override def appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], flag: TooltipFlag): Unit = {
    super.appendHoverText(stack, context, tooltip, flag)
    for (data <- stack.getComponent(OCComponents.TERMINAL_REFERENCE)) {
      tooltip.add(Component.literal("§8" + data.server().substring(0, 13) + "...§7"))
    }
  }


  override def use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder[ItemStack] = {
    val stack = player.getItemInHand(hand)
    for (reference <- stack.getComponent(OCComponents.TERMINAL_REFERENCE) if !player.isCrouching) {
      val key = reference.key()
      val server = reference.server()
      if (key.nonEmpty && server.nonEmpty) {
        if (level.isClientSide) {
          if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(server)) {
            component.TerminalServer.loaded.find(server) match {
              case Some(term) if term != null && term.rack != null => term.rack match {
                case rack: BaseBlockEntity with api.internal.Rack => {
                  def inRange = player.isAlive && !rack.isRemoved && player.distanceToSqr(rack.x + 0.5, rack.y + 0.5, rack.z + 0.5) < term.range * term.range

                  if (inRange) {
                    if (term.sidedKeys.contains(key)) showGui(stack, key, term, () => inRange)
                    else player.displayClientMessage(Localization.Terminal.InvalidKey, true)
                  }
                  else player.displayClientMessage(Localization.Terminal.OutOfRange, true)
                }
                case _ => // Eh?
              }
              case _ => player.displayClientMessage(Localization.Terminal.OutOfRange, true)
            }
          }
        }
      }
    }

    InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
  }

  @OnlyIn(Dist.CLIENT)
  private def showGui(stack: ItemStack, key: String, term: component.RemoteTerminalHost, inRange: () => Boolean): Unit = {
    term.buffer match {
      case buffer: component.TextBuffer => buffer.requestSynchronization()
      case _ =>
    }

    def remainsUsable(): Boolean = {
      // Check if someone else bound a term to our server.
      if (!stack.getComponent(OCComponents.TERMINAL_REFERENCE).exists(_.key == key) || !term.sidedKeys.contains(key)) Minecraft.getInstance.popGuiLayer
      // Check whether we're still in range.
      if (!inRange()) Minecraft.getInstance.popGuiLayer
      true
    }

    term match {
      case kvm: component.RackKVM => Minecraft.getInstance.pushGuiLayer(new gui.RackKVM(kvm, () => remainsUsable()))
      case _ => Minecraft.getInstance.pushGuiLayer(new gui.Screen(term.buffer, true, () => true, () => remainsUsable()))
    }
  }
}
