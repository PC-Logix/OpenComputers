package li.cil.oc.integration.opencomputers

import li.cil.oc.{Constants, api}
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.{HostAware, Slot}
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.server.component
import net.minecraft.world.item.ItemStack

/** Native Computronics OC drivers. Third-party peripherals deliberately do not enter this file. */
object DriverComputronicsCameraUpgrade extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsCameraUpgrade))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.ComputronicsCameraUpgrade(host)
  override def slot(stack: ItemStack) = Slot.Upgrade
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsCameraUpgrade] else null }
}

object DriverComputronicsChatUpgrade extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsChatUpgrade))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.ComputronicsChatUpgrade(host)
  override def slot(stack: ItemStack) = Slot.Upgrade
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsChatUpgrade] else null }
}

object DriverComputronicsRadarUpgrade extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsRadarUpgrade))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.ComputronicsRadarUpgrade(host)
  override def slot(stack: ItemStack) = Slot.Upgrade
  override def tier(stack: ItemStack) = 2
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsRadarUpgrade] else null }
}

object DriverComputronicsParticleCard extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsParticleCard))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.ComputronicsParticleCard(host)
  override def slot(stack: ItemStack) = Slot.Card
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsParticleCard] else null }
}

object DriverComputronicsSpoofingCard extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsSpoofingCard))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.ComputronicsSpoofingCard(host)
  override def slot(stack: ItemStack) = Slot.Card
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsSpoofingCard] else null }
}

object DriverComputronicsSelfDestructingCard extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsSelfDestructingCard))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.ComputronicsSelfDestructCard(host)
  override def slot(stack: ItemStack) = Slot.Card
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsSelfDestructCard] else null }
}

object DriverComputronicsColorfulUpgrade extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsColorfulUpgrade))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.ComputronicsColorfulUpgrade(host)
  override def slot(stack: ItemStack) = Slot.Upgrade
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsColorfulUpgrade] else null }
}

object DriverComputronicsSpeechUpgrade extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsSpeechUpgrade))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.ComputronicsSpeechUpgrade(host)
  override def slot(stack: ItemStack) = Slot.Upgrade
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsSpeechUpgrade] else null }
}

object DriverComputronicsServerSelfDestructor extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsServerSelfDestructor))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match {
    case rack: api.internal.Rack => new component.ComputronicsRackBoomBoard(rack)
    case _ => new component.ComputronicsSelfDestructCard(host)
  }
  override def slot(stack: ItemStack) = Slot.RackMountable
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsRackBoomBoard] else null }
}

object DriverComputronicsLightBoard extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsLightBoard))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match { case rack: api.internal.Rack => new component.ComputronicsLightBoard(rack); case _ => null }
  override def slot(stack: ItemStack) = Slot.RackMountable
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsLightBoard] else null }
}

object DriverComputronicsRackCapacitor extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsRackCapacitor))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match { case rack: api.internal.Rack => new component.ComputronicsRackCapacitor(rack); case _ => null }
  override def slot(stack: ItemStack) = Slot.RackMountable
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsRackCapacitor] else null }
}

object DriverComputronicsSwitchBoard extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsSwitchBoard))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match { case rack: api.internal.Rack => new component.ComputronicsSwitchBoard(rack); case _ => null }
  override def slot(stack: ItemStack) = Slot.RackMountable
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.ComputronicsSwitchBoard] else null }
}

object DriverComputronicsMagicalMemory extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get(Constants.ItemName.ComputronicsMagicalMemory))
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.ComputronicsMagicalMemory
  override def slot(stack: ItemStack) = Slot.Memory
  object Provider extends EnvironmentProvider { override def getEnvironment(stack: ItemStack): Class[_] = if (worksWith(stack)) classOf[component.Memory] else null }
}
