package li.cil.oc.common.blockentity

import li.cil.oc.{Constants, OpenComputers, Settings, api}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.{Node, Visibility}
import li.cil.oc.common.blockentity.traits
import li.cil.oc.common.init.OCBlocks
import li.cil.oc.common.item.Tape
import li.cil.oc.server.component.AudioCard
import net.minecraft.core.{BlockPos, Direction, HolderLookup}
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.MenuProvider
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension
import net.neoforged.neoforge.server.ServerLifecycleHooks
import net.minecraft.world.level.storage.LevelResource

import scala.collection.mutable
import java.io.{BufferedInputStream, BufferedOutputStream, FileInputStream, FileOutputStream}
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

private object TapeData {
  val Bytes = Settings.namespace + "tapeBytes"
  val Storage = Settings.namespace + "tapeStorage"
  val Label = Settings.namespace + "tapeLabel"
  def sha256(bytes: Array[Byte]): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xFF}%02x").mkString
  def tag(stack: ItemStack): CompoundTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
  private def storageFile(id: String): java.io.File = {
    val root = ServerLifecycleHooks.getCurrentServer.getWorldPath(new LevelResource(Settings.savePath + "tapes")).toFile
    root.mkdirs(); new java.io.File(root, id + ".dsk")
  }
  private def loadFile(id: String): Array[Byte] = {
    val file = storageFile(id)
    if (!file.exists()) return Array.emptyByteArray
    val in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)))
    try in.readAllBytes() finally in.close()
  }
  private def saveFile(id: String, bytes: Array[Byte]): Unit = {
    val file = storageFile(id); val temp = new java.io.File(file.getParentFile, file.getName + ".tmp")
    val out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(temp)))
    try { out.write(bytes); out.finish() } finally out.close()
    java.nio.file.Files.move(temp.toPath, file.toPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
  }
  def bytes(stack: ItemStack): Array[Byte] = synchronized {
    val t = tag(stack)
    if (!t.contains(Storage)) t.getByteArray(Bytes)
    else {
      val raw = loadFile(t.getString(Storage))
      val tapeCapacity = stack.getItem match {
        case tape: Tape => tape.capacity
        case _ => Int.MaxValue
      }

      // Computronics 1.12 stored: version byte, four-byte cursor, then a
      // fixed-capacity tape array. The NeoForge port stores only logical tape
      // bytes. Recognize the old container only when its size is larger than
      // the inserted tape can hold, so ordinary raw DFPWM data is untouched.
      if (raw.length > tapeCapacity && raw.length >= 5 && raw(0) == 1)
        java.util.Arrays.copyOfRange(raw, 5, math.min(raw.length, tapeCapacity + 5))
      else raw
    }
  }
  def save(stack: ItemStack, bytes: Array[Byte], label: String): Unit = synchronized {
    val t = tag(stack)
    val id = if (t.contains(Storage)) t.getString(Storage) else UUID.randomUUID().toString
    saveFile(id, bytes)
    OpenComputers.log.info(s"Tape bytes saved: length=${bytes.length}, sha256=${sha256(bytes)}")
    t.remove(Bytes); t.putString(Storage, id); t.putString(Label, label)
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t))
  }
}

object AudioRouter {
  private val MaxCables = 64

  type Session = (li.cil.oc.api.network.EnvironmentHost, Int)

