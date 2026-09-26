package li.cil.oc.common.block

import com.mojang.serialization.MapCodec
import li.cil.oc.common.block.Cable.CODEC
import li.cil.oc.common.block.property.PropertyCableConnection
import li.cil.oc.integration.multipart.ModCBMultipart
import li.cil.oc.common.blockentity
import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.util.{Color, ItemColorizer}
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.context.{BlockPlaceContext => BlockItemUseContext}
import net.minecraft.world.item.{DyeColor, ItemStack}
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.{BlockEntity => TileEntity}
import net.minecraft.world.level.block.state.BlockBehaviour.{Properties, simpleCodec}
import net.minecraft.world.level.block.state.{BlockState, StateDefinition => StateContainer}
import net.minecraft.world.level.{LevelReader, BlockGetter => IBlockReader, Level => World, LevelAccessor => IWorld}
import net.minecraft.world.phys.shapes.{VoxelShape, CollisionContext => ISelectionContext, Shapes => VoxelShapes}

class Cable(props: Properties) extends SimpleBlock(props) {
  override def codec(): MapCodec[Cable] = CODEC

  // For FMP part coloring.
  var colorMultiplierOverride: Option[Int] = None

  // ----------------------------------------------------------------------- //
  
  registerDefaultState(CableHelper.helperRegisterDefaultState(this.stateDefinition))

  override def getStateForPlacement(ctx: BlockItemUseContext): BlockState = {
    val color = Cable.getConnectionColor(ctx.getItemInHand)
    val fromPos = new BlockPos.MutableBlockPos()
    Direction.values.foldLeft(defaultBlockState)((state, fromSide) => {
      fromPos.setWithOffset(ctx.getClickedPos, fromSide)
      val fromState = ctx.getLevel.getBlockState(fromPos)
      Cable.updateState(state, null, color, fromSide, fromState, ctx.getLevel, fromPos)
    })
  }

  override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]): Unit = {
    builder.add(
      PropertyCableConnection.DOWN,
      PropertyCableConnection.UP,
      PropertyCableConnection.NORTH,
      PropertyCableConnection.SOUTH,
      PropertyCableConnection.WEST,
      PropertyCableConnection.EAST
    )
  }

  override def getCloneItemStack(world: LevelReader, pos: BlockPos, state: BlockState) =
    world.getBlockEntity(pos) match {
      case t: blockentity.Cable => t.createItemStack()
      case _ => createItemStack()
    }

  override def getShape(state: BlockState, world: IBlockReader, pos: BlockPos, ctx: ISelectionContext): VoxelShape = Cable.shape(state)

  override def neighborChanged(state: BlockState, world: World, pos: BlockPos, other: Block, otherPos: BlockPos, moved: Boolean): Unit = {
    if (world.isClientSide) return
    val newState = world.getBlockEntity(pos) match {
      case t: blockentity.Cable => {
        val fromPos = new BlockPos.MutableBlockPos()
        Direction.values.foldLeft(state)((state, fromSide) => {
          fromPos.setWithOffset(pos, fromSide)
          val fromState = world.getBlockState(fromPos)
          Cable.updateState(state, t, -1, fromSide, fromState, world, fromPos)
        })
      }
      case _ => state
    }
    if (newState != state) world.setBlock(pos, newState, 0x13)
  }

  override def updateShape(state: BlockState, fromSide: Direction, fromState: BlockState, world: IWorld, pos: BlockPos, fromPos: BlockPos): BlockState =
    Cable.updateState(state, world.getBlockEntity(pos), -1, fromSide, fromState, world, fromPos)

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(pos: BlockPos, state: BlockState) = new blockentity.Cable(pos, state)

  // ----------------------------------------------------------------------- //

  override def setPlacedBy(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity, stack: ItemStack): Unit = {
    super.setPlacedBy(world, pos, state, placer, stack)
    world.getBlockEntity(pos) match {
      case tileEntity: blockentity.Cable => {
        tileEntity.fromItemStack(stack)
        state.updateNeighbourShapes(world, pos, 2)
      }
      case _ =>
    }
  }
}

object Cable {
  final val CODEC = simpleCodec(new Cable(_))

  final val MIN = 0.375
  final val MAX = 1 - MIN

  final val DefaultShape: VoxelShape = VoxelShapes.box(MIN, MIN, MIN, MAX, MAX, MAX)

