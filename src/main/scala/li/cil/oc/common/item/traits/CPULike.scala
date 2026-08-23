package li.cil.oc.common.item.traits

import li.cil.oc.{api, Settings}
import li.cil.oc.api.driver.item.MutableProcessor
import li.cil.oc.integration.opencomputers.DriverCPU
import li.cil.oc.util.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.world.{InteractionHand, InteractionResultHolder}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.level.Level

import java.util
import scala.jdk.CollectionConverters._

trait CPULike extends SimpleItem {
  def cpuTier: Int

  override protected def tooltipData: Seq[Any] = Seq(Settings.get.cpuComponentSupport(cpuTier))

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[Component], flag: TooltipFlag): Unit = {
    Tooltip.add(tooltip, flag, "cpu.Architecture", api.Machine.getArchitectureName(DriverCPU.architecture(stack)))
  }

  override def use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder[ItemStack] = {
    val stack = player.getItemInHand(hand)
    if (!player.isCrouching) {
      if (!level.isClientSide) {
        api.Driver.driverFor(stack) match {
          case driver: MutableProcessor =>
            val architectures = driver.allArchitectures.asScala.toList
            if (architectures.nonEmpty) {
              val currentIndex = architectures.indexOf(driver.architecture(stack))
              val newIndex = (currentIndex + 1) % architectures.length
              val archClass = architectures(newIndex)
              val archName = api.Machine.getArchitectureName(archClass)
              driver.setArchitecture(stack, archClass)
              player.displayClientMessage(Component.translatable(Settings.namespace + "tooltip.cpu.Architecture", archName), true)
            }
          case _ => // No known driver for this processor.
        }
      }
    }

    InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
  }
}
