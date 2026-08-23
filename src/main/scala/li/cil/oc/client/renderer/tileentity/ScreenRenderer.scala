package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexConsumer
import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.client.PacketSender
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.common.component.{TextBuffer => ComponentTextBuffer}
import li.cil.oc.common.blockentity.Screen
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionHand
import net.minecraft.core.Direction
import com.mojang.math.Axis
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer => TileEntityRenderer}
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.level.block.Block
import net.neoforged.api.distmarker.{Dist, OnlyIn}
import net.minecraft.world.phys.{AABB, Vec3}
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.{Matrix4f, Vector4f}
import org.lwjgl.glfw.GLFW

import scala.collection.mutable

object ScreenRenderer extends BlockEntityRendererProvider[Screen] {
  private[tileentity] case class HoloHit(t: Double, x: Float, y: Float)

  private val queuedHoloScreens = mutable.LinkedHashMap.empty[Long, Screen]
  private val renderedHoloScreens = mutable.LinkedHashMap.empty[Long, Screen]
  private val lateRenderer = new ScreenRenderer()

  override def create(ctx: BlockEntityRendererProvider.Context): ScreenRenderer =
    new ScreenRenderer()

  def queueHolo(screen: Screen): Unit =
    queuedHoloScreens.update(screen.getBlockPos.asLong, screen)

  @SubscribeEvent
  def onRenderLevelStage(e: RenderLevelStageEvent): Unit = {
    if (e.getStage == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
      queuedHoloScreens.clear()
      return
    }
    if (e.getStage != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return

    val mc = Minecraft.getInstance
    if (mc.level == null || mc.player == null) {
      queuedHoloScreens.clear()
      renderedHoloScreens.clear()
      return
    }

    val stack = e.getPoseStack
    val camera = e.getCamera.getPosition
    val buffer = mc.renderBuffers.bufferSource
    val screens = queuedHoloScreens.values.filter(screen => screen.getLevel == mc.level && !screen.isRemoved).toVector

    renderedHoloScreens.clear()
    screens.foreach(screen => renderedHoloScreens.update(screen.getBlockPos.asLong, screen))
    if (screens.isEmpty) {
      queuedHoloScreens.clear()
      return
    }

    stack.pushPose()
    stack.translate(-camera.x, -camera.y, -camera.z)
    for (screen <- screens) {
      val pos = screen.getBlockPos
      stack.pushPose()
      stack.translate(pos.getX.toFloat, pos.getY.toFloat, pos.getZ.toFloat)
      lateRenderer.renderLateHolo(screen, stack, buffer)
      stack.popPose()
    }
    buffer.endBatch()
    stack.popPose()

    queuedHoloScreens.clear()
  }

  @SubscribeEvent
  def onInteractionKeyMapping(e: InputEvent.InteractionKeyMappingTriggered): Unit = {
    if (!e.isUseItem || e.getHand != InteractionHand.MAIN_HAND || renderedHoloScreens.isEmpty) return

    val mc = Minecraft.getInstance
    val player = mc.player
    if (mc.level == null || player == null || mc.screen != null || mc.gameMode == null) return

    val partialTicks = mc.getTimer.getGameTimeDeltaTicks
    val start = player.getEyePosition(partialTicks)
    val reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue
    val end = start.add(player.getViewVector(partialTicks).scale(reach))

    val hit = renderedHoloScreens.values.
      filter(screen => screen.getLevel == mc.level && !screen.isRemoved && screen.isInstanceOf[li.cil.oc.common.blockentity.HoloScreen]).
      flatMap(screen => lateRenderer.projectedHoloHit(screen, start, end).map(hit => (hit, screen))).
      toVector.
      sortBy(_._1.t).
      headOption

    hit.foreach {
      case (holoHit, screen: li.cil.oc.common.blockentity.HoloScreen) if isShiftHeld(mc) =>
        PacketSender.sendHoloScreenResize(screen, lateRenderer.resizeSideForHit(screen, holoHit.x, holoHit.y))
        e.setSwingHand(false)
        e.setCanceled(true)
      case (_, screen) if screen.hasKeyboard =>
          screen.getBlockState.getBlock match {
            case block: li.cil.oc.common.block.Screen =>
              val heldItem = player.getItemInHand(InteractionHand.MAIN_HAND)
              if (block.rightClick(mc.level, screen.getBlockPos, player, InteractionHand.MAIN_HAND, heldItem, screen.facing, 0.5f, 0.5f, 0.5f, force = true)) {
                e.setSwingHand(false)
                e.setCanceled(true)
              }
            case _ =>
          }
      case _ =>
    }
  }

  private def isShiftHeld(mc: Minecraft): Boolean = {
    val window = mc.getWindow.getWindow
    GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
      GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS
  }
}

class ScreenRenderer extends TileEntityRenderer[Screen] {
  private val maxRenderDistanceSq = Settings.get.maxScreenTextRenderDistance * Settings.get.maxScreenTextRenderDistance
  private val fadeDistanceSq      = Settings.get.screenTextFadeStartDistance * Settings.get.screenTextFadeStartDistance
  private val fadeRatio           = 1.0 / (maxRenderDistanceSq - fadeDistanceSq)

