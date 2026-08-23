package li.cil.oc.integration.opencomputers

import li.cil.oc.{api, Constants, OpenComputers, Settings}
import li.cil.oc.common.{item, Slot, Tier}
import li.cil.oc.server.component
import li.cil.oc.server.machine.luac.NativeLuaArchitecture
import li.cil.oc.util.ItemUtils
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

import scala.collection.convert.ImplicitConversionsToScala._

object DriverCPU extends DriverCPU

abstract class DriverCPU extends Item with api.driver.item.MutableProcessor with api.driver.item.CallBudget {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.CPUTier1),
    api.Items.get(Constants.ItemName.CPUTier2),
    api.Items.get(Constants.ItemName.CPUTier3),
    api.Items.get(Constants.ItemName.CPUTier4))

  override def createEnvironment(stack: ItemStack, host: api.network.EnvironmentHost): api.network.ManagedEnvironment = new component.CPU(tier(stack))

  override def slot(stack: ItemStack) = Slot.CPU

  override def tier(stack: ItemStack) = cpuTier(stack)

  def cpuTier(stack: ItemStack): Int =
    stack.getItem match {
      case cpu: item.CPU => cpu.cpuTier
      case _ => Tier.One
    }

  override def supportedComponents(stack: ItemStack) = Settings.get.cpuComponentSupport(cpuTier(stack))

  override def allArchitectures = api.Machine.architectures

  override def architecture(stack: ItemStack): Class[_ <: api.machine.Architecture] = {
    val tag = ItemUtils.getTag(stack)
    if(tag != null) {
      val archClass = tag.getString(Settings.namespace + "archClass") match {
        case clazz if clazz == classOf[NativeLuaArchitecture].getName =>
          // Migrate old saved CPUs to new versions (since the class they refer still
          // exists, but is abstract, which would lead to issues).
          api.Machine.LuaArchitecture.getName
        case clazz => clazz
      }
      if (!archClass.isEmpty) try return Class.forName(archClass).asSubclass(classOf[api.machine.Architecture]) catch {
        case t: Throwable =>
          OpenComputers.log.warn("Failed getting class for CPU architecture. Resetting CPU to use the default.", t)
          CustomData.update(DataComponents.CUSTOM_DATA, stack, tag => {
            tag.remove(Settings.namespace + "archClass")
            tag.remove(Settings.namespace + "archName")
          })
      }
    }
    api.Machine.architectures.headOption.orNull
  }

  override def setArchitecture(stack: ItemStack, architecture: Class[_ <: api.machine.Architecture]): Unit = {
    if (!worksWith(stack)) throw new IllegalArgumentException("Unsupported processor type.")

    CustomData.update(DataComponents.CUSTOM_DATA, stack, data => {
      data.putString(Settings.namespace + "archClass", architecture.getName)
      data.putString(Settings.namespace + "archName", api.Machine.getArchitectureName(architecture))
    })
  }

  override def getCallBudget(stack: ItemStack): Double = Settings.get.callBudgets(tier(stack) max Tier.One min Tier.Four)
}
