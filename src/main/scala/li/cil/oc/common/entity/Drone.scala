package li.cil.oc.common.entity

import org.joml.Vector3d
import li.cil.oc._
import li.cil.oc.api.driver.item
import li.cil.oc.api.internal.MultiTank
import li.cil.oc.api.machine.{Context, MachineHost}
import li.cil.oc.api.network._
import li.cil.oc.api.{Driver, Machine, Persistable, internal}
import li.cil.oc.common.container.{ComponentInventory, Inventory}
import li.cil.oc.common.datacomponents.{DroneState, OCComponents, Owner}
import li.cil.oc.common.item.data.DroneData
import li.cil.oc.common.menu.MenuTypes
import li.cil.oc.common.{EventHandler, menu}
import li.cil.oc.integration.util.Wrench
import li.cil.oc.server.{agent, component}
import li.cil.oc.util.ExtendedLevel._
import li.cil.oc.util.ExtendedDataComponentHolder._
import li.cil.oc.util.{BlockPosition, InventoryUtils}
import net.minecraft.core.component.DataComponentHolder
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.syncher.{EntityDataAccessor, EntityDataSerializers, SynchedEntityData}
import net.minecraft.server.level.{ServerEntity, ServerPlayer}
import net.minecraft.util.ColorRGBA
import net.minecraft.world.entity._
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.{Inventory => PlayerInventory}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.portal.DimensionTransition
import net.minecraft.world.phys.Vec3
import net.minecraft.world.{InteractionHand, InteractionResult, MenuProvider}
import net.neoforged.neoforge.common.MutableDataComponentHolder
import net.neoforged.neoforge.fluids.IFluidTank

import java.lang
import java.util.UUID
import scala.jdk.CollectionConverters._

object Drone {
  val DataRunning: EntityDataAccessor[lang.Boolean] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.BOOLEAN)
  val DataTargetX: EntityDataAccessor[lang.Float] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.FLOAT)
  val DataTargetY: EntityDataAccessor[lang.Float] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.FLOAT)
  val DataTargetZ: EntityDataAccessor[lang.Float] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.FLOAT)
  val DataMaxAcceleration: EntityDataAccessor[lang.Float] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.FLOAT)
  val DataSelectedSlot: EntityDataAccessor[Integer] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.INT)
  val DataCurrentEnergy: EntityDataAccessor[Integer] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.INT)
  val DataMaxEnergy: EntityDataAccessor[Integer] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.INT)
  val DataStatusText: EntityDataAccessor[Component] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.COMPONENT)
  val DataInventorySize: EntityDataAccessor[Integer] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.INT)
  val DataLightColor: EntityDataAccessor[Integer] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.INT)
}

abstract class DroneInventory(val drone: Drone) extends Inventory

// internal.Rotatable is also in internal.Drone, but it wasn't since the start
// so this is to ensure it is implemented here, in the very unlikely case that
// someone decides to ship that specific version of the API.
class Drone(selfType: EntityType[Drone], level: Level) extends Entity(selfType, level) with MachineHost with internal.Drone with internal.Rotatable with Analyzable with Context with Persistable {
  override def getEnvironmentLevel: Level = level

  // Some basic constants.
  val gravity = 0.05f
  // low for slow fall (float down)
  val drag = 0.8f
  val maxAcceleration = 0.1f
  val maxVelocity = 0.4f
  val maxInventorySize = 8

  // Rendering stuff, purely eyecandy.
  val targetFlapAngles: Array[Array[Float]] = Array.fill(4, 2)(0f)
  val flapAngles: Array[Array[Float]] = Array.fill(4, 2)(0f)
  var nextFlapChange = 0
  var bodyAngle: Float = math.random().toFloat * 90
  var angularVelocity = 0f
  var nextAngularVelocityChange = 0
  var lastEnergyUpdate = 0
  private var droppedAsItem = false