  private var screen: Screen = null

  private case class HologramColor(red: Int, green: Int, blue: Int)

  private case class HologramSurfaceLayout(left: Float, top: Float, depth: Float, width: Int, height: Int)

  private case class ProjectionBeamLayout(anchor: Float, depth: Float, projectorEdgeOffset: Float, width: Int)

  private case class MonitorContentLayout(border: Float, liftFromSurface: Float)

  private case class HologramLayout(surface: HologramSurfaceLayout, beam: Option[ProjectionBeamLayout], content: MonitorContentLayout)

  override def getRenderBoundingBox(screen: Screen): AABB = screen.getRenderBoundingBox

  override def render(
                       screen: Screen,
                       dt: Float,
                       stack: PoseStack,
                       buffer: MultiBufferSource,
                       light: Int,
                       overlay: Int
                     ): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    this.screen = screen
    if (!screen.isOrigin) return

    val distance = playerDistanceSq() / math.min(screen.width, screen.height)
    if (distance > maxRenderDistanceSq) return

    if (screen.isInstanceOf[li.cil.oc.common.blockentity.HoloScreen]) {
      ScreenRenderer.queueHolo(screen)
      RenderState.checkError(getClass.getName + ".render: leaving")
      return
    }

    if (!isFlatScreen) {
      val eye_pos   = Minecraft.getInstance.player.getEyePosition(dt)
      val eye_delta = screen.getBlockPos.getY - eye_pos.y

      val screenFacing = screen.facing.getOpposite
      val x            = screen.getBlockPos.getX - eye_pos.x
      val z            = screen.getBlockPos.getZ - eye_pos.z
      if (screenFacing.getStepX * (x + 0.5) + screenFacing.getStepY * (eye_delta + 0.5) + screenFacing.getStepZ * (z + 0.5) < 0) return
    }

    RenderSystem.setShaderColor(1, 1, 1, 1)

    stack.pushPose()
    stack.translate(0.5, 0.5, 0.5)

    RenderState.checkError(getClass.getName + ".render: setup")

    drawOverlay(stack, buffer.getBuffer(RenderTypes.BLOCK_OVERLAY))

    RenderState.checkError(getClass.getName + ".render: overlay")

    val alpha = if (distance > fadeDistanceSq)
      math.max(0, 1 - ((distance - fadeDistanceSq) * fadeRatio).toFloat)
    else 1f

    RenderState.checkError(getClass.getName + ".render: fade")

    if (screen.buffer.isRenderingEnabled) {
      val profiler = Minecraft.getInstance.getProfiler
      profiler.push("opencomputers:screen_text")
      draw(stack, alpha, buffer)
      profiler.pop()
    }

    stack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private def transform(stack: PoseStack): Unit = {
    screen.yaw match {
      case Direction.WEST  => stack.mulPose(Axis.YP.rotationDegrees(-90))
      case Direction.NORTH => stack.mulPose(Axis.YP.rotationDegrees(180))
      case Direction.EAST  => stack.mulPose(Axis.YP.rotationDegrees(90))
      case _               => // No yaw.
    }
    screen.pitch match {
      case Direction.DOWN => stack.mulPose(Axis.XP.rotationDegrees(90))
      case Direction.UP   => stack.mulPose(Axis.XP.rotationDegrees(-90))
      case _              => // No pitch.
    }

    stack.translate(-0.5f, -0.5f, 0.5f)
    stack.translate(0, screen.height.toFloat, 0)
    RenderState.mirrorScale(stack, 1, -1, 1)
  }

