package li.cil.oc.common.block

import li.cil.oc.client.KeyBindings
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.blockentity
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.common.item.data.RaidData
import li.cil.oc.common.menu.MenuTypes
import li.cil.oc.server.loot.LootFunctions
import li.cil.oc.util.ExtendedDataComponentHolder._
import li.cil.oc.util.Tooltip
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.{Component => ITextComponent}
import net.minecraft.server.level.{ServerPlayer => ServerPlayerEntity}
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.{Player => PlayerEntity}
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.level.{Level => World}
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.{BlockState, StateDefinition => StateContainer}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams

import java.util


class Raid(props: Properties) extends SimpleBlock(props) with traits.GUI {

  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Facing)

  override protected def tooltipTail(stack: ItemStack, context: TooltipContext, tooltip: util.List[ITextComponent], flag: TooltipFlag): Unit = {
    super.tooltipTail(stack, context, tooltip, flag)
    if (Tooltip.showExtendedTooltip(flag)) {
      val data = new RaidData(stack)
      for (disk <- data.disks if !disk.isEmpty) {
        tooltip.add(ITextComponent.literal("- " + disk.getHoverName.getString).setStyle(Tooltip.DefaultStyle))
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def openGui(player: ServerPlayerEntity, world: World, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: blockentity.Raid => MenuTypes.openRaidGui(player, te)
    case _ =>
  }

  override def newBlockEntity(pos: BlockPos, state: BlockState) = new blockentity.Raid(pos, state)

  // ----------------------------------------------------------------------- //

  override def hasAnalogOutputSignal(state: BlockState): Boolean = true

  override def getAnalogOutputSignal(state: BlockState, world: World, pos: BlockPos): Int =
    world.getBlockEntity(pos) match {
      case raid: blockentity.Raid if raid.presence.forall(ok => ok) => 15
      case _ => 0
    }

  override def setPlacedBy(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity, stack: ItemStack): Unit = {
    super.setPlacedBy(world, pos, state, placer, stack)
    world.getBlockEntity(pos) match {
      case tileEntity: blockentity.Raid if !world.isClientSide => {
        val data = new RaidData(stack)
        for (i <- 0 until math.min(data.disks.length, tileEntity.getContainerSize)) {
          tileEntity.setItem(i, data.disks(i))
        }
        data.label.foreach(tileEntity.label.setLabel)
        for (address <- stack.getComponent(OCComponents.ADDRESS)) {
          tileEntity.tryCreateRaid(address)
          tileEntity.filesystem.foreach(_.loadData(stack))
        }
      }
      case _ =>
    }
  }

  override def getDrops(state: BlockState, ctx: LootParams.Builder): util.List[ItemStack] = {
    val newCtx = ctx.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, f => {
      ctx.getOptionalParameter(LootContextParams.BLOCK_ENTITY) match {
        case tileEntity: blockentity.Raid =>
          val stack = createItemStack()
          if (tileEntity.items.exists(!_.isEmpty)) {
            val data = new RaidData()
            data.disks = tileEntity.items.clone()
            val reg = tileEntity.getLevel.registryAccess()
            tileEntity.filesystem.foreach(_.saveData(stack))
            data.label = Option(tileEntity.label.getLabel(reg))
            data.saveData(stack)
          }
          f.accept(stack)
        case _ =>
      }
    })
    super.getDrops(state, newCtx)
  }

  override def playerWillDestroy(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity): BlockState = {
    if (!world.isClientSide && player.isCreative) {
      world.getBlockEntity(pos) match {
        case tileEntity: blockentity.Raid if tileEntity.items.exists(!_.isEmpty) =>
          Block.dropResources(state, world, pos, tileEntity, player, player.getMainHandItem)
        case _ =>
      }
    }
    super.playerWillDestroy(world, pos, state, player)
  }
}
