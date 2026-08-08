package li.cil.oc.common.blockentity

import java.nio.charset.StandardCharsets
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

import li.cil.oc.{Constants, Settings, api}
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.{Node, Visibility}
import li.cil.oc.common.blockentity.traits
import li.cil.oc.server.component.{ComputronicsDeviceInfo, ComputronicsDeviceInfoEnvironment, ComputronicsNativeSupport}
import net.minecraft.core.{BlockPos, HolderLookup}
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.{Entity, LivingEntity}
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension

import scala.jdk.CollectionConverters._

/**
 * Base for Computronics' own block entities.
 *
 * This is deliberately an OpenComputers-native environment. ComputerCraft,
 * ProjectRed, RedLogic, Railcraft, and other third-party hooks stay outside
 * this class and are not part of the port.
 */
abstract class ComputronicsBlockEntity(
  pos: BlockPos,
  state: BlockState,
  val computronicsKind: String,
  val componentName: String,
  connectorBuffer: Double = 0
) extends BlockEntity(BlockEntityTypes.COMPUTRONICS.get(), pos, state)
  with traits.Environment with traits.Colored with IBlockEntityExtension {

  override val node: Node = {
    val builder = api.Network.newNode(this, Visibility.Network).withComponent(componentName)
    if (connectorBuffer > 0) builder.withConnector(connectorBuffer).create()
    else builder.create()
  }

  protected final def signal(name: String, values: Any*): Unit =
    node.sendToReachable("computer.signal", (name +: values.map(_.asInstanceOf[AnyRef])).toArray: _*)

  @Callback(direct = true)
  def getDeviceState(context: Context, args: Arguments): Array[AnyRef] =
    result(Map("type" -> computronicsKind, "color" -> getColor))

  protected def loadComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {}

  protected def saveComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {}

  override def loadForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForServer(nbt, provider)
    loadComputronics(nbt, provider)
  }

  override def saveForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveForServer(nbt, provider)
    saveComputronics(nbt, provider)
  }
}

object ComputronicsBlockEntity {
  def create(pos: BlockPos, state: BlockState): ComputronicsBlockEntity =
    create(pos, state, state.getBlock.asInstanceOf[li.cil.oc.common.block.ComputronicsBlock].computronicsKind)

  def create(pos: BlockPos, state: BlockState, kind: String): ComputronicsBlockEntity = kind match {
    case Constants.BlockName.ComputronicsIronNote => new ComputronicsIronNote(pos, state)
    case Constants.BlockName.ComputronicsAudioCable => new ComputronicsAudioCable(pos, state)
    case Constants.BlockName.ComputronicsSpeaker => new ComputronicsSpeaker(pos, state)
    case Constants.BlockName.ComputronicsTapeReader => new ComputronicsTapeDrive(pos, state)
    case Constants.BlockName.ComputronicsCamera => new ComputronicsCamera(pos, state)
    case Constants.BlockName.ComputronicsChatBox => new ComputronicsChatBox(pos, state)
    case Constants.BlockName.ComputronicsCipher => new ComputronicsCipher(pos, state)
    case Constants.BlockName.ComputronicsCipherAdvanced => new ComputronicsAdvancedCipher(pos, state)
    case Constants.BlockName.ComputronicsRadar => new ComputronicsRadar(pos, state)
    case Constants.BlockName.ComputronicsColorfulLamp => new ComputronicsColorfulLamp(pos, state)
    case Constants.BlockName.ComputronicsSpeechBox => new ComputronicsSpeechBox(pos, state)
    case _ => new ComputronicsAudioCable(pos, state)
  }
}

class ComputronicsIronNote(pos: BlockPos, state: BlockState)
  extends ComputronicsBlockEntity(pos, state, Constants.BlockName.ComputronicsIronNote, "iron_noteblock")
    with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Multimedia, "Music note emission device", "Yanaki Sound Systems", "Vanilla 1")
  private var lastNote = -1
  private var lastInstrument = 0
  private var lastVolume = 1.0

  @Callback
  def playNote(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.count() == 0) return result((), "missing note")
    lastNote = math.max(0, math.min(24, args.checkInteger(0)))
    lastInstrument = args.optInteger(1, 0)
    lastVolume = math.max(0, math.min(1, args.optDouble(2, 1.0)))
    if (isServer && getLevel != null) getLevel.levelEvent(1010, getBlockPos, lastNote)
    signal("note", lastNote, lastInstrument, lastVolume)
    result(true)
  }

  override protected def loadComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    lastNote = nbt.getInt(Settings.namespace + "note")
    lastInstrument = nbt.getInt(Settings.namespace + "instrument")
    lastVolume = nbt.getDouble(Settings.namespace + "volume")
  }

  override protected def saveComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    nbt.putInt(Settings.namespace + "note", lastNote)
    nbt.putInt(Settings.namespace + "instrument", lastInstrument)
    nbt.putDouble(Settings.namespace + "volume", lastVolume)
  }
}

