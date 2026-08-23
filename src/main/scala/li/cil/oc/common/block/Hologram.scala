package li.cil.oc.common.block

import li.cil.oc.common.blockentity
import li.cil.oc.common.blockentity.BlockEntityTypes
import li.cil.oc.util.Tooltip
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.{Component => ITextComponent}
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.level.{BlockGetter => IBlockReader}
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.{VoxelShape, CollisionContext => ISelectionContext, Shapes => VoxelShapes}

import java.util

class Hologram(props: Properties, val tier: Int) extends SimpleBlock(props) with traits.Tickable {
  val shape = VoxelShapes.box(0, 0, 0, 1, 0.5, 1)

  // ----------------------------------------------------------------------- //

  override def getShape(state: BlockState, world: IBlockReader, pos: BlockPos, ctx: ISelectionContext): VoxelShape = shape

  // ----------------------------------------------------------------------- //

  override protected def tooltipBody(stack: ItemStack, context: TooltipContext, tooltip: util.List[ITextComponent], flag: TooltipFlag): Unit = {
    Tooltip.add(tooltip, flag, getClass.getSimpleName.toLowerCase() + tier)
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(pos: BlockPos, state: BlockState) = new blockentity.Hologram(pos, state, tier)

  override def getBlockEntityType: BlockEntityType[_ <: BlockEntity] = BlockEntityTypes.HOLOGRAM.get()
}
