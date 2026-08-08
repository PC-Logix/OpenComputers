package li.cil.oc.server.component

import li.cil.oc.{Constants, api}
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.{EnvironmentHost, Visibility}
import li.cil.oc.api.prefab.{AbstractManagedEnvironment, ComponentConnectableRackMountableEnvironment}
import li.cil.oc.server.PacketSender
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.core.particles.{ParticleOptions, ParticleTypes}
import net.minecraft.world.entity.{Entity, LivingEntity}
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.{AABB, Vec3}
import net.minecraft.world.phys.shapes.CollisionContext

import scala.jdk.CollectionConverters._

private[oc] object ComputronicsNativeSupport {
  def direction(host: EnvironmentHost): Direction = host match {
    case rotatable: api.internal.Rotatable => rotatable.facing
    case _ => Direction.SOUTH
  }

  def ray(host: EnvironmentHost, xOffset: Double, yOffset: Double, vertical: Int = 0): Double = {
    val level = host.getEnvironmentLevel
    if (level == null) return -1
    val origin = new Vec3(host.xPosition, host.yPosition, host.zPosition)
    val facing = if (vertical == 0) direction(host) else if (vertical > 0) Direction.UP else Direction.DOWN
    val base = new Vec3(facing.getStepX, facing.getStepY, facing.getStepZ)
    val vector = new Vec3(base.x + xOffset, base.y + yOffset, base.z).normalize()
    val hit = level.clip(new ClipContext(origin, origin.add(vector.scale(64)), ClipContext.Block.COLLIDER,
      ClipContext.Fluid.NONE, null.asInstanceOf[CollisionContext]))
    if (hit.getType == net.minecraft.world.phys.HitResult.Type.MISS) -1 else origin.distanceTo(hit.getLocation)
  }

  def records(host: EnvironmentHost, range: Double, filter: Entity => Boolean): Seq[Map[String, Any]] = {
    val level = host.getEnvironmentLevel
    if (level == null) return Seq.empty
    val distance = math.max(1.0, math.min(32.0, range))
    val bounds = new AABB(host.xPosition - distance, host.yPosition - distance, host.zPosition - distance,
      host.xPosition + distance, host.yPosition + distance, host.zPosition + distance)
    level.getEntitiesOfClass(classOf[Entity], bounds).asScala.filter(entity => filter(entity)).map(entity => Map(
      "name" -> entity.getName.getString,
      "type" -> entity.getType.toString,
      "x" -> (entity.getX - host.xPosition),
      "y" -> (entity.getY - host.yPosition),
      "z" -> (entity.getZ - host.zPosition),
      "distance" -> math.sqrt(entity.distanceToSqr(host.xPosition, host.yPosition, host.zPosition))
    )).toSeq
  }

  def particle(name: String): ParticleOptions = name.toLowerCase match {
    case "flame" => ParticleTypes.FLAME
    case "smoke" | "smoke_normal" => ParticleTypes.SMOKE
    case "cloud" => ParticleTypes.CLOUD
    case "heart" => ParticleTypes.HEART
    case "crit" => ParticleTypes.CRIT
    case "happy_villager" => ParticleTypes.HAPPY_VILLAGER
    case "portal" => ParticleTypes.PORTAL
    case "end_rod" => ParticleTypes.END_ROD
    case "electric_spark" => ParticleTypes.ELECTRIC_SPARK
    case _ => null
  }
}

private[oc] object ComputronicsDeviceInfo {
  def apply(deviceClass: String, description: String, vendor: String, product: String): java.util.Map[String, String] =
    Map[String, String](
      DeviceAttribute.Class -> deviceClass,
      DeviceAttribute.Description -> description,
      DeviceAttribute.Vendor -> vendor,
      DeviceAttribute.Product -> product).asJava
}

private[oc] trait ComputronicsDeviceInfoEnvironment extends DeviceInfo {
  protected def computronicsDeviceInfo: java.util.Map[String, String]
  override def getDeviceInfo: java.util.Map[String, String] = computronicsDeviceInfo
}

class ComputronicsCameraUpgrade(val host: EnvironmentHost) extends AbstractManagedEnvironment with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Multimedia, "Rangefinder", "Siekierka Innovations", "Compact Spatiometer 1-C")
  override val node = api.Network.newNode(this, Visibility.Network).withConnector().withComponent("camera").create()

  @Callback(direct = true)
  def distance(context: Context, args: Arguments): Array[AnyRef] = result(ComputronicsNativeSupport.ray(host, args.optDouble(0, 0), args.optDouble(1, 0)))

  @Callback(direct = true)
  def distanceUp(context: Context, args: Arguments): Array[AnyRef] = result(ComputronicsNativeSupport.ray(host, args.optDouble(0, 0), args.optDouble(1, 0), 1))

  @Callback(direct = true)
  def distanceDown(context: Context, args: Arguments): Array[AnyRef] = result(ComputronicsNativeSupport.ray(host, args.optDouble(0, 0), args.optDouble(1, 0), -1))
}

