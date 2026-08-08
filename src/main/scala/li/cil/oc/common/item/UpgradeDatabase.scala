package li.cil.oc.common.item

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.container.DatabaseInventory
import li.cil.oc.common.menu.MenuTypes
import net.minecraft.core.component.DataComponents
import li.cil.oc.util.ExtendedItemStack._
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.entity.player.Player
import net.minecraft.world.InteractionResultHolder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.component.CustomData
import net.neoforged.neoforge.common.extensions.IItemExtension

class UpgradeDatabase(props: Properties, val tier: Int) extends Item(props) with traits.SimpleItem with traits.ItemTier with IItemExtension {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override protected def tooltipData = Seq(Settings.get.databaseEntriesPerTier(tier))

  override def use(stack: ItemStack, level: Level, player: Player): InteractionResultHolder[ItemStack] = {
    if (!player.isCrouching) {
      if (!level.isClientSide) player match {
        case srvPlr: ServerPlayer => MenuTypes.openDatabaseGui(srvPlr, new DatabaseInventory {
            override def container = stack

            override def stillValid(player: Player) = player == srvPlr
          })
        case _ =>
      }
      player.swing(InteractionHand.MAIN_HAND)
    }
    else {
      CustomData.update(DataComponents.CUSTOM_DATA, stack, data => {
        data.remove(Settings.namespace + "items")
      })
      player.swing(InteractionHand.MAIN_HAND)
    }
    new InteractionResultHolder(InteractionResult.sidedSuccess(level.isClientSide), stack)
  }
}