  // Logic stuff, components, machine and such.
  val info = new DroneData()
  val machine: api.machine.Machine = if (!getEnvironmentLevel.isClientSide) {
    val m = Machine.create(this)
    m.node.asInstanceOf[Connector].setLocalBufferSize(0)
    m
  } else null
  val control: component.Drone = if (!getEnvironmentLevel.isClientSide) new component.Drone(this) else null
  val components = new ComponentInventory {
    override def host: Drone = Drone.this

    override def items: Array[ItemStack] = info.components

    override def getContainerSize: Int = info.components.length

    override def setChanged(): Unit = {}

    override def canPlaceItem(slot: Int, stack: ItemStack) = true

    override def stillValid(player: Player) = true

    override def node: Node = Option(machine).map(_.node).orNull

    override def onConnect(node: Node): Unit = {}

    override def onDisconnect(node: Node): Unit = {}

    override def onMessage(message: Message): Unit = {}
  }
  val equipmentInventory = new Inventory {
    val items = Array.empty[ItemStack]

    override def getContainerSize = 0

    override def getMaxStackSize = 0

    override def setChanged(): Unit = {}

    override def canPlaceItem(slot: Int, stack: ItemStack) = false

    override def stillValid(player: Player) = false
  }
  val mainInventory = new DroneInventory(this) {
    val items: Array[ItemStack] = Array.fill[ItemStack](8)(ItemStack.EMPTY)

    override def getContainerSize: Int = inventorySize

    override def getMaxStackSize = 64

    override def setChanged(): Unit = {} // TODO update client GUI?

    override def canPlaceItem(slot: Int, stack: ItemStack): Boolean = slot >= 0 && slot < getContainerSize

    override def stillValid(player: Player): Boolean = player.distanceToSqr(drone) < 64
  }
  val tank = new MultiTank {
    override def tankCount: Int = components.componentSlots.count {
      case Some(tank: IFluidTank) => true
      case _ => false
    }

    override def getFluidTank(index: Int): IFluidTank = components.componentSlots.collect {
      case Some(tank: IFluidTank) => tank
    }.apply(index)
  }
  var selectedTank = 0

  override def setSelectedTank(index: Int): Unit = selectedTank = index

  override def tier: Int = info.tier

  override def player(): Player = {
    agent.Player.updatePositionAndRotation(player_, facing, facing)
    agent.Player.setPlayerInventoryItems(player_)
    player_
  }

  override def name: String = info.name

  override def setName(name: String): Unit = info.name = name

  var ownerName: String = Settings.get.fakePlayerName

  var ownerUUID: UUID = Settings.get.fakePlayerProfile.getId

  private lazy val player_ = new agent.Player(this)

  // ----------------------------------------------------------------------- //
  // Forward context stuff to our machine. Interface needed for some components
  // to work correctly (such as the chunkloader upgrade).

  override def node: Node = machine.node

  override def canInteract(player: String): Boolean = machine.canInteract(player)

  override def isPaused: Boolean = machine.isPaused

  override def start(): Boolean = {
    if (getEnvironmentLevel.isClientSide || machine.isRunning) {
      return false
    }
    preparePowerUp()
    machine.start()
  }

  override def pause(seconds: Double): Boolean = machine.pause(seconds)

  override def stop(): Boolean = machine.stop()

  override def consumeCallBudget(callCost: Double): Unit = machine.consumeCallBudget(callCost)

  override def signal(name: String, args: AnyRef*): Boolean = machine.signal(name, args: _*)

  // ----------------------------------------------------------------------- //

  override def getTarget = new Vector3d(targetX.floatValue(), targetY.floatValue(), targetZ.floatValue())

  override def setTarget(value: Vector3d): Unit = {
    targetX = value.x.toFloat
    targetY = value.y.toFloat
    targetZ = value.z.toFloat
  }

  override def getVelocity: Vector3d = {
    val v3d = getDeltaMovement
    new Vector3d(v3d.x, v3d.y, v3d.z)
  }

  // ----------------------------------------------------------------------- //

  override def isPickable = true

  override def isPushable = true

  // ----------------------------------------------------------------------- //

  override def xPosition: Double = getX

  override def yPosition: Double = getY

  override def zPosition: Double = getZ

  override def markChanged(): Unit = {}

  override def getRopeHoldPosition(dt: Float): Vec3 =
    getPosition(dt).add(0.0, -0.056, 0.0) // Offset: height * 0.85 * 0.7 - 0.25

