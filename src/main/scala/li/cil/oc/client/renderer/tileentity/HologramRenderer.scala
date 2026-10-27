package li.cil.oc.client.renderer.tileentity

import com.google.common.cache.{CacheBuilder, RemovalListener, RemovalNotification}
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex._
import com.mojang.math.Axis
import li.cil.oc.Settings
import li.cil.oc.common.blockentity.Hologram
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.client.renderer.{GameRenderer, MultiBufferSource}
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import org.joml.{Quaternionf, Matrix4f}

import java.util.concurrent.TimeUnit
import scala.util.Random

object HologramRenderer extends BlockEntityRendererProvider[Hologram] {
  override def create(ctx: BlockEntityRendererProvider.Context): BlockEntityRenderer[Hologram] =
    new HologramRenderer()

  // Per-hologram VBO cache, expires after 5 seconds of non-access.
  // Registered on MinecraftForge.EVENT_BUS in ClientProxy for tick-driven cleanup.
  private val cache = CacheBuilder.newBuilder()
    .expireAfterAccess(5, TimeUnit.SECONDS)
    .removalListener((n: RemovalNotification[Hologram, VertexBuffer]) => n.getValue.close())
    .asInstanceOf[CacheBuilder[Hologram, VertexBuffer]]
    .build[Hologram, VertexBuffer]()

  @SubscribeEvent
  def onClientTick(e: ClientTickEvent.Pre): Unit = cache.cleanUp()
}

class HologramRenderer extends BlockEntityRenderer[Hologram] {
  private val random = new Random()