class ComputronicsAudioCable(pos: BlockPos, state: BlockState)
  extends ComputronicsBlockEntity(pos, state, Constants.BlockName.ComputronicsAudioCable, "audio_cable") {
  private var activeConnections = 0

  @Callback(direct = true)
  def getConnections(context: Context, args: Arguments): Array[AnyRef] = result(activeConnections)

  def setConnections(value: Int): Unit = {
    activeConnections = math.max(0, value)
    setChanged()
  }

  override protected def loadComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit =
    activeConnections = nbt.getInt(Settings.namespace + "audioConnections")

  override protected def saveComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit =
    nbt.putInt(Settings.namespace + "audioConnections", activeConnections)
}

class ComputronicsCamera(pos: BlockPos, state: BlockState)
  extends ComputronicsBlockEntity(pos, state, Constants.BlockName.ComputronicsCamera, "camera", connectorBuffer = 1000)
    with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Multimedia, "Rangefinder", "Siekierka Innovations", "Simple Spatiometer 1")
  private var lastDistance = 0.0

  @Callback(direct = true)
  def distance(context: Context, args: Arguments): Array[AnyRef] = {
    lastDistance = ComputronicsNativeSupport.ray(this, args.optDouble(0, 0), args.optDouble(1, 0))
    result(lastDistance)
  }

  override protected def loadComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit =
    lastDistance = nbt.getDouble(Settings.namespace + "cameraDistance")

  override protected def saveComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit =
    nbt.putDouble(Settings.namespace + "cameraDistance", lastDistance)
}

class ComputronicsChatBox(pos: BlockPos, state: BlockState)
  extends ComputronicsBlockEntity(pos, state, Constants.BlockName.ComputronicsChatBox, "chatbox", connectorBuffer = 500)
    with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Multimedia, "Chat interface", "National Security Agency", "[CLASSIFIED]")
  private var distanceValue = 16
  private var nameValue = "Computronics"

  @Callback
  def say(context: Context, args: Arguments): Array[AnyRef] = {
    val message = args.checkString(0)
    val prefix = if (nameValue.isEmpty) "Computronics" else nameValue
    if (getLevel != null && getLevel.getServer != null) {
      val text = net.minecraft.network.chat.Component.literal("[" + prefix + "] " + message)
      getLevel.getServer.getPlayerList.getPlayers.asScala.filter(player => player.level == getLevel &&
        player.distanceToSqr(x + 0.5, y + 0.5, z + 0.5) <= distanceValue * distanceValue).foreach(_.sendSystemMessage(text))
    }
    signal("chat", prefix, message, distanceValue)
    result(true)
  }

  @Callback(direct = true)
  def getDistance(context: Context, args: Arguments): Array[AnyRef] = result(distanceValue)

  @Callback
  def setDistance(context: Context, args: Arguments): Array[AnyRef] = {
    distanceValue = math.max(1, math.min(64, args.checkInteger(0)))
    result(distanceValue)
  }

  @Callback(direct = true)
  def getName(context: Context, args: Arguments): Array[AnyRef] = result(nameValue)

  @Callback
  def setName(context: Context, args: Arguments): Array[AnyRef] = {
    nameValue = args.checkString(0).take(64)
    result(nameValue)
  }

  override protected def loadComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    distanceValue = nbt.getInt(Settings.namespace + "chatDistance")
    nameValue = nbt.getString(Settings.namespace + "chatName")
  }

  override protected def saveComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    nbt.putInt(Settings.namespace + "chatDistance", distanceValue)
    nbt.putString(Settings.namespace + "chatName", nameValue)
  }
}

