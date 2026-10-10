package li.cil.oc.common.component

import li.cil.oc.api
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.util.ExtendedDataComponentHolder._
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

trait RemoteTerminalHost extends api.network.EnvironmentHost {
  def rack: api.internal.Rack

  def slot: Int

  def node: api.network.Node

  def buffer: api.internal.TextBuffer

  def range: Double

  def sidedKeys: scala.collection.Seq[String]

  def hasAddress: Boolean = {
    if (rack != null && !rack.getEnvironmentLevel.isClientSide) return node.address != null
    if (rack != null) {
      val data = rack.getMountableData(slot)
      if (data != null) return data.has(OCComponents.ADDRESS)
    }
    false
  }

  def address: String = {
    if (!rack.getEnvironmentLevel.isClientSide) node.address
    else rack.getMountableData(slot).getComponent(OCComponents.ADDRESS).get
  }

  def isRemoteUsable(player: Player): Boolean = {
    if (player == null || !player.isAlive || rack == null || !hasAddress) return false

    val inRange = player.distanceToSqr(rack.xPosition + 0.5, rack.yPosition + 0.5, rack.zPosition + 0.5) < range * range
    if (!inRange) return false

    val stack = player.getItemInHand(InteractionHand.MAIN_HAND)
    isAuthorizedRemote(stack)
  }

  def isBufferUsable(candidate: api.internal.TextBuffer, player: Player): Boolean =
    (candidate eq buffer) && isRemoteUsable(player)

  protected def isAuthorizedRemote(stack: ItemStack): Boolean = {
    stack.getComponent(OCComponents.TERMINAL_REFERENCE).exists(reference =>
      reference.server() == address && sidedKeys.contains(reference.key()))
  }
}