  final val CachedParts: Array[VoxelShape] = Array(
    VoxelShapes.box( MIN, 0, MIN, MAX, MIN, MAX ), // Down
    VoxelShapes.box( MIN, MAX, MIN, MAX, 1, MAX ), // Up
    VoxelShapes.box( MIN, MIN, 0, MAX, MAX, MIN ), // North
    VoxelShapes.box( MIN, MIN, MAX, MAX, MAX, 1 ), // South
    VoxelShapes.box( 0, MIN, MIN, MIN, MAX, MAX ), // West
    VoxelShapes.box( MAX, MIN, MIN, 1, MAX, MAX )) // East

  final val CachedBounds = {
    // 6 directions = 6 bits = 11111111b >> 2 = 0xFF >> 2
    (0 to 0xFF >> 2).map(mask => {
      Direction.values.foldLeft(DefaultShape)((shape, side) => {
        if (((1 << side.get3DDataValue) & mask) != 0) VoxelShapes.or(shape, CachedParts(side.ordinal()))
        else shape
      })
    }).toArray
  }

  def mask(side: Direction, value: Int = 0) = value | (1 << side.get3DDataValue)

  def shape(state: BlockState): VoxelShape = {
    var result = 0
    for (side <- Direction.values) {
      val sideShape = CableHelper.getCableShape(state, side)
      if (sideShape != PropertyCableConnection.Shape.NONE) {
        result = mask(side, result)
      }
    }
    Cable.CachedBounds(result)
  }

  def updateState(state: BlockState, tileEntity: TileEntity, defaultColor: Int, fromSide: Direction, fromState: BlockState, world: IBlockReader, fromPos: BlockPos): BlockState = {
    val prop = PropertyCableConnection.BY_DIRECTION.get(fromSide)
    val neighborTileEntity = world.getBlockEntity(fromPos)
    if (neighborTileEntity != null && neighborTileEntity.getLevel != null) {
      val neighborHasNode = hasNetworkNode(neighborTileEntity, fromSide.getOpposite)
      val canConnectColor = canConnectBasedOnColor(tileEntity, neighborTileEntity, defaultColor)
      // The checked multipart cell is the neighbor; its direction back to this cable is opposite.
      val canConnectMultipart = ModCBMultipart.canConnectFromSide(world, fromPos, fromSide.getOpposite)
      if (neighborHasNode && canConnectColor && canConnectMultipart) {
        if (fromState.is(state.getBlock) || ModCBMultipart.isCable(world, fromPos)) {
          return CableHelper.helperSetCableShapeState(state, fromSide, PropertyCableConnection.Shape.CABLE)
        }
        else {
          return CableHelper.helperSetCableShapeState(state, fromSide, PropertyCableConnection.Shape.DEVICE)
        }
      }
    }
    CableHelper.helperSetCableShapeState(state, fromSide, PropertyCableConnection.Shape.NONE)
  }

  private def hasNetworkNode(tileEntity: TileEntity, side: Direction): Boolean = {
    if (tileEntity != null) {
      if (tileEntity.isInstanceOf[blockentity.RobotProxy]) return false

      val level = tileEntity.getLevel
      val pos = tileEntity.getBlockPos
      if (level != null) {
        Option(level.getCapability(Capabilities.SidedEnvironmentCapability, pos, side)) match {
          case Some(host) =>
            return if (level.isClientSide) host.canConnect(side) else host.sidedNode(side) != null
          case _ =>
        }
        if (level.getCapability(Capabilities.EnvironmentCapability, pos, side) != null) return true
      }
    }

    false
  }

  private def getConnectionColor(stack: ItemStack): Int = {
    val color = ItemColorizer.getColor(stack)
    if (color == -1) Color.rgbValues(DyeColor.LIGHT_GRAY) else color
  }

  private def getConnectionColor(tileEntity: TileEntity): Int = {
    if (tileEntity != null) {
      val level = tileEntity.getLevel
      val pos = tileEntity.getBlockPos
      if (level != null) {
        Option(level.getCapability(Capabilities.ColoredCapability, pos, null)) match {
          case Some(colored) if colored.controlsConnectivity => return colored.getColor
          case _ =>
        }
      }
    }

    Color.rgbValues(DyeColor.LIGHT_GRAY)
  }

  private def canConnectBasedOnColor(te1: TileEntity, te2: TileEntity, c1Default: Int = Color.rgbValues(DyeColor.LIGHT_GRAY)) = {
    val (c1, c2) = (if (te1 == null) c1Default else getConnectionColor(te1), getConnectionColor(te2))
    c1 == c2 || c1 == Color.rgbValues(DyeColor.LIGHT_GRAY) || c2 == Color.rgbValues(DyeColor.LIGHT_GRAY)
  }
}
