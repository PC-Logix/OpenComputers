package li.cil.oc.common.blockentity

import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.datacomponents.MutableNbtComponentHolder
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.{Constants, Settings, api}
import net.minecraft.core.{BlockPos, Direction, HolderLookup}
import net.minecraft.core.component.DataComponentHolder
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.neoforged.neoforge.common.MutableDataComponentHolder
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension
import li.cil.oc.util.PackedColor

import java.util
import scala.jdk.CollectionConverters._

object Projector {
  final val Width = 320
  final val Height = 200
}

class Projector(pos: BlockPos, state: BlockState)
  extends BlockEntity(BlockEntityTypes.PROJECTOR.get(), pos, state)
    with traits.Environment with traits.Tickable with DeviceInfo with IBlockEntityExtension {

  final val width = Projector.Width
  final val height = Projector.Height
  final val pixels = new Array[Int](width * height)

  final val node = api.Network.newNode(this, Visibility.Network)
    .withComponent("projector")
    .withConnector()
    .create()

  /** The alternate native OpenComputers screen component. */
  final val screenBuffer = new TextBuffer(this)
  private final val ScreenWidth = 160
  private final val ScreenHeight = 50
  private final val ScreenDepth = api.internal.TextBuffer.ColorDepth.EightBit

  screenBuffer.setMaximumResolution(ScreenWidth, ScreenHeight)
  screenBuffer.setMaximumColorDepth(ScreenDepth)
  // The component has no network address during construction, so initialize
  // its backing buffer directly instead of sending change packets here.
  screenBuffer.data.size = (ScreenWidth, ScreenHeight)
  screenBuffer.data.format = PackedColor.Depth.format(ScreenDepth)
  screenBuffer.viewport = (ScreenWidth, ScreenHeight)
  if (isServer) setModeComponentVisibility()

  private final val PowerPerTick = Settings.get.screenCost
  private final val PixelsTag = Settings.namespace + "projectorPixels"
  private final val OnTag = Settings.namespace + "projectorOn"
  private final val ScreenModeTag = Settings.namespace + "projectorScreenMode"
  private final val ScreenNodeAddressTag = Settings.namespace + "projectorScreenNodeAddress"

  private var pendingScreenNodeAddress: String = null
  private var modeNetwork: api.network.Network = null

  var isOn = true
  var hasPower = true
  var clientPixelsDirty = true
  // Keep this in block state as well as NBT. Block state is included in the
  // chunk's normal sync/save path, which makes the mode survive both a world
  // reload and the client arriving before the block entity update tag.
  var screenMode = getBlockState.getValue(li.cil.oc.common.block.Projector.ScreenMode)

  var dirtyFromX = width
  var dirtyFromY = height
  var dirtyUntilX = 0
  var dirtyUntilY = 0

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Display,
    DeviceAttribute.Description -> "Framebuffer projector",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Projector P1",
    DeviceAttribute.Capacity -> (width * height).toString,
    DeviceAttribute.Width -> width.toString
  ).asInstanceOf[Map[String, String]]

  override def getDeviceInfo: util.Map[String, String] = {
    import scala.jdk.CollectionConverters._
    deviceInfo.asJava
  }

  @Callback(direct = true, doc = "function():number, number -- Returns the framebuffer resolution.")
  def getResolution(context: Context, args: Arguments): Array[AnyRef] = result(width, height)

  def isScreenMode: Boolean = screenMode

  def toggleMode(): Unit = {
    screenMode = !screenMode
    if (getLevel != null && isServer) {
      val state = getBlockState
      if (state.hasProperty(li.cil.oc.common.block.Projector.ScreenMode)) {
        getLevel.setBlock(getBlockPos,
          state.setValue(li.cil.oc.common.block.Projector.ScreenMode, screenMode), 3)
      }
    }
    setModeComponentVisibility()
    if (screenMode) screenBuffer.setPowerState(value = true)
    else screenBuffer.setPowerState(value = false)
    markChanged()
    if (getLevel != null && isServer) getLevel.sendBlockUpdated(getBlockPos, getBlockState, getBlockState, 3)
  }

  /** Keep the block-light state in step with the persisted projector power state. */
  private def updateLightState(): Unit = {
    if (getLevel != null && isServer) {
      val state = getBlockState
      val lit = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) &&
        state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT).booleanValue()
      if (lit != isOn) {
        getLevel.setBlock(getBlockPos,
          state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,
            java.lang.Boolean.valueOf(isOn)), 3)
      }
    }
  }

  private def setModeComponentVisibility(): Unit = {
    if (!isServer) return
    val projectorComponent = node.asInstanceOf[api.network.Component]
    val screenComponent = screenBuffer.node.asInstanceOf[api.network.Component]

    // Component.loadData restores the visibility value, but that does not
    // notify machines when the node is subsequently attached to a freshly
    // rebuilt network. Force one transition after each network rejoin so the
    // computer's component list is repopulated without requiring a wrench.
    if (node.network != null && modeNetwork != node.network) {
      projectorComponent.setVisibility(Visibility.None)
      screenComponent.setVisibility(Visibility.None)
      modeNetwork = node.network
    }

    projectorComponent.setVisibility(if (screenMode) Visibility.None else Visibility.Network)
    screenComponent.setVisibility(if (screenMode) Visibility.Network else Visibility.None)
  }

  @Callback(direct = true, doc = "function():boolean -- Returns whether the projector is enabled and powered.")
  def isProjecting(context: Context, args: Arguments): Array[AnyRef] = result(isOn && hasPower)

  @Callback(doc = "function():boolean -- Turns the projector on.")
  def turnOn(context: Context, args: Arguments): Array[AnyRef] = {
    val changed = !isOn
    isOn = true
    updateLightState()
    markChanged()
    if (isServer) ServerPacketSender.sendProjectorPowerChange(this)
    result(changed, isOn)
  }

  @Callback(doc = "function():boolean -- Turns the projector off.")
  def turnOff(context: Context, args: Arguments): Array[AnyRef] = {
    val changed = isOn
    isOn = false
    updateLightState()
    markChanged()
    if (isServer) ServerPacketSender.sendProjectorPowerChange(this)
    result(changed, isOn)
  }

  @Callback(direct = true, doc = "function(x:number, y:number):number -- Returns the RGB pixel at one-based coordinates.")
  def get(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val (x, y) = checkCoordinates(args)
    result(pixels(x + y * width) & 0xFFFFFF)
  }

  @Callback(limit = 256, doc = "function(x:number, y:number, color:number) -- Sets an ARGB pixel at one-based coordinates.")
  def set(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val (x, y) = checkCoordinates(args)
    setPixel(x, y, args.checkInteger(2))
    null
  }

  @Callback(limit = 128, doc = "function(x:number, y:number, width:number, height:number, color:number) -- Fills a rectangle with an ARGB color.")
  def fill(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    val w = args.checkInteger(2)
    val h = args.checkInteger(3)
    if (w <= 0 || h <= 0) throw new IllegalArgumentException("rectangle is empty")
    val color = normalizeColor(args.checkInteger(4))
    val x0 = math.max(0, x)
    val y0 = math.max(0, y)
    val x1 = math.min(width, x + w)
    val y1 = math.min(height, y + h)
    for (py <- y0 until y1; px <- x0 until x1) pixels(px + py * width) = color
    if (x0 < x1 && y0 < y1) markDirty(x0, y0, x1, y1)
    null
  }

  @Callback(doc = "function():boolean -- Clears the framebuffer to transparent black.")
  def clear(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    java.util.Arrays.fill(pixels, 0)
    markDirty(0, 0, width, height)
    null
  }

  @Callback(doc = "function(data:string) -- Sets row-major RGBA bytes for the framebuffer; incomplete data clears the remaining pixels.")
  def setRaw(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val data = args.checkByteArray(0)
    java.util.Arrays.fill(pixels, 0)
    val count = math.min(pixels.length, data.length / 4)
    for (i <- 0 until count) {
      val offset = i * 4
      val r = data(offset) & 0xFF
      val g = data(offset + 1) & 0xFF
      val b = data(offset + 2) & 0xFF
      val a = data(offset + 3) & 0xFF
      pixels(i) = (a << 24) | (r << 16) | (g << 8) | b
    }
    markDirty(0, 0, width, height)
    context.pause(0.5)
    null
  }

  private def checkCoordinates(args: Arguments): (Int, Int) = {
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    if (x < 0 || x >= width) throw new ArrayIndexOutOfBoundsException("x")
    if (y < 0 || y >= height) throw new ArrayIndexOutOfBoundsException("y")
    (x, y)
  }

  private def setPixel(x: Int, y: Int, color: Int): Unit = {
    val normalized = normalizeColor(color)
    val index = x + y * width
    if (pixels(index) != normalized) {
      pixels(index) = normalized
      markDirty(x, y, x + 1, y + 1)
    }
  }

  private def normalizeColor(color: Int): Int =
    if (color == 0 || (color & 0xFF000000) != 0) color else color | 0xFF000000

  private def markDirty(x0: Int, y0: Int, x1: Int, y1: Int): Unit = {
    dirtyFromX = math.min(dirtyFromX, x0)
    dirtyFromY = math.min(dirtyFromY, y0)
    dirtyUntilX = math.max(dirtyUntilX, x1)
    dirtyUntilY = math.max(dirtyUntilY, y1)
    clientPixelsDirty = true
    setChanged()
  }

  private def resetDirty(): Unit = {
    dirtyFromX = width
    dirtyFromY = height
    dirtyUntilX = 0
    dirtyUntilY = 0
  }

  override def updateEntity(): Unit = {
    super.updateEntity()
    if (isServer) {
      updateLightState()
      if (node.network == null) {
        api.Network.joinOrCreateNetwork(this)
      }
    if (screenBuffer.node.network != node.network && node.network != null) {
      node.connect(screenBuffer.node)
    }
      if (screenBuffer.node.network == node.network && node.network != null) {
        // Keep the emulated screen directly adjacent to the same physical
        // neighbors as the projector. This lets TextBuffer discover and send
        // keyboard events to a normal OC keyboard placed beside the block.
        val screenNeighbors = screenBuffer.node.neighbors.asScala.toSet
        node.neighbors.asScala
          .filter(neighbor => neighbor != screenBuffer.node && !screenNeighbors.contains(neighbor))
          .foreach(screenBuffer.node.connect)
      }
      // Reapply the saved mode after both nodes have joined their network.
      // During world loading the component data can be restored before the
      // network exists, so the visibility transition otherwise gets missed
      // until a wrench toggles the mode manually.
      if (node.network != null) setModeComponentVisibility()
    }
    if (isClient || (isScreenMode && screenBuffer.node.network != null)) screenBuffer.update()
    if (isServer) {
      if (!isScreenMode && isOn) {
        val hadPower = hasPower
        hasPower = node.tryChangeBuffer(-PowerPerTick)
        if (hasPower != hadPower) {
          getLevel.sendBlockUpdated(getBlockPos, getBlockState, getBlockState, 3)
          ServerPacketSender.sendProjectorPowerChange(this)
        }
      }
      this.synchronized {
        if (dirtyUntilX > dirtyFromX && dirtyUntilY > dirtyFromY) {
          ServerPacketSender.sendProjectorFrame(this, dirtyFromX, dirtyFromY, dirtyUntilX, dirtyUntilY)
          resetDirty()
        }
      }
    }
  }

  def clientFrame(x: Int, y: Int, w: Int, h: Int, data: Array[Int]): Unit = this.synchronized {
    for (py <- 0 until h; px <- 0 until w) {
      val dx = x + px
      val dy = y + py
      if (dx >= 0 && dx < width && dy >= 0 && dy < height && px + py * w < data.length)
        pixels(dx + dy * width) = data(px + py * w)
    }
    clientPixelsDirty = true
  }

  def getProjectionDirection: Direction = getBlockState.getValue(PropertyRotatable.Facing)

  def projectionBounds: AABB = new AABB(getBlockPos).inflate(16)

  override def loadForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForServer(nbt, provider)
    val stored = nbt.getIntArray(PixelsTag)
    if (stored.length == pixels.length) stored.copyToArray(pixels)
    isOn = if (nbt.contains(OnTag)) nbt.getBoolean(OnTag) else true
    screenMode = if (nbt.contains(ScreenModeTag)) nbt.getBoolean(ScreenModeTag)
    else getBlockState.getValue(li.cil.oc.common.block.Projector.ScreenMode)
    restoreScreenNodeAddress(nbt)
  }

  override def saveForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = this.synchronized {
    super.saveForServer(nbt, provider)
    nbt.putIntArray(PixelsTag, pixels)
    nbt.putBoolean(OnTag, isOn)
    nbt.putBoolean(ScreenModeTag, screenMode)
    saveScreenNodeAddress(nbt)
  }

  override def loadForClient(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForClient(nbt, provider)
    val stored = nbt.getIntArray(PixelsTag)
    if (stored.length == pixels.length) stored.copyToArray(pixels)
    isOn = nbt.getBoolean(OnTag)
    screenMode = if (nbt.contains(ScreenModeTag)) nbt.getBoolean(ScreenModeTag)
    else getBlockState.getValue(li.cil.oc.common.block.Projector.ScreenMode)
    hasPower = nbt.getBoolean(Settings.namespace + "projectorPower")
    pendingScreenNodeAddress = nbt.getString(ScreenNodeAddressTag)
    if (pendingScreenNodeAddress != null && pendingScreenNodeAddress.isEmpty) pendingScreenNodeAddress = null
    clientPixelsDirty = true
  }

  override def saveForClient(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = this.synchronized {
    super.saveForClient(nbt, provider)
    // The framebuffer is 320 * 200 * 4 bytes. Keeping it out of the
    // client-facing update tag is important: update tags are embedded in
    // level_chunk_with_light packets, whose NBT read budget is only 2 MiB.
    // The initial framebuffer is sent through the compressed projector frame
    // packet when a player starts watching this chunk instead.
    nbt.putBoolean(OnTag, isOn)
    nbt.putBoolean(ScreenModeTag, screenMode)
    nbt.putBoolean(Settings.namespace + "projectorPower", hasPower)
    saveScreenNodeAddress(nbt)
  }

  override def loadComponentsForServer(holder: DataComponentHolder): Unit = {
    super.loadComponentsForServer(holder)
    screenBuffer.loadData(holder)
    setModeComponentVisibility()
  }

  override def saveComponentsForServer(holder: MutableDataComponentHolder): Unit = {
    super.saveComponentsForServer(holder)
    screenBuffer.saveData(holder)
  }

  override def loadComponentsForClient(holder: DataComponentHolder): Unit = {
    super.loadComponentsForClient(holder)
    screenBuffer.loadData(holder)
    if (screenBuffer.proxy.nodeAddress.isEmpty && pendingScreenNodeAddress != null) {
      screenBuffer.proxy.nodeAddress = pendingScreenNodeAddress
      screenBuffer.requestSynchronization()
    }
    pendingScreenNodeAddress = null
  }

  override def saveComponentsForClient(holder: MutableDataComponentHolder): Unit = {
    super.saveComponentsForClient(holder)
    screenBuffer.saveData(holder)
  }

  override def dispose(): Unit = {
    if (isServer) Option(screenBuffer.node).foreach(_.remove())
    super.dispose()
  }

  private def restoreScreenNodeAddress(nbt: CompoundTag): Unit = {
    val address = nbt.getString(ScreenNodeAddressTag)
    if (address != null && !address.isEmpty && screenBuffer.node != null) {
      val holder = new MutableNbtComponentHolder()
      holder.set(OCComponents.ADDRESS.get(), address)
      screenBuffer.node.loadData(holder)
    }
  }

  private def saveScreenNodeAddress(nbt: CompoundTag): Unit = {
    if (screenBuffer.node != null && screenBuffer.node.address != null) {
      nbt.putString(ScreenNodeAddressTag, screenBuffer.node.address)
    }
  }
}
