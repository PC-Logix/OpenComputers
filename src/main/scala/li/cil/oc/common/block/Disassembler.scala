package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.blockentity
import li.cil.oc.common.blockentity.BlockEntityTypes
import li.cil.oc.common.menu.MenuTypes
import li.cil.oc.util.Tooltip
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.level.{Level => World}
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState

import java.util

class Disassembler(props: Properties) extends SimpleBlock(props) with traits.PowerAcceptor with traits.StateAware with traits.GUI with traits.Tickable {
  override protected def tooltipBody(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], flag: TooltipFlag): Unit = {
    val chance = Settings.get.disassemblerBreakChance
    if (chance > 0) {
      Tooltip.add(tooltip, flag, "disassembler.loss", (chance * 100).toInt.toString)
    } else {
      Tooltip.add(tooltip, flag, "disassembler")
    }
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.disassemblerRate

  override def openGui(player: ServerPlayer, world: World, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: blockentity.Disassembler => MenuTypes.openDisassemblerGui(player, te)
    case _ =>
  }

  override def newBlockEntity(pos: BlockPos, state: BlockState) = new blockentity.Disassembler(pos, state)

  override def getBlockEntityType: BlockEntityType[_ <: BlockEntity] = BlockEntityTypes.DISASSEMBLER.get()
}
