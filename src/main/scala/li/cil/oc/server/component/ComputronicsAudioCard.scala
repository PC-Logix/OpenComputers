package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.{EnvironmentHost, Visibility}
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.server.PacketSender
import net.minecraft.world.level.Level

import scala.collection.mutable
import scala.jdk.CollectionConverters._

/** Native OC implementations of Computronics' beep, noise, and sound cards. */
abstract class ComputronicsAudioCard(val host: EnvironmentHost, componentName: String)
  extends AbstractManagedEnvironment with ComputronicsDeviceInfoEnvironment {
  override val node = api.Network.newNode(this, Visibility.Neighbors).
    withComponent(componentName).
    withConnector().
    create()

  protected val channels = Array.fill(8)(new ChannelState)
  protected val waveModes = Array("square", "sine", "triangle", "sawtooth", "noise")
  protected var outputVolume = 1.0

  protected def modeTable: Map[Any, Any] = {
    val table = mutable.Map.empty[Any, Any]
    waveModes.zipWithIndex.foreach { case (name, index) =>
      table(index + 1) = name
      table(name) = index + 1
    }
    table.toMap
  }

  protected def level: Level = host.getEnvironmentLevel

  protected def now: Long = if (level == null) 0L else level.getGameTime

  protected def activeChannels: Int = channels.count(_.until > now)

  protected def consume(cost: Double): Boolean = cost <= 0 || node.tryChangeBuffer(-cost)

  protected def clampFrequency(value: Double): Int = math.max(20, math.min(2000, value.toInt))

  protected def clampDuration(value: Double): Int = math.max(50, math.min(5000, (value * 1000).toInt))

  protected def play(frequency: Double, duration: Double, channel: Int = -1, delayMilliseconds: Int = 0): Boolean = {
    val hz = clampFrequency(frequency)
    val milliseconds = clampDuration(duration)
    if (!consume(milliseconds / 1000.0)) return false
    if (level != null && !level.isClientSide) {
      val state = if (channel >= 0 && channel < channels.length) channels(channel) else null
      val mode = if (state == null) 0 else state.mode
      val volume = if (state == null) outputVolume else outputVolume * state.volume
      val fmFrequency = if (state != null && state.fmChannel >= 0) channels(state.fmChannel).frequency else 0
      val amFrequency = if (state != null && state.amChannel >= 0) channels(state.amChannel).frequency else 0
      PacketSender.sendComputronicsTone(level, host.xPosition, host.yPosition, host.zPosition, mode, hz, milliseconds, delayMilliseconds, volume,
        fmFrequency, if (state == null) 0 else state.fmIntensity, amFrequency,
        if (state == null) 0 else state.attack, if (state == null) 0 else state.decay,
        if (state == null) 1 else state.sustain, if (state == null) 0 else state.release)
    }
    if (channel >= 0 && channel < channels.length) {
      channels(channel).until = now + math.max(1L, (milliseconds + 49) / 50)
      channels(channel).frequency = hz
    }
    true
  }

  protected def tableNumber(value: Any): Option[Double] = value match {
    case n: Number => Some(n.doubleValue())
    case _ => None
  }

  protected def tableValues(value: Any): Option[Map[Any, Any]] = value match {
    case map: java.util.Map[_, _] => Some(map.asScala.toMap)
    case map: scala.collection.Map[_, _] => Some(map.toMap)
    case _ => None
  }

  protected def channelIndex(value: Int): Int = {
    if (value < 1 || value > channels.length) throw new IllegalArgumentException("channel must be in [1, 8]")
    value - 1
  }

  @Callback(direct = true)
  def channel_count(context: Context, args: Arguments): Array[AnyRef] = result(channels.length)

  @Callback(direct = true)
  def getActiveChannels(context: Context, args: Arguments): Array[AnyRef] = result(activeChannels)

  @Callback(direct = true)
  def isReady(context: Context, args: Arguments): Array[AnyRef] = result(true)

  protected final class ChannelState {
    var until = 0L
    var frequency = 440
    var mode = 0
    var volume = 1.0
    var lfsrInitial = 0
    var lfsrMask = 0
    var fmChannel = -1
    var fmIntensity = 0.0
    var amChannel = -1
    var attack = 0
    var decay = 0
    var sustain = 1.0
    var release = 0
    val buffer = mutable.ArrayBuffer.empty[(Double, Double, Double)]
  }
}

