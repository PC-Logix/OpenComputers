package li.cil.oc.client.renderer.tileentity

import com.google.common.base.Strings
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.driver.item.UpgradeRenderer
import li.cil.oc.api.driver.item.UpgradeRenderer.MountPointName
import li.cil.oc.api.event.RobotRenderEvent
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.common.EventHandler
import li.cil.oc.common.RobotFlags
import li.cil.oc.common.blockentity
import li.cil.oc.util.RenderState
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer => TileEntityRenderer}
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.item.{BlockItem, ItemDisplayContext, ItemStack, Items}
import net.minecraft.core.{Direction, Vec3i}
import net.minecraft.world.phys.AABB
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.common.NeoForge

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object RobotRenderer extends BlockEntityRendererProvider[blockentity.RobotProxy] {
  val RainbowFlag = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "rainbow_flag")
  val TransFlag = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "trans_flag")

  override def create(ctx: BlockEntityRendererProvider.Context): RobotRenderer =
    new RobotRenderer()

  private val instance = new RobotRenderer()

  def renderChassis(
                      stack: PoseStack,
                      buffer: MultiBufferSource,
                      light: Int,
                      offset: Double = 0,
                      isRunningOverride: Boolean = false,
                      flag: Option[ResourceLocation] = None
                    ): Unit = instance.renderChassis(stack, buffer, light, null, offset, isRunningOverride, flag)
}

class RobotRenderer extends TileEntityRenderer[blockentity.RobotProxy] {
  // Robot rendering extends outside the proxy block's normal one-block bounds
  // while hovering and moving, so use an expanded culling box.
  override def getRenderBoundingBox(entity: blockentity.RobotProxy): AABB =
    new AABB(entity.getBlockPos).inflate(0.5)

  private val mountPoints = new Array[RobotRenderEvent.MountPoint](7)

  private val slotNameMapping = Map(
    UpgradeRenderer.MountPointName.TopLeft     -> 0,
    UpgradeRenderer.MountPointName.TopRight    -> 1,
    UpgradeRenderer.MountPointName.TopBack     -> 2,
    UpgradeRenderer.MountPointName.BottomLeft  -> 3,
    UpgradeRenderer.MountPointName.BottomRight -> 4,
    UpgradeRenderer.MountPointName.BottomBack  -> 5,
    UpgradeRenderer.MountPointName.BottomFront -> 6
  )

  for ((name, index) <- slotNameMapping) {
    mountPoints(index) = new RobotRenderEvent.MountPoint(name)
  }

  private val size = 0.4f
  private val l    = 0.5f - size
  private val h    = 0.5f + size
  private val gap  = 1.0f / 28.0f
  private val gt   = 0.5f + gap
  private val gb   = 0.5f - gap

