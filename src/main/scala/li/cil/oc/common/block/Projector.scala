package li.cil.oc.common.block

import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.blockentity
import li.cil.oc.common.blockentity.BlockEntityTypes
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.Tooltip
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.{BlockBehaviour, BlockState, StateDefinition}
import net.minecraft.world.level.block.state.properties.{BlockStateProperties, BooleanProperty}
import net.minecraft.world.phys.shapes.{CollisionContext, Shapes, VoxelShape}

import java.util

object Projector {
  /** Block state is the authoritative, always-synced copy of the display mode. */
  final val ScreenMode: BooleanProperty = BooleanProperty.create("screen_mode")

  /** A small amount of block light while the projector is enabled. */
  final val LightLevel = 4

  final val LightEmission: java.util.function.ToIntFunction[BlockState] =
    new java.util.function.ToIntFunction[BlockState] {
      override def applyAsInt(state: BlockState): Int =
        if (state.getValue(BlockStateProperties.LIT)) LightLevel else 0
    }
}

class Projector(props: BlockBehaviour.Properties) extends SimpleBlock(props.lightLevel(Projector.LightEmission)) with traits.Tickable {
  val shape: VoxelShape = Shapes.box(0, 0, 0, 1, 0.75, 1)

  registerDefaultState(defaultBlockState()
    .setValue(PropertyRotatable.Facing, Direction.NORTH)
    .setValue(BlockStateProperties.LIT, java.lang.Boolean.TRUE)
    .setValue(Projector.ScreenMode, java.lang.Boolean.FALSE))

  override protected def createBlockStateDefinition(builder: StateDefinition.Builder[Block, BlockState]): Unit = {
    builder.add(PropertyRotatable.Facing, BlockStateProperties.LIT, Projector.ScreenMode)
  }

  override def getStateForPlacement(ctx: net.minecraft.world.item.context.BlockPlaceContext): BlockState =
    defaultBlockState().setValue(PropertyRotatable.Facing, ctx.getHorizontalDirection.getOpposite)

  override def getShape(state: BlockState, world: BlockGetter, pos: BlockPos, ctx: CollisionContext): VoxelShape = shape

  // The OC wrench handles SimpleBlocks through onItemUseFirst, before block
  // activation. Projectors use their facing property for the beam direction,
  // but a wrench hit is reserved for switching display modes.
  override def rotateBlock(world: net.minecraft.world.level.Level, pos: BlockPos, axis: Direction): Boolean = {
    world.getBlockEntity(pos) match {
      case projector: blockentity.Projector =>
        if (!world.isClientSide) projector.toggleMode()
        true
      case _ => false
    }
  }

  override def localOnBlockActivated(world: net.minecraft.world.level.Level, pos: BlockPos, player: Player,
                                     hand: InteractionHand, heldItem: ItemStack, side: Direction,
                                     hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (Wrench.holdsApplicableWrench(player, pos)) {
      if (!world.isClientSide) {
        world.getBlockEntity(pos) match {
          case projector: blockentity.Projector =>
            projector.toggleMode()
            Wrench.wrenchUsed(player, pos)
          case _ =>
        }
      }
      true
    }
    else false
  }

  override protected def tooltipBody(stack: ItemStack, context: TooltipContext, tooltip: util.List[net.minecraft.network.chat.Component], flag: TooltipFlag): Unit =
    Tooltip.add(tooltip, flag, "projector")

  override def newBlockEntity(pos: BlockPos, state: BlockState) = new blockentity.Projector(pos, state)

  override def getBlockEntityType: net.minecraft.world.level.block.entity.BlockEntityType[_ <: net.minecraft.world.level.block.entity.BlockEntity] = BlockEntityTypes.PROJECTOR.get()
}
