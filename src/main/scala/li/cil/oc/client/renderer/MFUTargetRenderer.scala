package li.cil.oc.client.renderer

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexConsumer
import li.cil.oc.{api, Constants}
import li.cil.oc.common.datacomponents.{MFCoords, OCComponents}
import li.cil.oc.util.{BlockPosition, RenderState}
import li.cil.oc.util.ExtendedDataComponentHolder._
import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionHand
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Matrix4f

object MFUTargetRenderer {
  private val (drawRed, drawGreen, drawBlue) = (0.0f, 1.0f, 0.0f)

  private lazy val mfu = api.Items.get(Constants.ItemName.MFU)

  @SubscribeEvent
  def onRenderWorldLastEvent(e: RenderLevelStageEvent): Unit = {
    val mc = Minecraft.getInstance
    val player = mc.player
    if (player == null) return
    player.getItemInHand(InteractionHand.MAIN_HAND) match {
      case stack: ItemStack if api.Items.get(stack) == mfu && stack.has(OCComponents.MF_COORD) =>
        for(MFCoords(dimension, blockPos, side) <- stack.getComponent(OCComponents.MF_COORD)) {
          if (!player.level.dimension.location.equals(dimension)) return
          val (x, y, z) = (blockPos.getX, blockPos.getY, blockPos.getZ)
          if (player.distanceToSqr(x, y, z) > 64 * 64) return

          val bounds = BlockPosition(x, y, z).bounds.inflate(0.1, 0.1, 0.1)

          RenderState.checkError(getClass.getName + ".onRenderWorldLastEvent: entering (aka: wasntme)")

          val poseStack = e.getPoseStack
          poseStack.pushPose()
          val camPos = Minecraft.getInstance.gameRenderer.getMainCamera.getPosition
          poseStack.translate(-camPos.x, -camPos.y, -camPos.z)

          RenderSystem.disableDepthTest() // Default state for depth test is disabled, but it's enabled here so we have to change it manually.
          val buffer = Minecraft.getInstance.renderBuffers.bufferSource
          drawBox(poseStack.last.pose, buffer.getBuffer(RenderTypes.MFU_LINES), bounds.minX.toFloat, bounds.minY.toFloat, bounds.minZ.toFloat,
            bounds.maxX.toFloat, bounds.maxY.toFloat, bounds.maxZ.toFloat, drawRed, drawGreen, drawBlue)
          drawFace(poseStack.last.pose, buffer.getBuffer(RenderTypes.MFU_QUADS), bounds.minX.toFloat, bounds.minY.toFloat, bounds.minZ.toFloat,
            bounds.maxX.toFloat, bounds.maxY.toFloat, bounds.maxZ.toFloat, side, drawRed, drawGreen, drawBlue)
          buffer.endBatch()

          poseStack.popPose()

          RenderState.checkError(getClass.getName + ".onRenderWorldLastEvent: leaving")
        }
      case _ => // Nothing
    }
  }

  def drawBox(matrix: Matrix4f, builder: VertexConsumer, minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float, r: Float, g: Float, b: Float) = {
    // Bottom square.
    builder.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, 0.5f)

    // Vertical bars.
    builder.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, 0.5f)

    // Top square.
    builder.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, 0.5f)
    builder.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, 0.5f)
  }

  private def drawFace(matrix: Matrix4f, builder: VertexConsumer, minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float, side: Direction, r: Float, g: Float, b: Float): Unit = {
    side match {
      case Direction.DOWN =>
        builder.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, 0.25f)
      case Direction.UP =>
        builder.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, 0.25f)
      case Direction.NORTH =>
        builder.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, 0.25f)
      case Direction.SOUTH =>
        builder.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, 0.25f)
      case Direction.EAST =>
        builder.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, 0.25f)
      case Direction.WEST =>
        builder.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, 0.25f)
        builder.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, 0.25f)
      //case _ => // WTF? (unreachable)
    }
  }

}
