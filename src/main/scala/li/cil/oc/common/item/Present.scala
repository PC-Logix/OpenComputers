package li.cil.oc.common.item

import li.cil.oc.OpenComputers
import li.cil.oc.common.item.Present.LOOT_TABLE
import li.cil.oc.util.InventoryUtils
import net.minecraft.core.registries.Registries
import net.minecraft.resources.{ResourceKey, ResourceLocation}
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.{SoundEvents, SoundSource}
import net.minecraft.world.{InteractionHand, InteractionResultHolder}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.{Item, ItemStack}
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.loot.{LootParams, LootTable}
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.neoforged.neoforge.common.extensions.IItemExtension

class Present(props: Properties) extends Item(props) with traits.SimpleItem with IItemExtension {
  override def use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder[ItemStack] = {
    val stack = player.getItemInHand(hand)
    if (!stack.isEmpty) {
      stack.shrink(1)
      if (!level.isClientSide) {
        level.playSound(player, player.getX, player.getY, player.getZ, SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.2f, 1f)

        val serverLevel = level.asInstanceOf[ServerLevel]
        serverLevel.getServer.reloadableRegistries().getLootTable(LOOT_TABLE).getRandomItems(
          new LootParams.Builder(serverLevel).withLuck(player.getLuck).create(LootContextParamSets.EMPTY),
          InventoryUtils.addToPlayerInventory(_, player)
        )
      }
    }
    InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
  }
}

object Present {
  val LOOT_TABLE: ResourceKey[LootTable] = ResourceKey.create(
    Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "presents")
  )
}