  private def drawFlag(
                        stack: PoseStack,
                        buffer: MultiBufferSource,
                        light: Int,
                        flag: ResourceLocation
                      ): Unit = {
    val definition = RobotFlags.byId(flag) match {
      case Some(value) => value
      case _ => return
    }
    val renderType = RenderTypes.robotFlag(definition.id)
    val flagHeight = definition.height

    val r = buffer.getBuffer(renderType)
    val x = 2f / 16f
    val flagBottom = 13.5f / 16f
    val flagTop = (13.5f + flagHeight / 2f) / 16f
    val flagFront = 10.5f / 16f
    val flagBack = 15.5f / 16f
    val poleBottom = 10.5f / 16f
    val poleTop = 13.5f / 16f
    val poleBack = 11f / 16f

    @inline def lu(value: Float) = value / 16f
    @inline def lv(value: Float) = value / 16f

    def quad(y0: Float, y1: Float, z0: Float, z1: Float,
             u0: Float, v0: Float, u1: Float, v1: Float): Unit = {
      r.addVertex(stack.last.pose(), x, y0, z0).setColor(0xFF, 0xFF, 0xFF, 0xFF).setUv(lu(u0), lv(v1)).setLight(light).setNormal(stack.last, 1, 0, 0)
      r.addVertex(stack.last.pose(), x, y1, z0).setColor(0xFF, 0xFF, 0xFF, 0xFF).setUv(lu(u0), lv(v0)).setLight(light).setNormal(stack.last, 1, 0, 0)
      r.addVertex(stack.last.pose(), x, y1, z1).setColor(0xFF, 0xFF, 0xFF, 0xFF).setUv(lu(u1), lv(v0)).setLight(light).setNormal(stack.last, 1, 0, 0)
      r.addVertex(stack.last.pose(), x, y0, z1).setColor(0xFF, 0xFF, 0xFF, 0xFF).setUv(lu(u1), lv(v1)).setLight(light).setNormal(stack.last, 1, 0, 0)
    }

    stack.pushPose()
    stack.translate(x, 11f / 16f, 10.75f / 16f)
    stack.mulPose(Axis.XP.rotationDegrees(22.5f))
    stack.translate(-x, -11f / 16f, -10.75f / 16f)

    quad(flagBottom, flagTop, flagFront, flagBack, 0, 0, 7, flagHeight)
    quad(poleBottom, poleTop, flagFront, poleBack, 13, 0, 14, 6)

    stack.popPose()
  }

  private def drawTop(
                       stack: PoseStack,
                       buffer: MultiBufferSource,
                       light: Int,
                       red: Int, green: Int, blue: Int
                     ): Unit = {
    val r = buffer.getBuffer(RenderTypes.ROBOT_CHASSIS)

    r.addVertex(stack.last.pose(), 0.5f, 1, 0.5f)    .setColor(red, green, blue, 0xFF).setUv(0.25f, 0.25f).setLight(light).setNormal(stack.last(), 0.0f, 0.196116135f, 0.980580676f)
    r.addVertex(stack.last.pose(), l, gt, h)           .setColor(red, green, blue, 0xFF).setUv(0, 0.5f)    .setLight(light).setNormal(stack.last(), 0.0f, 0.196116135f, 0.980580676f)
    r.addVertex(stack.last.pose(), h, gt, h)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 0.5f) .setLight(light).setNormal(stack.last(), 0.0f, 0.196116135f, 0.980580676f)

