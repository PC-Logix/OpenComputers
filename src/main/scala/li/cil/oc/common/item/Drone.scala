package li.cil.oc.common.item

import li.cil.oc.common.entity
import li.cil.oc.common.item.data.DroneData
import li.cil.oc.server.agent
import li.cil.oc.util.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.{Item, ItemStack, TooltipFlag}
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.InteractionResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.extensions.IItemExtension

import java.util

class Drone(props: Properties) extends Item(props) with traits.SimpleItem with IItemExtension {
  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[Component], flag: TooltipFlag): Unit = {
    if (Tooltip.showExtendedTooltip(flag)) {
      val info = new DroneData(stack)
      for (component <- info.components if !component.isEmpty) {
        tooltip.add(Component.literal("- " + component.getHoverName.getString).setStyle(Tooltip.DefaultStyle))
      }
    }
  }

  override def useOn(ctx: UseOnContext): InteractionResult = {
    val world = ctx.getLevel
    if (!world.isClientSide) {
      val drone = entity.EntityTypes.DRONE.get().create(world)
      ctx.getPlayer match {
        case fakePlayer: agent.Player =>
          drone.ownerName = fakePlayer.agent.ownerName
          drone.ownerUUID = fakePlayer.agent.ownerUUID
        case player: Player =>
          drone.ownerName = player.getName.getString
          drone.ownerUUID = player.getGameProfile.getId
      }
      drone.initializeAfterPlacement(ctx.getItemInHand, new Vec3(
        ctx.getClickedPos.getX + (ctx.getClickLocation.x - ctx.getClickedPos.getX) * 1.1,
        ctx.getClickedPos.getY + (ctx.getClickLocation.y - ctx.getClickedPos.getY) * 1.1,
        ctx.getClickedPos.getZ + (ctx.getClickLocation.z - ctx.getClickedPos.getZ) * 1.1,
      ))
      world.addFreshEntity(drone)
    }
    ctx.getItemInHand.shrink(1)
    InteractionResult.sidedSuccess(world.isClientSide)
  }
}
