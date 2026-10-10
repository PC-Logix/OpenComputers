package li.cil.oc.common.blockentity

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.common.datacomponents.{OCComponents, VideoMode}
import li.cil.oc.util.ExtendedDataComponentHolder._
import net.minecraft.core.component.DataComponentHolder
import net.minecraft.core.{BlockPos, Direction, HolderLookup}
import li.cil.oc.api
import li.cil.oc.api.network.Node
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.menu
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.{Container, MenuProvider}
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.neoforged.neoforge.common.MutableDataComponentHolder

class HoloScreen(pos: BlockPos, state: BlockState, tier: Int) extends Screen(pos, state, tier) with traits.Inventory with MenuProvider {
  private final val ConfigWidthTag = Settings.namespace + "configWidth"
  private final val ConfigHeightTag = Settings.namespace + "configHeight"
  private final val KeyboardTag = Settings.namespace + "keyboard"
  private final val HasInternalKeyboardTag = Settings.namespace + "hasInternalKeyboard"
  private final val KeyboardSlot = 0

  private var internalKeyboard: Option[api.internal.Keyboard] = None
  private var clientHasInternalKeyboard = false

  shouldCheckForMultiBlock = false
  delayUntilCheckForMultiBlock = 0

  override def facing: Direction = mount

  def mount: Direction = if (getLevel != null && getLevel.isLoaded(getBlockPos)) getBlockState.getValue(PropertyRotatable.Mount) else Direction.UP

  override def hasKeyboard: Boolean = hasInternalKeyboard || super.hasKeyboard

  def hasInternalKeyboard: Boolean = if (isClient) clientHasInternalKeyboard else !getItem(KeyboardSlot).isEmpty

  override protected def inventoryName = "holoscreen"

  override def getContainerSize: Int = 1

  override def getMaxStackSize: Int = 1

  override def getInventoryStackRequired: Int = 1

  override def canPlaceItem(slot: Int, stack: ItemStack): Boolean =
    slot == KeyboardSlot && api.Items.get(stack) == api.Items.get(Constants.BlockName.Keyboard)

  override def createMenu(id: Int, playerInventory: Inventory, player: Player): menu.HoloScreen =
    new menu.HoloScreen(id, playerInventory, this)

  override def getDisplayName: Component = Component.translatable(Settings.namespace + "container.holoscreen")

  override def checkMultiBlock(): Unit = {
    shouldCheckForMultiBlock = false
    origin = this
    screens.clear()
    screens += this
    //cachedBounds = None
  }

  def resize(operation: Direction): Boolean = {
    val oldWidth = width
    val oldHeight = height
    operation match {
      case Direction.UP =>
        height = (height + 1) min Settings.get.maxScreenHeight
      case Direction.DOWN =>
        height = (height - 1) max 1
      case Direction.EAST =>
        width = (width + 1) min Settings.get.maxScreenWidth
      case Direction.WEST =>
        width = (width - 1) max 1
      case _ =>
    }
    if (oldWidth != width || oldHeight != height) {
      buffer.setAspectRatio(width, height)
      //cachedBounds = None
      setChanged()
      if (getLevel != null && !getLevel.isClientSide) {
        getLevel.sendBlockUpdated(getBlockPos, getBlockState, getBlockState, 3)
      }
      true
    }
    else false
  }

  def resizeOperationForWorldSide(side: Direction): Direction =
    if (side == screenTopSide) Direction.UP
    else if (side == screenTopSide.getOpposite) Direction.DOWN
    else if (side == screenRight) Direction.EAST
    else if (side == screenRight.getOpposite) Direction.WEST
    else Direction.UP

  def projectionBeamAnchor(screenHeight: Int): Float =
    if (isCeilingMounted) screenHeight.toFloat else screenHeight - 1f

  def projectionBeamProjectorEdgeOffset: Float =
    if (isCeilingMounted) -0.6f else 0.6f

  def projectionPlaneLeft(screenWidth: Int): Float =
    -((screenWidth - 1) / 2f)

  def projectionPlaneTop(screenHeight: Int): Float =
    if (isCeilingMounted) screenHeight.toFloat else -1f

  def projectionDepth: Float =
    -0.46f