  // ----------------------------------------------------------------------- //

  override def facing = Direction.SOUTH

  override def toLocal(value: Direction): Direction = value

  override def toGlobal(value: Direction): Direction = value

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: Player, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = Array(machine.node)

  // ----------------------------------------------------------------------- //

  override def internalComponents(): java.lang.Iterable[ItemStack] = info.components.iterator.to(Iterable).asJava

  override def componentSlot(address: String): Int = components.componentSlots.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  override def onMachineConnect(node: Node): Unit = {}

  override def onMachineDisconnect(node: Node): Unit = {}

  def computeInventorySize(): Int = math.min(maxInventorySize, info.components.foldLeft(0)((acc, component) => acc + (Option(component) match {
    case Some(stack) => Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: item.Inventory) => math.max(1, driver.inventoryCapacity(stack) / 4)
      case _ => 0
    }
    case _ => 0
  })))

  // ----------------------------------------------------------------------- //

  override def defineSynchedData(builder: SynchedEntityData.Builder): Unit = {
    builder.define(Drone.DataRunning, java.lang.Boolean.FALSE)
    builder.define(Drone.DataTargetX, Float.box(0f))
    builder.define(Drone.DataTargetY, Float.box(0f))
    builder.define(Drone.DataTargetZ, Float.box(0f))
    builder.define(Drone.DataMaxAcceleration, Float.box(0f))
    builder.define(Drone.DataSelectedSlot, Int.box(0))
    builder.define(Drone.DataCurrentEnergy, Int.box(0))
    builder.define(Drone.DataMaxEnergy, Int.box(100))
    builder.define(Drone.DataStatusText, Component.empty())
    builder.define(Drone.DataInventorySize, Int.box(0))
    builder.define(Drone.DataLightColor, Int.box(0x66DD55))
  }

  def initializeAfterPlacement(stack: ItemStack, position: Vec3): Unit = {
    info.loadData(stack)
    control.node.changeBuffer(info.storedEnergy - control.node.localBuffer)
    wireThingsTogether()
    inventorySize = computeInventorySize()
    setPos(position.x, position.y, position.z)
  }

  def preparePowerUp(): Unit = {
    targetX = math.floor(getX).toFloat + 0.5f
    targetY = math.round(getY).toFloat + 0.5f
    targetZ = math.floor(getZ).toFloat + 0.5f
    targetAcceleration = maxAcceleration

    wireThingsTogether()
  }

  private def wireThingsTogether(): Unit = {
    api.Network.joinNewNetwork(machine.node)
    machine.node.connect(control.node)
    machine.setCostPerTick(Settings.get.droneCost)
    components.connectComponents()
  }

  def isRunning: Boolean = {
    val value: lang.Boolean = entityData.get(Drone.DataRunning)
    value: lang.Boolean
  }

  def targetX: lang.Float = entityData.get(Drone.DataTargetX)

  def targetY: lang.Float = entityData.get(Drone.DataTargetY)

  def targetZ: lang.Float = entityData.get(Drone.DataTargetZ)

  def targetAcceleration: lang.Float = entityData.get(Drone.DataMaxAcceleration)

  def selectedSlot: Int = entityData.get(Drone.DataSelectedSlot) & 0xFF

  def globalBuffer: Integer = entityData.get(Drone.DataCurrentEnergy)

  def globalBufferSize: Integer = entityData.get(Drone.DataMaxEnergy)

  def statusText: Component = entityData.get(Drone.DataStatusText)

  def inventorySize: Int = entityData.get(Drone.DataInventorySize) & 0xFF

  def lightColor: Integer = entityData.get(Drone.DataLightColor)

  def setRunning(value: Boolean): Unit = entityData.set(Drone.DataRunning, Boolean.box(value))

  // Round target values to low accuracy to avoid floating point errors accumulating.
  def targetX_=(value: Float): Unit = entityData.set(Drone.DataTargetX, Float.box(math.round(value * 4) / 4f))

  def targetY_=(value: Float): Unit = entityData.set(Drone.DataTargetY, Float.box(math.round(value * 4) / 4f))

  def targetZ_=(value: Float): Unit = entityData.set(Drone.DataTargetZ, Float.box(math.round(value * 4) / 4f))

  def targetAcceleration_=(value: Float): Unit = entityData.set(Drone.DataMaxAcceleration, Float.box(math.max(0, math.min(maxAcceleration, value))))

  def setSelectedSlot(value: Int): Unit = entityData.set(Drone.DataSelectedSlot, Int.box(value.toByte))

  def globalBuffer_=(value: Int): Unit = entityData.set(Drone.DataCurrentEnergy, Int.box(value))

  def globalBufferSize_=(value: Int): Unit = entityData.set(Drone.DataMaxEnergy, Int.box(value))

  def statusText_=(value: Component): Unit = entityData.set(Drone.DataStatusText, value)

  def inventorySize_=(value: Int): Unit = entityData.set(Drone.DataInventorySize, Int.box(value.toByte))

  def lightColor_=(value: Int): Unit = entityData.set(Drone.DataLightColor, Int.box(value))

  override def lerpTo(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, steps: Int): Unit = {
    // Only set exact position if we're too far away from the server's
    // position, otherwise keep interpolating. This removes jitter and
    // is good enough for drones.
    if (!isRunning || distanceToSqr(x, y, z) > 1) {
      super.absMoveTo(x, y, z, yaw, pitch)
    }
    else {
      targetX = x.toFloat
      targetY = y.toFloat
      targetZ = z.toFloat
    }
  }

  override def tick(): Unit = {
    super.tick()

    if (!getEnvironmentLevel.isClientSide) {
      if (isInWater || isInLava) {
        // We're not water-proof!
        machine.stop()
      }
      machine.update()
      components.updateComponents()
      setRunning(machine.isRunning)

      val buffer = math.round(machine.node.asInstanceOf[Connector].globalBuffer).toInt
      if (math.abs(lastEnergyUpdate - buffer) > 1 || getEnvironmentLevel.getGameTime % 200 == 0) {
        lastEnergyUpdate = buffer
        globalBuffer = buffer
        globalBufferSize = machine.node.asInstanceOf[Connector].globalBufferSize.toInt
      }
    }
    else {
      if (isRunning) {
        // Client side update; occasionally update wing pitch and rotation to
        // make the drones look a bit more dynamic.
        val rng = getEnvironmentLevel.random
        nextFlapChange -= 1
        nextAngularVelocityChange -= 1

        if (nextFlapChange < 0) {
          nextFlapChange = 5 + rng.nextInt(10)
          for (i <- 0 until 2) {
            val flap = rng.nextInt(targetFlapAngles.length)
            targetFlapAngles(flap)(0) = math.toRadians(rng.nextFloat() * 4 - 2).toFloat
            targetFlapAngles(flap)(1) = math.toRadians(rng.nextFloat() * 4 - 2).toFloat
          }
        }

        if (nextAngularVelocityChange < 0) {
          if (angularVelocity != 0) {
            angularVelocity = 0
            nextAngularVelocityChange = 20
          }
          else {
            angularVelocity = if (rng.nextBoolean()) 0.1f else -0.1f
            nextAngularVelocityChange = 100
          }
        }

        // Interpolate wing rotations.
        (flapAngles, targetFlapAngles).zipped.foreach((f, t) => {
          f(0) = f(0) * 0.7f + t(0) * 0.3f
          f(1) = f(1) * 0.7f + t(1) * 0.3f
        })

        // Update body rotation.
        bodyAngle += angularVelocity
      }
    }

    xo = getX
    yo = getY
    zo = getZ
    noPhysics = !getEnvironmentLevel.noCollision(this)
    if (noPhysics) moveTowardsClosestSpace(getX, (getBoundingBox.minY + getBoundingBox.maxY) / 2, getZ)

    if (isRunning) {
      val toTarget = new Vec3(targetX - getX, targetY - getY, targetZ - getZ)
      val distance = toTarget.length()
      val velocity = getDeltaMovement
      if (distance > 0 && (distance > 0.005f || velocity.dot(velocity) > 0.005f)) {
        val acceleration = math.min(targetAcceleration.floatValue(), distance) / distance
        val velocityX = velocity.x + toTarget.x * acceleration
        val velocityY = velocity.y + toTarget.y * acceleration
        val velocityZ = velocity.z + toTarget.z * acceleration
        setDeltaMovement(new Vec3(math.max(-maxVelocity, math.min(maxVelocity, velocityX)),
          math.max(-maxVelocity, math.min(maxVelocity, velocityY)),
          math.max(-maxVelocity, math.min(maxVelocity, velocityZ))))
      }
      else {
        setDeltaMovement(Vec3.ZERO)
        setPos(targetX.floatValue(), targetY.floatValue(), targetZ.floatValue())
      }
    } else {
      // No power, free fall: engage!
      setDeltaMovement(getDeltaMovement.subtract(0, gravity, 0))
    }

    move(MoverType.SELF, getDeltaMovement)

    // Make sure we don't get infinitely faster.
    if (isRunning) {
      setDeltaMovement(getDeltaMovement.scale(drag))
    }
    else {
      val groundDrag = getEnvironmentLevel.getBlock(BlockPosition(this: Entity).offset(Direction.DOWN)).getFriction * drag
      setDeltaMovement(getDeltaMovement.multiply(groundDrag, drag * (if (onGround) -0.5 else 1), groundDrag))
    }
  }

  override def skipAttackInteraction(entity: Entity): Boolean = {
    if (isRunning) {
      val direction = new Vec3(entity.getX - getX, entity.getY + entity.getEyeHeight - getY, entity.getZ - getZ).normalize()
      if (!getEnvironmentLevel.isClientSide) {
        if (Settings.get.inputUsername)
          machine.signal("hit", Double.box(direction.x), Double.box(direction.z), Double.box(direction.y), entity.getName.getString)
        else
          machine.signal("hit", Double.box(direction.x), Double.box(direction.z), Double.box(direction.y))
      }
      setDeltaMovement(getDeltaMovement.subtract(direction).scale(0.5))
    }
    super.skipAttackInteraction(entity)
  }

  // Not implemented in Drone itself because spectators would open this via vanilla Player.openMenu (without extra data).
  val containerProvider = new MenuProvider {
    override def getDisplayName = Component.empty

    override def createMenu(id: Int, playerInventory: PlayerInventory, player: Player) =
      new menu.Drone(id, playerInventory, mainInventory, mainInventory.getContainerSize)
  }

  override def interact(player: Player, hand: InteractionHand): InteractionResult = {
    if (!isAlive) return InteractionResult.PASS
    if (player.isCrouching) {
      if (Wrench.isWrench(player.getItemInHand(InteractionHand.MAIN_HAND))) {
        if(!getEnvironmentLevel.isClientSide) {
          dropAsItemAndDiscard()
        }
      }
      else if (!getEnvironmentLevel.isClientSide && !machine.isRunning) {
        start()
      }
    }
    else player match {
      case srvPlr: ServerPlayer if !getEnvironmentLevel.isClientSide => MenuTypes.openDroneGui(srvPlr, this)
      case _ =>
    }
    InteractionResult.sidedSuccess(getEnvironmentLevel.isClientSide)
  }

  // No step sounds. Except on that one day.
  override def playStepSound(pos: BlockPos, state: BlockState): Unit = {
    if (EventHandler.isItTime) super.playStepSound(pos, state)
  }

  // ----------------------------------------------------------------------- //

  private var isChangingDimension = false

  override def changeDimension(dimension: DimensionTransition): Entity = {
    // Store relative target as target, to allow adding that in our "new self"
    // (entities get re-created after changing dimension).
    targetX = (targetX - getX).toFloat
    targetY = (targetY - getY).toFloat
    targetZ = (targetZ - getZ).toFloat
    try {
      isChangingDimension = true
      super.changeDimension(dimension)
    }
    finally {
      isChangingDimension = false
      remove(Entity.RemovalReason.CHANGED_DIMENSION) // Again, to actually close old machine state after copying it.
    }
  }

  override def restoreFrom(entity: Entity): Unit = {
    super.restoreFrom(entity)
    // Compute relative target based on old position and update, because our
    // frame of reference most certainly changed (i.e. we'll spawn at different
    // coordinates than the ones we started traveling from, e.g. when porting
    // to the nether it'll be oldpos / 8).
    entity match {
      case drone: Drone =>
        targetX = (getX + drone.targetX).toFloat
        targetY = (getY + drone.targetY).toFloat
        targetZ = (getZ + drone.targetZ).toFloat
      case _ =>
        targetX = getX.toFloat
        targetY = getY.toFloat
        targetZ = getZ.toFloat
    }
  }

  override def remove(reason: Entity.RemovalReason): Unit = {
    super.remove(reason)
    if (!getEnvironmentLevel.isClientSide && !isChangingDimension) {
      machine.stop()
      machine.node.remove()
      components.disconnectComponents()
      components.saveComponents()
    }
  }

  private def dropAsItemAndDiscard(): Unit = {
    if (!getEnvironmentLevel.isClientSide && !droppedAsItem) {
      droppedAsItem = true
      val stack = api.Items.get(Constants.ItemName.Drone).createItemStack(1)
      info.storedEnergy = control.node.localBuffer.toInt
      info.saveData(stack)
      val entity = new ItemEntity(getEnvironmentLevel, getX, getY, getZ, stack)
      entity.setPickUpDelay(15)
      getEnvironmentLevel.addFreshEntity(entity)
      InventoryUtils.dropAllSlots(BlockPosition(this: Entity), mainInventory)
      remove(Entity.RemovalReason.DISCARDED)
    }
  }

  override def checkBelowWorld(): Unit = {
    if (getY < getEnvironmentLevel.getMinBuildHeight - 64) {
      dropAsItemAndDiscard()
    }
    else {
      super.checkBelowWorld()
    }
  }

  override def getName: Component = Localization.localizeLater("entity.oc.Drone.name")

  override def getAddEntityPacket(entityTrackerEntry: ServerEntity) =
    new ClientboundAddEntityPacket(this, entityTrackerEntry)

  override def loadData(holder: DataComponentHolder): Unit = {
    info.loadData(holder)

    if(!getEnvironmentLevel.isClientSide) {
      machine.loadData(holder)
      control.loadData(holder)
      components.loadData(holder)
      mainInventory.loadData(holder)
    }

    for(owner <- holder.getComponent(OCComponents.OWNER)) {
      ownerName = owner.name()
      ownerUUID = owner.id()
    }

    for(DroneState(x, y, z, accel, slot, tank) <- holder.getComponent(OCComponents.DRONE_STATE)) {
      targetX = x
      targetY = y
      targetZ = z
      targetAcceleration = accel
      setSelectedSlot(slot & 0xFF)
      setSelectedTank(tank & 0xFF)
    }

    for(text <- holder.getComponent(OCComponents.STATUS_TEXT)) {
      statusText = text
    }

    for(color <- holder.getComponent(OCComponents.LIGHT_COLOR)) {
      lightColor = color.rgba
    }
  }

  override def saveData(holder: MutableDataComponentHolder): Unit = {
    info.saveData(holder)

    if(!getEnvironmentLevel.isClientSide) {
      machine.saveData(holder)
      control.saveData(holder)
      components.saveData(holder)
      mainInventory.saveData(holder)
    }

    holder.set(OCComponents.OWNER, new Owner(ownerName, ownerUUID))
    holder.set(OCComponents.DRONE_STATE, DroneState(
      targetX,
      targetY,
      targetZ,
      targetAcceleration,
      selectedSlot.toByte,
      selectedTank.toByte
    ))

    holder.set(OCComponents.STATUS_TEXT, statusText)
    holder.set(OCComponents.LIGHT_COLOR, new ColorRGBA(lightColor))
  }

  override protected def readAdditionalSaveData(nbt: CompoundTag): Unit = {
    val provider = this.level.registryAccess()
    this.loadData(nbt, provider)

    inventorySize = computeInventorySize()
    if (!getEnvironmentLevel.isClientSide) {
      wireThingsTogether()
    }
  }

  override protected def addAdditionalSaveData(nbt: CompoundTag): Unit = {
    if (getEnvironmentLevel.isClientSide) return
    val provider = this.level.registryAccess()
    components.saveComponents()
    info.storedEnergy = globalBuffer.toInt

    this.saveData(nbt, provider)
  }
}
