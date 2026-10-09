package li.cil.oc.client.renderer

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.{DefaultVertexFormat, PoseStack, VertexFormat}
import li.cil.oc.Settings
import li.cil.oc.server.network.WirelessNetwork
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.{
  GameRenderer,
  RenderStateShard,
  RenderType
}
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.lwjgl.opengl.GL11

import scala.jdk.CollectionConverters._

object WirelessNetworkDebugRenderer {
  val colors = Array(0xFF0000, 0x00FFFF, 0x00FF00, 0x0000FF, 0xFF00FF, 0xFFFF00, 0xFFFFFF, 0x000000)

  private val RENDER_TYPE = RenderType.create(
    "oc_wireless_debug",
    DefaultVertexFormat.POSITION_COLOR,
    VertexFormat.Mode.QUADS,
    131072, false, true,
    RenderType.CompositeState.builder()
      .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer.getPositionColorShader _))
      .setTransparencyState(new RenderStateShard.TransparencyStateShard("translucent",
        () => {
          RenderSystem.enableBlend()
          RenderSystem.defaultBlendFunc()
        },
        () => RenderSystem.disableBlend()
      ))
      .setCullState(new RenderStateShard.CullStateShard(false))
      .setDepthTestState(new RenderStateShard.DepthTestStateShard("always", GL11.GL_ALWAYS))
      .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
      .createCompositeState(false)
  )

  @SubscribeEvent
  def onRenderWorldLastEvent(e: RenderLevelStageEvent): Unit = {
    if (e.getStage != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return
    if (!Settings.rTreeDebugRenderer) return

    val world = Minecraft.getInstance.level
    WirelessNetwork.dimensions.get(world.dimension) match {
      case Some(tree) =>
        val player = Minecraft.getInstance.player
        val px = player.xOld + (player.getX - player.xOld) * e.getPartialTick.getGameTimeDeltaTicks.toDouble
        val py = player.yOld + (player.getY - player.yOld) * e.getPartialTick.getGameTimeDeltaTicks.toDouble
        val pz = player.zOld + (player.getZ - player.zOld) * e.getPartialTick.getGameTimeDeltaTicks.toDouble

        val stack = e.getPoseStack
        stack.pushPose()
        stack.translate(-px, -py, -pz)

        val bufferSource = Minecraft.getInstance.renderBuffers.bufferSource
        val consumer = bufferSource.getBuffer(RENDER_TYPE)

        for (bounds <- tree.allBounds().asScala) {
          val min = bounds.min()
          val max = bounds.max()
          val minX = min.x()
          val minY = min.y()
          val minZ = min.z()
          val maxX = max.x()
          val maxY = max.y()
          val maxZ = max.z()
          val level = bounds.level()
          val color = colors(level % colors.length)
          val r = ((color >> 16) & 0xFF)
          val g = ((color >> 8) & 0xFF)
          val b = ((color >> 0) & 0xFF)
          val a = 64 // 0.25 * 255

          val size = 0.5f - level * 0.05f
          drawBox(stack, consumer,
            minX.toFloat - size, minY.toFloat - size, minZ.toFloat - size,
            maxX.toFloat + size, maxY.toFloat + size, maxZ.toFloat + size,
            r, g, b, a)
        }

        bufferSource.endBatch(RENDER_TYPE)
        stack.popPose()

      case _ =>
    }
  }

  private def vertex(stack: PoseStack, consumer: com.mojang.blaze3d.vertex.VertexConsumer,
                     x: Float, y: Float, z: Float, r: Int, g: Int, b: Int, a: Int): Unit = {
    consumer.addVertex(stack.last.pose, x, y, z).setColor(r, g, b, a)
  }

  private def drawBox(stack: PoseStack, consumer: com.mojang.blaze3d.vertex.VertexConsumer,
                      minX: Float, minY: Float, minZ: Float,
                      maxX: Float, maxY: Float, maxZ: Float,
                      r: Int, g: Int, b: Int, a: Int): Unit = {
    def v(x: Float, y: Float, z: Float) = vertex(stack, consumer, x, y, z, r, g, b, a)

    // Bottom
    v(minX, minY, minZ); v(minX, minY, maxZ); v(maxX, minY, maxZ); v(maxX, minY, minZ)
    // Front
    v(minX, minY, minZ); v(maxX, minY, minZ); v(maxX, maxY, minZ); v(minX, maxY, minZ)
    // Top
    v(maxX, maxY, minZ); v(maxX, maxY, maxZ); v(minX, maxY, maxZ); v(minX, maxY, minZ)
    // Back
    v(maxX, maxY, maxZ); v(maxX, minY, maxZ); v(minX, minY, maxZ); v(minX, maxY, maxZ)
    // Left
    v(minX, minY, minZ); v(minX, maxY, minZ); v(minX, maxY, maxZ); v(minX, minY, maxZ)
    // Right
    v(maxX, minY, minZ); v(maxX, minY, maxZ); v(maxX, maxY, maxZ); v(maxX, maxY, minZ)
  }
}
