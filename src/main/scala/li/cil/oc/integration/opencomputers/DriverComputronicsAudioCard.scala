package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.world.item.ItemStack

/** OC item drivers for Computronics' native audio cards. */
object DriverComputronicsBeepCard extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsBeepCard))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.getEnvironmentLevel != null && host.getEnvironmentLevel.isClientSide) null
    else new component.ComputronicsBeepCard(host)

  override def slot(stack: ItemStack) = Slot.Card

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsBeepCard] else null
  }
}

object DriverComputronicsNoiseCard extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsNoiseCard))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.getEnvironmentLevel != null && host.getEnvironmentLevel.isClientSide) null
    else new component.ComputronicsNoiseCard(host)

  override def slot(stack: ItemStack) = Slot.Card

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsNoiseCard] else null
  }
}

object DriverComputronicsSoundCard extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsSoundCard))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.getEnvironmentLevel != null && host.getEnvironmentLevel.isClientSide) null
    else new component.ComputronicsSoundCard(host)

  override def slot(stack: ItemStack) = Slot.Card

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsSoundCard] else null
  }
}