  private def transformHolo(stack: PoseStack): Unit = {
    screen.yaw match {
      case Direction.WEST  => stack.mulPose(Axis.YP.rotationDegrees(-90))
      case Direction.NORTH => stack.mulPose(Axis.YP.rotationDegrees(180))
      case Direction.EAST  => stack.mulPose(Axis.YP.rotationDegrees(90))
      case _               => // No yaw.
    }

    stack.translate(-0.5f, -0.5f, 0.5f)
    stack.translate(0, screen.height.toFloat, 0)
    RenderState.mirrorScale(stack, 1, -1, 1)
  }

  private[tileentity] def renderLateHolo(screen: Screen, stack: PoseStack, buffer: MultiBufferSource): Unit = {
    this.screen = screen
    renderHolo(stack, buffer)
  }

  private[tileentity] def projectedHoloHit(screen: Screen, start: Vec3, end: Vec3): Option[ScreenRenderer.HoloHit] = {
    this.screen = screen

    val sx = screen.width
    val sy = screen.height
    val pos = screen.getBlockPos
    val stack = new PoseStack()
    stack.translate(pos.getX.toFloat, pos.getY.toFloat, pos.getZ.toFloat)
    stack.translate(0.5, 0.5, 0.5)
    transformHolo(stack)
    val layout = createHologramLayout(sx, sy)
    stack.translate(layout.surface.left, layout.surface.top, layout.surface.depth)

    val inverse = new Matrix4f(stack.last.pose)
    inverse.invert()

    def transform(v: Vec3) = {
      val local = new Vector4f(v.x.toFloat, v.y.toFloat, v.z.toFloat, 1f)
      inverse.transform(local)
      local
    }

    val a = transform(start)
    val b = transform(end)
    val dz = b.z - a.z
    if (math.abs(dz) < 1.0e-5f) return None

    val t = -a.z / dz
    if (t < 0 || t > 1) return None

    val x = a.x + (b.x - a.x) * t
    val y = a.y + (b.y - a.y) * t
    if (x >= 0 && x <= sx && y >= 0 && y <= sy) Some(ScreenRenderer.HoloHit(t.toDouble, x, y)) else None
  }

  private[tileentity] def resizeSideForHit(screen: Screen, x: Float, y: Float): Direction = {
    val edge = 0.35f
    if (y >= screen.height - edge) Direction.UP
    else if (y <= edge) Direction.DOWN
    else if (x >= screen.width - edge) Direction.EAST
    else if (x <= edge) Direction.WEST
    else Direction.UP
  }

  private def isScreen(stack: ItemStack): Boolean = Block.byItem(stack.getItem) match {
    case _: li.cil.oc.common.block.Screen => true
    case _                                => false
  }

  private def isFlatScreen: Boolean =
    screen.getBlockState.getBlock.isInstanceOf[li.cil.oc.common.block.FlatScreen]

  private def isBackFlatScreen: Boolean =
    screen.getBlockState.getBlock match {
      case flatScreen: li.cil.oc.common.block.FlatScreen => flatScreen.isBack
      case _ => false
    }

  // 1.18.2: IVertexBuilder → VertexConsumer
  private def drawOverlay(matrix: PoseStack, r: VertexConsumer): Unit =
    if (screen.facing == Direction.UP || screen.facing == Direction.DOWN) {
      // 1.18.2: Hand.MAIN_HAND → InteractionHand.MAIN_HAND
      val stack = Minecraft.getInstance.player.getItemInHand(InteractionHand.MAIN_HAND)
      if (!stack.isEmpty) {
        if (Wrench.holdsApplicableWrench(Minecraft.getInstance.player, screen.getBlockPos) || isScreen(stack)) {
          matrix.pushPose()
          transform(matrix)
          matrix.translate(screen.width / 2f - 0.5f, screen.height / 2f - 0.5f, if (isBackFlatScreen) -0.935f else 0.05f)

          val icon = Textures.getSprite(Textures.Block.ScreenUpIndicator)
          r.addVertex(matrix.last.pose, 0, 1, 0).setUv(icon.getU0, icon.getV1)
          r.addVertex(matrix.last.pose, 1, 1, 0).setUv(icon.getU1, icon.getV1)
          r.addVertex(matrix.last.pose, 1, 0, 0).setUv(icon.getU1, icon.getV0)
          r.addVertex(matrix.last.pose, 0, 0, 0).setUv(icon.getU0, icon.getV0)

          matrix.popPose()
        }
      }
    }

