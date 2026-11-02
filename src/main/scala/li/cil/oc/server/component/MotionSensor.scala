package li.cil.oc.server.component

import java.util
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.util.SideTracker
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.effect.MobEffects

class MotionSensor(val host: EnvironmentHost) extends prefab.AbstractManagedEnvironment with DeviceInfo {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("motion_sensor").
    withConnector().
    create()

  private val radius = 8

  private var sensitivity = 0.4

  private val trackedEntities = mutable.Map.empty[LivingEntity, (Double, Double, Double)]

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Motion sensor",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Blinker M1K0",
    DeviceAttribute.Capacity -> radius.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  private def world = host.getEnvironmentLevel

  private def x = host.xPosition

  private def y = host.yPosition

  private def z = host.zPosition

  private def isServer: Boolean = if (world != null) !world.isClientSide else SideTracker.isServer

  override def canUpdate: Boolean = isServer

  override def update(): Unit = {
    super.update()
    if (world.getGameTime % 10 == 0) {
      // Get a list of all living entities we could possibly detect, using a rough
      // bounding box check, then refining it using the actual distance and an
      // actual visibility check.
      val entities = world.getEntitiesOfClass(classOf[LivingEntity], sensorBounds)
        .map(_.asInstanceOf[LivingEntity])
        .filter(entity => entity.isAlive && isInRange(entity) && isVisible(entity))
        .toSet
      // Get rid of all tracked entities that are no longer visible.
      trackedEntities.retain((key, _) => entities.contains(key))
      // Check for which entities we should generate a signal.
      for (entity <- entities) {
        trackedEntities.get(entity) match {
          case Some((prevX, prevY, prevZ)) =>
            // Known entity, check if it moved enough to trigger.
            if (entity.distanceToSqr(prevX, prevY, prevZ) > sensitivity * sensitivity * 2) {
              sendSignal(entity)
            }
          case _ =>
            // New, unknown entity, always trigger.
            sendSignal(entity)
        }
        // Update tracked position.
        trackedEntities += entity ->(entity.getX, entity.getY, entity.getZ)
      }
    }
  }

  private def sensorBounds = new AABB(
    x + 0.5 - radius, y + 0.5 - radius, z + 0.5 - radius,
    x + 0.5 + radius, y + 0.5 + radius, z + 0.5 + radius)

  private def isInRange(entity: LivingEntity) = entity.distanceToSqr(x + 0.5, y + 0.5, z + 0.5) <= radius * radius

  private def isClearPath(target: Vec3): Boolean = {
    val origin = new Vec3(x, y, z)
    val path = target.subtract(origin).normalize()
    val eye = origin.add(path)
    val trace = world.clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()))
    trace.getType == HitResult.Type.MISS
  }

  private def isVisible(entity: LivingEntity) =
    entity.getEffect(MobEffects.INVISIBILITY) == null &&
      // Note: it only working in lit conditions works and is neat, but this
      // is pseudo-infrared driven (it only works for *living* entities, after
      // all), so I think it makes more sense for it to work in the dark, too.
      /* entity.getBrightness(0) > 0.2 && */ {
      val target = entity.position
      isClearPath(target) || isClearPath(target.add(0.0D, entity.getEyeHeight, 0.0D))
    }

  private def sendSignal(entity: LivingEntity): Unit = {
    if (Settings.get.inputUsername) {
      node.sendToReachable("computer.signal", "motion", Double.box(entity.getX - (x + 0.5)), Double.box(entity.getY - (y + 0.5)), Double.box(entity.getZ - (z + 0.5)), entity.getName.getString)
    }
    else {
      node.sendToReachable("computer.signal", "motion", Double.box(entity.getX - (x + 0.5)), Double.box(entity.getY - (y + 0.5)), Double.box(entity.getZ - (z + 0.5)))
    }
  }

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():number -- Gets the current sensor sensitivity.""")
  def getSensitivity(computer: Context, args: Arguments): Array[AnyRef] = result(sensitivity)

  @Callback(direct = true, doc = """function(value:number):number -- Sets the sensor's sensitivity. Returns the old value.""")
  def setSensitivity(computer: Context, args: Arguments): Array[AnyRef] = {
    val oldValue = sensitivity
    sensitivity = math.max(0.2, args.checkDouble(0))
    result(oldValue)
  }

  // ---------------------------------------------------------------------- //

  private final val SensitivityTag = Settings.namespace + "sensitivity"

  override def loadData(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadData(nbt, provider)
    sensitivity = nbt.getDouble(SensitivityTag)
  }

  override def saveData(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveData(nbt, provider)
    nbt.putDouble(SensitivityTag, sensitivity)
  }

  // ----------------------------------------------------------------------- //
}