  private def outputs(source: traits.Environment): Seq[li.cil.oc.api.network.EnvironmentHost] = {
    val level = source.getLevel
    if (level == null || level.isClientSide) return Seq.empty
    val origin = source.getBlockPos
    val pending = mutable.Queue(origin)
    val visited = mutable.Set(origin)
    val speakers = mutable.ArrayBuffer.empty[Speaker]
    while (pending.nonEmpty && visited.size <= MaxCables) {
      val current = pending.dequeue()
      Direction.values.foreach { direction =>
        val next = li.cil.oc.util.BlockPosHelper.relative(current, direction)
        if (!visited(next)) {
          val entity = level.getBlockEntity(next)
          if (entity.isInstanceOf[AudioCable] ||
            li.cil.oc.integration.Mods.CBMultipart.isModAvailable && li.cil.oc.integration.multipart.ModCBMultipart.isAudioCable(level, next)) {
            visited += next
            pending.enqueue(next)
          }
          else entity match {
            case speaker: Speaker => visited += next; speakers += speaker
            case _ =>
          }
        }
      }
    }
    if (speakers.isEmpty) Seq(source) else speakers.toSeq
  }

  def play(source: traits.Environment, pcm: Array[Byte], sampleRate: Int = 32768): Seq[Session] = {
    // Keep tape PCM packets aligned with AsieLib's 1024-byte streaming
    // buffers. This is deliberately transport-only; DFPWM decoding remains
    // stateful and is still performed by the authoritative codec.
    outputs(source).flatMap(output => AudioCard.playPcm(output, pcm, sampleRate, 1024).map(handle => output -> handle))
  }

  def playDfpwm(source: traits.Environment, encoded: Array[Byte], sampleRate: Int, volume: Float): Seq[Session] = {
    outputs(source).flatMap(output => AudioCard.playDfpwm(output, encoded, sampleRate, volume).map(handle => output -> handle))
  }

  def stop(sessions: Seq[Session]): Unit = sessions.foreach { case (host, handle) => li.cil.oc.server.PacketSender.sendAudioStop(host, handle) }
}

class AudioCable(pos: BlockPos, state: BlockState)
  extends BlockEntity(BlockEntityTypes.AUDIO_CABLE.get(), pos, state) with IBlockEntityExtension

class Speaker(pos: BlockPos, state: BlockState)
  extends BlockEntity(BlockEntityTypes.SPEAKER.get(), pos, state) with traits.Environment with IBlockEntityExtension {
  override val node: Node = api.Network.newNode(this, Visibility.Network).withComponent("speaker").create()
  @Callback def play(context: Context, args: Arguments): Array[AnyRef] = {
    val frequency = args.optInteger(0, 440).max(20).min(2000)
    val duration = (args.optDouble(1, 0.2) * 1000).toInt.max(50).min(5000)
    val level = getLevel
    if (level != null && !level.isClientSide) li.cil.oc.server.PacketSender.sendComputronicsTone(level, x + 0.5, y + 0.5, z + 0.5, 0, frequency, duration, 0, 1)
    result(true)
  }
}

object TapeDrive {
  private final val Stopped = "stopped"
  private final val Playing = "playing"
  private final val Rewinding = "rewinding"
  private final val Forwarding = "forwarding"
  private final val DfpwmSampleRate = 48000
  private final val BaseBytesPerTick = DfpwmSampleRate / 8.0 / 20.0
}

