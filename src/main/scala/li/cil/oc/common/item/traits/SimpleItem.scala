package li.cil.oc.common.item.traits

import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.api
import li.cil.oc.api.event.RobotRenderEvent.MountPoint
import li.cil.oc.api.internal.Robot
import li.cil.oc.client.renderer.item.ItemUpgradeRenderer
import li.cil.oc.common.blockentity
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.util.ExtendedDataComponentHolder.convert
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.{Item, ItemStack, TooltipFlag}
import net.minecraft.world.level.LevelReader
import net.neoforged.api.distmarker.{Dist, OnlyIn}
import net.neoforged.neoforge.common.extensions.IItemExtension

import java.util
import scala.collection.convert.ImplicitConversionsToScala._

trait SimpleItem extends Item with api.driver.item.UpgradeRenderer with IItemExtension {
  def createItemStack(amount: Int = 1) = new ItemStack(this, amount)

  @Deprecated
  protected var unlocalizedName = getClass.getSimpleName.toLowerCase

  @Deprecated
  override def getDescriptionId = "item.oc." + unlocalizedName

  override def doesSneakBypassUse(stack: ItemStack, level: LevelReader, pos: BlockPos, player: Player): Boolean = {
    level.getBlockEntity(pos) match {
      case drive: blockentity.DiskDrive => true
      case _ => super.doesSneakBypassUse(stack, level, pos, player)
    }
  }

  protected def tierFromDriver(stack: ItemStack): Int =
    api.Driver.driverFor(stack) match {
      case driver: api.driver.DriverItem => driver.tier(stack)
      case _ => 0
    }

  protected def tooltipName = Option(unlocalizedName)

  protected def tooltipData = Seq.empty[Any]

  override def appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], flag: TooltipFlag): Unit = {
    if (tooltipName.isDefined) {
      Tooltip.add(tooltip, flag, tooltipName.get, tooltipData: _*)
      tooltipExtended(stack, tooltip, flag)
    }
    else {
      Tooltip.add(tooltip, flag, getClass.getSimpleName.toLowerCase)
    }
    tooltipCosts(stack, tooltip)
  }

  // For stuff that goes to the normal 'extended' tooltip, before the costs.
  protected def tooltipExtended(stack: ItemStack, tooltip: util.List[Component], flag: TooltipFlag): Unit = {}

  protected def tooltipCosts(stack: ItemStack, tooltip: java.util.List[Component]): Unit = {
    stack.getComponent(OCComponents.ADDRESS).foreach { address =>
      val shortened = if (address.length > 13) address.substring(0, 13) + "..." else address
      tooltip.add(Component.literal("§8" + shortened + "§7"))
    }
  }

  // ----------------------------------------------------------------------- //

  override def computePreferredMountPoint(stack: ItemStack, robot: Robot, availableMountPoints: util.Set[String]): String =
    ItemUpgradeRenderer.preferredMountPoint(stack, availableMountPoints)

  @OnlyIn(Dist.CLIENT)
  override def render(matrix: PoseStack, buffer: MultiBufferSource, light: Int, stack: ItemStack, mountPoint: MountPoint, robot: Robot, pt: Float): Unit =
    ItemUpgradeRenderer.render(matrix, buffer, light, stack, mountPoint)
}