class ComputronicsCipher(
  pos: BlockPos,
  state: BlockState,
  kind: String = "cipher",
  component: String = "cipher"
) extends ComputronicsBlockEntity(pos, state, kind, component, connectorBuffer = 2000)
    with traits.Inventory with ComputronicsDeviceInfoEnvironment {
  override protected def computronicsDeviceInfo = if (kind == "cipher_advanced")
    ComputronicsDeviceInfo(DeviceClass.Processor, "Data encryption device", "Siekierka Innovations", "Cryptotron 6-X")
  else
    ComputronicsDeviceInfo(DeviceClass.Processor, "Data encryption device", "Siekierka Innovations", "Cryptotron 5-X")
  override val items: Array[ItemStack] = Array.fill(6)(ItemStack.EMPTY)

  override def getContainerSize: Int = 6

  private var locked = false
  private var secret = Array.emptyByteArray

  private def keyBytes: Array[Byte] = {
    if (secret.isEmpty) {
      val digest = MessageDigest.getInstance("SHA-256")
      val ingredients = items.map(stack => if (stack.isEmpty) "" else stack.getItem.toString + ":" + stack.getCount).mkString("|")
      secret = digest.digest((kind + ":" + x + ":" + y + ":" + z + ":" + ingredients).getBytes(StandardCharsets.UTF_8)).take(16)
    }
    secret
  }

  override def setItem(slot: Int, stack: ItemStack): Unit = if (!locked) {
    super.setItem(slot, stack)
    secret = Array.emptyByteArray
    setChanged()
  }

  override def getItem(slot: Int): ItemStack = if (locked) ItemStack.EMPTY else super.getItem(slot)

  private def crypt(mode: Int, value: Array[Byte]): String = {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val key = new SecretKeySpec(keyBytes, "AES")
    val iv = new IvParameterSpec(keyBytes)
    cipher.init(mode, key, iv)
    Base64.getEncoder.encodeToString(cipher.doFinal(value))
  }

  private def decrypt(value: String): Array[Byte] = {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val key = new SecretKeySpec(keyBytes, "AES")
    val iv = new IvParameterSpec(keyBytes)
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    cipher.doFinal(Base64.getDecoder.decode(value))
  }

  @Callback(direct = true)
  def encrypt(context: Context, args: Arguments): Array[AnyRef] = {
    val data = if (args.isByteArray(0)) args.checkByteArray(0) else args.checkString(0).getBytes(StandardCharsets.UTF_8)
    result(crypt(Cipher.ENCRYPT_MODE, data))
  }

  @Callback(direct = true)
  def decrypt(context: Context, args: Arguments): Array[AnyRef] =
    result(new String(decrypt(args.checkString(0)), StandardCharsets.UTF_8))

  @Callback(direct = true)
  def isLocked(context: Context, args: Arguments): Array[AnyRef] = result(locked)

  @Callback
  def setLocked(context: Context, args: Arguments): Array[AnyRef] = {
    locked = args.checkBoolean(0)
    setChanged()
    result(locked)
  }

  override protected def loadComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    locked = nbt.getBoolean(Settings.namespace + "cipherLocked")
    secret = nbt.getByteArray(Settings.namespace + "cipherSecret")
  }

  override protected def saveComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    nbt.putBoolean(Settings.namespace + "cipherLocked", locked)
    nbt.putByteArray(Settings.namespace + "cipherSecret", secret)
  }
}

