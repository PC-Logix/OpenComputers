package li.cil.oc.common.item.traits

import java.util
import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.client.gui
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.common.item.data.DriveData
import li.cil.oc.util.ExtendedItemStack._
import li.cil.oc.util.{ItemUtils, Tooltip}
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.minecraft.world.level.Level
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item.TooltipContext

trait FileSystemLike extends SimpleItem {
  override protected def tooltipName: Option[String] = None

  def kiloBytes: Int

  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], flag: TooltipFlag): Unit = {
    super.appendHoverText(stack, context, tooltip, flag)
    val nbt = ItemUtils.getTag(stack)
    if (nbt != null) {
      if (nbt.contains(Settings.namespace + "data")) {
        val data = nbt.getCompound(Settings.namespace + "data")
        if (data.contains(Settings.namespace + "fs.label")) {
          tooltip.add(Component.literal(data.getString(Settings.namespace + "fs.label")).setStyle(Tooltip.DefaultStyle))
        }
        if (flag.isAdvanced && data.contains("fs")) {
          val fsNbt = data.getCompound("fs")
          if (fsNbt.contains("capacity.used")) {
            val used = fsNbt.getLong("capacity.used")
            tooltip.add(Component.literal(Localization.Tooltip.DiskUsage(used, kiloBytes * 1024)).setStyle(Tooltip.DefaultStyle))
          }
        }
      }

      val data = new DriveData(stack)
      tooltip.add(Component.literal(Localization.Tooltip.DiskMode(data.isUnmanaged)).setStyle(Tooltip.DefaultStyle))
      tooltip.add(Component.literal(Localization.Tooltip.DiskLock(data.lockInfo)).setStyle(Tooltip.DefaultStyle))
    }
  }

  override def use(stack: ItemStack, level: Level, player: Player): InteractionResultHolder[ItemStack] = {
    val tag = ItemUtils.getTag(stack)
    if (!player.isCrouching && (tag == null || !stack.has(OCComponents.LOOT_DISK))) {
      if (level.isClientSide) showGui(stack, player)
      player.swing(InteractionHand.MAIN_HAND)
    }
    new InteractionResultHolder(InteractionResult.sidedSuccess(level.isClientSide), stack)
  }

  @OnlyIn(Dist.CLIENT)
  private def showGui(stack: ItemStack, player: Player): Unit = {
    Minecraft.getInstance.pushGuiLayer(new gui.Drive(player.getInventory, () => stack))
  }
}
