package li.cil.oc.common.blockentity

import li.cil.oc.Settings
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network._
import li.cil.oc.client.gui
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.blockentity.traits.RedstoneChangedEventArgs
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedLevel._
import li.cil.oc.util.ExtendedDataComponentHolder._
import li.cil.oc.client.renderer.block.ScreenModel
import li.cil.oc.common.Tier
import li.cil.oc.common.datacomponents.OCComponents
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponentHolder
import net.minecraft.core.{BlockPos, Direction, HolderLookup}
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.minecraft.world.phys.AABB

import scala.collection.mutable
import scala.language.postfixOps
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.Arrow
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.client.model.data.ModelData
import net.neoforged.neoforge.common.MutableDataComponentHolder
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension

class Screen(pos: BlockPos, state: BlockState, var tier: Int) extends BlockEntity(BlockEntityTypes.SCREEN.get(), pos, state)
  with traits.TextBuffer with SidedEnvironment with traits.Rotatable with traits.RedstoneAware with traits.Colored with Analyzable with Ordered[Screen]
  with IBlockEntityExtension {
  def this(pos: BlockPos, state: BlockState) = this(pos, state, 0)

  // Enable redstone functionality.
  _isOutputEnabled = true

  override def validFacings = Direction.values

  @OnlyIn(Dist.CLIENT)
  override def getModelData: ModelData =
    ModelData.builder()
  .`with`(ScreenModel.SCREEN_PROPERTY, this)
    .build()

  // ----------------------------------------------------------------------- //

  /**
    * Check for multi-block screen option in next update. We do this in the
    * update to avoid unnecessary checks on chunk unload.
    */
  var shouldCheckForMultiBlock = true

  /**
    * On the client we delay connecting screens a little, to avoid glitches
    * when not all tile entity data for a chunk has been received within a
    * single tick (meaning some screens are still "missing").
    */
  var delayUntilCheckForMultiBlock = 40

  var width, height = 1

  var cachedBounds: Option[(Boolean, Int, Int, AABB)] = None

  var origin = this

  val screens = mutable.Set(this)

  var hadRedstoneInput = false

  var invertTouchMode = false

  private val arrows = mutable.Set.empty[Arrow]

  private val lastWalked = mutable.WeakHashMap.empty[Entity, (Int, Int)]

  setColor(Color.byTier(tier))

  @OnlyIn(Dist.CLIENT)
  override def canConnect(side: Direction) = side != facing

  // Allow connections from front for keyboards, and keyboards only...
  override def sidedNode(side: Direction) = if (side != facing || (getLevel.isLoaded(getBlockPos.relative(side)) && getLevel.getBlockEntity(getBlockPos.relative(side)).isInstanceOf[Keyboard])) node else null

  // ----------------------------------------------------------------------- //

  def isOrigin = origin == this

  def getRenderBoundingBox: AABB = {
    cachedBounds match {
      case Some((o, w, h, b)) if o == isOrigin && w == width && h == height => b
      case _ =>
        val bb = if ((width == 1 && height == 1) || !isOrigin) new AABB(getBlockPos) else {
          val size = unproject(width - 1, height - 1, 0)
          new AABB(getBlockPos).expandTowards(size.x, size.y, size.z)
        }
        cachedBounds = Some((isOrigin, width, height, bb))
        bb
    }
  }

  def localPosition = {
    val lpos = project(this)
    val opos = project(origin)
    (lpos.x - opos.x, lpos.y - opos.y)
  }

  def hasKeyboard = screens.exists(screen =>
    Direction.values.map(side => (side, {
      val blockPos = BlockPosition(screen).offset(side)
      if (getLevel.blockExists(blockPos)) getLevel.getBlockEntity(blockPos.toBlockPos)
      else null
    })).exists {
      case (side, keyboard: Keyboard) => keyboard.hasNodeOnSide(side.getOpposite)
      case _ => false
    })

  def checkMultiBlock(): Unit = {
    shouldCheckForMultiBlock = true
    width = 1
    height = 1
    origin = this
    screens.clear()
    screens += this
    invertTouchMode = false
  }

  def toScreenCoordinates(hitX: Double, hitY: Double, hitZ: Double): (Boolean, Option[(Double, Double)]) = {
    // Compute absolute position of the click on the face, measured in blocks.
    def dot(f: Direction) = f.getStepX * hitX + f.getStepY * hitY + f.getStepZ * hitZ
    val (hx, hy) = (dot(toGlobal(Direction.EAST)), dot(toGlobal(Direction.UP)))
    val tx = if (hx < 0) 1 + hx else hx
    val ty = 1 - (if (hy < 0) 1 + hy else hy)
    val (lx, ly) = localPosition
    val (ax, ay) = (lx + tx, height - 1 - ly + ty)

    // Get the relative position in the *display area* of the face.
    val border = 2.25 / 16.0
    if (ax <= border || ay <= border || ax >= width - border || ay >= height - border) {
      return (false, None)
    }
    if (!getLevel.isClientSide) return (true, None)

    val (iw, ih) = (width - border * 2, height - border * 2)
    val (rx, ry) = ((ax - border) / iw, (ay - border) / ih)

    // Make it a relative position in the displayed buffer.
    val bw = origin.buffer.getViewportWidth
    val bh = origin.buffer.getViewportHeight
    val (bpw, bph) = (origin.buffer.renderWidth / iw.toDouble, origin.buffer.renderHeight / ih.toDouble)
    val (brx, bry) = if (bpw > bph) {
      val rh = bph.toDouble / bpw.toDouble
      val bry = (ry - (1 - rh) * 0.5) / rh
      (rx, bry)
    }
    else if (bph > bpw) {
      val rw = bpw.toDouble / bph.toDouble
      val brx = (rx - (1 - rw) * 0.5) / rw
      (brx, ry)
    }
    else {
      (rx, ry)
    }

    val inBounds = bry >= 0 && bry <= 1 && brx >= 0 || brx <= 1
    (inBounds, Some((brx * bw, bry * bh)))
  }

  def copyToAnalyzer(hitX: Double, hitY: Double, hitZ: Double): Boolean = {
    val (inBounds, coordinates) = toScreenCoordinates(hitX, hitY, hitZ)
    coordinates match {
      case Some((x, y)) => origin.buffer match {
        case buffer: TextBuffer =>
          buffer.copyToAnalyzer(y.toInt, null)
          true
        case _ => false
      }
      case _ => inBounds
    }
  }

  def click(hitX: Double, hitY: Double, hitZ: Double): Boolean = {
    val (inBounds, coordinates) = toScreenCoordinates(hitX, hitY, hitZ)
    coordinates match {
      case Some((x, y)) =>
        // Send the packet to the server (manually, for accuracy).
        origin.buffer.mouseDown(x, y, 0, null)
        true
      case _ => inBounds
    }
  }

  def walk(entity: Entity): Unit = {
    val (x, y) = localPosition
    origin.lastWalked.put(entity, localPosition) match {
      case Some((oldX, oldY)) if oldX == x && oldY == y => // Ignore
      case _ => entity match {
        case player: Player if Settings.get.inputUsername =>
          origin.node.sendToReachable("computer.signal", "walk", Int.box(x + 1), Int.box(height - y), player.getName.getString)
        case _ =>
          origin.node.sendToReachable("computer.signal", "walk", Int.box(x + 1), Int.box(height - y))
      }
    }
  }

  def shot(arrow: Arrow): Unit = {
    arrows.add(arrow)
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity(): Unit = {
    super.updateEntity()
    if (shouldCheckForMultiBlock && ((isClient && isClientReadyForMultiBlockCheck) || (isServer && isConnected))) {
      // Make sure we merge in a deterministic order, to avoid getting
      // different results on server and client due to the update order
      // differing between the two. This also saves us from having to save
      // any multi-block specific state information.
      val pending = mutable.SortedSet(this)
      val queue = mutable.Queue(this)
      while (queue.nonEmpty) {
        val current = queue.dequeue()
        val lpos = project(current)
        def tryQueue(dx: Int, dy: Int): Unit = {
          val npos = unproject(lpos.x + dx, lpos.y + dy, lpos.z)
          if (getLevel.blockExists(npos)) getLevel.getBlockEntity(npos.toBlockPos) match {
            case s: Screen if s.pitch == pitch && s.yaw == yaw && pending.add(s) => queue += s
            case _ => // Ignore.
          }
        }
        tryQueue(-1, 0)
        tryQueue(1, 0)
        tryQueue(0, -1)
        tryQueue(0, 1)
      }
      // Perform actual merges.
      while (pending.nonEmpty) {
        val current = pending.firstKey
        while (current.tryMerge()) {}
        current.screens.foreach {
          screen =>
            screen.shouldCheckForMultiBlock = false
            pending.remove(screen)
            queue += screen
        }
      }
      if (isClient) updateMergedModels()
      // Update visibility after everything is done, to avoid noise.
      queue.foreach(screen => {
        val buffer = screen.buffer
        if (screen.isOrigin) {
          if (isServer) {
            buffer.node.asInstanceOf[Component].setVisibility(Visibility.Network)
            buffer.setEnergyCostPerTick(Settings.get.screenCost * screen.width * screen.height)
            buffer.setAspectRatio(screen.width, screen.height)
          }
        }
        else {
          if (isServer) {
            buffer.node.asInstanceOf[Component].setVisibility(Visibility.None)
            buffer.setEnergyCostPerTick(Settings.get.screenCost)
          }
          buffer.setAspectRatio(1, 1)
          val w = buffer.getWidth
          val h = buffer.getHeight
          buffer.setForegroundColor(0xFFFFFF, false)
          buffer.setBackgroundColor(0x000000, false)
          buffer.fill(0, 0, w, h, 0x20)
        }
      })
    }
    if (arrows.nonEmpty) {
      for (arrow <- arrows) {
        val hitX = arrow.getX - x
        val hitY = arrow.getY - y
        val hitZ = arrow.getZ - z
        arrow.getOwner match {
          case player: Player if player == Minecraft.getInstance.player => click(hitX, hitY, hitZ)
          case _ =>
        }
      }
      arrows.clear()
    }
  }

  private def updateMergedModels(): Unit = {
    if (getLevel == Minecraft.getInstance.level) {
      val renderer = Minecraft.getInstance.levelRenderer
      screens.foreach(screen => {
        val pos = screen.getBlockPos
        renderer.setSectionDirty(pos.getX >> 4, pos.getY >> 4, pos.getZ >> 4)
      })
    }
  }

  private def isClientReadyForMultiBlockCheck = if (delayUntilCheckForMultiBlock > 0) {
    delayUntilCheckForMultiBlock -= 1
    false
  } else true

  override def dispose(): Unit = {
    super.dispose()
    screens.clone().foreach(_.checkMultiBlock())
    if (isClient) {
      Minecraft.getInstance.screen match {
        case screenGui: gui.Screen if screenGui.buffer == buffer => screenGui.onClose()
        case _ =>
      }
    }
  }

  override protected def onColorChanged(): Unit = {
    super.onColorChanged()
    screens.clone().foreach(_.checkMultiBlock())
  }

  // ----------------------------------------------------------------------- //

  override def loadComponentsCommon(holder: DataComponentHolder): Unit = {
    for(t <- holder.getComponent(OCComponents.TIER)) tier = t
    setColor(Color.byTier(tier))
    super.loadComponentsCommon(holder)

    invertTouchMode = holder.has(OCComponents.INVERT_TOUCH)
  }

  override def saveComponentsCommon(holder: MutableDataComponentHolder): Unit = {
    holder.setComponent(OCComponents.TIER, tier.toByte)
    super.saveComponentsCommon(holder)
    holder.setComponent(OCComponents.INVERT_TOUCH, invertTouchMode)
  }

  override def loadComponentsForServer(holder: DataComponentHolder): Unit = {
    super.loadComponentsForServer(holder)
    hadRedstoneInput = holder.getComponent(OCComponents.HAS_REDSTONE_INPUT) getOrElse false
  }

  override def saveComponentsForServer(holder: MutableDataComponentHolder): Unit = {
    super.saveComponentsForServer(holder)
    holder.setComponent(OCComponents.HAS_REDSTONE_INPUT, hadRedstoneInput)
  }

  // Explicit entry point used by BaseBlockEntity.getUpdateTag. Scala trait
  // dispatch can otherwise skip TextBuffer.saveForClient, leaving clients
  // with an empty buffer (see OpenComputers-CE PR #6 for the 1.20 port).
  override def saveForClient(nbt: CompoundTag, provider: HolderLookup.Provider): Unit =
    saveForClientDirect(nbt, provider)

  def saveForClientDirect(nbt: CompoundTag, provider: HolderLookup.Provider): Unit =
    super.saveForClient(nbt, provider)

  override def loadComponentsForClient(holder: DataComponentHolder): Unit = {
    super.loadComponentsForClient(holder)
    requestModelDataUpdate()
  }

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: Player, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = Array(origin.node)

  override protected def onRedstoneInputChanged(args: RedstoneChangedEventArgs): Unit = {
    super.onRedstoneInputChanged(args)
    val hasRedstoneInput = screens.map(_.maxInput).max > 0
    if (hasRedstoneInput != hadRedstoneInput) {
      hadRedstoneInput = hasRedstoneInput
      if (hasRedstoneInput) {
        origin.buffer.setPowerState(!origin.buffer.getPowerState)
      }
    }
  }

  override def onRotationChanged(): Unit = {
    super.onRotationChanged()
    screens.clone().foreach(_.checkMultiBlock())
  }

  // ----------------------------------------------------------------------- //

  override def compare(that: Screen) =
    if (x != that.x) x - that.x
    else if (y != that.y) y - that.y
    else z - that.z

  // ----------------------------------------------------------------------- //

  private def tryMerge(): Boolean = {
    val opos = project(origin)
    def tryMergeTowards(dx: Int, dy: Int) = {
      val npos = unproject(opos.x + dx, opos.y + dy, opos.z)
      getEnvironmentLevel.blockExists(npos) && (getLevel.getBlockEntity(npos) match {
        case s: Screen if s.tier == tier && s.pitch == pitch && s.getColor == getColor && s.yaw == yaw && !screens.contains(s) =>
          val spos = project(s.origin)
          val canMergeAlongX = spos.y == opos.y && s.height == height && s.width + width <= Settings.get.maxScreenWidth
          val canMergeAlongY = spos.x == opos.x && s.width == width && s.height + height <= Settings.get.maxScreenHeight
          if (canMergeAlongX || canMergeAlongY) {
            val (newOrigin) =
              if (canMergeAlongX) {
                if (spos.x < opos.x) s.origin else origin
              }
              else {
                if (spos.y < opos.y) s.origin else origin
              }
            val (newWidth, newHeight) =
              if (canMergeAlongX) (width + s.width, height)
              else (width, height + s.height)
            val newScreens = screens ++ s.screens
            for (screen <- newScreens) {
              screen.width = newWidth
              screen.height = newHeight
              screen.origin = newOrigin
              screen.screens ++= newScreens // It's a set, so there won't be duplicates.
            }
            true
          }
          else false // Cannot merge.
        case _ => false
      })
    }
    tryMergeTowards(0, height) || tryMergeTowards(0, -1) || tryMergeTowards(width, 0) || tryMergeTowards(-1, 0)
  }

  private def project(t: Screen) = {
    def dot(f: Direction, s: Screen) = f.getStepX * s.x + f.getStepY * s.y + f.getStepZ * s.z
    BlockPosition(dot(toGlobal(Direction.EAST), t), dot(toGlobal(Direction.UP), t), dot(toGlobal(Direction.SOUTH), t))
  }

  private def unproject(x: Int, y: Int, z: Int) = {
    def dot(f: Direction) = f.getStepX * x + f.getStepY * y + f.getStepZ * z
    BlockPosition(dot(toLocal(Direction.EAST)), dot(toLocal(Direction.UP)), dot(toLocal(Direction.SOUTH)))
  }
}
