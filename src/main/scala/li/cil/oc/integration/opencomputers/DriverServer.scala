package li.cil.oc.integration.opencomputers

import li.cil.oc.{api, Constants}
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.{EnvironmentHost, ManagedEnvironment}
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import li.cil.oc.util.ExtendedInventory._
import net.minecraft.world.item.ItemStack

object DriverServer extends Item with HostAware {
  override def worksWith(stack: ItemStack): Boolean = isOneOf(stack,
    api.Items.get(Constants.ItemName.ServerTier1),
    api.Items.get(Constants.ItemName.ServerTier2),
    api.Items.get(Constants.ItemName.ServerTier3),
    api.Items.get(Constants.ItemName.ServerTier4),
    api.Items.get(Constants.ItemName.ServerCreative))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost): ManagedEnvironment = host match {
    case rack: api.internal.Rack => new component.Server(rack, rack.indexOf(stack))
    case _ => null // Welp.
  }

  override def slot(stack: ItemStack): String = Slot.RackMountable
}
