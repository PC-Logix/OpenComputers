package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.{EnvironmentHost, ManagedEnvironment}
import li.cil.oc.common.datacomponents.{MFCoords, OCComponents}
import li.cil.oc.common.{Slot, Tier}
import li.cil.oc.server.component
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedDataComponentHolder._
import li.cil.oc.{Constants, api}
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.server.ServerLifecycleHooks

/**
  * @author Vexatos
  */
object DriverUpgradeMF extends Item with HostAware {
  override def worksWith(stack: ItemStack): Boolean = isOneOf(stack,
    api.Items.get(Constants.ItemName.MFU))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]): Boolean =
    worksWith(stack) && isAdapter(host)

  override def slot(stack: ItemStack): String = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Three

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost): ManagedEnvironment = {
    if (host.getEnvironmentLevel != null && !host.getEnvironmentLevel.isClientSide) {
      for(coords <- stack.getComponent(OCComponents.MF_COORD)) {
        val dimension = coords.dimension()
        val blockPos = coords.blockPos()
        val side = coords.side()
        ServerLifecycleHooks.getCurrentServer.getLevel(ResourceKey.create(Registries.DIMENSION, dimension)) match {
          case world: ServerLevel => return new component.UpgradeMF(host, BlockPosition(blockPos, world), side)
          case _ => // Invalid dimension ID
        }
      }
    }
    null
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.UpgradeMF]
      else null
  }

}