class TapeDrive(pos: BlockPos, state: BlockState)
  extends BlockEntity(BlockEntityTypes.TAPE_DRIVE.get(), pos, state) with traits.Environment with traits.Inventory with traits.Tickable with MenuProvider with IBlockEntityExtension {
  import TapeDrive._
  override def createMenu(id: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
    new li.cil.oc.common.menu.TapeDrive(id, playerInventory, this)
  override val node: Node = api.Network.newNode(this, Visibility.Network).withComponent("tape_drive").create()
  override def getContainerSize: Int = 1
  private var cursor = 0
  private var state = Stopped
  private var speed = 1.0
  private var volume = 1.0
  private var activeAudio = Seq.empty[AudioRouter.Session]

  /** Small synchronized state value used by the tape-drive GUI. */
  def guiState: Int = state match {
    case Playing => 1
    case Rewinding => 2
    case Forwarding => 3
    case _ => 0
  }

  private def tape: Option[ItemStack] = Option(getItem(0)).filter(stack => !stack.isEmpty && stack.getItem.isInstanceOf[Tape])
  private def capacity(stack: ItemStack): Int = stack.getItem.asInstanceOf[Tape].capacity
  private def data(stack: ItemStack): Array[Byte] = TapeData.bytes(stack)
  private def persist(stack: ItemStack, bytes: Array[Byte]): Unit = TapeData.save(stack, bytes, TapeData.tag(stack).getString(TapeData.Label))

  def interact(player: Player, held: ItemStack): Unit = {
    if (player.isCrouching) {
      if (tape.nonEmpty) { stopTape(); dropSlot(0, 1, None) }
      if (getItem(0).isEmpty && held.getItem.isInstanceOf[Tape]) setItem(0, held.split(1))
    } else playTape()
  }

  def onRedstone(powered: Boolean): Unit = {
    if (powered && tape.nonEmpty && state != Playing) playTape()
    else if (!powered) stopTape()
    setChanged()
  }

  @Callback(direct = true) def isReady(context: Context, args: Arguments): Array[AnyRef] = result(tape.nonEmpty)
  @Callback(direct = true) def getSize(context: Context, args: Arguments): Array[AnyRef] = result(tape.map(capacity).getOrElse(0))
  @Callback(direct = true) def getPosition(context: Context, args: Arguments): Array[AnyRef] = result(cursor)
  /** Legacy seek is relative and returns the distance actually moved. */
  @Callback def seek(context: Context, args: Arguments): Array[AnyRef] = tape match {
    case Some(stack) =>
      val old = cursor; cursor = (cursor + args.checkInteger(0)).max(0).min(capacity(stack)); setChanged(); result(cursor - old)
    case _ => result(null, "no tape inserted")
  }
  @Callback def read(context: Context, args: Arguments): Array[AnyRef] = tape match {
    case Some(stack) if args.count == 0 =>
      val bytes = data(stack); val value = if (cursor < bytes.length) bytes(cursor) & 0xFF else 0; cursor = (cursor + 1).min(capacity(stack)); result(value)
    case Some(stack) =>
      val bytes = data(stack); val n = args.checkInteger(0).max(0).min(256); val end = (cursor + n).min(capacity(stack))
      val resultBytes = Array.tabulate(end - cursor)(i => if (cursor + i < bytes.length) bytes(cursor + i) else 0.toByte)
      cursor = end; result(resultBytes)
    case _ => result(null, "no tape inserted")
  }
  @Callback def write(context: Context, args: Arguments): Array[AnyRef] = tape match {
    case Some(stack) =>
      val input = if (args.isInteger(0)) Array(args.checkInteger(0).toByte) else args.checkByteArray(0); val bytes = data(stack); val end = cursor + input.length
      if (end > capacity(stack)) result(null, "tape is full")
      else { val next = java.util.Arrays.copyOf(bytes, bytes.length.max(end)); System.arraycopy(input, 0, next, cursor, input.length); cursor = end; persist(stack, next); setChanged(); result(input.length) }
    case _ => result(null, "no tape inserted")
  }
  @Callback(direct = true) def getLabel(context: Context, args: Arguments): Array[AnyRef] = result(tape.map(s => TapeData.tag(s).getString(TapeData.Label)).orNull)
  @Callback def setLabel(context: Context, args: Arguments): Array[AnyRef] = tape match { case Some(stack) => TapeData.save(stack, data(stack), args.checkString(0).take(64)); setChanged(); result(true); case _ => result(null, "no tape inserted") }
  @Callback def play(context: Context, args: Arguments): Array[AnyRef] = result(playTape())
  @Callback def stop(context: Context, args: Arguments): Array[AnyRef] = { stopTape(); result(true) }
  @Callback def setSpeed(context: Context, args: Arguments): Array[AnyRef] = { speed = args.checkDouble(0).max(0.25).min(2.0); result(speed) }
  @Callback def setVolume(context: Context, args: Arguments): Array[AnyRef] = { volume = args.checkDouble(0).max(0).min(1); result(volume) }
  @Callback(direct = true) def getState(context: Context, args: Arguments): Array[AnyRef] = result(state)
  @Callback(direct = true) def isEnd(context: Context, args: Arguments): Array[AnyRef] = result(tape.forall(stack => cursor + 1024 >= capacity(stack)))

  override def loadForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForServer(nbt, provider)
    cursor = nbt.getInt("tapeCursor")
    state = Option(nbt.getString("tapeState")).filter(Set(Stopped, Playing, Rewinding, Forwarding)).getOrElse(Stopped)
    speed = nbt.getDouble("tapeSpeed") match { case value if value >= 0.25 && value <= 2.0 => value; case _ => 1.0 }
    volume = nbt.getDouble("tapeVolume") match { case value if value >= 0 && value <= 1 => value; case _ => 1.0 }
    // Migrate pre-file tapes out of item CustomData the first time the drive loads.
    tape.foreach { stack =>
      val t = TapeData.tag(stack)
      if (!t.contains(TapeData.Storage) && t.contains(TapeData.Bytes))
        TapeData.save(stack, t.getByteArray(TapeData.Bytes), t.getString(TapeData.Label))
    }
    tape.foreach(stack => cursor = cursor.max(0).min(capacity(stack)))
  }

  override def saveForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveForServer(nbt, provider)
    nbt.putInt("tapeCursor", cursor)
    nbt.putString("tapeState", state)
    nbt.putDouble("tapeSpeed", speed)
    nbt.putDouble("tapeVolume", volume)
  }

  override def updateEntity(): Unit = {
    super.updateEntity()
    if (getLevel == null || getLevel.isClientSide || state == Stopped) return
    tape match {
      case Some(stack) =>
        val old = cursor
        val step = math.max(1, (BaseBytesPerTick * speed).toInt)
        state match {
          case Playing | Forwarding => cursor = math.min(data(stack).length, cursor + step)
          case Rewinding => cursor = math.max(0, cursor - step)
          case _ =>
        }
        if ((state == Playing || state == Forwarding) && cursor >= data(stack).length || state == Rewinding && cursor <= 0) {
          state = Stopped
          activeAudio = Seq.empty
        }
        if (cursor != old) setChanged()
      case _ => state = Stopped
    }
  }

  private def playTape(): Boolean = tape.exists { stack =>
    val bytes = data(stack)
    if (cursor >= bytes.length) false
    else {
      val encoded = bytes.slice(cursor, bytes.length)
      OpenComputers.log.info(s"Tape playback source: cursor=$cursor, tapeLength=${bytes.length}, tapeSha256=${TapeData.sha256(bytes)}, encodedLength=${encoded.length}, encodedSha256=${TapeData.sha256(encoded)}, sampleRate=${(DfpwmSampleRate * speed).toInt}")
      // Computronics 1.12 transported the encoded 1024-byte packets and kept
      // the stateful AsieLib decoder on the client. Preserve that entire path.
      activeAudio = AudioRouter.playDfpwm(this, encoded, (DfpwmSampleRate * speed).toInt, volume.toFloat)
      if (activeAudio.nonEmpty) { state = Playing; setChanged() }
      activeAudio.nonEmpty
    }
  }

  private def stopTape(): Unit = {
    AudioRouter.stop(activeAudio)
    activeAudio = Seq.empty
    state = Stopped
    setChanged()
  }

  /** Handle a validated tape-drive GUI action from the server packet handler. */
  def handleControl(action: Int, player: Player): Unit = {
    if (getLevel == null || getLevel.isClientSide || !stillValid(player)) return
    action match {
      case 0 => stopTape(); state = Rewinding; setChanged()
      case 1 => if (state == Playing) stopTape() else playTape()
      case 2 => stopTape()
      case 3 => stopTape(); state = Forwarding; setChanged()
      case _ =>
    }
  }
}