  override def render(
                       hologram: Hologram,
                       partialTick: Float,
                       stack: PoseStack,
                       buffer: MultiBufferSource,
                       packedLight: Int,
                       packedOverlay: Int
                     ): Unit = {
    if (!hologram.hasPower) return

    RenderState.checkError(getClass.getName + ".render: entering")

    val pos = hologram.getBlockPos
    val eye = Minecraft.getInstance.player.getEyePosition(partialTick)
    val dx = eye.x - (pos.getX + 0.5)
    val dy = eye.y - (pos.getY + 0.5)
    val dz = eye.z - (pos.getZ + 0.5)
    val distSq = dx * dx + dy * dy + dz * dz

    val fadeDistSq = hologram.getFadeStartDistanceSquared
    val maxDistSq  = hologram.getViewDistance * hologram.getViewDistance
    val alpha = 0.75f * (
      if (distSq > maxDistSq) return
      else if (distSq > fadeDistSq && maxDistSq > fadeDistSq)
        math.max(0f, 1f - ((distSq - fadeDistSq) / (maxDistSq - fadeDistSq)).toFloat)
      else 1f
      )

    // Flush any pending MultiBufferSource geometry before switching to direct VBO rendering.
    buffer match {
      case bs: MultiBufferSource.BufferSource => bs.endBatch()
      case _ =>
    }

    RenderState.makeItBlend()
    // Additive blending, same as 1.12.2.
    RenderSystem.blendFuncSeparate(
      GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
      GlStateManager.SourceFactor.ONE,       GlStateManager.DestFactor.ZERO
    )
    RenderSystem.setShaderColor(1f, 1f, 1f, alpha)

    stack.pushPose()
    stack.translate(0.5, 0.5, 0.5)

    hologram.yaw match {
      case Direction.WEST  => stack.mulPose(Axis.YP.rotationDegrees(-90))
      case Direction.NORTH => stack.mulPose(Axis.YP.rotationDegrees(180))
      case Direction.EAST  => stack.mulPose(Axis.YP.rotationDegrees(90))
      case _ =>
    }
    hologram.pitch match {
      case Direction.DOWN => stack.mulPose(Axis.XP.rotationDegrees(90))
      case Direction.UP   => stack.mulPose(Axis.XP.rotationDegrees(-90))
      case _ =>
    }

    applyRotation(stack, hologram.rotationAngle, hologram.rotationX, hologram.rotationY, hologram.rotationZ)
    val animAngle = hologram.rotationSpeed *
      (hologram.getLevel.getGameTime % (360 * 20 - 1) + partialTick) / 20f
    applyRotation(stack, animAngle, hologram.rotationSpeedX, hologram.rotationSpeedY, hologram.rotationSpeedZ)

    stack.scale(1.001f, 1.001f, 1.001f)
    stack.translate(
      (hologram.translation.x * hologram.width / 16.0 - 1.5) * hologram.scale,
      hologram.translation.y * hologram.height / 16.0      * hologram.scale,
      (hologram.translation.z * hologram.width / 16.0 - 1.5) * hologram.scale
    )

    if (Settings.get.hologramFlickerFrequency > 0 &&
      random.nextDouble() < Settings.get.hologramFlickerFrequency) {
      stack.scale(
        math.max(1f + (random.nextGaussian() * 0.01 ).toFloat, 0.001f),
        math.max(1f + (random.nextGaussian() * 0.001).toFloat, 0.001f),
        math.max(1f + (random.nextGaussian() * 0.01 ).toFloat, 0.001f)
      )
    }

    // After this scale, hologram voxels occupy [0..width] x [0..height] x [0..width] in world units/16.
    RenderState.mirrorScale(
      stack,
      (hologram.scale / 16.0).toFloat,
      (hologram.scale / 16.0).toFloat,
      (hologram.scale / 16.0).toFloat
    )

    val vbo = HologramRenderer.cache.get(hologram, () => {
      hologram.needsRendering = true
      new VertexBuffer(VertexBuffer.Usage.DYNAMIC)
    })

    if (hologram.needsRendering) {
      rebuildVBO(hologram, vbo)
      hologram.needsRendering = false
    }

    if (hologram.visibleQuads > 0) {
      val modelView  = stack.last.pose
      val projection = new Matrix4f(RenderSystem.getProjectionMatrix)
                       .mul(RenderSystem.getModelViewMatrix)
      val shader     = GameRenderer.getPositionColorShader

      RenderSystem.enableDepthTest()
      RenderSystem.disableCull()

      // Two-pass rendering (mirrors 1.12.2):
      //   Pass 1 — depth pre-pass: write only to the depth buffer to find the
      //            frontmost voxel fragment along each ray.
      //   Pass 2 — color pass: use GL_EQUAL so only the front fragment is shaded,
      //            preventing semi-transparent voxels from double-blending.
      RenderSystem.colorMask(false, false, false, false)
      RenderSystem.depthMask(true)
      vbo.bind()
      vbo.drawWithShader(modelView, projection, shader)
      VertexBuffer.unbind()

      RenderSystem.colorMask(true, true, true, true)
      RenderSystem.depthMask(false)
      RenderSystem.depthFunc(514) // GL_EQUAL
      vbo.bind()
      vbo.drawWithShader(modelView, projection, shader)
      VertexBuffer.unbind()

      RenderSystem.depthFunc(515) // GL_LEQUAL (default)
      RenderSystem.depthMask(true)
      RenderSystem.enableCull()
    }

    stack.popPose()

    RenderSystem.defaultBlendFunc()
    RenderState.disableBlend()
    RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private def applyRotation(stack: PoseStack, degrees: Float, x: Float, y: Float, z: Float): Unit = {
    if (degrees != 0 && x * x + y * y + z * z > 1e-6f) {
      stack.mulPose(new Quaternionf().rotationAxis(
        degrees * (Math.PI / 180.0).toFloat,
        x, y, z
      ))
    }
  }

  private def rebuildVBO(hologram: Hologram, vbo: VertexBuffer): Unit = {
    val byteBuffer = new ByteBufferBuilder(1 << 20)
    val builder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)

    def value(x: Int, y: Int, z: Int): Int =
      if (x >= 0 && y >= 0 && z >= 0 && x < hologram.width && y < hologram.height && z < hologram.width)
        hologram.getColor(x, y, z)
      else 0

    def solid(x: Int, y: Int, z: Int): Boolean = value(x, y, z) != 0

    // colorsByTier uses 0xBBGGRR packing (chosen for 1.12.2's little-endian glColorPointer).
    // Extract in the correct order: R = bits 0-7, G = bits 8-15, B = bits 16-23.
    def vertex(x: Int, y: Int, z: Int, r: Int, g: Int, b: Int): Unit =
      builder.addVertex(x.toFloat, y.toFloat, z.toFloat).setColor(r, g, b, 255)

    hologram.visibleQuads = 0

    for {
      x <- 0 until hologram.width
      y <- 0 until hologram.height
      z <- 0 until hologram.width
      if solid(x, y, z)
    } {
      val c = hologram.colors(value(x, y, z) - 1)
      val r =  c        & 0xFF // RR (least-significant byte)
      val g = (c >>  8) & 0xFF // GG
      val b = (c >> 16) & 0xFF // BB (most-significant byte)

      // South (+z): visible if the adjacent voxel is empty
      if (!solid(x, y, z + 1)) {
        vertex(x+1, y+1, z+1, r, g, b); vertex(x, y+1, z+1, r, g, b)
        vertex(x,   y,   z+1, r, g, b); vertex(x+1, y, z+1, r, g, b)
        hologram.visibleQuads += 1
      }
      // North (-z)
      if (!solid(x, y, z - 1)) {
        vertex(x+1, y,   z, r, g, b); vertex(x, y,   z, r, g, b)
        vertex(x,   y+1, z, r, g, b); vertex(x+1, y+1, z, r, g, b)
        hologram.visibleQuads += 1
      }
      // East (+x)
      if (!solid(x + 1, y, z)) {
        vertex(x+1, y+1, z+1, r, g, b); vertex(x+1, y,   z+1, r, g, b)
        vertex(x+1, y,   z,   r, g, b); vertex(x+1, y+1, z,   r, g, b)
        hologram.visibleQuads += 1
      }
      // West (-x)
      if (!solid(x - 1, y, z)) {
        vertex(x, y,   z+1, r, g, b); vertex(x, y+1, z+1, r, g, b)
        vertex(x, y+1, z,   r, g, b); vertex(x, y,   z,   r, g, b)
        hologram.visibleQuads += 1
      }
      // Up (+y)
      if (!solid(x, y + 1, z)) {
        vertex(x+1, y+1, z,   r, g, b); vertex(x, y+1, z,   r, g, b)
        vertex(x,   y+1, z+1, r, g, b); vertex(x+1, y+1, z+1, r, g, b)
        hologram.visibleQuads += 1
      }
      // Down (-y)
      if (!solid(x, y - 1, z)) {
        vertex(x+1, y, z+1, r, g, b); vertex(x, y, z+1, r, g, b)
        vertex(x,   y, z,   r, g, b); vertex(x+1, y, z,   r, g, b)
        hologram.visibleQuads += 1
      }
    }

    try {
      // An empty hologram has no mesh. BufferBuilder.buildOrThrow rejects an
      // empty buffer, which is the normal state of a newly placed projector.
      if (hologram.visibleQuads > 0) {
        vbo.bind()
        try {
          vbo.upload(builder.buildOrThrow())
        }
        finally {
          VertexBuffer.unbind()
        }
      }
    }
    finally {
      byteBuffer.close()
    }
  }

  private final val Sqrt2 = Math.sqrt(2)

  override def getRenderBoundingBox(entity: Hologram) = {
    val cx = entity.x + 0.5
    val cy = entity.y + 0.5
    val cz = entity.z + 0.5
    val sh = entity.width / 16 * entity.scale * Sqrt2
    // overscale to take into account 45 degree rotation
    val sv = entity.height / 16 * entity.scale * Sqrt2
    new AABB(
      cx + (-0.5 + entity.translation.x) * sh,
      cy + entity.translation.y * sv,
      cz + (-0.5 + entity.translation.z) * sh,
      cx + (0.5 + entity.translation.x) * sh,
      cy + (1 + entity.translation.y) * sv,
      cz + (0.5 + entity.translation.x) * sh)
  }
}
