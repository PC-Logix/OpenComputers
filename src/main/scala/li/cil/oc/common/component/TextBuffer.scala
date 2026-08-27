package li.cil.oc.common.component

import com.google.common.base.Strings
import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network._
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.font.TextBufferRenderData
import li.cil.oc.client.{ComponentTracker => ClientComponentTracker, PacketSender => ClientPacketSender}
import li.cil.oc.common._
import li.cil.oc.common.component.traits.VideoRamRasterizer
import li.cil.oc.common.datacomponents.{CompoundStorage, MaximumVideoMode, OCComponents, VideoMode}
import li.cil.oc.server.component.Keyboard
import li.cil.oc.server.{ComponentTracker => ServerComponentTracker, PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedDataComponentHolder._
import li.cil.oc.util.{BlockPosition, PackedColor, SideTracker}
import li.cil.oc.{Constants, OpenComputers, Settings, api, util}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.core.component.{DataComponentHolder, DataComponents}
import net.minecraft.nbt.{CompoundTag, NbtOps}
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.{Dist, OnlyIn}
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.common.MutableDataComponentHolder
import net.neoforged.neoforge.event.level.{ChunkEvent, LevelEvent}

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

class TextBuffer(val host: EnvironmentHost) extends AbstractManagedEnvironment with traits.TextBufferProxy with VideoRamRasterizer with DeviceInfo {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("screen").
    withConnector().
    create()

  private var maxResolution: (Int, Int) = Settings.screenResolutionsByTier(Tier.One)

  private var maxDepth = Settings.screenDepthsByTier(Tier.One)

  private var aspectRatio = (1.0, 1.0)

  private var powerConsumptionPerTick = Settings.get.screenCost

  private var precisionMode = false

  // For client side only.
  private var isRendering = true

  private var isDisplaying = true

  private var hasPower = true

  private var relativeLitArea = -1.0

  private var _pendingCommands: Option[PacketBuilder] = None

  private val syncInterval = 100

  private var syncCooldown = syncInterval

  private def pendingCommands = _pendingCommands.getOrElse {
    val pb = new CompressedPacketBuilder(PacketType.TextBufferMulti)
    pb.writeUTF(node.address)
    _pendingCommands = Some(pb)
    pb
  }

  var fullyLitCost: Double = computeFullyLitCost()

  // This computes the energy cost (per tick) to keep the screen running if
  // every single "pixel" is lit. This cost increases with higher tiers as
  // their maximum resolution (pixel density) increases. For a basic screen
  // this is simply the configured cost.
  def computeFullyLitCost(): Double = {
    val (w, h) = Settings.screenResolutionsByTier(0)
    val mw = getMaximumWidth
    val mh = getMaximumHeight
    powerConsumptionPerTick * (mw * mh) / (w * h)
  }

  val proxy: TextBuffer.Proxy =
    if (SideTracker.isClient) new TextBuffer.ClientProxy(this)
    else new TextBuffer.ServerProxy(this)

  val data = new util.TextBuffer(maxResolution, PackedColor.Depth.format(maxDepth))

  var viewport: (Int, Int) = data.size

  def markInitialized(): Unit = {
    syncCooldown = -1 // Stop polling for init state.
    relativeLitArea = -1 // Recompute lit area, avoid screens blanking out until something changes.
  }

  def requestSynchronization(): Unit = if (SideTracker.isClient) {
    // Do not turn an initialized buffer back into an uninitialized one. While
    // uninitialized, multi-update packets are intentionally ignored until the
    // authoritative snapshot arrives, so resetting this here would make live
    // terminal input appear only after closing and reopening the GUI.
    if (!isInitialized) TextBuffer.registerClientBuffer(this)
  }

  /** Register a captured Create screen with the real client world. */
  def registerClientBufferOnLevel(level: Level): Boolean =
    TextBuffer.registerClientBuffer(this, level)

  /** Remove a captured Create screen from the client tracker after disassembly. */
  def unregisterClientBufferOnLevel(level: Level): Unit =
    TextBuffer.unregisterClientBuffer(this, level)

  def isInitialized: Boolean = syncCooldown < 0

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Display,
    DeviceAttribute.Description -> "Text buffer",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Text Screen V0",
    DeviceAttribute.Capacity -> (maxResolution._1 * maxResolution._2).toString,
    DeviceAttribute.Width -> Array("1", "4", "8", "16").apply(maxDepth.ordinal())
  )

  override def getDeviceInfo: java.util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update(): Unit = {
    super.update()
    if (isDisplaying && host.getEnvironmentLevel.getGameTime % Settings.get.tickFrequency == 0) {
      if (relativeLitArea < 0) {
        // The relative lit area is the number of pixels that are not blank
        // versus the number of pixels in the *current* resolution. This is
        // scaled to multi-block screens, since we only compute this for the
        // origin.
        val w = getViewportWidth
        val h = getViewportHeight
        var acc = 0f
        // Description packets and legacy saves may resize the character and
        // color planes in separate steps. Never let one inconsistent frame
        // take down the client tick loop while the next update repairs it.
        val safeHeight = math.min(h, math.min(data.buffer.length, data.color.length))
        for (y <- 0 until safeHeight) {
          val line = data.buffer(y)
          val colors = data.color(y)
          val safeWidth = math.min(w, math.min(line.length, colors.length))
          for (x <- 0 until safeWidth) {
            val char = line(x)
            val color = colors(x)
            val bg = PackedColor.unpackBackground(color, data.format)
            val fg = PackedColor.unpackForeground(color, data.format)
            acc += (if (char == ' ') if (bg == 0) 0 else 1
            else if (char == 0x2588) if (fg == 0) 0 else 1
            else if (fg == 0 && bg == 0) 0 else 1)
          }
        }
        relativeLitArea = if (w > 0 && h > 0) acc / (w * h).toDouble else 0
      }
      if (node != null) {
        val hadPower = hasPower
        val neededPower = relativeLitArea * fullyLitCost * Settings.get.tickFrequency
        hasPower = node.tryChangeBuffer(-neededPower)
        if (hasPower != hadPower) {
          ServerPacketSender.sendTextBufferPowerChange(node.address, isDisplaying && hasPower, host)
        }
      }
    }

    this.synchronized {
      _pendingCommands.foreach(_.sendToPlayersNearHost(host, Option(Settings.get.maxWirelessRange(Tier.Two) * Settings.get.maxWirelessRange(Tier.Two))))
      _pendingCommands = None
    }

    if (SideTracker.isClient && syncCooldown > 0) {
      syncCooldown -= 1
      if (syncCooldown == 0) {
        syncCooldown = syncInterval
        ClientPacketSender.sendTextBufferInit(proxy.nodeAddress)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():boolean -- Returns whether the screen is currently on.""")
  def isOn(computer: Context, args: Arguments): Array[AnyRef] = result(isDisplaying)

  @Callback(doc = """function():boolean -- Turns the screen on. Returns whether the state changed, and whether it is now on.""")
  def turnOn(computer: Context, args: Arguments): Array[AnyRef] = {
    val oldPowerState = isDisplaying
    setPowerState(value = true)
    result(isDisplaying != oldPowerState, isDisplaying)
  }

  @Callback(doc = """function():boolean -- Turns off the screen. Returns whether the state changed, and whether it is now on.""")
  def turnOff(computer: Context, args: Arguments): Array[AnyRef] = {
    val oldPowerState = isDisplaying
    setPowerState(value = false)
    result(isDisplaying != oldPowerState, isDisplaying)
  }

  @Callback(direct = true, doc = """function():number, number -- The aspect ratio of the screen. For multi-block screens this is the number of blocks, horizontal and vertical.""")
  def getAspectRatio(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    result(aspectRatio._1, aspectRatio._2)
  }

  @Callback(doc = """function():table -- The list of keyboards attached to the screen.""")
  def getKeyboards(context: Context, args: Arguments): Array[AnyRef] = {
    context.pause(0.25)
    host match {
      case screen: blockentity.Screen =>
        Array(screen.screens.map(_.node).flatMap(_.neighbors.filter(_.host.isInstanceOf[Keyboard]).map(_.address)).toArray)
      case _ =>
        Array(node.neighbors.filter(_.host.isInstanceOf[Keyboard]).map(_.address).toArray)
    }
  }

  @Callback(direct = true, doc = """function():boolean -- Returns whether the screen is in high precision mode (sub-pixel mouse event positions).""")
  def isPrecise(computer: Context, args: Arguments): Array[AnyRef] = result(precisionMode)

  @Callback(doc = """function(enabled:boolean):boolean -- Set whether to use high precision mode (sub-pixel mouse event positions).""")
  def setPrecise(computer: Context, args: Arguments): Array[AnyRef] = {
    // Available for T3 screens only... easiest way to check for us is to
    // base it off of the maximum color depth.
    if (maxDepth == Settings.screenDepthsByTier(Tier.Four)) {
      val oldValue = precisionMode
      precisionMode = args.checkBoolean(0)
      result(oldValue)
    }
    else result((), "unsupported operation")
  }

  @Callback(doc = """function():number -- Get the maximum resolution supported by the screen.""")
  def hardwareResolution(context: Context, args: Arguments): Array[AnyRef] = {
    val (smw, smh) = maxResolution
    result(smw, smh)
  }

  @Callback(direct = true, doc = """function():number -- Get the maximum color depth supported by the screen.""")
  def hardwareDepth(context: Context, args: Arguments): Array[AnyRef] =
    result(PackedColor.Depth.bits(maxDepth))

  // ----------------------------------------------------------------------- //

  override def setEnergyCostPerTick(value: Double): Unit = {
    powerConsumptionPerTick = value
    fullyLitCost = computeFullyLitCost()
  }

  override def getEnergyCostPerTick: Double = powerConsumptionPerTick

  override def setPowerState(value: Boolean): Unit = {
    if (isDisplaying != value) {
      isDisplaying = value
      if (isDisplaying) {
        val neededPower = fullyLitCost * Settings.get.tickFrequency
        hasPower = node.changeBuffer(-neededPower) == 0
      }
      ServerPacketSender.sendTextBufferPowerChange(node.address, isDisplaying && hasPower, host)
    }
  }

  override def getPowerState: Boolean = isDisplaying

  override def setMaximumResolution(width: Int, height: Int): Unit = {
    if (width < 1) throw new IllegalArgumentException("width must be larger or equal to one")
    if (height < 1) throw new IllegalArgumentException("height must be larger or equal to one")
    maxResolution = (width, height)
    fullyLitCost = computeFullyLitCost()
    proxy.onBufferMaxResolutionChange(width, height)
  }

  override def getMaximumWidth: Int = maxResolution._1

  override def getMaximumHeight: Int = maxResolution._2

  override def setAspectRatio(width: Double, height: Double): Unit = this.synchronized(this.aspectRatio = (width, height))

  override def getAspectRatio: Double = aspectRatio._1 / aspectRatio._2

  override def setResolution(w: Int, h: Int): Boolean = {
    val (mw, mh) = maxResolution
    if (w < 1 || h < 1 || w > mw || h > mw || h * w > mw * mh)
      throw new IllegalArgumentException("unsupported resolution")
    // Always send to clients, their state might be dirty.
    proxy.onBufferResolutionChange(w, h)
    // Force set viewport to new resolution. This is partially for
    // backwards compatibility, and partially to enforce a valid one.
    val sizeChanged = data.size = (w, h)
    val viewportChanged = setViewport(w, h)
    if (sizeChanged || viewportChanged) {
      if (!viewportChanged && node != null) {
        node.sendToReachable("computer.signal", "screen_resized", Int.box(w), Int.box(h))
      }
      true
    }
    else false
  }

  override def setViewport(w: Int, h: Int): Boolean = {
    val (mw, mh) = data.size
    if (w < 1 || h < 1 || w > mw || h > mh)
      throw new IllegalArgumentException("unsupported viewport resolution")
    // Always send to clients, their state might be dirty.
    proxy.onBufferViewportResolutionChange(w, h)
    val (cw, ch) = viewport
    if (w != cw || h != ch) {
      viewport = (w, h)
      if (node != null) {
        node.sendToReachable("computer.signal", "screen_resized", Int.box(w), Int.box(h))
      }
      true
    }
    else false
  }

  override def getViewportWidth: Int = viewport._1

  override def getViewportHeight: Int = viewport._2

  override def setMaximumColorDepth(depth: api.internal.TextBuffer.ColorDepth): Unit = maxDepth = depth

  override def getMaximumColorDepth: api.internal.TextBuffer.ColorDepth = maxDepth

  override def setColorDepth(depth: api.internal.TextBuffer.ColorDepth): Boolean = {
    val colorDepthChanged: Boolean = super.setColorDepth(depth)
    // Always send to clients, their state might be dirty.
    proxy.onBufferDepthChange(depth)
    colorDepthChanged
  }

  override def onBufferPaletteChange(index: Int): Unit =
    proxy.onBufferPaletteChange(index)

  override def onBufferColorChange(): Unit =
    proxy.onBufferColorChange()

  override def onBufferCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int): Unit = {
    proxy.onBufferCopy(col, row, w, h, tx, ty)
  }

  override def onBufferFill(col: Int, row: Int, w: Int, h: Int, c: Int): Unit = {
    proxy.onBufferFill(col, row, w, h, c)
  }

  override def onBufferSet(col: Int, row: Int, s: String, vertical: Boolean): Unit = {
    proxy.onBufferSet(col, row, s, vertical)
  }

  override def onBufferBitBlt(col: Int, row: Int, w: Int, h: Int, ram: component.GpuTextBuffer, fromCol: Int, fromRow: Int): Unit = {
    proxy.onBufferBitBlt(col, row, w, h, ram, fromCol, fromRow)
  }

  override def onBufferRamInit(ram: component.GpuTextBuffer): Unit = {
    proxy.onBufferRamInit(ram)
  }

  override def onBufferRamDestroy(ram: component.GpuTextBuffer): Unit = {
    proxy.onBufferRamDestroy(ram)
  }

  override def rawSetText(col: Int, row: Int, text: Array[Array[Int]]): Unit = {
    super.rawSetText(col, row, text)
    proxy.onBufferRawSetText(col, row, text)
  }

  override def rawSetBackground(col: Int, row: Int, color: Array[Array[Int]]): Unit = {
    super.rawSetBackground(col, row, color)
    // Better for bandwidth to send packed shorts here. Would need a special case for handling on client,
    // though, so let's be wasteful for once...
    proxy.onBufferRawSetBackground(col, row, color)
  }

  override def rawSetForeground(col: Int, row: Int, color: Array[Array[Int]]): Unit = {
    super.rawSetForeground(col, row, color)
    // Better for bandwidth to send packed shorts here. Would need a special case for handling on client,
    // though, so let's be wasteful for once...
    proxy.onBufferRawSetForeground(col, row, color)
  }

  @OnlyIn(Dist.CLIENT)
  override def renderText(stack: PoseStack): Boolean = relativeLitArea != 0 && proxy.render(stack)

  @OnlyIn(Dist.CLIENT)
  def renderText(stack: PoseStack, renderBuffer: MultiBufferSource): Boolean = relativeLitArea != 0 && (proxy match {
    case client: TextBuffer.ClientProxy => client.render(stack, renderBuffer)
    case _ => proxy.render(stack)
  })

  @OnlyIn(Dist.CLIENT)
  override def renderWidth: Int = TextBufferRenderCache.renderer.charRenderWidth * getViewportWidth

  @OnlyIn(Dist.CLIENT)
  override def renderHeight: Int = TextBufferRenderCache.renderer.charRenderHeight * getViewportHeight

  @OnlyIn(Dist.CLIENT)
  override def setRenderingEnabled(enabled: Boolean): Unit = isRendering = enabled

  @OnlyIn(Dist.CLIENT)
  override def isRenderingEnabled: Boolean = isRendering

  override def keyDown(character: Char, code: Int, player: Player): Unit =
    proxy.keyDown(character, code, player)

  override def keyUp(character: Char, code: Int, player: Player): Unit =
    proxy.keyUp(character, code, player)

  override def textInput(codePt: Int, player: Player): Unit =
    proxy.textInput(codePt, player)

  override def clipboard(value: String, player: Player): Unit =
    proxy.clipboard(value, player)

  override def mouseDown(x: Double, y: Double, button: Int, player: Player): Unit =
    proxy.mouseDown(x, y, button, player)

  override def mouseDrag(x: Double, y: Double, button: Int, player: Player): Unit =
    proxy.mouseDrag(x, y, button, player)

  override def mouseUp(x: Double, y: Double, button: Int, player: Player): Unit =
    proxy.mouseUp(x, y, button, player)

  override def mouseScroll(x: Double, y: Double, delta: Int, player: Player): Unit =
    proxy.mouseScroll(x, y, delta, player)

  def copyToAnalyzer(line: Int, player: Player): Unit = {
    proxy.copyToAnalyzer(line, player)
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node): Unit = {
    super.onConnect(node)
    if (node == this.node) {
      ServerComponentTracker.add(host.getEnvironmentLevel, node.address, this)
    }
  }

  override def onDisconnect(node: Node): Unit = {
    super.onDisconnect(node)
    if (node == this.node) {
      ServerComponentTracker.remove(host.getEnvironmentLevel, this)
    }
  }

  // ----------------------------------------------------------------------- //

  private def bufferPath = node.address + "_buffer"

  override def loadData(holder: DataComponentHolder): Unit = {
    super.loadData(holder)
    for(address <- holder.getComponent(OCComponents.ADDRESS)) {
      if (SideTracker.isClient) {
        if (!Strings.isNullOrEmpty(proxy.nodeAddress)) return // Only load once.
        proxy.nodeAddress = address
        requestSynchronization()
      }
      else {
        holder.getComponent(OCComponents.TEXT_BUFFER) match {
          case Some(_) => data.loadData(holder)
          case None =>
            val saved = SaveHandler.loadNBT(host.getEnvironmentLevel.dimension().location(), new ChunkPos(new BlockPos(host.xPosition().toInt, host.yPosition().toInt, host.zPosition().toInt)), bufferPath)
            if (!saved.isEmpty) {
              val storage = CompoundStorage.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow()
              data.loadData(storage)
            }
        }
      }
    }

    for(isOnComponent <- holder.getComponent(OCComponents.IS_ON))
      isDisplaying = isOnComponent
    for(isPoweredComponent <- holder.getComponent(OCComponents.IS_POWERED))
      hasPower = isPoweredComponent

    for(MaximumVideoMode(maxWidth, maxHeight, depth) <- holder.getComponent(OCComponents.MAX_VIDEO_MODE)) {
      maxResolution = (maxWidth, maxHeight)

      // Restore maxDepth so that getMaximumColorDepth() returns the correct tier
      // even if setMaximumColorDepth() was not called after construction (e.g.
      // when the buffer lazy val was initialised before load(nbt) ran).
      val depthValues = api.internal.TextBuffer.ColorDepth.values
      val ordinal = depth min (depthValues.length - 1) max 0
      maxDepth = depthValues(ordinal)
    }

    precisionMode = holder.getOrDefault(OCComponents.IS_PRECISE, false)

    viewport = holder.getComponent(OCComponents.VIDEO_MODE) match {
      case Some(VideoMode(vpw, vph)) => (vpw min data.width max 1, vph min data.height max 1)
      case None => data.size
    }
  }

  // Null check for Waila (and other mods that may call this client side).
  override def saveData(holder: MutableDataComponentHolder): Unit = if (node != null) {
    super.saveData(holder)
    // Happy thread synchronization hack! Here's the problem: GPUs allow direct
    // calls for modifying screens to give a more responsive experience. This
    // causes the following problem: when saving, if the screen is saved first,
    // then the executor runs in parallel and changes the screen *before* the
    // server thread begins saving that computer, the saved computer will think
    // it changed the screen, although the saved screen wasn't. To avoid that we
    // wait for all computers the screen is connected to to finish their current
    // execution and pausing them (which will make them resume in the next tick
    // when their update() runs).
    if (node.network != null) {
      for (node <- node.network.nodes) node.host match {
        case computer: blockentity.traits.Computer if !computer.machine.isPaused =>
          computer.machine.pause(0.1)
        case _ =>
      }
    }

    host match {
      // These screens are themselves stored inside another ItemStack-backed
      // environment. Persist their contents inline so restoring them does not
      // depend on an auxiliary file being available before the containing
      // environment resumes. In particular, a terminal server otherwise starts
      // with its ScreenTier1 constructor buffer and can overwrite the saved
      // higher resolution before the auxiliary state is recovered.
      case _: api.internal.Tablet | _: RemoteTerminalHost => data.saveData(holder)
      // Create's moving block entities are restored at the train's current
      // coordinates, while physical screen buffers normally live in a
      // SaveHandler file keyed by the chunk where they were saved. That makes
      // the post-reload lookup coordinate-dependent and can replace a real
      // terminal with a blank buffer. The captured block-entity NBT travels
      // with the contraption, so keep this snapshot inline while it is moving.
      case moving: blockentity.traits.BaseBlockEntity if moving.isMoving => data.saveData(holder)
      case environmentHost: EnvironmentHost =>
        SaveHandler.scheduleSave(environmentHost, new CompoundTag(), bufferPath, (tag: CompoundTag) => {
          val storage = new CompoundStorage()
          data.saveData(storage)
          tag.merge(CompoundStorage.CODEC.encodeStart(NbtOps.INSTANCE, storage).getOrThrow().asInstanceOf[CompoundTag])
          ()
        })
      case _ =>
    }
    holder.setComponent(OCComponents.IS_ON, isDisplaying)
    holder.setComponent(OCComponents.IS_POWERED, hasPower)
    holder.setComponent(OCComponents.MAX_VIDEO_MODE, MaximumVideoMode(maxResolution._1, maxResolution._2, maxDepth.ordinal))
    holder.setComponent(OCComponents.IS_PRECISE, precisionMode)
    holder.setComponent(OCComponents.VIDEO_MODE, VideoMode(viewport._1, viewport._2))
  }
}

object TextBuffer {
  var clientBuffers = mutable.ListBuffer.empty[TextBuffer]

  @SubscribeEvent
  def onChunkUnloaded(e: ChunkEvent.Unload): Unit = {
    val chunk = e.getChunk
    clientBuffers = clientBuffers.filter(t => {
      val blockPos = BlockPosition(t.host)
      val chunkPos = chunk.getPos
      val keep = t.host.getEnvironmentLevel != e.getLevel || ((blockPos.x >> 4) != chunkPos.x || (blockPos.z >> 4) != chunkPos.z)
      if (!keep) {
        ClientComponentTracker.remove(t.host.getEnvironmentLevel, t)
      }
      keep
    })
  }

  @SubscribeEvent
  def onWorldUnload(e: LevelEvent.Unload): Unit = {
    clientBuffers = clientBuffers.filter(t => {
      val keep = t.host.getEnvironmentLevel != e.getLevel
      if (!keep) {
        ClientComponentTracker.remove(t.host.getEnvironmentLevel, t)
      }
      keep
    })
  }

  def registerClientBuffer(t: TextBuffer): Unit = {
    registerClientBuffer(t, t.host.getEnvironmentLevel)
  }

  def registerClientBuffer(t: TextBuffer, level: Level): Boolean = {
    if (level == null || Strings.isNullOrEmpty(t.proxy.nodeAddress)) return false

    // Captured block entities initially register against Create's virtual
    // render level. Move the same buffer to the real client level instead.
    val hostLevel = t.host.getEnvironmentLevel
    if (hostLevel != null && hostLevel != level) {
      ClientComponentTracker.remove(hostLevel, t)
    }

    // Re-applying component data during chunk/menu synchronization must not
    // leave duplicate/stale client buffer registrations behind.
    ClientComponentTracker.remove(level, t)
    ClientComponentTracker.add(level, t.proxy.nodeAddress, t)

    if (!clientBuffers.contains(t)) {
      clientBuffers += t
    }

    ClientPacketSender.sendTextBufferInit(t.proxy.nodeAddress)
    true
  }

  def unregisterClientBuffer(t: TextBuffer, level: Level): Unit = {
    if (level == null) return

    ClientComponentTracker.remove(level, t)
    val hostLevel = t.host.getEnvironmentLevel
    if (hostLevel != null && hostLevel != level) {
      ClientComponentTracker.remove(hostLevel, t)
    }
    clientBuffers -= t
  }

  abstract class Proxy {
    def owner: TextBuffer

    var dirty = false

    var nodeAddress = ""

    def setChanged(): Unit = {
      dirty = true
    }

    @OnlyIn(Dist.CLIENT)
    def render(stack: PoseStack) = false

    def onBufferColorChange(): Unit

    def onBufferCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferDepthChange(depth: api.internal.TextBuffer.ColorDepth): Unit

    def onBufferFill(col: Int, row: Int, w: Int, h: Int, c: Int): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferPaletteChange(index: Int): Unit

    def onBufferResolutionChange(w: Int, h: Int): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferViewportResolutionChange(w: Int, h: Int): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferMaxResolutionChange(w: Int, h: Int): Unit = {
    }

    def onBufferSet(col: Int, row: Int, s: String, vertical: Boolean): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferBitBlt(col: Int, row: Int, w: Int, h: Int, ram: component.GpuTextBuffer, fromCol: Int, fromRow: Int): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferRamInit(ram: component.GpuTextBuffer): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferRamDestroy(ram: component.GpuTextBuffer): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferRawSetText(col: Int, row: Int, text: Array[Array[Int]]): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferRawSetBackground(col: Int, row: Int, color: Array[Array[Int]]): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferRawSetForeground(col: Int, row: Int, color: Array[Array[Int]]): Unit = {
      owner.relativeLitArea = -1
    }

    def keyDown(character: Char, code: Int, player: Player): Unit

    def keyUp(character: Char, code: Int, player: Player): Unit

    def textInput(codePt: Int, player: Player): Unit

    def clipboard(value: String, player: Player): Unit

    def mouseDown(x: Double, y: Double, button: Int, player: Player): Unit

    def mouseDrag(x: Double, y: Double, button: Int, player: Player): Unit

    def mouseUp(x: Double, y: Double, button: Int, player: Player): Unit

    def mouseScroll(x: Double, y: Double, delta: Int, player: Player): Unit

    def copyToAnalyzer(line: Int, player: Player): Unit
  }

  class ClientProxy(val owner: TextBuffer) extends Proxy {
    val renderer = new TextBufferRenderData {
      override def dirty = ClientProxy.this.dirty

      override def dirty_=(value: Boolean) = ClientProxy.this.dirty = value

      override def data = owner.data

      override def viewport: (Int, Int) = owner.viewport
    }

    @OnlyIn(Dist.CLIENT)
    override def render(stack: PoseStack) = {
      val wasDirty = dirty
      TextBufferRenderCache.render(stack, renderer)
      wasDirty
    }

    @OnlyIn(Dist.CLIENT)
    def render(stack: PoseStack, renderBuffer: MultiBufferSource): Boolean = {
      TextBufferRenderCache.renderImmediate(stack, renderBuffer, renderer)
      dirty
    }

    override def onBufferColorChange(): Unit = {
      setChanged()
    }

    override def onBufferCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int): Unit = {
      super.onBufferCopy(col, row, w, h, tx, ty)
      setChanged()
    }

    override def onBufferDepthChange(depth: api.internal.TextBuffer.ColorDepth): Unit = {
      setChanged()
    }

    override def onBufferFill(col: Int, row: Int, w: Int, h: Int, c: Int): Unit = {
      super.onBufferFill(col, row, w, h, c)
      setChanged()
    }

    override def onBufferPaletteChange(index: Int): Unit = {
      setChanged()
    }

    override def onBufferResolutionChange(w: Int, h: Int): Unit = {
      super.onBufferResolutionChange(w, h)
      setChanged()
    }

    override def onBufferViewportResolutionChange(w: Int, h: Int): Unit = {
      super.onBufferViewportResolutionChange(w, h)
      setChanged()
    }

    override def onBufferSet(col: Int, row: Int, s: String, vertical: Boolean): Unit = {
      super.onBufferSet(col, row, s, vertical)
      setChanged()
    }

    override def onBufferBitBlt(col: Int, row: Int, w: Int, h: Int, ram: component.GpuTextBuffer, fromCol: Int, fromRow: Int): Unit = {
      super.onBufferBitBlt(col, row, w, h, ram, fromCol, fromRow)
      setChanged()
    }

    override def onBufferRamInit(ram: component.GpuTextBuffer): Unit = {
      super.onBufferRamInit(ram)
    }

    override def onBufferRamDestroy(ram: component.GpuTextBuffer): Unit = {
      super.onBufferRamDestroy(ram)
    }

    override def keyDown(character: Char, code: Int, player: Player): Unit = {
      debug(s"{type = keyDown, char = $character, code = $code}")
      ClientPacketSender.sendKeyDown(nodeAddress, character, code)
    }

    override def keyUp(character: Char, code: Int, player: Player): Unit = {
      debug(s"{type = keyUp, char = $character, code = $code}")
      ClientPacketSender.sendKeyUp(nodeAddress, character, code)
    }

    override def textInput(codePt: Int, player: Player): Unit = {
      debug(s"{type = textInput, codePt = $codePt}")
      ClientPacketSender.sendTextInput(nodeAddress, codePt)
    }

    override def clipboard(value: String, player: Player): Unit = {
      debug(s"{type = clipboard}")
      ClientPacketSender.sendClipboard(nodeAddress, value)
    }

    override def mouseDown(x: Double, y: Double, button: Int, player: Player): Unit = {
      debug(s"{type = mouseDown, x = $x, y = $y, button = $button}")
      ClientPacketSender.sendMouseClick(nodeAddress, x, y, drag = false, button)
    }

    override def mouseDrag(x: Double, y: Double, button: Int, player: Player): Unit = {
      debug(s"{type = mouseDrag, x = $x, y = $y, button = $button}")
      ClientPacketSender.sendMouseClick(nodeAddress, x, y, drag = true, button)
    }

    override def mouseUp(x: Double, y: Double, button: Int, player: Player): Unit = {
      debug(s"{type = mouseUp, x = $x, y = $y, button = $button}")
      ClientPacketSender.sendMouseUp(nodeAddress, x, y, button)
    }

    override def mouseScroll(x: Double, y: Double, delta: Int, player: Player): Unit = {
      debug(s"{type = mouseScroll, x = $x, y = $y, delta = $delta}")
      ClientPacketSender.sendMouseScroll(nodeAddress, x, y, delta)
    }

    override def copyToAnalyzer(line: Int, player: Player): Unit = {
      ClientPacketSender.sendCopyToAnalyzer(nodeAddress, line)
    }

    private lazy val Debugger = api.Items.get(Constants.ItemName.Debugger)

    private def debug(message: String): Unit = {
      if (Minecraft.getInstance != null && Minecraft.getInstance.player != null && api.Items.get(Minecraft.getInstance.player.getItemInHand(InteractionHand.MAIN_HAND)) == Debugger) {
        OpenComputers.log.info(s"[NETWORK DEBUGGER] Sending packet to node $nodeAddress: " + message)
      }
    }
  }

  class ServerProxy(val owner: TextBuffer) extends Proxy {
    override def onBufferColorChange(): Unit = {
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferColorChange(owner.pendingCommands, owner.data.foreground, owner.data.background))
    }

    override def onBufferCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int): Unit = {
      super.onBufferCopy(col, row, w, h, tx, ty)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferCopy(owner.pendingCommands, col, row, w, h, tx, ty))
    }

    override def onBufferDepthChange(depth: api.internal.TextBuffer.ColorDepth): Unit = {
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferDepthChange(owner.pendingCommands, depth))
    }

    override def onBufferFill(col: Int, row: Int, w: Int, h: Int, c: Int): Unit = {
      super.onBufferFill(col, row, w, h, c)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferFill(owner.pendingCommands, col, row, w, h, c))
    }

    override def onBufferPaletteChange(index: Int): Unit = {
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferPaletteChange(owner.pendingCommands, index, owner.getPaletteColor(index)))
    }

    override def onBufferResolutionChange(w: Int, h: Int): Unit = {
      super.onBufferResolutionChange(w, h)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferResolutionChange(owner.pendingCommands, w, h))
    }

    override def onBufferViewportResolutionChange(w: Int, h: Int): Unit = {
      super.onBufferViewportResolutionChange(w, h)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferViewportResolutionChange(owner.pendingCommands, w, h))
    }

    override def onBufferMaxResolutionChange(w: Int, h: Int): Unit = {
      if (owner.node.network != null) {
        super.onBufferMaxResolutionChange(w, h)
        owner.host.markChanged()
        owner.synchronized(ServerPacketSender.appendTextBufferMaxResolutionChange(owner.pendingCommands, w, h))
      }
    }

    override def onBufferSet(col: Int, row: Int, s: String, vertical: Boolean): Unit = {
      super.onBufferSet(col, row, s, vertical)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferSet(owner.pendingCommands, col, row, s, vertical))
    }

    override def onBufferBitBlt(col: Int, row: Int, w: Int, h: Int, ram: component.GpuTextBuffer, fromCol: Int, fromRow: Int): Unit = {
      super.onBufferBitBlt(col, row, w, h, ram, fromCol, fromRow)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferBitBlt(owner.pendingCommands, col, row, w, h, ram.owner, ram.id, fromCol, fromRow))
    }

    override def onBufferRamInit(ram: component.GpuTextBuffer): Unit = {
      super.onBufferRamInit(ram)
      owner.host.markChanged()
      val nbt = new CompoundStorage()
      ram.saveData(nbt)
      owner.synchronized(ServerPacketSender.appendTextBufferRamInit(owner.pendingCommands, ram.owner, ram.id, CompoundStorage.CODEC.encode(nbt, NbtOps.INSTANCE, new CompoundTag()).getOrThrow().asInstanceOf[CompoundTag]))
    }

    override def onBufferRamDestroy(ram: component.GpuTextBuffer): Unit = {
      super.onBufferRamDestroy(ram)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferRamDestroy(owner.pendingCommands, ram.owner, ram.id))
    }

    override def onBufferRawSetText(col: Int, row: Int, text: Array[Array[Int]]): Unit = {
      super.onBufferRawSetText(col, row, text)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferRawSetText(owner.pendingCommands, col, row, text))
    }

    override def onBufferRawSetBackground(col: Int, row: Int, color: Array[Array[Int]]): Unit = {
      super.onBufferRawSetBackground(col, row, color)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferRawSetBackground(owner.pendingCommands, col, row, color))
    }

    override def onBufferRawSetForeground(col: Int, row: Int, color: Array[Array[Int]]): Unit = {
      super.onBufferRawSetForeground(col, row, color)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferRawSetForeground(owner.pendingCommands, col, row, color))
    }

    override def keyDown(character: Char, code: Int, player: Player): Unit = {
      sendToKeyboards("keyboard.keyDown", player, Char.box(character), Int.box(code))
    }

    override def keyUp(character: Char, code: Int, player: Player): Unit = {
      sendToKeyboards("keyboard.keyUp", player, Char.box(character), Int.box(code))
    }

    override def textInput(codePt: Int, player: Player): Unit = {
      sendToKeyboards("keyboard.textInput", player, Int.box(codePt))
    }

    override def clipboard(value: String, player: Player): Unit = {
      sendToKeyboards("keyboard.clipboard", player, value)
    }

    override def mouseDown(x: Double, y: Double, button: Int, player: Player): Unit = {
      sendMouseEvent(player, "touch", x, y, button)
    }

    override def mouseDrag(x: Double, y: Double, button: Int, player: Player): Unit = {
      sendMouseEvent(player, "drag", x, y, button)
    }

    override def mouseUp(x: Double, y: Double, button: Int, player: Player): Unit = {
      sendMouseEvent(player, "drop", x, y, button)
    }

    override def mouseScroll(x: Double, y: Double, delta: Int, player: Player): Unit = {
      sendMouseEvent(player, "scroll", x, y, delta)
    }

    override def copyToAnalyzer(line: Int, player: Player): Unit = {
      val stack = player.getItemInHand(InteractionHand.MAIN_HAND)
      if (!stack.isEmpty) {
        stack.update(
          DataComponents.CUSTOM_DATA,
          CustomData.EMPTY,
          (customData: CustomData) => {
            val tag = customData.copyTag()
            tag.remove(Settings.namespace + "clipboard")
            CustomData.of(tag)
          }
        )

        if (line >= 0 && line < owner.getViewportHeight) {
          val text = owner.data.lineToString(line)
          if (!Strings.isNullOrEmpty(text)) {
            stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe.putString(Settings.namespace + "clipboard", text)
          }
        }
      }
    }

    private def sendMouseEvent(player: Player, name: String, x: Double, y: Double, data: Int) = {
      val args = mutable.ArrayBuffer.empty[AnyRef]

      args += player
      args += name
      if (owner.precisionMode) {
        args += Double.box(x)
        args += Double.box(y)
      }
      else {
        args += Int.box(x.toInt + 1)
        args += Int.box(y.toInt + 1)
      }
      args += Int.box(data)
      if (Settings.get.inputUsername) {
        args += player.getName.getString
      }

      owner.node.sendToReachable("computer.checked_signal", args.toSeq: _*)
    }

    private def sendToKeyboards(name: String, values: AnyRef*): Unit = {
      owner.host match {
        case screen: blockentity.Screen =>
          screen.screens.foreach(_.node.sendToNeighbors(name, values: _*))
        case _ =>
          owner.node.sendToNeighbors(name, values: _*)
      }
    }
  }

}