/** RSA key-table variant used by Computronics' advanced cipher block. */
class ComputronicsAdvancedCipher(pos: BlockPos, state: BlockState)
  extends ComputronicsCipher(pos, state, "cipher_advanced", "cipher_advanced") {
  private val base64 = Base64.getEncoder

  private def unsigned(bytes: Array[Byte]): BigInteger = new BigInteger(1, bytes)
  private def encoded(value: BigInteger): String = base64.encodeToString(value.toByteArray)
  private def table(args: Arguments, index: Int): Map[Any, Any] = args.checkTable(index).asScala.toMap
  private def number(key: Map[Any, Any], index: Int): BigInteger = key.get(index).orElse(key.get(index.toDouble)) match {
    case Some(value: String) => unsigned(Base64.getDecoder.decode(value))
    case _ => throw new IllegalArgumentException("invalid RSA key")
  }

  private def keySet(p: BigInteger, q: BigInteger): Array[AnyRef] = {
    val n = p.multiply(q)
    val phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE))
    val e = if (phi.gcd(BigInteger.valueOf(65537)).equals(BigInteger.ONE)) BigInteger.valueOf(65537) else BigInteger.valueOf(3)
    val d = e.modInverse(phi)
    result(Map(1 -> encoded(n), 2 -> encoded(e), 3 -> "prime"),
      Map(1 -> encoded(n), 2 -> encoded(d), 3 -> "prime"))
  }

  @Callback(direct = true)
  def createKeySet(context: Context, args: Arguments): Array[AnyRef] = {
    def prime(value: Int): BigInteger = {
      val result = BigInteger.valueOf(value)
      if (!result.isProbablePrime(20)) throw new IllegalArgumentException("prime expected")
      result
    }
    val p = prime(args.checkInteger(0))
    val q = prime(args.checkInteger(1))
    keySet(p, q)
  }

  @Callback(direct = true)
  def createRandomKeySet(context: Context, args: Arguments): Array[AnyRef] = {
    val bits = math.max(16, math.min(1024, args.optInteger(0, 256)))
    val p = BigInteger.probablePrime(bits / 2, new java.util.Random())
    val q = BigInteger.probablePrime(bits / 2, new java.util.Random())
    keySet(p, q)
  }

  @Callback(direct = true)
  override def encrypt(context: Context, args: Arguments): Array[AnyRef] = {
    val message = args.checkByteArray(0)
    val key = table(args, 1)
    val value = unsigned(message).modPow(number(key, 2), number(key, 1))
    result(encoded(value))
  }

  @Callback(direct = true)
  override def decrypt(context: Context, args: Arguments): Array[AnyRef] = {
    val message = unsigned(Base64.getDecoder.decode(args.checkString(0)))
    val key = table(args, 1)
    result(new String(message.modPow(number(key, 2), number(key, 1)).toByteArray, StandardCharsets.UTF_8))
  }
}

class ComputronicsRadar(pos: BlockPos, state: BlockState)
  extends ComputronicsBlockEntity(pos, state, Constants.BlockName.ComputronicsRadar, "radar", connectorBuffer = 4000)
    with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Multimedia, "Radar", "Trumbour Technology", "Detectotron M1")
  private def records(range: Double, filter: Entity => Boolean): Seq[Map[String, Any]] = {
    val distance = math.max(1.0, math.min(32.0, range))
    val bounds = new AABB(x + 0.5 - distance, y + 0.5 - distance, z + 0.5 - distance,
      x + 0.5 + distance, y + 0.5 + distance, z + 0.5 + distance)
    getLevel.getEntitiesOfClass(classOf[Entity], bounds).asScala.filter(entity => entity != this && filter(entity)).map(entity => Map(
      "name" -> entity.getName.getString,
      "type" -> entity.getType.toString,
      "x" -> (entity.getX - x - 0.5),
      "y" -> (entity.getY - y - 0.5),
      "z" -> (entity.getZ - z - 0.5),
      "distance" -> math.sqrt(entity.distanceToSqr(x + 0.5, y + 0.5, z + 0.5))
    )).toSeq
  }

  @Callback
  def getEntities(context: Context, args: Arguments): Array[AnyRef] = result(records(args.optDouble(0, 16), _ => true))

  @Callback
  def getPlayers(context: Context, args: Arguments): Array[AnyRef] = result(records(args.optDouble(0, 16), _.isInstanceOf[Player]))

  @Callback
  def getMobs(context: Context, args: Arguments): Array[AnyRef] = result(records(args.optDouble(0, 16), _.isInstanceOf[LivingEntity]))

  @Callback
  def getItems(context: Context, args: Arguments): Array[AnyRef] = result(records(args.optDouble(0, 16), _.isInstanceOf[ItemEntity]))
}

