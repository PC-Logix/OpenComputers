package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.util.{PackedColor, Tooltip}
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.network.chat.{Component => ITextComponent}
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.context.{BlockPlaceContext => BlockItemUseContext}
import net.minecraft.world.level.{BlockGetter => IBlockReader}
import net.minecraft.world.level.block.state.{BlockState, StateDefinition => StateContainer}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.shapes.{VoxelShape, CollisionContext => ISelectionContext, Shapes => VoxelShapes}

import java.util

class FlatScreen(props: Properties, tier: Int, val isBack: Boolean) extends Screen(props, tier) {
  private val NorthShape = VoxelShapes.box(0, 0, 15.0 / 16.0, 1, 1, 1)
  private val EastShape = VoxelShapes.box(0, 0, 0, 1.0 / 16.0, 1, 1)
  private val SouthShape = VoxelShapes.box(0, 0, 0, 1, 1, 1.0 / 16.0)
  private val WestShape = VoxelShapes.box(15.0 / 16.0, 0, 0, 1, 1, 1)
  private val UpShape = VoxelShapes.box(0, 0, 0, 1, 1.0 / 16.0, 1)
  private val DownShape = VoxelShapes.box(0, 15.0 / 16.0, 0, 1, 1, 1)

  override protected def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw)

  override def getStateForPlacement(ctx: BlockItemUseContext): BlockState = {
    val (pitch, yaw) = ctx.getClickedFace match {
      case side@(Direction.DOWN | Direction.UP) => (side, ctx.getHorizontalDirection)
      case side => (Direction.NORTH, side)
    }
    super.getStateForPlacement(ctx).setValue(PropertyRotatable.Pitch, pitch).setValue(PropertyRotatable.Yaw, yaw)
  }

  override def getShape(state: BlockState, world: IBlockReader, pos: BlockPos, ctx: ISelectionContext): VoxelShape =
    shapeFor(mountFace(state))

  override protected def tooltipBody(stack: ItemStack, context: TooltipContext, tooltip: util.List[ITextComponent], flag: TooltipFlag): Unit = {
    val (w, h) = Settings.screenResolutionsByTier(tier)
    val depth = PackedColor.Depth.bits(Settings.screenDepthsByTier(tier))
    Tooltip.add(tooltip, flag, "screen", w, h, depth)
  }

  private def mountFace(state: BlockState): Direction = {
    val facing = state.getValue(PropertyRotatable.Pitch) match {
      case side@(Direction.DOWN | Direction.UP) => side
      case _ => state.getValue(PropertyRotatable.Yaw)
    }
    if (isBack) facing else facing.getOpposite
  }

  private def shapeFor(facing: Direction): VoxelShape = facing match {
    case Direction.NORTH => NorthShape
    case Direction.EAST => EastShape
    case Direction.SOUTH => SouthShape
    case Direction.WEST => WestShape
    case Direction.UP => UpShape
    case Direction.DOWN => DownShape
  }
}