class ComputronicsChatUpgrade(val host: EnvironmentHost) extends AbstractManagedEnvironment with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Multimedia, "Chat interface", "National Security Agency", "[CLASSIFIED]")
  override val node = api.Network.newNode(this, Visibility.Network).withConnector().withComponent("chat", Visibility.Neighbors).create()
  private var distanceValue = 16
  private var nameValue = "Computronics"

  @Callback(direct = true) def getDistance(context: Context, args: Arguments): Array[AnyRef] = result(distanceValue)
  @Callback def setDistance(context: Context, args: Arguments): Array[AnyRef] = { distanceValue = math.max(1, math.min(64, args.checkInteger(0))); result(distanceValue) }
  @Callback(direct = true) def getName(context: Context, args: Arguments): Array[AnyRef] = result(nameValue)
  @Callback def setName(context: Context, args: Arguments): Array[AnyRef] = { nameValue = args.checkString(0).take(64); result(nameValue) }

  @Callback
  def say(context: Context, args: Arguments): Array[AnyRef] = {
    val message = args.checkString(0)
    val range = math.max(1, math.min(64, args.optInteger(1, distanceValue)))
    val prefix = if (nameValue.isEmpty) "Computronics" else nameValue
    val level = host.getEnvironmentLevel
    if (level != null && level.getServer != null) {
      val text = net.minecraft.network.chat.Component.literal("[" + prefix + "] " + message)
      level.getServer.getPlayerList.getPlayers.asScala.filter(player => player.level == level &&
        player.distanceToSqr(host.xPosition, host.yPosition, host.zPosition) <= range * range).foreach(_.sendSystemMessage(text))
    }
    result(true)
  }
}

class ComputronicsRadarUpgrade(val host: EnvironmentHost) extends AbstractManagedEnvironment with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Multimedia, "Radar", "Trumbour Technology", "Detectotron M1 Mini")
  override val node = api.Network.newNode(this, Visibility.Network).withConnector(4000).withComponent("radar").create()
  private def scan(range: Double, filter: Entity => Boolean): Array[AnyRef] = result(ComputronicsNativeSupport.records(host, range, filter))
  @Callback def getEntities(context: Context, args: Arguments): Array[AnyRef] = scan(args.optDouble(0, 16), _ => true)
  @Callback def getPlayers(context: Context, args: Arguments): Array[AnyRef] = scan(args.optDouble(0, 16), _.isInstanceOf[Player])
  @Callback def getMobs(context: Context, args: Arguments): Array[AnyRef] = scan(args.optDouble(0, 16), _.isInstanceOf[LivingEntity])
  @Callback def getItems(context: Context, args: Arguments): Array[AnyRef] = scan(args.optDouble(0, 16), _.isInstanceOf[ItemEntity])
}

class ComputronicsParticleCard(val host: EnvironmentHost) extends AbstractManagedEnvironment with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Generic, "Particle emitter", "Siekierka Innovations", "Holotron FX-84")
  override val node = api.Network.newNode(this, Visibility.Neighbors).withConnector().withComponent("particle").create()
  @Callback(direct = true)
  def spawn(context: Context, args: Arguments): Array[AnyRef] = {
    val particle = ComputronicsNativeSupport.particle(args.checkString(0))
    if (particle == null) return result(false, "unknown particle")
    val x = host.xPosition + args.checkDouble(1)
    val y = host.yPosition + args.checkDouble(2)
    val z = host.zPosition + args.checkDouble(3)
    val velocity = args.optDouble(4, 0)
    val level = host.getEnvironmentLevel
    if (level != null && !level.isClientSide) PacketSender.sendParticleEffect(li.cil.oc.util.BlockPosition(x.toInt, y.toInt, z.toInt, level), particle, 1, velocity)
    result(true)
  }
}

