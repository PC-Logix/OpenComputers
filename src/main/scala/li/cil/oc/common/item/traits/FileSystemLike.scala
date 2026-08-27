package li.cil.oc.common.item.traits

import li.cil.oc.Localization
import li.cil.oc.client.gui
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.common.item.data.DriveData
import li.cil.oc.util.{ItemUtils, Tooltip}
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.{InteractionHand, InteractionResultHolder}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.{Dist, OnlyIn}

import java.util

trait FileSystemLike extends SimpleItem {
  override protected def tooltipName = None

  def kiloBytes: Int

  override def appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], flag: TooltipFlag): Unit = {
    val label = stack.get(OCComponents.LABEL)
    if (label != null) tooltip.add(Component.literal(label).setStyle(Tooltip.DefaultStyle))

    if (flag.isAdvanced) {
      val fsNbt = stack.get(OCComponents.FILESYSTEM_DATA)
      if (fsNbt != null && fsNbt.contains("capacity.used")) {
        val used = fsNbt.getLong("capacity.used")
        tooltip.add(Component.literal(Localization.Tooltip.DiskUsage(used, kiloBytes * 1024)).setStyle(Tooltip.DefaultStyle))
      }
    }

    val data = new DriveData(stack)
    tooltip.add(Component.literal(Localization.Tooltip.DiskMode(data.isUnmanaged)).setStyle(Tooltip.DefaultStyle))
    if (data.isLocked) tooltip.add(Component.literal(Localization.Tooltip.DiskLock(data.lockInfo)).setStyle(Tooltip.DefaultStyle))

    super.appendHoverText(stack, context, tooltip, flag)
  }

  override def use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder[ItemStack] = {
    val stack = player.getItemInHand(hand)
    val tag = ItemUtils.getTag(stack)
    if (!player.isCrouching && (tag == null || !stack.has(OCComponents.LOOT_DISK))) {
      if (level.isClientSide) showGui(stack, player)
    }

    InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
  }

  @OnlyIn(Dist.CLIENT)
  private def showGui(stack: ItemStack, player: Player): Unit = {
    Minecraft.getInstance.pushGuiLayer(new gui.Drive(player.getInventory, () => stack))
  }
}
