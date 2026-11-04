package li.cil.oc.util

import java.nio.Buffer
import java.nio.ByteBuffer
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.client.PacketHandler
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10

import scala.collection.mutable
import net.minecraft.sounds.{SoundEvents, SoundSource}
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.world.phys.Vec3

import java.util.concurrent.Executor

/**
  * This class contains the logic used by computers' internal "speakers".
  * It can generate square waves with a specific frequency and duration
  * and will play them through OpenAL, acquiring sources as necessary.
  * Tones that have finished playing are disposed automatically in the
  * tick handler.
  */
object Audio {
  private def sampleRate = Settings.get.beepSampleRate

  private def amplitude = Settings.get.beepAmplitude

  private def maxDistance = Settings.get.beepRadius

  private val sources = mutable.Set.empty[Source]

  private def volume(source: SoundSource) = Minecraft.getInstance.options.getSoundSourceVolume(source)

  private var disableAudio = false

  /** Convert a physical packet position into the client's local OpenAL space. */
  def localAudioPosition(physical: Vec3): Vec3 = {
    val mc = Minecraft.getInstance
    if (mc == null || mc.player == null || mc.level == null) physical
    else {
      val listener = mc.player.position()
      listener.add(physical.subtract(SableCompat.physicalPosition(mc.level, listener)))
    }
  }

  def physicalGain(physical: Vec3, distance: Double): Double = {
    val mc = Minecraft.getInstance
    if (mc == null || mc.player == null || mc.level == null) 0
    else math.max(0, 1 - SableCompat.physicalPosition(mc.level, mc.player.position()).distanceTo(physical) / distance)
  }

  /**
    * Run OpenAL work on Minecraft's sound thread, but only while its OpenAL
    * context is ready. The sound executor is created before the sound engine
    * is loaded, so checking for the executor alone is not sufficient.
    */
  def runOnSoundEngine(action: => Unit): Boolean = {
    val mc = Minecraft.getInstance
    if (mc == null || mc.getSoundManager == null || mc.getSoundManager.soundEngine == null) return false

    val soundEngine = mc.getSoundManager.soundEngine
    if (!soundEngine.loaded || soundEngine.executor == null) return false

    soundEngine.executor.asInstanceOf[Executor].execute(() => {
      // The engine may have been stopped for a resource reload after this
      // task was submitted.
      if (soundEngine.loaded) action
    })
    true
  }

  def play(x: Float, y: Float, z: Float, pcm: Array[Byte], gain: Float): Unit = {
    if (pcm == null || pcm.isEmpty) return

    runOnSoundEngine {
      try {
        // LWJGL's OpenAL bindings pass the buffer address directly to the
        // native driver. A heap ByteBuffer (ByteBuffer.wrap) is not valid for
        // that call and can crash the JVM in nalBufferData on Windows.
        val nativePcm = BufferUtils.createByteBuffer(pcm.length)
        nativePcm.put(pcm)
        nativePcm.flip()
        sources.synchronized {
          sources += new Source(x, y, z, nativePcm, gain)
        }
      } catch {
        case e: OpenALException =>
          if (e.errorCode == AL10.AL_OUT_OF_MEMORY) disableAudio = true
      }
    }
  }

  def play(x: Float, y: Float, z: Float, source: SoundSource, frequencyInHz: Int, durationInMilliseconds: Int): Unit = {
    play(x, y, z, ".", source, frequencyInHz, durationInMilliseconds)
  }

