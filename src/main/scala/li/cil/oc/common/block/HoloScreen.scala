package li.cil.oc.common.block

import li.cil.oc.{api, Constants, Settings}
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.blockentity
import li.cil.oc.common.menu.MenuTypes
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.{PackedColor, Tooltip}
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.network.chat.{Component => ITextComponent}
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.{InteractionHand => Hand}
import net.minecraft.world.entity.player.{Player => PlayerEntity}
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.context.{BlockPlaceContext => BlockItemUseContext}
import net.minecraft.world.level.{BlockGetter => IBlockReader, Level => World}
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.{BlockState, StateDefinition => StateContainer}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.phys.shapes.{VoxelShape, CollisionContext => ISelectionContext, Shapes => VoxelShapes}

import java.util

class HoloScreen(props: Properties, tier: Int) extends Screen(props, tier) {
  private val FloorShape = VoxelShapes.box(0, 0, 0, 1, 0.5, 1)
  private val CeilingShape = VoxelShapes.box(0, 0.5, 0, 1, 1, 1)

  registerDefaultState(stateDefinition.any.
    setValue(PropertyRotatable.Mount, Direction.UP).
    setValue(PropertyRotatable.Facing, Direction.NORTH))

  override protected def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Mount, PropertyRotatable.Facing)

  override def getStateForPlacement(ctx: BlockItemUseContext): BlockState = {
    val mount =
      if (ctx.getClickedFace == Direction.DOWN) Direction.DOWN
      else Direction.UP

    super.getStateForPlacement(ctx).
      setValue(PropertyRotatable.Mount, mount).
      setValue(PropertyRotatable.Facing, ctx.getHorizontalDirection.getOpposite)
  }

  override def getShape(state: BlockState, world: IBlockReader, pos: BlockPos, ctx: ISelectionContext): VoxelShape =
    state.getValue(PropertyRotatable.Mount) match {
      case Direction.DOWN => CeilingShape
      case _ => FloorShape
    }

  override def newBlockEntity(pos: BlockPos, state: BlockState) = new blockentity.HoloScreen(pos, state, tier)

  override def getValidRotations(world: World, pos: BlockPos): Array[Direction] =
    Array(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)

  override def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean =
    if (Wrench.holdsApplicableWrench(player, pos) || api.Items.get(heldItem) == api.Items.get(Constants.ItemName.Analyzer)) {
      super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ)
    }
    else if (isResizeModifierDown(player)) {
      world.getBlockEntity(pos) match {
        case screen: blockentity.HoloScreen =>
          val changed = screen.resize(screen.resizeOperationForWorldSide(side))
          if (changed) {
            world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
          }
          true
        case _ => false
      }
    }
    else {
      world.getBlockEntity(pos) match {
        case screen: blockentity.HoloScreen =>
          player match {
            case serverPlayer: ServerPlayer if !world.isClientSide => MenuTypes.openHoloScreenGui(serverPlayer, screen)
            case _ =>
          }
          true
        case _ => false
      }
    }

  private def isResizeModifierDown(player: PlayerEntity): Boolean =
    player.isCrouching || player.isShiftKeyDown

  override protected def tooltipBody(stack: ItemStack, context: TooltipContext, tooltip: util.List[ITextComponent], flag: TooltipFlag): Unit = {
    val (w, h) = Settings.screenResolutionsByTier(tier)
    val depth = PackedColor.Depth.bits(Settings.screenDepthsByTier(tier))
    Tooltip.add(tooltip, flag, "screen", w, h, depth)
    Tooltip.addExtended(tooltip, flag, "holoscreen")
  }
}