class ComputronicsBeepCard(host: EnvironmentHost) extends ComputronicsAudioCard(host, "beep") {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Multimedia, "Audio interface", "Yanaki Sound Systems", "SQ532")
  @Callback(direct = true)
  def getBeepCount(context: Context, args: Arguments): Array[AnyRef] = result(activeChannels)

  @Callback(direct = true)
  def beep(context: Context, args: Arguments): Array[AnyRef] = {
    val entries = args.checkTable(0).asScala.toSeq.flatMap { case (key, value) =>
      for {
        frequency <- tableNumber(key)
        duration <- Option(value).flatMap(tableNumber).orElse(Some(0.1))
      } yield (frequency, duration)
    }
    if (entries.size > 8) return result(false, "table must not contain more than 8 frequencies")
    if (activeChannels + entries.size > 8) return result(false, "already too many sounds playing, maximum is 8")
    entries.foreach { case (frequency, duration) => play(frequency, duration) }
    result(true)
  }
}

class ComputronicsNoiseCard(host: EnvironmentHost) extends ComputronicsAudioCard(host, "noise") {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Multimedia, "Audio interface", "Yanaki Sound Systems", "SQ1289-4")
  @Callback(direct = true)
  def getMode(context: Context, args: Arguments): Array[AnyRef] = result(channels(channelIndex(args.checkInteger(0))).mode + 1)

  @Callback
  def setMode(context: Context, args: Arguments): Array[AnyRef] = {
    val channel = channelIndex(args.checkInteger(0))
    val mode = args.checkInteger(1) - 1
    if (mode < 0 || mode >= waveModes.length) throw new IllegalArgumentException("unknown wave mode")
    channels(channel).mode = mode
    result(true)
  }

  @Callback(direct = true)
  def modes(context: Context, args: Arguments): Array[AnyRef] = result(modeTable)

  @Callback
  def add(context: Context, args: Arguments): Array[AnyRef] = {
    val channel = channelIndex(args.checkInteger(0))
    if (channels(channel).buffer.size >= 8) return result(false, "channel buffer is full")
    channels(channel).buffer += ((args.checkDouble(1), args.optDouble(2, 0.1), args.optDouble(3, 0.0)))
    result(true)
  }

  @Callback
  def clear(context: Context, args: Arguments): Array[AnyRef] = {
    channels.foreach(_.buffer.clear())
    result(true)
  }

  @Callback
  def process(context: Context, args: Arguments): Array[AnyRef] = {
    channels.zipWithIndex.foreach { case (channel, index) =>
      channel.buffer.foreach { case (frequency, duration, delay) =>
        if (delay > 0) context.pause(delay)
        play(frequency, duration, index)
      }
      channel.buffer.clear()
    }
    result(true)
  }

  @Callback(direct = true)
  def play(context: Context, args: Arguments): Array[AnyRef] = {
    val entries = args.checkTable(0).asScala.toSeq
    if (entries.size > 8) return result(false, "table must not contain more than 8 channels")
    entries.foreach { case (key, table) =>
      val channel = key match {
        case n: Number if n.intValue() >= 1 && n.intValue() <= 8 => n.intValue() - 1
        case _ => -1
      }
      for {
        value <- tableValues(table)
        frequency <- value.get(1).flatMap(tableNumber)
        duration <- value.get(2).flatMap(tableNumber).orElse(Some(0.1))
        if channel >= 0
      } play(frequency, duration, channel)
    }
    result(true)
  }
}

class ComputronicsSoundCard(host: EnvironmentHost) extends ComputronicsAudioCard(host, "sound") {
  override protected val computronicsDeviceInfo = ComputronicsDeviceInfo(DeviceClass.Multimedia, "Audio interface", "Yanaki Sound Systems", "MinoSound 244-X")
  private var totalVolume = 1.0
  private sealed trait Instruction
  private case class Open(channel: Int) extends Instruction
  private case class Close(channel: Int) extends Instruction
  private case class Delay(milliseconds: Int) extends Instruction
  private val instructions = mutable.ArrayBuffer.empty[Instruction]