class ComputronicsSpoofingCard(val host: EnvironmentHost) extends AbstractManagedEnvironment with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Network, "Ethernet contorter", "Hosencorp AG", "42i520 (MPN-01) - Br1ck")
  override val node = api.Network.newNode(this, Visibility.Neighbors).withConnector().withComponent("modem", Visibility.Neighbors).create()
  private val ports = scala.collection.mutable.Set.empty[Int]
  private def port(value: Int): Int = if (value < 0 || value > 65535) throw new IllegalArgumentException("invalid port") else value
  @Callback def open(context: Context, args: Arguments): Array[AnyRef] = result(ports.add(port(args.checkInteger(0))))
  @Callback def close(context: Context, args: Arguments): Array[AnyRef] = if (args.count == 0) { val changed = ports.nonEmpty; ports.clear(); result(changed) } else result(ports.remove(port(args.checkInteger(0))))
  @Callback(direct = true) def isOpen(context: Context, args: Arguments): Array[AnyRef] = result(ports.contains(port(args.checkInteger(0))))
  @Callback def broadcast(context: Context, args: Arguments): Array[AnyRef] = {
    val packet = api.Network.newPacket(node.address, null, port(args.checkInteger(0)), args.iterator.asScala.drop(1).toArray)
    node.sendToNeighbors("network.message", packet)
    result(true)
  }
  @Callback def send(context: Context, args: Arguments): Array[AnyRef] = {
    val packet = api.Network.newPacket(args.checkString(0), args.checkString(1), port(args.checkInteger(2)), args.iterator.asScala.drop(3).toArray)
    node.sendToNeighbors("network.message", packet)
    result(true)
  }
}

class ComputronicsSelfDestructCard(val host: EnvironmentHost) extends AbstractManagedEnvironment with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Generic, "Machine destruction service", "Hugging Creeper Industries", "SD-Struct 1")
  override val node = api.Network.newNode(this, Visibility.Neighbors).withComponent("self_destruct").create()
  private var ticks = -1
  @Callback def start(context: Context, args: Arguments): Array[AnyRef] = {
    if (ticks >= 0) return result(-1, "fuse has already been set")
    val seconds = math.max(0, math.min(100000, args.optDouble(0, 5)))
    ticks = math.round(seconds * 20).toInt
    result(seconds)
  }
  @Callback(direct = true) def time(context: Context, args: Arguments): Array[AnyRef] = if (ticks < 0) result(-1, "fuse has not been set") else result(ticks / 20.0)
  override def canUpdate = true
  override def update(): Unit = if (ticks >= 0) {
    if (ticks == 0) {
      host.getEnvironmentLevel.getBlockEntity(new BlockPos(host.xPosition.toInt, host.yPosition.toInt, host.zPosition.toInt)) match {
        case _: net.minecraft.world.level.block.entity.BlockEntity => host.getEnvironmentLevel.destroyBlock(new BlockPos(host.xPosition.toInt, host.yPosition.toInt, host.zPosition.toInt), true)
        case _ =>
      }
      ticks = -1
    } else ticks -= 1
  }
}

class ComputronicsColorfulUpgrade(val host: EnvironmentHost) extends AbstractManagedEnvironment with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Display, "Color overlay", "Lumiose Lighting", "Holonaut H4-1463 v2")
  override val node = api.Network.newNode(this, Visibility.Network).withConnector().withComponent("colors", Visibility.Neighbors).create()
  private var color = -1
  @Callback(direct = true) def getColor(context: Context, args: Arguments): Array[AnyRef] = result(color)
  @Callback def setColor(context: Context, args: Arguments): Array[AnyRef] = { val value = args.checkInteger(0); if (value < 0 || value > 0xFFFFFF) result(false, "color must be between 0 and 16777215") else { color = value; result(true) } }
  @Callback def resetColor(context: Context, args: Arguments): Array[AnyRef] = { color = -1; result(true) }
}

class ComputronicsSpeechUpgrade(val host: EnvironmentHost) extends AbstractManagedEnvironment with ComputronicsDeviceInfoEnvironment {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Multimedia, "Text-To-Speech Interface", "DFKI GmbH", "Mary")
  override val node = api.Network.newNode(this, Visibility.Network).withConnector().withComponent("speech").create()
  private var processing = false
  @Callback def say(context: Context, args: Arguments): Array[AnyRef] = { val text = args.checkString(0); processing = text.nonEmpty; node.sendToReachable("computer.signal", "speech", text); result(true) }
  @Callback def stop(context: Context, args: Arguments): Array[AnyRef] = { processing = false; result(true) }
  @Callback(direct = true) def isProcessing(context: Context, args: Arguments): Array[AnyRef] = result(processing)
  @Callback def setVolume(context: Context, args: Arguments): Array[AnyRef] = result(math.max(0, math.min(1, args.checkDouble(0))))
}

