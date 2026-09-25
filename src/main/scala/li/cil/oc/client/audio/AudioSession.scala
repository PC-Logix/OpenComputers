package li.cil.oc.client.audio


import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10
import li.cil.oc.util.{Audio, DFPWM}
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3

import java.security.MessageDigest
import scala.collection.mutable

class AudioSession(
                    val handle: Int,
                    val channel: Int,
                    val sampleRate: Int,
                    val channels: Int,
                    val format: Int,
                    val pos: Vec3,
                    val encodedDfpwm: Boolean = false,
                    val streamGain: Float = 1f
                  ) {
  // Computronics 1.12 decoded each 1024-byte DFPWM packet to 8192 bytes of
  // MONO8 PCM and queued that complete decoded packet as one OpenAL buffer.
  // Keep this boundary; splitting it into 1024-byte buffers changes the
  // historical packet cadence and is not equivalent to StreamingAudioPlayer.
  private final val OpenALBufferSize = 1024 * 8
  private final val OpenALBufferCount = 3
  private val pendingBuffers = mutable.Queue.empty[Array[Byte]]
  private val nativeBuffers = mutable.ArrayBuffer.empty[Int]
  private val dfpwmDecoder = if (encodedDfpwm) new DFPWM() else null
  private val pcmDigest = MessageDigest.getInstance("SHA-256")
  private var appendedBytes = 0L
  @volatile var loop: Boolean = false

  @volatile private var alSource: Int = -1
  @volatile private var alBuffer: Int = -1
  @volatile private var isPlayCalled: Boolean = false
  @volatile private var finished: Boolean = false

  def append(data: Array[Byte]): Unit = {
    if (!isPlayCalled) {
      val pcm = if (encodedDfpwm) {
        // This is the exact Computronics 1.12 client path: one persistent
        // AsieLib decoder, one 8192-byte PCM block per 1024-byte DFPWM packet,
        // then signed-to-unsigned conversion for OpenAL MONO8.
        val decoded = new Array[Byte](data.length * 8)
        dfpwmDecoder.decompress(decoded, data, 0, 0, data.length)
        var i = 0
        while (i < decoded.length) {
          decoded(i) = ((decoded(i) & 0xFF) ^ 0x80).toByte
          i += 1
        }
        decoded
      }
      else data

      appendedBytes += pcm.length
      pcmDigest.update(pcm)
      var offset = 0
      while (offset < pcm.length) {
        val length = math.min(OpenALBufferSize, pcm.length - offset)
        pendingBuffers.enqueue(java.util.Arrays.copyOfRange(pcm, offset, offset + length))
        offset += length
      }
    }
  }

  def play(): Unit = {
    if (isPlayCalled) {
      if (alSource != -1) {
        Audio.runOnSoundEngine {
          if (alSource != -1) {
            finished = false
            AL10.alSourcePlay(alSource)
          }
        }
      }
      return
    }
    isPlayCalled = true

    val pcmSha256 = pcmDigest.digest().map(byte => f"${byte & 0xFF}%02x").mkString
    OpenComputers.log.info(s"Audio stream play: handle=$handle, sampleRate=$sampleRate, format=$format, pcmBytes=$appendedBytes, pcmSha256=$pcmSha256, buffers=${pendingBuffers.size}")

    if (pendingBuffers.isEmpty) return

    val mc = Minecraft.getInstance
    val queued = Audio.runOnSoundEngine {
      try {
        AL10.alGetError()

        alSource = AL10.alGenSources()
        Audio.checkALError()

        if (loop) {
          // Preserve the old exact looping behavior for explicitly looping
          // audio. Tape playback is non-looping and uses the streaming path.
          val pcmData = pendingBuffers.foldLeft(Array.emptyByteArray)(_ ++ _)
          pendingBuffers.clear()
          alBuffer = AL10.alGenBuffers()
          Audio.checkALError()
          nativeBuffers += alBuffer
          val dataBuffer = BufferUtils.createByteBuffer(pcmData.length)
          dataBuffer.put(pcmData).flip()
          AL10.alBufferData(alBuffer, format, dataBuffer, sampleRate)
          Audio.checkALError()
          AL10.alSourceQueueBuffers(alSource, alBuffer)
          Audio.checkALError()
        }
        else {
          for (_ <- 0 until OpenALBufferCount if pendingBuffers.nonEmpty) {
            val pcmData = pendingBuffers.dequeue()
            val buffer = AL10.alGenBuffers()
            Audio.checkALError()
            nativeBuffers += buffer
            val dataBuffer = BufferUtils.createByteBuffer(pcmData.length)
            dataBuffer.put(pcmData).flip()
            AL10.alBufferData(buffer, format, dataBuffer, sampleRate)
            Audio.checkALError()
            AL10.alSourceQueueBuffers(alSource, buffer)
            Audio.checkALError()
          }
        }

        val local = Audio.localAudioPosition(pos)
        val x = local.x.toFloat
        val y = local.y.toFloat
        val z = local.z.toFloat
        AL10.alSource3f(alSource, AL10.AL_POSITION, x, y, z)

        val maxDistance = Settings.get.beepRadius
        val volume = mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.BLOCKS)
        val distanceBasedGain = Audio.physicalGain(pos, maxDistance).toFloat
        val gain = distanceBasedGain * volume

        AL10.alSourcef(alSource, AL10.AL_REFERENCE_DISTANCE, maxDistance)
        AL10.alSourcef(alSource, AL10.AL_MAX_DISTANCE, maxDistance)
        // The 0.3 multiplier belongs to the generic PCM-card path. Applying
        // it to tapes made them more than 10 dB quieter than Computronics.
        val outputGain = if (encodedDfpwm) gain * streamGain else gain * 0.3f * streamGain
        AL10.alSourcef(alSource, AL10.AL_GAIN, outputGain)
        AL10.alSourcei(alSource, AL10.AL_LOOPING, if (loop) AL10.AL_TRUE else AL10.AL_FALSE)
        Audio.checkALError()

        AL10.alSourcePlay(alSource)
        Audio.checkALError()
      } catch {
        case t: Throwable =>
          OpenComputers.log.error("Failed to play audio", t)
          cleanupOnSoundThread()
      }
    }
    if (!queued) isPlayCalled = false
  }

  def pause(): Unit = {
    if (alSource != -1) {
      Audio.runOnSoundEngine {
        if (alSource != -1 && AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
          AL10.alSourcePause(alSource)
        }
      }
    }
  }

  def resume(): Unit = {
    if (alSource != -1) {
      Audio.runOnSoundEngine {
        if (alSource != -1 && AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE) == AL10.AL_PAUSED) {
          finished = false
          AL10.alSourcePlay(alSource)
        }
      }
    }
  }

  def stop(): Unit = {
    if (alSource != -1) {
      Audio.runOnSoundEngine {
        if (alSource != -1) {
          AL10.alSourceStop(alSource)
        }
      }
    }
  }

  def setLoopMode(newLoop: Boolean): Unit = {
    loop = newLoop
    if (alSource != -1) {
      Audio.runOnSoundEngine {
        if (alSource != -1) {
          AL10.alSourcei(alSource, AL10.AL_LOOPING, if (loop) AL10.AL_TRUE else AL10.AL_FALSE)
        }
      }
    }
  }

  def update(): Unit = {
    if (isPlayCalled && alSource != -1) {
      Audio.runOnSoundEngine {
        if (alSource != -1) {
          updateSpatialAudio()
          if (!loop) refillProcessedBuffers()
          val state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE)
          val queued = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_QUEUED)
          finished = !loop && pendingBuffers.isEmpty && queued == 0 && state != AL10.AL_PLAYING && state != AL10.AL_PAUSED
        }
      }
    }
  }

  def checkFinished: Boolean = finished

  def cleanup(): Unit = {
    if (!Audio.runOnSoundEngine(cleanupOnSoundThread())) {
      // The old source and buffer are no longer usable if the engine is not
      // loaded. Forget them without making OpenAL calls on the client thread.
      alSource = -1
      alBuffer = -1
      finished = true
    }
  }

  private def cleanupOnSoundThread(): Unit = {
    if (alSource != -1) {
      try AL10.alDeleteSources(alSource) catch { case _: Throwable => }
      alSource = -1
    }
    // Non-looping streams use one native buffer per 1024 PCM samples. The
    // source deletion above releases the queue, after which each buffer can be
    // returned to OpenAL safely.
    nativeBuffers.foreach(buffer => try AL10.alDeleteBuffers(buffer) catch { case _: Throwable => })
    nativeBuffers.clear()
    alBuffer = -1
    pendingBuffers.clear()
    finished = true
  }

  /** Reuse processed OpenAL buffers instead of queueing an entire tape at once. */
  private def refillProcessedBuffers(): Unit = {
    var processed = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_PROCESSED)
    while (processed > 0) {
      val buffer = AL10.alSourceUnqueueBuffers(alSource)
      if (pendingBuffers.nonEmpty) {
        val pcmData = pendingBuffers.dequeue()
        val dataBuffer = BufferUtils.createByteBuffer(pcmData.length)
        dataBuffer.put(pcmData)
        dataBuffer.flip()
        AL10.alBufferData(buffer, format, dataBuffer, sampleRate)
        Audio.checkALError()
        AL10.alSourceQueueBuffers(alSource, buffer)
        Audio.checkALError()
      }
      processed -= 1
    }
    val state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE)
    if (state != AL10.AL_PLAYING && pendingBuffers.nonEmpty && AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_QUEUED) > 0)
      AL10.alSourcePlay(alSource)
  }

  /** Keep the stream attached to its physical source as the listener moves. */
  private def updateSpatialAudio(): Unit = {
    val mc = Minecraft.getInstance
    if (mc == null || mc.player == null || mc.level == null) return

    val local = Audio.localAudioPosition(pos)
    AL10.alSource3f(alSource, AL10.AL_POSITION, local.x.toFloat, local.y.toFloat, local.z.toFloat)

    val maxDistance = Settings.get.beepRadius
    val volume = mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.BLOCKS)
    val distanceGain = Audio.physicalGain(pos, maxDistance).toFloat
    val gain = distanceGain * volume
    // OpenAL's AL_MAX_DISTANCE clamps attenuation; it does not silence a
    // source beyond that distance. physicalGain supplies the hard cutoff.
    val outputGain = if (encodedDfpwm) gain * streamGain else gain * 0.3f * streamGain
    AL10.alSourcef(alSource, AL10.AL_GAIN, outputGain)
  }
}