  @Callback(direct = true)
  def modes(context: Context, args: Arguments): Array[AnyRef] = result(modeTable)

  @Callback(direct = true)
  def setTotalVolume(context: Context, args: Arguments): Array[AnyRef] = {
    totalVolume = math.max(0, math.min(1, args.checkDouble(0)))
    outputVolume = totalVolume
    result(totalVolume)
  }

  @Callback
  def open(context: Context, args: Arguments): Array[AnyRef] = { instructions += Open(channelIndex(args.checkInteger(0))); result(true) }

  @Callback
  def close(context: Context, args: Arguments): Array[AnyRef] = { instructions += Close(channelIndex(args.checkInteger(0))); result(true) }

  @Callback
  def clear(context: Context, args: Arguments): Array[AnyRef] = { instructions.clear(); channels.foreach(_.buffer.clear()); result(true) }

  @Callback
  def setWave(context: Context, args: Arguments): Array[AnyRef] = {
    val channel = channelIndex(args.checkInteger(0))
    val index = args.checkInteger(1) - 1
    if (index < 0 || index >= waveModes.length) throw new IllegalArgumentException("unknown wave mode")
    channels(channel).mode = index
    result(true)
  }

  @Callback
  def setFrequency(context: Context, args: Arguments): Array[AnyRef] = { channels(channelIndex(args.checkInteger(0))).frequency = clampFrequency(args.checkDouble(1)); result(true) }

  @Callback
  def setLFSR(context: Context, args: Arguments): Array[AnyRef] = {
    val channel = channels(channelIndex(args.checkInteger(0)))
    channel.lfsrInitial = args.checkInteger(1)
    channel.lfsrMask = args.checkInteger(2)
    channel.mode = 4
    result(true)
  }

  @Callback
  def setFM(context: Context, args: Arguments): Array[AnyRef] = {
    val channel = channels(channelIndex(args.checkInteger(0)))
    channel.fmChannel = channelIndex(args.checkInteger(1))
    channel.fmIntensity = args.checkDouble(2)
    result(true)
  }

  @Callback
  def resetFM(context: Context, args: Arguments): Array[AnyRef] = { channels(channelIndex(args.checkInteger(0))).fmChannel = -1; result(true) }

  @Callback
  def setAM(context: Context, args: Arguments): Array[AnyRef] = {
    val channel = channels(channelIndex(args.checkInteger(0)))
    channel.amChannel = channelIndex(args.checkInteger(1))
    result(true)
  }

  @Callback
  def resetAM(context: Context, args: Arguments): Array[AnyRef] = { channels(channelIndex(args.checkInteger(0))).amChannel = -1; result(true) }

  @Callback
  def setADSR(context: Context, args: Arguments): Array[AnyRef] = {
    val channel = channels(channelIndex(args.checkInteger(0)))
    channel.attack = args.checkInteger(1)
    channel.decay = args.checkInteger(2)
    channel.sustain = args.checkDouble(3)
    channel.release = args.checkInteger(4)
    result(true)
  }

  @Callback
  def resetEnvelope(context: Context, args: Arguments): Array[AnyRef] = {
    val channel = channels(channelIndex(args.checkInteger(0)))
    channel.attack = 0
    channel.decay = 0
    channel.sustain = 1
    channel.release = 0
    result(true)
  }

  @Callback
  def setVolume(context: Context, args: Arguments): Array[AnyRef] = { channels(channelIndex(args.checkInteger(0))).volume = math.max(0, math.min(1, args.checkDouble(1))); result(true) }

  @Callback
  def delay(context: Context, args: Arguments): Array[AnyRef] = { instructions += Delay(math.max(0, math.min(16000, args.checkInteger(0)))); result(true) }

  @Callback
  def process(context: Context, args: Arguments): Array[AnyRef] = {
    val opened = Array.fill(channels.length)(false)
    var elapsed = 0
    instructions.foreach {
      case Open(channel) => opened(channel) = true
      case Close(channel) => opened(channel) = false
      case Delay(milliseconds) =>
        channels.zipWithIndex.foreach { case (channel, index) =>
          if (opened(index)) play(channel.frequency, milliseconds / 1000.0, index, elapsed)
        }
        elapsed += milliseconds
    }
    instructions.clear()
    result(true)
  }
}