  def playWave(x: Float, y: Float, z: Float, mode: Int, frequency: Int, durationMillis: Int, delayMillis: Int,
               requestedGain: Float, fmFrequency: Int, fmIntensity: Float, amFrequency: Int,
               attack: Int, decay: Int, sustain: Float, release: Int, source: SoundSource): Unit = {
    val mc = Minecraft.getInstance
    if (mc == null || mc.player == null) return
    // Wave packets carry a physical position. This keeps sounds attached to
    // moving Sable sublevels while retaining OpenAL's local listener space.
    val physical = new Vec3(x, y, z)
    val gain = physicalGain(physical, maxDistance).toFloat * volume(source) * requestedGain.max(0).min(1)
    if (gain <= 0 || amplitude <= 0) return
    val delay = delayMillis.max(0).min(16000)
    val duration = durationMillis.max(50).min(5000)
    val samples = (delay + duration) * sampleRate / 1000
    val data = Array.fill[Byte](samples)(127.toByte)
    val start = delay * sampleRate / 1000
    var phase = 0.0; var fmPhase = 0.0; var amPhase = 0.0
    for (i <- 0 until duration * sampleRate / 1000 if start + i < data.length) {
      val elapsed = i * 1000.0 / sampleRate
      val fm = if (fmFrequency > 0) math.sin(fmPhase * 2 * math.Pi) * fmIntensity * fmFrequency else 0
      val value = mode match {
        case 1 => math.sin(phase * 2 * math.Pi)
        case 2 => 1 - 4 * math.abs(phase - 0.5)
        case 3 => 2 * phase - 1
        case 4 => if (scala.util.Random.nextBoolean()) 1 else -1
        case _ => if (phase < 0.5) 1 else -1
      }
      val attackGain = if (attack > 0 && elapsed < attack) elapsed / attack else 1.0
      val decayGain = if (decay > 0 && elapsed >= attack && elapsed < attack + decay) 1 - (1 - sustain) * (elapsed - attack) / decay else sustain
      val releaseGain = if (release > 0 && elapsed > duration - release) math.max(0, (duration - elapsed) / release) else 1.0
      val amGain = if (amFrequency > 0) 0.5 + 0.5 * math.sin(amPhase * 2 * math.Pi) else 1.0
      data(start + i) = (127 + value * amplitude * attackGain * decayGain * releaseGain * amGain).toByte
      phase = (phase + math.max(20, math.min(2000, frequency + fm)) / sampleRate) % 1.0
      fmPhase = (fmPhase + math.max(20, fmFrequency) / sampleRate) % 1.0
      amPhase = (amPhase + math.max(20, amFrequency) / sampleRate) % 1.0
    }
    val local = localAudioPosition(physical)
    play(local.x.toFloat, local.y.toFloat, local.z.toFloat, data, gain)
  }