class ComputronicsMagicalMemory extends Memory(0) {
  override def getDeviceInfo: java.util.Map[String, String] =
    ComputronicsDeviceInfo(DeviceClass.Memory, "Memory vortex", "ACME Co.", "Mnemomagic 47")
}

class ComputronicsLightBoard(val rack: api.internal.Rack) extends ComponentConnectableRackMountableEnvironment with ComputronicsDeviceInfoEnvironment {
  setNode(api.Network.newNode(this, Visibility.Network).withConnector().withComponent("light_board", Visibility.Network).create())
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Display, "Light board", "Lumiose Lighting", "LED-15 X")
  private val active = Array.fill(4)(false)
  private val colors = Array.fill(4)(0xC0C0C0)
  private def index(value: Int): Int = if (value < 1 || value > 4) throw new IllegalArgumentException("index out of range") else value - 1
  @Callback(direct = true) def light_count(context: Context, args: Arguments): Array[AnyRef] = result(4)
  @Callback(direct = true) def getColor(context: Context, args: Arguments): Array[AnyRef] = result(colors(index(args.checkInteger(0))))
  @Callback def setColor(context: Context, args: Arguments): Array[AnyRef] = { val i = index(args.checkInteger(0)); val c = args.checkInteger(1); if (c < 0 || c > 0xFFFFFF) result(false, "invalid color") else { colors(i) = c; result(true) } }
  @Callback(direct = true) def isActive(context: Context, args: Arguments): Array[AnyRef] = result(active(index(args.checkInteger(0))))
  @Callback def setActive(context: Context, args: Arguments): Array[AnyRef] = { val i = index(args.checkInteger(0)); active(i) = args.checkBoolean(1); result(true) }
}

class ComputronicsSwitchBoard(val rack: api.internal.Rack) extends ComponentConnectableRackMountableEnvironment with ComputronicsDeviceInfoEnvironment {
  setNode(api.Network.newNode(this, Visibility.Network).withConnector().withComponent("switch_board", Visibility.Network).create())
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Input, "Switch board", "Soluna Technologies", "Clickety-Clack Q3")
  private val active = Array.fill(4)(false)
  private def index(value: Int): Int = if (value < 1 || value > 4) throw new IllegalArgumentException("index out of range") else value - 1
  @Callback(direct = true) def isActive(context: Context, args: Arguments): Array[AnyRef] = result(active(index(args.checkInteger(0))))
  @Callback def setActive(context: Context, args: Arguments): Array[AnyRef] = { val i = index(args.checkInteger(0)); val value = args.checkBoolean(1); val changed = active(i) != value; active(i) = value; if (changed) node.sendToReachable("computer.signal", "switch_flipped", i + 1, value); result(changed) }
}

class ComputronicsRackCapacitor(val rack: api.internal.Rack) extends ComponentConnectableRackMountableEnvironment with ComputronicsDeviceInfoEnvironment {
  setNode(api.Network.newNode(this, Visibility.Network).withConnector(100000).withComponent("rack_capacitor", Visibility.Network).create())
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Power, "Battery", "Soluna Technologies", "CapCube 64 (Rev. 2)")
  @Callback(direct = true) def energy(context: Context, args: Arguments): Array[AnyRef] = result(node.localBuffer())
  @Callback(direct = true) def maxEnergy(context: Context, args: Arguments): Array[AnyRef] = result(node.localBufferSize())
}

class ComputronicsRackBoomBoard(val rack: api.internal.Rack) extends ComponentConnectableRackMountableEnvironment with ComputronicsDeviceInfoEnvironment {
  setNode(api.Network.newNode(this, Visibility.Network).withComponent("self_destruct", Visibility.Network).create())
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Generic, "Server-cleaning service", "Hugging Creeper Industries", "SSD-Struct M4")
  private var ticks = -1
  @Callback def start(context: Context, args: Arguments): Array[AnyRef] = {
    if (ticks >= 0) return result(-1, "fuse has already been set")
    val seconds = math.max(0, math.min(100000, args.optDouble(0, 5)))
    ticks = math.round(seconds * 20).toInt
    result(seconds)
  }
  @Callback(direct = true) def time(context: Context, args: Arguments): Array[AnyRef] = if (ticks < 0) result(-1, "fuse has not been set") else result(ticks / 20.0)
  override def canUpdate = true
  override def update(): Unit = if (ticks >= 0) {
    if (ticks == 0) { node.sendToReachable("computer.signal", "self_destruct"); ticks = -1 }
    else ticks -= 1
  }
}