  private def draw(stack: PoseStack, alpha: Float, buffer: MultiBufferSource): Unit = {
    RenderState.checkError(getClass.getName + ".draw: entering (aka: wasntme)")

    val sx = screen.width
    val sy = screen.height
    val tw = sx * 16f
    val th = sy * 16f

    transform(stack)

    val border = if (isFlatScreen) 0.5f else 2.25f
    stack.translate(sx * border / tw, sy * border / th, 0)

    val isx = sx - (border / 8)
    val isy = sy - (border / 8)

    val sizeX  = screen.buffer.renderWidth
    val sizeY  = screen.buffer.renderHeight
    val scaleX = isx / sizeX
    val scaleY = isy / sizeY

    if (true) {
      if (scaleX > scaleY) {
        stack.translate(sizeX * 0.5f * (scaleX - scaleY), 0, 0)
        stack.scale(scaleY, scaleY, 1)
      } else {
        stack.translate(0, sizeY * 0.5f * (scaleY - scaleX), 0)
        stack.scale(scaleX, scaleX, 1)
      }
    } else {
      stack.scale(scaleX, scaleY, 1)
    }

    stack.translate(0, 0, (if (isBackFlatScreen) -0.94f else 0) + 0.01f)

    RenderState.checkError(getClass.getName + ".draw: setup")

    screen.buffer match {
      case textBuffer: ComponentTextBuffer => textBuffer.renderText(stack, buffer)
      case _ => screen.buffer.renderText(stack)
    }

    RenderState.checkError(getClass.getName + ".draw: text")
  }

  @OnlyIn(Dist.CLIENT)
  override def shouldRenderOffScreen(screen: Screen): Boolean = screen.isOrigin && (screen.width > 1 || screen.height > 1)

  private def renderHolo(stack: PoseStack, buffer: MultiBufferSource): Unit = {
    RenderSystem.setShaderColor(1, 1, 1, 1)

    stack.pushPose()
    stack.translate(0.5, 0.5, 0.5)
    transformHolo(stack)

    val layout = createHologramLayout(screen.width, screen.height)
    val color = hologramColor
    val quad = buffer.getBuffer(RenderTypes.HOLOGRAM)

    renderProjectionBeam(stack, quad, color, layout.beam)
    renderHologramSurface(stack, quad, color, layout.surface, layout.content, buffer)

    stack.popPose()
  }

  private def createHologramLayout(width: Int, height: Int): HologramLayout = {
    val holo = holoScreen
    val surface = HologramSurfaceLayout(
      left = holo.projectionPlaneLeft(width),
      top = holo.projectionPlaneTop(height),
      depth = holo.projectionDepth,
      width = width,
      height = height
    )
    val beam =
      if (width > 1) {
        Some(ProjectionBeamLayout(
          anchor = holo.projectionBeamAnchor(height),
          depth = holo.projectionDepth,
          projectorEdgeOffset = holo.projectionBeamProjectorEdgeOffset,
          width = width
        ))
      }
      else None
    HologramLayout(surface, beam, MonitorContentLayout(border = 0.5f, liftFromSurface = 0.01f))
  }

  private def hologramColor: HologramColor = {
    val color = screen.getColor match {
      case 11250603 | 4473924 => 0
      case value => value
    }
    HologramColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF)
  }

  private def renderProjectionBeam(stack: PoseStack, quad: VertexConsumer, color: HologramColor, beam: Option[ProjectionBeamLayout]): Unit =
    beam.foreach { layout =>
      stack.pushPose()
      stack.translate(0, layout.anchor, layout.depth)
      drawProjectionQuad(stack, quad, color, alpha = 64, layout)
      stack.popPose()
    }

  private def renderHologramSurface(stack: PoseStack, quad: VertexConsumer, color: HologramColor, surface: HologramSurfaceLayout, content: MonitorContentLayout, buffer: MultiBufferSource): Unit = {
    stack.pushPose()
    stack.translate(surface.left, surface.top, surface.depth)
    drawColoredQuad(stack, quad, color, 128, 0, 0, surface.width.toFloat, surface.height.toFloat)

    stack.translate(0, 0, 0.001f)
    drawColoredQuad(stack, quad, color, 192, 0, 0, 0.01f, surface.height.toFloat)
    drawColoredQuad(stack, quad, color, 192, surface.width - 0.01f, 0, surface.width.toFloat, surface.height.toFloat)
    drawColoredQuad(stack, quad, color, 192, 0, 0, surface.width.toFloat, 0.01f)
    if (surface.width == 1) {
      drawColoredQuad(stack, quad, color, 192, 0, surface.height - 0.01f, surface.width.toFloat, surface.height.toFloat)
    }

    renderMonitorContent(stack, buffer, surface, content)
    stack.popPose()
  }