  def play(x: Float, y: Float, z: Float, pattern: String, source: SoundSource = SoundSource.BLOCKS, frequencyInHz: Int = 1000, durationInMilliseconds: Int = 200): Unit = {
    val mc = Minecraft.getInstance
    val distanceBasedGain = math.max(0, 1 - mc.player.position.distanceTo(new Vec3(x, y, z)) / maxDistance).toFloat
    val gain = distanceBasedGain * volume(source)
    if (gain <= 0 || amplitude <= 0) return

    if (disableAudio) {
      // Fallback audio generation, using built-in Minecraft sound. This can be
      // necessary on certain systems with audio cards that do not have enough
      // memory. May still fail, but at least we can say we tried!
      // Valid range is 20-2000Hz, clamp it to that and get a relative value.
      // MC's pitch system supports a minimum pitch of 0.5, however, so up it
      // by that.
      val clampedFrequency = ((frequencyInHz - 20) max 0 min 1980) / 1980f + 0.5f
      var delay = 0
      for (ch <- pattern) {
        val record = new SimpleSoundInstance(SoundEvents.NOTE_BLOCK_HARP.value, SoundSource.BLOCKS, gain, clampedFrequency, mc.level.random, new BlockPos(x.toInt, y.toInt, z.toInt))
        if (delay == 0) mc.getSoundManager.play(record)
        else mc.getSoundManager.playDelayed(record, delay)
        delay += ((if (ch == '.') durationInMilliseconds else 2 * durationInMilliseconds) * 20 / 1000) max 1
      }
    }
    else {
      val sampleCounts = pattern.toCharArray.
        map(ch => if (ch == '.') durationInMilliseconds else 2 * durationInMilliseconds).
        map(_ * sampleRate / 1000)
      // 50ms pause between pattern parts.
      val pauseSampleCount = 50 * sampleRate / 1000
      val data = BufferUtils.createByteBuffer(sampleCounts.sum + (sampleCounts.length - 1) * pauseSampleCount)
      val step = frequencyInHz / sampleRate.toFloat
      var offset = 0f
      for (sampleCount <- sampleCounts) {
        for (sample <- 0 until sampleCount) {
          val angle = 2 * math.Pi * offset
          val value = (math.signum(math.sin(angle)) * amplitude).toByte ^ 0x80
          offset += step
          if (offset > 1) offset -= 1
          data.put(value.toByte)
        }
        if (data.hasRemaining) {
          for (sample <- 0 until pauseSampleCount) {
            data.put(127: Byte)
          }
        }
      }
      data.asInstanceOf[Buffer].rewind()

      // Watch out for sound cards running out of memory... this apparently
      // really does happen. I'm assuming this is due to too many sounds being
      // kept loaded, since from what I can see OC's releasing its audio
      // memory as it should.
      runOnSoundEngine {
        try sources.synchronized(sources += new Source(x, y, z, data, gain)) catch {
          case e: OpenALException =>
            if (e.errorCode == AL10.AL_OUT_OF_MEMORY) {
              // Well... let's just stop here.
              OpenComputers.log.info("Couldn't play computer speaker sound because your sound card ran out of memory. Either your sound card is just really low-end, or there are just too many sounds in use already by other mods. Disabling computer speakers to avoid spamming your log file now.")
              disableAudio = true
            }
            else {
              OpenComputers.log.warn("Error playing computer speaker sound.", e)
            }
        }
      }
    }
  }

  def update(): Unit = {
    val hasSources = sources.synchronized(sources.nonEmpty)
    if (!disableAudio && hasSources) {
      runOnSoundEngine {
        sources.synchronized(sources --= sources.filter(_.checkFinished))

        // Clear error stack.
        try AL10.alGetError() catch {
          case _: UnsatisfiedLinkError =>
            OpenComputers.log.warn("Negotiations with OpenAL broke down, disabling sounds.")
            disableAudio = true
        }
      }
    }
    PacketHandler.update()
  }

  private class Source(val x: Float, y: Float, z: Float, val data: ByteBuffer, val gain: Float) {
    // Clear error stack.
    AL10.alGetError()

    val (source, buffer) = {
      val buffer = AL10.alGenBuffers()
      checkALError()

      try {
        AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO8, data, sampleRate)
        checkALError()

        val source = AL10.alGenSources()
        checkALError()

        try {
          AL10.alSourceQueueBuffers(source, buffer)
          checkALError()

          AL10.alSource3f(source, AL10.AL_POSITION, x, y, z)
          AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, maxDistance)
          AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, maxDistance)
          AL10.alSourcef(source, AL10.AL_GAIN, gain * 0.3f)
          checkALError()

          AL10.alSourcePlay(source)
          checkALError()

          (source, buffer)
        }
        catch {
          case t: Throwable =>
            AL10.alDeleteSources(source)
            throw t
        }
      }
      catch {
        case t: Throwable =>
          AL10.alDeleteBuffers(buffer)
          throw t
      }
    }

    def checkFinished = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING && {
      AL10.alDeleteSources(source)
      AL10.alDeleteBuffers(buffer)
      true
    }
  }

  // Having the error code in an accessible way is really cool, you know.
  class OpenALException(val errorCode: Int) extends RuntimeException {
    override def getMessage: String = errorCode.toString
  }

  // Custom implementation of Util.checkALError() that uses our custom exception.
  def checkALError(): Unit = {
    val errorCode = AL10.alGetError()
    if (errorCode != AL10.AL_NO_ERROR) {
      throw new OpenALException(errorCode)
    }
  }

  @SubscribeEvent
  def onTick(e: ClientTickEvent.Pre): Unit = {
    update()
  }
}
