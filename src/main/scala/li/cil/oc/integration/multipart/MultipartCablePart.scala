package li.cil.oc.integration.multipart

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.multipart.api.MultipartType
import codechicken.multipart.api.part.{CapabilityProviderPart, MultiPart}
import codechicken.multipart.minecraft.McStatePart
import codechicken.multipart.util.PartRayTraceResult
import li.cil.oc.{Constants, Settings, api}
import li.cil.oc.api.network.{Environment, EnvironmentHost, Message, Node, Visibility}
import li.cil.oc.common.{Capabilities, EventHandler}
import li.cil.oc.common.block.CableHelper
import li.cil.oc.common.block.property.PropertyCableConnection
import li.cil.oc.common.init.OCBlocks
import li.cil.oc.util.{Color, ItemColorizer}
import net.minecraft.core.{BlockPos, Direction, HolderLookup}
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.{DyeColor, ItemStack}
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.{CollisionContext, VoxelShape}
import net.neoforged.neoforge.capabilities.BlockCapability

/** Cable part used only while CB Multipart owns the containing block entity. */
final class MultipartCablePart(
  initialState: BlockState = OCBlocks.Cable.get.defaultBlockState(),
  original: Option[li.cil.oc.common.blockentity.Cable] = None)
  extends McStatePart(initialState) with CapabilityProviderPart with Environment with EnvironmentHost with li.cil.oc.api.internal.Colored {

  private var color = Color.rgbValues(DyeColor.LIGHT_GRAY)
  override val node: Node = api.Network.newNode(this, Visibility.None).create()
  original.foreach(cable => color = cable.getColor)

  override def getType: MultipartType[_] = MultipartIntegration.cableMultipartType
  override def defaultBlockState: BlockState = OCBlocks.Cable.get.defaultBlockState()
  /** Resolve the same connection properties as the normal cable immediately before baking its block model. */
  override def getCurrentState: BlockState = {
    updateConnections()
    state
  }
  override def getDropStack: ItemStack = createItemStack()
  override def getCloneStack(hit: PartRayTraceResult, player: Player): ItemStack = createItemStack()
  override def getCloneStack(hit: PartRayTraceResult): ItemStack = createItemStack()

  override def useItemOn(stack: ItemStack, player: Player, hit: PartRayTraceResult, hand: InteractionHand): ItemInteractionResult = {
    if (!Color.isDye(stack)) return super.useItemOn(stack, player, hit, hand)
    setColor(Color.rgbValues(Color.dyeColor(stack)))
    if (!player.isCreative) stack.shrink(1)
    ItemInteractionResult.sidedSuccess(level.isClientSide)
  }

  override def useWithoutItem(player: Player, hit: PartRayTraceResult): InteractionResult = {
    val stack = player.getItemInHand(InteractionHand.MAIN_HAND)
    if (!Color.isDye(stack)) return super.useWithoutItem(player, hit)
    setColor(Color.rgbValues(Color.dyeColor(stack)))
    if (!player.isCreative) stack.shrink(1)
    InteractionResult.sidedSuccess(level.isClientSide)
  }

  def copyColor(value: Int): Unit = color = value
  override def getColor: Int = color
  override def setColor(value: Int): Unit = if (value != color) {
    color = value
    updateConnections()
    markChanged()
    if (hasLevel && !level.isClientSide) {
      sendUpdate(out => writeDesc(out))
      api.Network.joinOrCreateNetwork(level, pos)
      refreshAdjacentBlocks()
    }
  }
  override def controlsConnectivity: Boolean = true

  private def createItemStack(): ItemStack = {
    val stack = api.Items.get(Constants.BlockName.Cable).createItemStack(1)
    if (color != Color.rgbValues(DyeColor.LIGHT_GRAY)) ItemColorizer.setColor(stack, color)
    stack
  }

  override def getShape(context: CollisionContext): VoxelShape = state.getShape(level, pos, context)
  override def getCollisionShape(context: CollisionContext): VoxelShape = state.getCollisionShape(level, pos, context)
  // The block's default occlusion shape is a full cube even though the cable model is not.
  // Multipart parts must report the geometry they actually render or covers treat the
  // whole cable cell as solid and hide adjacent multipart geometry incorrectly.
  override def getRenderOcclusionShape: VoxelShape = state.getShape(level, pos, CollisionContext.empty())

  override def getCapability[T, C](capability: BlockCapability[T, C], context: C): T = {
    if (capability == Capabilities.EnvironmentCapability) this.asInstanceOf[T]
    else if (capability == Capabilities.ColoredCapability) this.asInstanceOf[T]
    else null.asInstanceOf[T]
  }

  override def getEnvironmentLevel: Level = if (hasLevel) level else null
  override def xPosition: Double = pos.getX + 0.5
  override def yPosition: Double = pos.getY + 0.5
  override def zPosition: Double = pos.getZ + 0.5
  override def markChanged(): Unit = if (hasTile) tile.setChanged()

  override def onMessage(message: Message): Unit = ()
  override def onConnect(other: Node): Unit = ()
  override def onDisconnect(other: Node): Unit = ()

  override def onAdded(): Unit = {
    super.onAdded()
    updateConnections()
    if (hasLevel && !level.isClientSide) EventHandler.scheduleServer(() => if (hasLevel) api.Network.joinOrCreateNetwork(level, pos))
    refreshAdjacentBlocks()
  }

  override def onPartChanged(other: MultiPart): Unit = {
    super.onPartChanged(other)
    updateConnections()
    if (hasLevel && !level.isClientSide) EventHandler.scheduleServer(() => if (hasLevel) api.Network.joinOrCreateNetwork(level, pos))
    refreshAdjacentBlocks()
  }

  override def onNeighborBlockChanged(changedPos: BlockPos): Unit = {
    super.onNeighborBlockChanged(changedPos)
    updateConnections()
  }

  override def onRemoved(): Unit = {
    super.onRemoved()
    if (node != null) node.remove()
  }

  override def onChunkUnload(): Unit = {
    super.onChunkUnload()
    if (node != null) node.remove()
  }

  override def onChunkLoad(chunk: net.minecraft.world.level.chunk.LevelChunk): Unit = {
    super.onChunkLoad(chunk)
    if (hasLevel && !level.isClientSide) EventHandler.scheduleServer(() => if (hasLevel) {
      api.Network.joinOrCreateNetwork(level, pos)
    })
    refreshAdjacentBlocks()
  }

  override def onWorldJoin(): Unit = {
    super.onWorldJoin()
    updateConnections()
    if (hasLevel && !level.isClientSide) EventHandler.scheduleServer(() => if (hasLevel) api.Network.joinOrCreateNetwork(level, pos))
    refreshAdjacentBlocks()
  }

  override def onWorldSeparate(): Unit = {
    super.onWorldSeparate()
    if (node != null) node.remove()
  }

  override def invalidateConvertedTile(): Unit = {
    super.invalidateConvertedTile()
    if (hasLevel && !level.isClientSide) original.foreach(cable =>
      cable.node.neighbors.forEach(neighbor => neighbor.connect(node)))
  }

  override def save(tag: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.save(tag, provider)
    tag.putInt(Settings.namespace + "renderColor", color)
    if (node != null) {
      val nodeTag = new CompoundTag()
      node.saveData(nodeTag, provider)
      tag.put(Settings.namespace + "node", nodeTag)
    }
  }

  override def load(tag: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.load(tag, provider)
    color = tag.getInt(Settings.namespace + "renderColor")
    if (color == 0) color = Color.rgbValues(DyeColor.LIGHT_GRAY)
    if (node != null && tag.contains(Settings.namespace + "node")) node.loadData(tag.getCompound(Settings.namespace + "node"), provider)
  }

  override def writeDesc(out: MCDataOutput): Unit = {
    super.writeDesc(out)
    out.writeInt(color)
  }

  override def readDesc(in: MCDataInput): Unit = {
    super.readDesc(in)
    color = in.readInt()
  }

  private def refreshAdjacentBlocks(): Unit = if (hasLevel && !level.isClientSide)
    EventHandler.scheduleServer(() => if (hasLevel && MultipartColorLookup.cablePart(level, pos).contains(this))
      level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock))

  /** Keep the cable model's arms and end caps aligned with current neighbors. */
  private def updateConnections(): Unit = if (hasLevel) {
    val gray = Color.rgbValues(DyeColor.LIGHT_GRAY)
    val baseState = OCBlocks.Cable.get.defaultBlockState()
    val nextState = Direction.values.foldLeft(baseState) { (next, side) =>
      val neighborPos = pos.relative(side)
      val neighborSide = side.getOpposite
      val multipartCable = MultipartColorLookup.cablePart(level, neighborPos)
      val canPassMultipart = MultipartColorLookup.canConnectFromSide(level, pos, side) &&
        MultipartColorLookup.canConnectFromSide(level, neighborPos, side.getOpposite)
      val sided = level.getCapability(Capabilities.SidedEnvironmentCapability, neighborPos, neighborSide)
      // MultipartCablePart is an unsided Environment. A sided capability can
      // be present on the multipart tile while returning no sided node on the
      // server, even though this neighboring cable part has a live network node.
      val hasNode = canPassMultipart && (multipartCable.nonEmpty || (if (sided != null) {
        if (level.isClientSide) sided.canConnect(neighborSide)
        else sided.sidedNode(neighborSide) != null
      }
      else level.getCapability(Capabilities.EnvironmentCapability, neighborPos, neighborSide) != null))

      val neighborColor = level.getCapability(Capabilities.ColoredCapability, neighborPos, null)
      val otherColor = multipartCable.map(_.getColor).getOrElse(if (neighborColor == null) gray else neighborColor.getColor)
      val compatibleColor = color == otherColor || color == gray || otherColor == gray
      // A normal OC cable uses its block state to distinguish cable-to-cable arms.
      // The neighboring block entity can be temporarily absent while CB Multipart
      // converts the other cable, so do not classify it as a device based only on caps.
      val isCable = multipartCable.nonEmpty || level.getBlockState(neighborPos).getBlock == OCBlocks.Cable.get
      val shape = if (!hasNode || !compatibleColor) PropertyCableConnection.Shape.NONE
      else if (isCable || neighborColor != null && neighborColor.controlsConnectivity) PropertyCableConnection.Shape.CABLE
      else PropertyCableConnection.Shape.DEVICE
      CableHelper.helperSetCableShapeState(next, side, shape)
    }

    if (state != nextState) {
      state = nextState
      if (hasTile) tile.notifyShapeChange()
      if (hasLevel && !level.isClientSide) sendUpdate(out => writeDesc(out))
    }
  }
}