  private def renderMonitorContent(stack: PoseStack, buffer: MultiBufferSource, surface: HologramSurfaceLayout, content: MonitorContentLayout): Unit = {
    if (screen.buffer.isRenderingEnabled) {
      val border = content.border
      val tw = surface.width * 16f
      val th = surface.height * 16f

      stack.translate(surface.width * border / tw, surface.height * border / th, content.liftFromSurface)

      val isx = surface.width - (border / 8)
      val isy = surface.height - (border / 8)

      val sizeX = screen.buffer.renderWidth
      val sizeY = screen.buffer.renderHeight
      val scaleX = isx / sizeX
      val scaleY = isy / sizeY
      if (scaleX > scaleY) {
        stack.translate(sizeX * 0.5f * (scaleX - scaleY), 0, 0)
        stack.scale(scaleY, scaleY, 1)
      } else {
        stack.translate(0, sizeY * 0.5f * (scaleY - scaleX), 0)
        stack.scale(scaleX, scaleX, 1)
      }

      screen.buffer match {
        case textBuffer: ComponentTextBuffer => textBuffer.renderText(stack, buffer)
        case _ => screen.buffer.renderText(stack)
      }
    }
  }

  private def holoScreen: li.cil.oc.common.blockentity.HoloScreen =
    screen.asInstanceOf[li.cil.oc.common.blockentity.HoloScreen]

  private def drawProjectionQuad(stack: PoseStack, r: VertexConsumer, color: HologramColor, alpha: Int, layout: ProjectionBeamLayout): Unit = {
    r.addVertex(stack.last.pose, 0, layout.projectorEdgeOffset, 0).setColor(color.red, color.green, color.blue, alpha)
    r.addVertex(stack.last.pose, 1, layout.projectorEdgeOffset, 0).setColor(color.red, color.green, color.blue, alpha)
    r.addVertex(stack.last.pose, (layout.width + 1) / 2f, 0, 0).setColor(color.red, color.green, color.blue, alpha)
    r.addVertex(stack.last.pose, -((layout.width - 1) / 2f), 0, 0).setColor(color.red, color.green, color.blue, alpha)
  }

  private def drawColoredQuad(stack: PoseStack, r: VertexConsumer, color: HologramColor, alpha: Int, x1: Float, y1: Float, x2: Float, y2: Float): Unit =
    drawColoredQuad(stack, r, color.red, color.green, color.blue, alpha, x1, y1, x2, y2)

  private def drawColoredQuad(stack: PoseStack, r: VertexConsumer, red: Int, green: Int, blue: Int, alpha: Int, x1: Float, y1: Float, x2: Float, y2: Float): Unit = {
    r.addVertex(stack.last.pose, x1, y2, 0).setColor(red / 255f, green / 255f, blue / 255f, alpha / 255f)
    r.addVertex(stack.last.pose, x2, y2, 0).setColor(red / 255f, green / 255f, blue / 255f, alpha / 255f)
    r.addVertex(stack.last.pose, x2, y1, 0).setColor(red / 255f, green / 255f, blue / 255f, alpha / 255f)
    r.addVertex(stack.last.pose, x1, y1, 0).setColor(red / 255f, green / 255f, blue / 255f, alpha / 255f)
  }

  private def playerDistanceSq(): Double = {
    val player = Minecraft.getInstance.player
    val bounds = getRenderBoundingBox(screen)

    val px = player.getX
    val py = player.getY
    val pz = player.getZ

    val ex = bounds.maxX - bounds.minX
    val ey = bounds.maxY - bounds.minY
    val ez = bounds.maxZ - bounds.minZ
    val cx = bounds.minX + ex * 0.5
    val cy = bounds.minY + ey * 0.5
    val cz = bounds.minZ + ez * 0.5
    val dx = px - cx
    val dy = py - cy
    val dz = pz - cz

    (if (dx < -ex) { val d = dx + ex; d * d }
    else if (dx > ex) { val d = dx - ex; d * d }
    else 0.0) +
      (if (dy < -ey) { val d = dy + ey; d * d }
      else if (dy > ey) { val d = dy - ey; d * d }
      else 0.0) +
      (if (dz < -ez) { val d = dz + ez; d * d }
      else if (dz > ez) { val d = dz - ez; d * d }
      else 0.0)
  }
}