class ComputronicsColorfulLamp(pos: BlockPos, state: BlockState)
  extends ComputronicsBlockEntity(pos, state, Constants.BlockName.ComputronicsColorfulLamp, "colorful_lamp")
    with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Display, "Colored Lamp", "Lumiose Lighting", "LED-4")
  private var lampColor = 0x6318
  private var binaryMode = false

  @Callback(direct = true)
  def getLampColor(context: Context, args: Arguments): Array[AnyRef] = result(lampColor)

  @Callback
  def setLampColor(context: Context, args: Arguments): Array[AnyRef] = {
    val value = args.checkInteger(0)
    if (value < 0 || value > 0xFFFF) result(false, "color must be between 0 and 65535")
    else {
      lampColor = value
      setColor(value)
      if (getLevel != null) getLevel.sendBlockUpdated(getBlockPos, getBlockState, getBlockState, 3)
      result(true)
    }
  }

  @Callback(direct = true)
  def isBinaryMode(context: Context, args: Arguments): Array[AnyRef] = result(binaryMode)

  @Callback
  def setBinaryMode(context: Context, args: Arguments): Array[AnyRef] = {
    binaryMode = args.checkBoolean(0)
    setChanged()
    result(binaryMode)
  }

  override protected def loadComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    lampColor = nbt.getInt(Settings.namespace + "lampColor")
    binaryMode = nbt.getBoolean(Settings.namespace + "lampBinary")
  }

  override protected def saveComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    nbt.putInt(Settings.namespace + "lampColor", lampColor)
    nbt.putBoolean(Settings.namespace + "lampBinary", binaryMode)
  }
}

class ComputronicsSpeaker(pos: BlockPos, state: BlockState)
  extends ComputronicsBlockEntity(pos, state, Constants.BlockName.ComputronicsSpeaker, "speaker", connectorBuffer = 2000)
    with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo: java.util.Map[String, String] = null
  private var playing = false
  private var volume = 1.0

  @Callback
  def play(context: Context, args: Arguments): Array[AnyRef] = {
    val frequency = math.max(20, math.min(2000, args.optInteger(0, 440)))
    val duration = math.max(50, math.min(5000, (args.optDouble(1, 0.2) * 1000).toInt))
    playing = true
    if (isServer && getLevel != null) {
      li.cil.oc.server.PacketSender.sendComputronicsTone(getLevel, x + 0.5, y + 0.5, z + 0.5, 0, frequency, duration, 0, volume)
    }
    signal("audio", "play")
    result(true)
  }

  @Callback
  def stop(context: Context, args: Arguments): Array[AnyRef] = {
    playing = false
    signal("audio", "stop")
    result(true)
  }

  @Callback(direct = true)
  def isPlaying(context: Context, args: Arguments): Array[AnyRef] = result(playing)

  @Callback
  def setVolume(context: Context, args: Arguments): Array[AnyRef] = {
    volume = math.max(0, math.min(1, args.checkDouble(0)))
    result(volume)
  }

  override protected def loadComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    playing = nbt.getBoolean(Settings.namespace + "speakerPlaying")
    volume = nbt.getDouble(Settings.namespace + "speakerVolume")
  }

  override protected def saveComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    nbt.putBoolean(Settings.namespace + "speakerPlaying", playing)
    nbt.putDouble(Settings.namespace + "speakerVolume", volume)
  }
}

class ComputronicsSpeechBox(pos: BlockPos, state: BlockState)
  extends ComputronicsBlockEntity(pos, state, Constants.BlockName.ComputronicsSpeechBox, "speech_box", connectorBuffer = 2000)
    with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Multimedia, "Text-To-Speech Interface", "DFKI GmbH", "Mary")
  private var processing = false
  private var volume = 1.0

  @Callback
  def say(context: Context, args: Arguments): Array[AnyRef] = {
    val text = args.checkString(0)
    processing = text.nonEmpty
    signal("speech", text)
    result(true)
  }

  @Callback
  def stop(context: Context, args: Arguments): Array[AnyRef] = {
    processing = false
    result(true)
  }

  @Callback(direct = true)
  def isProcessing(context: Context, args: Arguments): Array[AnyRef] = result(processing)

  @Callback
  def setVolume(context: Context, args: Arguments): Array[AnyRef] = {
    volume = math.max(0, math.min(1, args.checkDouble(0)))
    result(volume)
  }

  override protected def loadComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    processing = nbt.getBoolean(Settings.namespace + "speechProcessing")
    volume = nbt.getDouble(Settings.namespace + "speechVolume")
  }

  override protected def saveComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    nbt.putBoolean(Settings.namespace + "speechProcessing", processing)
    nbt.putDouble(Settings.namespace + "speechVolume", volume)
  }
}