  private def updateInternalKeyboard(): Unit = {
    val shouldHaveKeyboard = hasInternalKeyboard
    if (shouldHaveKeyboard && internalKeyboard.isEmpty && isServer) {
      val keyboardItem = api.Items.get(Constants.BlockName.Keyboard).createItemStack(1)
      internalKeyboard = Option(api.Driver.driverFor(keyboardItem, getClass).createEnvironment(keyboardItem, this).asInstanceOf[api.internal.Keyboard])
      connectInternalKeyboard()
    }
    else if (!shouldHaveKeyboard && internalKeyboard.nonEmpty) {
      internalKeyboard.foreach(_.node.remove())
      internalKeyboard = None
    }
  }

  private def connectInternalKeyboard(): Unit =
    if (isServer && isConnected) internalKeyboard.foreach(keyboard => buffer.node.connect(keyboard.node))

  override def onConnect(node: Node): Unit = {
    super.onConnect(node)
    if (node == this.node) connectInternalKeyboard()
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack): Unit = {
    super.onItemAdded(slot, stack)
    updateInternalKeyboard()
    cachedBounds = None
    syncToClient()
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack): Unit = {
    super.onItemRemoved(slot, stack)
    updateInternalKeyboard()
    cachedBounds = None
    syncToClient()
  }

  override def dispose(): Unit = {
    internalKeyboard.foreach(_.node.remove())
    internalKeyboard = None
    super.dispose()
  }

  override def loadForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForServer(nbt, provider)
    updateInternalKeyboard()
    internalKeyboard.foreach(_.loadData(nbt.getCompound(KeyboardTag), provider))
  }

  override def saveForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveForServer(nbt, provider)
    internalKeyboard.foreach(keyboard =>
      nbt.setNewCompoundTag(KeyboardTag, (tag: CompoundTag) => keyboard.saveData(tag, provider)))
  }

  override def loadForClient(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForClient(nbt, provider)
    clientHasInternalKeyboard = nbt.getBoolean(HasInternalKeyboardTag)
  }

  override def saveForClientDirect(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveForClientDirect(nbt, provider)
    nbt.putBoolean(HasInternalKeyboardTag, hasInternalKeyboard)
  }

  override def loadComponentsCommon(holder: DataComponentHolder): Unit = {
    super.loadComponentsCommon(holder)
    for(mode <- holder.getComponent(OCComponents.VIDEO_MODE)) {
      val w = mode.width()
      val h = mode.height()
      width = w max 1 min Settings.get.maxScreenWidth
      height = h max 1 min Settings.get.maxScreenHeight
      checkMultiBlock()
    }
  }

  override def loadComponentsForServer(holder: DataComponentHolder): Unit = {
    super.loadComponentsForServer(holder)
    updateInternalKeyboard()
  }

  override def saveComponentsCommon(holder: MutableDataComponentHolder): Unit = {
    super.saveComponentsCommon(holder)
    holder.setComponent(OCComponents.VIDEO_MODE, new VideoMode(width, height))
  }

  override def getRenderBoundingBox: AABB = {
    val horizontalRange = math.max(width, 1).toDouble + 1.0
    val verticalRange = math.max(height, 1).toDouble + 1.0
    val (minY, maxY) =
      if (isCeilingMounted) (y - verticalRange, y + 1.0)
      else (y.toDouble, y + 1.0 + verticalRange)
    new AABB(
      x - horizontalRange, minY, z - horizontalRange,
      x + 1.0 + horizontalRange, maxY, z + 1.0 + horizontalRange
    ).inflate(0.25)
  }

  private def syncToClient(): Unit =
    if (getLevel != null && !getLevel.isClientSide) {
      setChanged()
      getLevel.sendBlockUpdated(getBlockPos, getBlockState, getBlockState, 3)
    }

  private def isCeilingMounted: Boolean = mount == Direction.DOWN

  private def screenTopSide: Direction = mount

  private def screenRight: Direction = counterClockwise(yaw)

  private def counterClockwise(facing: Direction): Direction = facing match {
    case Direction.NORTH => Direction.WEST
    case Direction.WEST => Direction.SOUTH
    case Direction.SOUTH => Direction.EAST
    case Direction.EAST => Direction.NORTH
    case other => other
  }
}