    r.addVertex(stack.last.pose(), 0.5f, 1, 0.5f)    .setColor(red, green, blue, 0xFF).setUv(0.25f, 0.25f).setLight(light).setNormal(stack.last(), 0.0f, 0.196116135f, 0.980580676f)
    r.addVertex(stack.last.pose(), h, gt, h)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 0.5f) .setLight(light).setNormal(stack.last(), 0.0f, 0.196116135f, 0.980580676f)
    r.addVertex(stack.last.pose(), h, gt, l)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 0)    .setLight(light).setNormal(stack.last(), 0.980580676f, 0.196116135f, 0.0f)

    r.addVertex(stack.last.pose(), 0.5f, 1, 0.5f)    .setColor(red, green, blue, 0xFF).setUv(0.25f, 0.25f).setLight(light).setNormal(stack.last(), 0.0f, 0.196116135f, 0.980580676f)
    r.addVertex(stack.last.pose(), h, gt, l)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 0)    .setLight(light).setNormal(stack.last(), 0.980580676f, 0.196116135f, 0.0f)
    r.addVertex(stack.last.pose(), l, gt, l)           .setColor(red, green, blue, 0xFF).setUv(0, 0)       .setLight(light).setNormal(stack.last(), 0.0f, 0.196116135f, -0.980580676f)

    r.addVertex(stack.last.pose(), 0.5f, 1, 0.5f)    .setColor(red, green, blue, 0xFF).setUv(0.25f, 0.25f).setLight(light).setNormal(stack.last(), 0.0f, 0.196116135f, 0.980580676f)
    r.addVertex(stack.last.pose(), l, gt, l)           .setColor(red, green, blue, 0xFF).setUv(0, 0)       .setLight(light).setNormal(stack.last(), 0.0f, 0.196116135f, -0.980580676f)
    r.addVertex(stack.last.pose(), l, gt, h)           .setColor(red, green, blue, 0xFF).setUv(0, 0.5f)    .setLight(light).setNormal(stack.last(), -0.980580676f, 0.196116135f, 0.0f)

    r.addVertex(stack.last.pose(), l, gt, h)           .setColor(red, green, blue, 0xFF).setUv(0, 1)       .setLight(light).setNormal(stack.last(), 0.0f, -1.0f, 0.0f)
    r.addVertex(stack.last.pose(), l, gt, l)           .setColor(red, green, blue, 0xFF).setUv(0, 0.5f)    .setLight(light).setNormal(stack.last(), 0.0f, -1.0f, 0.0f)
    r.addVertex(stack.last.pose(), h, gt, l)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 0.5f) .setLight(light).setNormal(stack.last(), 0.0f, -1.0f, 0.0f)

    r.addVertex(stack.last.pose(), l, gt, h)           .setColor(red, green, blue, 0xFF).setUv(0, 1)       .setLight(light).setNormal(stack.last(), 0.0f, -1.0f, 0.0f)
    r.addVertex(stack.last.pose(), h, gt, l)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 0.5f) .setLight(light).setNormal(stack.last(), 0.0f, -1.0f, 0.0f)
    r.addVertex(stack.last.pose(), h, gt, h)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 1)    .setLight(light).setNormal(stack.last(), 0.0f, -1.0f, 0.0f)
  }

  private def drawBottom(
                          stack: PoseStack,
                          buffer: MultiBufferSource,
                          light: Int,
                          red: Int, green: Int, blue: Int
                        ): Unit = {
    val r = buffer.getBuffer(RenderTypes.ROBOT_CHASSIS)

    r.addVertex(stack.last.pose(), 0.5f, 0.03f, 0.5f).setColor(red, green, blue, 0xFF).setUv(0.75f, 0.25f).setLight(light).setNormal(stack.last(), 0.0f, -0.196116135f, 0.980580676f)
    r.addVertex(stack.last.pose(), l, gb, l)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 0)    .setLight(light).setNormal(stack.last(), 0.0f, -0.196116135f, 0.980580676f)
    r.addVertex(stack.last.pose(), h, gb, l)           .setColor(red, green, blue, 0xFF).setUv(1, 0)       .setLight(light).setNormal(stack.last(), 0.0f, -0.196116135f, 0.980580676f)

    r.addVertex(stack.last.pose(), 0.5f, 0.03f, 0.5f).setColor(red, green, blue, 0xFF).setUv(0.75f, 0.25f).setLight(light).setNormal(stack.last(), 0.0f, -0.196116135f, 0.980580676f)
    r.addVertex(stack.last.pose(), h, gb, l)           .setColor(red, green, blue, 0xFF).setUv(1, 0)       .setLight(light).setNormal(stack.last(), 0.0f, -0.196116135f, 0.980580676f)
    r.addVertex(stack.last.pose(), h, gb, h)           .setColor(red, green, blue, 0xFF).setUv(1, 0.5f)    .setLight(light).setNormal(stack.last(), 0.980580676f, -0.196116135f, 0.0f)

    r.addVertex(stack.last.pose(), 0.5f, 0.03f, 0.5f).setColor(red, green, blue, 0xFF).setUv(0.75f, 0.25f).setLight(light).setNormal(stack.last(), 0.0f, -0.196116135f, 0.980580676f)
    r.addVertex(stack.last.pose(), h, gb, h)           .setColor(red, green, blue, 0xFF).setUv(1, 0.5f)    .setLight(light).setNormal(stack.last(), 0.980580676f, -0.196116135f, 0.0f)
    r.addVertex(stack.last.pose(), l, gb, h)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 0.5f) .setLight(light).setNormal(stack.last(), 0.0f, -0.196116135f, -0.980580676f)

    r.addVertex(stack.last.pose(), 0.5f, 0.03f, 0.5f).setColor(red, green, blue, 0xFF).setUv(0.75f, 0.25f).setLight(light).setNormal(stack.last(), 0.0f, -0.196116135f, 0.980580676f)
    r.addVertex(stack.last.pose(), l, gb, h)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 0.5f) .setLight(light).setNormal(stack.last(), 0.0f, -0.196116135f, -0.980580676f)
    r.addVertex(stack.last.pose(), l, gb, l)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 0)    .setLight(light).setNormal(stack.last(), -0.980580676f, -0.196116135f, 0.0f)

    r.addVertex(stack.last.pose(), l, gb, l)           .setColor(red, green, blue, 0xFF).setUv(0, 0.5f)    .setLight(light).setNormal(stack.last(), 0.0f, 1.0f, 0.0f)
    r.addVertex(stack.last.pose(), l, gb, h)           .setColor(red, green, blue, 0xFF).setUv(0, 1)       .setLight(light).setNormal(stack.last(), 0.0f, 1.0f, 0.0f)
    r.addVertex(stack.last.pose(), h, gb, h)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 1)    .setLight(light).setNormal(stack.last(), 0.0f, 1.0f, 0.0f)

    r.addVertex(stack.last.pose(), l, gb, l)           .setColor(red, green, blue, 0xFF).setUv(0, 0.5f)    .setLight(light).setNormal(stack.last(), 0.0f, 1.0f, 0.0f)
    r.addVertex(stack.last.pose(), h, gb, h)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 1)    .setLight(light).setNormal(stack.last(), 0.0f, 1.0f, 0.0f)
    r.addVertex(stack.last.pose(), h, gb, l)           .setColor(red, green, blue, 0xFF).setUv(0.5f, 0.5f) .setLight(light).setNormal(stack.last(), 0.0f, 1.0f, 0.0f)
  }

  def resetMountPoints(running: Boolean): Unit = {
    val offset = if (running) 0 else -0.06f

    mountPoints(0).offset.set(0, 0.2f, 0.24f)
    mountPoints(0).rotation.set(0, 1, 0, 90)
    mountPoints(1).offset.set(0, 0.2f, 0.24f)
    mountPoints(1).rotation.set(0, 1, 0, -90)
    mountPoints(2).offset.set(0, 0.2f, 0.24f)
    mountPoints(2).rotation.set(0, 1, 0, 180)
    mountPoints(3).offset.set(0, -0.2f - offset, 0.24f)
    mountPoints(3).rotation.set(0, 1, 0, 90)
    mountPoints(4).offset.set(0, -0.2f - offset, 0.24f)
    mountPoints(4).rotation.set(0, 1, 0, -90)
    mountPoints(5).offset.set(0, -0.2f - offset, 0.24f)
    mountPoints(5).rotation.set(0, 1, 0, 180)
    mountPoints(6).offset.set(0, -0.2f - offset, 0.24f)
    mountPoints(6).rotation.set(0, 1, 0, 0)
  }

  def renderChassis(
                     stack: PoseStack,
                     buffer: MultiBufferSource,
                     light: Int,
                     robot: blockentity.Robot = null,
                     offset: Double = 0,
                     isRunningOverride: Boolean = false,
                     flag: Option[ResourceLocation] = None
                   ): Unit = {
    val isRunning = if (robot == null) isRunningOverride else robot.isRunning

    val size  = 0.3f
    val l     = 0.5f - size
    val h     = 0.5f + size
    val vStep = 1.0f / 32.0f

    val offsetV = ((offset - offset.toInt) * 16).toInt * vStep
    val (u0, u1, v0, v1) = {
      if (isRunning)
        (0.5f, 1f, 0.5f + offsetV, 0.5f + vStep + offsetV)
      else
        (0.25f - vStep, 0.25f + vStep, 0.75f - vStep, 0.75f + vStep)
    }

    resetMountPoints(robot != null && robot.isRunning)
    val event = new RobotRenderEvent(robot, mountPoints)
    NeoForge.EVENT_BUS.post(event)
    if (!event.isCanceled) {
      val color        = event.getColorMultiplier
      val (cr, cg, cb) = ((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF)
      if (!isRunning) stack.translate(0, -2 * gap, 0)
      drawBottom(stack, buffer, light, cr, cg, cb)
      if (!isRunning) stack.translate(0, -2 * gap, 0)
      drawTop(stack, buffer, light, cr, cg, cb)

      (if (robot != null) robot.info.flag else flag).foreach(drawFlag(stack, buffer, light, _))

      if (isRunning) {
        val lightColor = if (event.lightColor < 0) {
          if (robot != null && robot.info != null) robot.info.lightColor else 0xF23030
        } else event.lightColor & 0xFFFFFF
        val red   = (lightColor >>> 16) & 0xFF
        val green = (lightColor >>> 8)  & 0xFF
        val blue  = (lightColor >>> 0)  & 0xFF

        val r = buffer.getBuffer(RenderTypes.ROBOT_LIGHT)
        r.addVertex(stack.last.pose(), l, gt, l).setColor(red, green, blue, 0xFF).setUv(u0, v0).setLight(light).setNormal(stack.last(), -1, 0, 0)
        r.addVertex(stack.last.pose(), l, gb, l).setColor(red, green, blue, 0xFF).setUv(u0, v1).setLight(light).setNormal(stack.last(), -1, 0, 0)
        r.addVertex(stack.last.pose(), l, gb, h).setColor(red, green, blue, 0xFF).setUv(u1, v1).setLight(light).setNormal(stack.last(), -1, 0, 0)
        r.addVertex(stack.last.pose(), l, gt, h).setColor(red, green, blue, 0xFF).setUv(u1, v0).setLight(light).setNormal(stack.last(), -1, 0, 0)

        r.addVertex(stack.last.pose(), l, gt, h).setColor(red, green, blue, 0xFF).setUv(u0, v0).setLight(light).setNormal(stack.last(), 0, 0, 1)
        r.addVertex(stack.last.pose(), l, gb, h).setColor(red, green, blue, 0xFF).setUv(u0, v1).setLight(light).setNormal(stack.last(), 0, 0, 1)
        r.addVertex(stack.last.pose(), h, gb, h).setColor(red, green, blue, 0xFF).setUv(u1, v1).setLight(light).setNormal(stack.last(), 0, 0, 1)
        r.addVertex(stack.last.pose(), h, gt, h).setColor(red, green, blue, 0xFF).setUv(u1, v0).setLight(light).setNormal(stack.last(), 0, 0, 1)

        r.addVertex(stack.last.pose(), h, gt, h).setColor(red, green, blue, 0xFF).setUv(u0, v0).setLight(light).setNormal(stack.last(), 1, 0, 0)
        r.addVertex(stack.last.pose(), h, gb, h).setColor(red, green, blue, 0xFF).setUv(u0, v1).setLight(light).setNormal(stack.last(), 1, 0, 0)
        r.addVertex(stack.last.pose(), h, gb, l).setColor(red, green, blue, 0xFF).setUv(u1, v1).setLight(light).setNormal(stack.last(), 1, 0, 0)
        r.addVertex(stack.last.pose(), h, gt, l).setColor(red, green, blue, 0xFF).setUv(u1, v0).setLight(light).setNormal(stack.last(), 1, 0, 0)

        r.addVertex(stack.last.pose(), h, gt, l).setColor(red, green, blue, 0xFF).setUv(u0, v0).setLight(light).setNormal(stack.last(), 0, 0, -1)
        r.addVertex(stack.last.pose(), h, gb, l).setColor(red, green, blue, 0xFF).setUv(u0, v1).setLight(light).setNormal(stack.last(), 0, 0, -1)
        r.addVertex(stack.last.pose(), l, gb, l).setColor(red, green, blue, 0xFF).setUv(u1, v1).setLight(light).setNormal(stack.last(), 0, 0, -1)
        r.addVertex(stack.last.pose(), l, gt, l).setColor(red, green, blue, 0xFF).setUv(u1, v0).setLight(light).setNormal(stack.last(), 0, 0, -1)
      }
    }
  }

  override def render(
                       proxy: blockentity.RobotProxy,
                       f: Float,
                       matrix: PoseStack,
                       buffer: MultiBufferSource,
                       light: Int,
                       overlay: Int
                     ): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    val robot     = proxy.robot
    val worldTime = proxy.getLevel.getGameTime + f

    matrix.pushPose()
    matrix.translate(0.5, 0.5, 0.5)

    if (robot.proxy != proxy) {
      matrix.translate((robot.proxy.x - proxy.x).toDouble, (robot.proxy.y - proxy.y).toDouble, (robot.proxy.z - proxy.z).toDouble)
    }

    if (robot.isAnimatingMove) {
      val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toDouble
      val delta = (robot.moveFrom.get: Vec3i).subtract(robot.getBlockPos)
      matrix.translate(delta.getX * remaining, delta.getY * remaining, delta.getZ * remaining)
    }

    val timeJitter = robot.hashCode ^ 0xFF
    val hover =
      if (robot.isRunning) (Math.sin(timeJitter + worldTime / 20.0) * 0.03).toFloat
      else -0.03f
    matrix.translate(0, hover, 0)

    matrix.pushPose()

    if (robot.isAnimatingTurn) {
      val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toFloat
      val axis      = if (robot.turnAxis < 0) Axis.YN else Axis.YP
      matrix.mulPose(axis.rotationDegrees(90 * remaining))
    }

    robot.yaw match {
      case Direction.WEST  => matrix.mulPose(Axis.YP.rotationDegrees(-90))
      case Direction.NORTH => matrix.mulPose(Axis.YP.rotationDegrees(180))
      case Direction.EAST  => matrix.mulPose(Axis.YP.rotationDegrees(90))
      case _               =>
    }

    matrix.translate(-0.5f, -0.5f, -0.5f)

    val offset = timeJitter + worldTime / 20.0
    renderChassis(matrix, buffer, light, robot, offset)

    val pos  = proxy.getBlockPos
    val dist = Minecraft.getInstance.player.position.distanceToSqr(pos.getX + 0.5, pos.getY + 0.5, pos.getZ + 0.5)
    if (!robot.renderingErrored && dist < 24 * 24) {
      val itemRenderer = Minecraft.getInstance.getItemRenderer
      StackOption(robot.getItem(0)) match {
        case SomeStack(stack) =>
          matrix.pushPose()
          try {
            RenderState.mirrorScale(matrix, 1, -1, -1)
            matrix.translate(0, -8 * 0.0625F - 0.0078125F, -0.5F)

            if (robot.isAnimatingSwing) {
              val wantedTicksPerCycle = 10
              val cycles              = math.max(robot.animationTicksTotal / wantedTicksPerCycle, 1)
              val ticksPerCycle       = robot.animationTicksTotal / cycles
              val remaining           = (robot.animationTicksLeft - f) / ticksPerCycle.toDouble
              matrix.mulPose(Axis.XP.rotationDegrees((Math.sin((remaining - remaining.toInt) * Math.PI) * 45).toFloat))
            }

            val item = stack.getItem
            if (item.isInstanceOf[BlockItem]) {
              matrix.mulPose(Axis.XP.rotationDegrees(180.0F))
              matrix.mulPose(Axis.YP.rotationDegrees(90.0F))
              val scale = 0.625F
              matrix.scale(scale, scale, scale)
            } else if (item == Items.BOW) {
              matrix.translate(0, -3f / 16f, -0.125F)
              matrix.mulPose(Axis.ZP.rotationDegrees(170.0F))
              val scale = 0.625F
              matrix.scale(scale, scale, scale)
            } else {
              matrix.translate(1f / 16f, 1f / 16f, -2f / 16f)
              val scale = 0.625F
              matrix.scale(scale, scale, scale)
              matrix.mulPose(Axis.ZP.rotationDegrees(180.0F))
            }

            itemRenderer.renderStatic(
              Minecraft.getInstance.player,
              stack,
              ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
              false,
              matrix,
              buffer,
              proxy.getLevel,
              light,
              overlay,
              proxy.getBlockPos.asLong.toInt
            )
          } catch {
            case e: Throwable =>
              OpenComputers.log.warn("Failed rendering equipped item.", e)
              robot.renderingErrored = true
          }
          matrix.popPose()
        case _ =>
      }

      lazy val availableSlots    = slotNameMapping.keys.to(mutable.Set).asJava
      lazy val wildcardRenderers = mutable.Buffer.empty[(ItemStack, UpgradeRenderer)]
      lazy val slotMapping       = Array.fill(mountPoints.length)(null: (ItemStack, UpgradeRenderer))

      val renderers = (robot.componentSlotRange ++ robot.containerSlots).map(robot.getItem).collect {
        case stack if !stack.isEmpty && stack.getItem.isInstanceOf[UpgradeRenderer] =>
          (stack, stack.getItem.asInstanceOf[UpgradeRenderer])
      }

      for ((stack, renderer) <- renderers) {
        val preferredSlot = renderer.computePreferredMountPoint(stack, robot, availableSlots)
        if (availableSlots.remove(preferredSlot)) {
          slotMapping(slotNameMapping(preferredSlot)) = (stack, renderer)
        } else if (preferredSlot == MountPointName.Any) {
          wildcardRenderers += ((stack, renderer))
        }
      }

      var firstEmpty = slotMapping.indexOf(null)
      for (entry <- wildcardRenderers if firstEmpty >= 0) {
        slotMapping(firstEmpty) = entry
        firstEmpty = slotMapping.indexOf(null)
      }

      for ((info, mountPoint) <- slotMapping.lazyZip(mountPoints) if info != null) try {
        val (stack, renderer) = info
        matrix.pushPose()
        matrix.translate(0.5f, 0.5f, 0.5f)
        renderer.render(matrix, buffer, light, stack, mountPoint, robot, f)
        matrix.popPose()
      } catch {
        case e: Throwable =>
          OpenComputers.log.warn("Failed rendering equipped upgrade.", e)
          robot.renderingErrored = true
      }
    }
    matrix.popPose()

    val name = robot.name
    if (
      Settings.get.robotLabels &&
        !Strings.isNullOrEmpty(name) &&
        dist < 64 * 64
    ) {
      val font      = Minecraft.getInstance.font
      val scale     = 1.6f / 60f
      val width     = font.width(name)
      val halfWidth = width / 2
      val bgColor   = (255f * Minecraft.getInstance.options.getBackgroundOpacity(0.25F)).asInstanceOf[Int] << 24

      matrix.translate(0, 0.8, 0)
      matrix.mulPose(Minecraft.getInstance.getEntityRenderDispatcher.cameraOrientation)
      RenderState.mirrorScale(matrix, -scale, -scale, scale)

      font.drawInBatch(
        (if (EventHandler.isItTime) ChatFormatting.OBFUSCATED.toString else "") + name,
        -halfWidth.toFloat, 0f,
        -1,
        false,
        matrix.last.pose(),
        buffer,
        Font.DisplayMode.NORMAL,
        bgColor,
        light
      )
    }

    matrix.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