class ComputronicsTapeDrive(pos: BlockPos, state: BlockState)
  extends ComputronicsBlockEntity(pos, state, Constants.BlockName.ComputronicsTapeReader, "tape_reader", connectorBuffer = 2000)
    with traits.Inventory with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Tape, "Tape drive", "Yanaki Sound Systems", "DFPWM 1")
  override val items: Array[ItemStack] = Array(ItemStack.EMPTY)
  private var tape = Array.emptyByteArray
  private var cursor = 0
  private var label = ""
  private var playing = false

  override def getContainerSize: Int = 1

  private val TapeData = Settings.namespace + "computronicsTape"

  override def setItem(slot: Int, stack: ItemStack): Unit = {
    super.setItem(slot, stack)
    if (slot == 0) {
      val data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
      tape = data.getByteArray(TapeData)
      cursor = math.max(0, math.min(tape.length, data.getInt(Settings.namespace + "tapePosition")))
      label = data.getString(Settings.namespace + "tapeLabel")
      playing = false
    }
  }

  private def saveTapeToItem(): Unit = if (!items(0).isEmpty) {
    val data = items(0).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
    data.putByteArray(TapeData, tape)
    data.putInt(Settings.namespace + "tapePosition", cursor)
    data.putString(Settings.namespace + "tapeLabel", label)
    items(0).set(DataComponents.CUSTOM_DATA, CustomData.of(data))
  }

  @Callback(direct = true)
  def isEnd(context: Context, args: Arguments): Array[AnyRef] = result(cursor >= tape.length)

  @Callback(direct = true)
  def isReady(context: Context, args: Arguments): Array[AnyRef] = result(!items(0).isEmpty)

  @Callback(direct = true)
  def getSize(context: Context, args: Arguments): Array[AnyRef] = result(tape.length)

  @Callback(direct = true)
  def getPosition(context: Context, args: Arguments): Array[AnyRef] = result(cursor)

  @Callback
  def seek(context: Context, args: Arguments): Array[AnyRef] = {
    cursor = math.max(0, math.min(tape.length, args.checkInteger(0)))
    result(cursor)
  }

  @Callback
  def read(context: Context, args: Arguments): Array[AnyRef] = {
    val count = math.max(0, math.min(4096, args.optInteger(0, 1)))
    val output = tape.slice(cursor, math.min(tape.length, cursor + count))
    cursor += output.length
    result(output)
  }

  @Callback
  def write(context: Context, args: Arguments): Array[AnyRef] = {
    val input = args.checkByteArray(0)
    val end = cursor + input.length
    if (end > tape.length) tape = java.util.Arrays.copyOf(tape, end)
    System.arraycopy(input, 0, tape, cursor, input.length)
    cursor = end
    saveTapeToItem()
    setChanged()
    result(input.length)
  }

  @Callback
  def setLabel(context: Context, args: Arguments): Array[AnyRef] = {
    label = args.checkString(0).take(64)
    saveTapeToItem()
    setChanged()
    result(label)
  }

  @Callback(direct = true)
  def getLabel(context: Context, args: Arguments): Array[AnyRef] = result(label)

  @Callback
  def play(context: Context, args: Arguments): Array[AnyRef] = {
    playing = true
    signal("tape", "play")
    result(true)
  }

  @Callback
  def stop(context: Context, args: Arguments): Array[AnyRef] = {
    playing = false
    signal("tape", "stop")
    result(true)
  }

  @Callback(direct = true)
  def getState(context: Context, args: Arguments): Array[AnyRef] = result(Map(
    "ready" -> !items(0).isEmpty,
    "playing" -> playing,
    "position" -> cursor,
    "size" -> tape.length,
    "label" -> label
  ))

  override protected def loadComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    tape = nbt.getByteArray(Settings.namespace + "tapeData")
    cursor = nbt.getInt(Settings.namespace + "tapePosition")
    label = nbt.getString(Settings.namespace + "tapeLabel")
    playing = nbt.getBoolean(Settings.namespace + "tapePlaying")
  }

  override protected def saveComputronics(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    nbt.putByteArray(Settings.namespace + "tapeData", tape)
    nbt.putInt(Settings.namespace + "tapePosition", cursor)
    nbt.putString(Settings.namespace + "tapeLabel", label)
    nbt.putBoolean(Settings.namespace + "tapePlaying", playing)
  }
}
