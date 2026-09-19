package li.cil.oc.server.agent

import com.mojang.authlib.GameProfile
import com.mojang.datafixers.util.Either
import li.cil.oc.{OpenComputers, Settings}
import li.cil.oc.api.event._
import li.cil.oc.api.internal
import li.cil.oc.api.network.Connector
import li.cil.oc.common.EventHandler
import li.cil.oc.server.agent.{Inventory => AgentInventory}
import li.cil.oc.util.{BlockPosition, InventoryUtils}
import net.minecraft.core.{BlockPos, Direction, NonNullList}
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
import net.minecraft.server.level.{ClientInformation, ServerLevel}
import net.minecraft.server.network.{CommonListenerCookie, ServerGamePacketListenerImpl}
import net.minecraft.server.players.ServerOpListEntry
import net.minecraft.world.{Container, InteractionHand, InteractionResult, MenuProvider}
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity.RemovalReason
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player.{BedSleepingProblem => BedStatus}
import net.minecraft.world.entity.player.{Player => PlayerEntity}
import net.minecraft.world.entity._
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.item.{BlockItem, ItemStack, Items}
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.trading.MerchantOffers
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.{CommandBlockEntity, SignBlockEntity}
import net.minecraft.world.level.block.piston.PistonBaseBlock
import net.minecraft.world.level.{BaseCommandBlock, Level}
import net.minecraft.world.phys.{BlockHitResult, Vec3}
import net.neoforged.bus.api.{EventPriority, ICancellableEvent, SubscribeEvent}
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.common.util.{FakePlayer, TriState}
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.network.connection.ConnectionType
import net.neoforged.neoforge.common.CommonHooks

import java.util
import java.util.UUID
import scala.jdk.CollectionConverters._

object Player {
  def profileFor(agent: internal.Agent): GameProfile = {
    val uuid = agent.ownerUUID
    val randomId = (agent.getEnvironmentLevel.random.nextInt(0xFFFFFF) + 1).toString
    val name = Settings.get.nameFormat.
      replace("$player$", agent.ownerName).
      replace("$random$", randomId)
    new GameProfile(uuid, name)
  }

  def determineUUID(playerUUID: Option[UUID] = None): UUID = {
    val format = Settings.get.uuidFormat
    val randomUUID = UUID.randomUUID()
    try UUID.fromString(format.
      replace("$random$", randomUUID.toString).
      replace("$player$", playerUUID.getOrElse(randomUUID).toString)) catch {
      case t: Throwable =>
        OpenComputers.log.warn("Failed determining robot UUID, check your config's `uuidFormat` entry!", t)
        randomUUID
    }
  }

  def updatePositionAndRotation(player: Player, facing: Direction, side: Direction): Unit = {
    player.facing = facing
    player.side = side
    val direction = new Vec3(
      facing.getStepX + side.getStepX,
      facing.getStepY + side.getStepY,
      facing.getStepZ + side.getStepZ).normalize()
    val yaw = Math.toDegrees(-Math.atan2(direction.x, direction.z)).toFloat
    val pitch = Math.toDegrees(-Math.atan2(direction.y, Math.sqrt((direction.x * direction.x) + (direction.z * direction.z)))).toFloat * 0.99f
    player.setPos(player.agent.xPosition, player.agent.yPosition, player.agent.zPosition)
    player.setYRot(pitch % 360f)
    player.setXRot(yaw % 360f)
    player.xRotO = player.getXRot
    player.yRotO = player.getYRot
  }

  def setPlayerInventoryItems(player: Player): Unit = {
    // the offhand is simply the agent's tool item
    val agent = player.agent
    def setCopyOrNull(inv: NonNullList[ItemStack], agentInv: Container, slot: Int): Unit = {
      val item = agentInv.getItem(slot)
      inv.set(slot, if (item != null) item.copy() else ItemStack.EMPTY)
    }

    for (i <- 0 until 4) {
      setCopyOrNull(player.inventory.armor, agent.equipmentInventory, i)
    }

    // items is 36 items
    // the agent inventory is 100 items with some space for components
    // leaving us 88..we'll copy what we can
    val size = player.inventory.items.size min agent.mainInventory.getContainerSize
    for (i <- 0 until size) {
      setCopyOrNull(player.inventory.items, agent.mainInventory, i)
    }
    player.inventoryMenu.broadcastChanges()
  }

  def detectPlayerInventoryChanges(player: Player): Unit = {
    val agent = player.agent
    player.inventoryMenu.broadcastChanges()
    // The follow code will set agent.inventories = FakePlayer's inv.stack
    def setCopy(inv: Container, index: Int, item: ItemStack): Unit = {
      val result = if (item != null) item.copy else ItemStack.EMPTY
      val current = inv.getItem(index)
      if (!ItemStack.matches(result, current)) {
        inv.setItem(index, result)
      }
    }
    for (i <- 0 until 4) {
      setCopy(agent.equipmentInventory(), i, player.inventory.armor.get(i))
    }
    val size = player.inventory.items.size min agent.mainInventory.getContainerSize
    for (i <- 0 until size) {
      setCopy(agent.mainInventory, i, player.inventory.items.get(i))
    }
  }
}

class Player(val agent: internal.Agent) extends FakePlayer(agent.getEnvironmentLevel.asInstanceOf[ServerLevel], Player.profileFor(agent)) {
  // NeoForge 1.21: FakePlayer already sets up connection internally
  val abilities = getAbilities

  abilities.mayfly = true
  abilities.invulnerable = true
  abilities.flying = true
  setOnGround(true)

  override def getDefaultDimensions(pose: Pose) = new EntityDimensions(1, 1, 1, EntityAttachments.createDefault(1, 1), true)
  refreshDimensions()

  {
    this.inventory = new AgentInventory(this, agent)
    // because the inventory was just overwritten, the container is now detached
    this.inventoryMenu = new InventoryMenu(inventory, !level.isClientSide, this)
    this.containerMenu = this.inventoryMenu

    // NeoForge 1.21.1: LazyOptional-based inventory capability fields removed; AgentInventory is used directly
  }

  var facing, side = Direction.SOUTH

  override def getName = Component.literal(agent.name)

  // ----------------------------------------------------------------------- //

  def closestEntity[Type <: Entity](clazz: Class[Type], side: Direction = facing): Option[Entity] = {
    val bounds = BlockPosition(agent).offset(side).bounds
    val candidates = level.getEntitiesOfClass(clazz, bounds, (_: Type) => true)
    if (candidates.isEmpty) return None
    Some(candidates.asScala.minBy(e => distanceToSqr(e)))
  }

  def entitiesOnSide[Type <: Entity](clazz: Class[Type], side: Direction): util.List[Type] = {
    entitiesInBlock(clazz, BlockPosition(agent).offset(side))
  }

  def entitiesInBlock[Type <: Entity](clazz: Class[Type], blockPos: BlockPosition): util.List[Type] = {
    level.getEntitiesOfClass(clazz, blockPos.bounds, (_: Type) => true)
  }

  private def adjacentItems: util.List[ItemEntity] = {
    level.getEntitiesOfClass(classOf[ItemEntity], BlockPosition(agent).bounds.inflate(2, 2, 2), _ => true)
  }

  private def collectDroppedItems(itemsBefore: Iterable[ItemEntity]): Unit = {
    val itemsDropped = adjacentItems.asScala --= itemsBefore
    if (itemsDropped.nonEmpty) {
      for (drop <- itemsDropped) {
        drop.setDefaultPickUpDelay()
        drop.playerTouch(this)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def attack(entity: Entity): Unit = {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => entity match {
      case player: PlayerEntity if !canHarmPlayer(player) => // Avoid player damage.
      case _ =>
        val event = new RobotAttackEntityEvent.Pre(agent, entity)
        NeoForge.EVENT_BUS.post(event)
        if (!event.isCanceled) {
          super.attack(entity)
          NeoForge.EVENT_BUS.post(new RobotAttackEntityEvent.Post(agent, entity))
        }
    })
  }

  override def interactOn(entity: Entity, hand: InteractionHand): InteractionResult = {
    val cancel = try {
      NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(this, hand, entity))
      true
    } catch {
      case t: Throwable =>
        if (!t.getStackTrace.exists(_.getClassName.startsWith("mods.battlegear2."))) {
          OpenComputers.log.warn("Some event handler screwed up!", t)
        }
        false
    }
    if(!cancel && callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      val result = isItemUseAllowed(stack) && (entity.interact(this, hand).consumesAction || (entity match {
        case living: LivingEntity if !getItemInHand(InteractionHand.MAIN_HAND).isEmpty => getItemInHand(InteractionHand.MAIN_HAND).interactLivingEntity(this, living, hand).consumesAction
        case _ => false
      }))
      if (!getItemInHand(InteractionHand.MAIN_HAND).isEmpty) {
        if (getItemInHand(InteractionHand.MAIN_HAND).getCount <= 0) {
          val orig = getItemInHand(InteractionHand.MAIN_HAND)
          this.inventory.setItem(this.inventory.selected, ItemStack.EMPTY)
        } else {
          // because of various hacks for IC2, we expect the in-hand result to be moved to our offhand buffer
          this.inventory.offhand.set(0, getItemInHand(InteractionHand.MAIN_HAND))
          this.inventory.setItem(this.inventory.selected, ItemStack.EMPTY)
        }
      }
      result
    })) InteractionResult.sidedSuccess(level.isClientSide) else InteractionResult.PASS
  }

  def activateBlockOrUseItem(pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float, duration: Double): ActivationType.Value = {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      if (shouldCancel(() => fireRightClickBlock(pos, side))) {
        return ActivationType.None
      }

      val item = if (!stack.isEmpty) stack.getItem else null
      val state = level.getBlockState(pos)
      val traceEndPos = new Vec3(pos.getX + hitX, pos.getY + hitY, pos.getZ + hitZ)
      val traceCtx = if (state.isAir()) BlockHitResult.miss(traceEndPos, side, pos) else new BlockHitResult(traceEndPos, side, pos, false)
      if (item != null && item.onItemUseFirst(stack, new UseOnContext(level, this, InteractionHand.OFF_HAND, stack, traceCtx)).consumesAction) {
        return ActivationType.ItemUsed
      }

      val canActivate = !state.isAir() && Settings.get.allowActivateBlocks
      val shouldActivate = canActivate && (!isCrouching || (item == null || item.doesSneakBypassUse(stack, level, pos, this)))
      val result =
        if (shouldActivate && state.useItemOn(stack, level, this, InteractionHand.OFF_HAND, new BlockHitResult(new Vec3(hitX, hitY, hitZ), side, pos, false)).consumesAction)
          ActivationType.BlockActivated
        else if (duration <= Double.MinPositiveValue && isItemUseAllowed(stack) && tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ))
          ActivationType.ItemPlaced
        else if (useEquippedItem(duration, Option(stack)))
          ActivationType.ItemUsed
        else
          ActivationType.None

      result
    })
  }

  override def setItemSlot(slotIn: EquipmentSlot, stack: ItemStack): Unit = {
    var superCall: () => Unit = () => super.setItemSlot(slotIn, stack)
    if (slotIn == EquipmentSlot.MAINHAND) {
      agent.equipmentInventory.setItem(0, stack)
      superCall = () => {
        val slot = inventory.selected
        // So, if we're not in the main inventory, selected is set to -1
        // for compatibility with mods that try accessing the inv directly
        // using inventory.selected. See li.cil.oc.server.agent.Inventory
        if(inventory.selected < 0) inventory.selected = ~inventory.selected
        super.setItemSlot(slotIn, stack)
        inventory.selected = slot
      }
    } else if(slotIn == EquipmentSlot.OFFHAND) {
      inventory.offhand.set(0, stack)
    }
    superCall()
  }

  override def getItemBySlot(slotIn: EquipmentSlot): ItemStack = {
    if (slotIn == EquipmentSlot.MAINHAND)
      agent.equipmentInventory.getItem(0)
    else if(slotIn == EquipmentSlot.OFFHAND)
      inventory.offhand.get(0)
    else super.getItemBySlot(slotIn)
  }

  def fireRightClickBlock(pos: BlockPos, side: Direction): PlayerInteractEvent.RightClickBlock = {
    val hitVec = new Vec3(0.5 + side.getStepX * 0.5, 0.5 + side.getStepY * 0.5, 0.5 + side.getStepZ * 0.5)
    val event = new PlayerInteractEvent.RightClickBlock(this, InteractionHand.OFF_HAND, pos, new BlockHitResult(hitVec, side, pos, false))
    NeoForge.EVENT_BUS.post(event)
    event
  }

  def fireLeftClickBlock(pos: BlockPos, side: Direction): PlayerInteractEvent.LeftClickBlock = {
    CommonHooks.onLeftClickBlock(this, pos, side, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK)
  }

  def fireRightClickAir(): PlayerInteractEvent.RightClickItem = {
    val event = new PlayerInteractEvent.RightClickItem(this, InteractionHand.OFF_HAND)
    NeoForge.EVENT_BUS.post(event)
    event
  }

  private def trySetActiveHand(duration: Double): Boolean = {
    releaseUsingItem()
    val entity = this
    val durationHandler = new {
      @SubscribeEvent(priority = EventPriority.LOWEST)
      def onItemUseStart(startUse: LivingEntityUseItemEvent.Start): Unit = {
        if (startUse.getEntity == entity && !startUse.isCanceled) {
          startUse.setDuration(duration.toInt)
        }
      }
    }
    NeoForge.EVENT_BUS.register(durationHandler)
    try {
      startUsingItem(InteractionHand.OFF_HAND)
      isUsingItem
    } catch {
        case _: Exception => false
    } finally {
      NeoForge.EVENT_BUS.unregister(durationHandler)
    }
  }

  def useItemWithHand(duration: Double, stack: ItemStack): Boolean = {
    if (!trySetActiveHand(duration)) {
      if (duration > 0) {
        return false
      }
    }

    val oldStack = stack.copy
    if (!isItemUseAllowed(stack)) {
      return false
    }

    val maxDuration = stack.getUseDuration(this)
    val heldTicks = Math.max(0, Math.min(maxDuration, (duration * 20).toInt))
    agent.machine.pause(heldTicks / 20.0)

    // setting the active hand will also set its initial duration
    val useItemResult = stack.use(level, this, InteractionHand.OFF_HAND)
    releaseUsingItem()

    if (!useItemResult.getResult.consumesAction) {
      return false
    }

    val newStack = useItemResult.getObject
    val stackChanged: Boolean =
      !ItemStack.matches(oldStack, newStack) ||
      !ItemStack.matches(oldStack, stack)

    if (stackChanged) {
      inventory.offhand.set(0, newStack)
    }
    stackChanged
  }

  def useEquippedItem(duration: Double, stackOption: Option[ItemStack] = None): Boolean = {
    if (stackOption.isEmpty) {
      return callUsingItemInSlot(agent.equipmentInventory, 0, {
        case item: ItemStack if item != null => useEquippedItem(duration, Option(item))
        case _ => false
      })
    }

    if (shouldCancel(() => fireRightClickAir())) {
      return false
    }

    // Change the offset at which items are used, to avoid hitting
    // the robot itself (e.g. with bows, potions, mining laser, ...).
    setPos(getX + facing.getStepX / 2.0, getY, getZ + facing.getStepZ / 2.0)

    try {
      useItemWithHand(duration, stackOption.get)
    }
    finally {
      setPos(getX - facing.getStepX / 2.0, getY, getZ - facing.getStepZ / 2.0)
    }
  }

  def placeBlock(slot: Int, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    callUsingItemInSlot(agent.mainInventory, slot, stack => {
      if (shouldCancel(() => fireRightClickBlock(pos, side))) {
        return false
      }

      tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ)
    }, repair = false)
  }

  def clickBlock(pos: BlockPos, side: Direction): Double = callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
    val state = level.getBlockState(pos)
    val block = state.getBlock

    if (!state.canHarvestBlock(level, pos, this)) return 0

    val hardness = state.getDestroySpeed(level, pos)
    val cobwebOverride = block == Blocks.COBWEB && Settings.get.screwCobwebs

    val strength = getDigSpeed(state, pos)
    val breakTime =
      if (cobwebOverride) Settings.get.swingDelay
      else hardness * 1.5 / strength

    if (breakTime.isInfinity) return 0
    if (breakTime < 0) return breakTime

    val preEvent = new RobotBreakBlockEvent.Pre(agent, level, pos, breakTime * Settings.get.harvestRatio)
    NeoForge.EVENT_BUS.post(preEvent)
    if (preEvent.isCanceled) return 0
    val adjustedBreakTime = Math.max(0.05, preEvent.getBreakTime)

    if (!PlayerInteractionManagerHelper.onBlockClicked(this, pos, side)) {
      if (level.isEmptyBlock(pos)) {
        return 1.0 / 20.0
      }
      return 0
    }

    EventHandler.scheduleServer(() => new DamageOverTime(this, pos, side, (adjustedBreakTime * 20).toInt).tick())

    adjustedBreakTime
  })

  private def isItemUseAllowed(stack: ItemStack) = stack.isEmpty || {
    (Settings.get.allowUseItemsWithDuration || stack.getUseDuration(this) <= 0) && !ItemStack.isSameItem(stack, new ItemStack(Items.LEAD))
  }

  override def drop(stack: ItemStack, dropAround: Boolean, traceItem: Boolean): ItemEntity =
    InventoryUtils.spawnStackInWorld(BlockPosition(agent), stack, if (dropAround) None else Option(facing))

  private def shouldCancel(f: () => PlayerInteractEvent) = {
    try {
      val event = f()
      (event.isInstanceOf[ICancellableEvent] && event.asInstanceOf[ICancellableEvent].isCanceled) || (event match {
        case rightClick: PlayerInteractEvent.RightClickBlock => rightClick.getUseBlock == TriState.FALSE || rightClick.getUseItem == TriState.FALSE
        case leftClick: PlayerInteractEvent.LeftClickBlock => leftClick.getUseBlock == TriState.FALSE || leftClick.getUseItem == TriState.FALSE
        case rightClick: PlayerInteractEvent.RightClickItem => rightClick.getCancellationResult == InteractionResult.FAIL
        case _ => false
      })
    }
    catch {
      case t: Throwable =>
        if (!t.getStackTrace.exists(_.getClassName.startsWith("mods.battlegear2."))) {
          OpenComputers.log.warn("Some event handler screwed up!", t)
        }
        false
    }
  }

  private def callUsingItemInSlot[T](inventory: Container, slot: Int, f: ItemStack => T, repair: Boolean = true) = {
    val itemsBefore = adjacentItems
    val stack = inventory.getItem(slot)
    val oldStack = stack.copy()
    this.inventory.selected = if (inventory == agent.mainInventory) slot else ~slot
    this.inventory.offhand.set(0, inventory.getItem(slot))
    try {
      f(stack)
    }
    finally {
      this.inventory.selected = 0
      inventory.setItem(slot, this.inventory.offhand.get(0))
      this.inventory.offhand.set(0, ItemStack.EMPTY)
      val newStack = inventory.getItem(slot)
      // this is only possible if f() modified the stack object in-place
      // looking at you, ic2
      if (ItemStack.matches(oldStack, newStack) &&
         !ItemStack.matches(oldStack, stack)) {
        inventory.setItem(slot, stack)
      }
      if (!newStack.isEmpty) {
        if (newStack.getCount <= 0) {
          inventory.setItem(slot, ItemStack.EMPTY)
        }
        if (repair) {
          if (newStack.getCount > 0) tryRepair(newStack, oldStack)
          else inventory.setItem(slot, ItemStack.EMPTY)
        }
      }
      collectDroppedItems(itemsBefore.asScala)
    }
  }

  private def tryRepair(stack: ItemStack, oldStack: ItemStack): Unit = {
    // Only if the underlying type didn't change.
    if (!stack.isEmpty && !oldStack.isEmpty && stack.getItem == oldStack.getItem) {
      val damageRate = new RobotUsedToolEvent.ComputeDamageRate(agent, oldStack, stack, Settings.get.itemDamageRate)
      NeoForge.EVENT_BUS.post(damageRate)
      if (damageRate.getDamageRate < 1) {
        NeoForge.EVENT_BUS.post(new RobotUsedToolEvent.ApplyDamageRate(agent, oldStack, stack, damageRate.getDamageRate))
      }
    }
  }

  private def tryPlaceBlockWhileHandlingFunnySpecialCases(stack: ItemStack, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = {
    !stack.isEmpty && stack.getCount > 0 && {
      val event = new RobotPlaceBlockEvent.Pre(agent, stack, level, pos)
      NeoForge.EVENT_BUS.post(event)
      if (event.isCanceled) false
      else {
        val fakeEyeHeight = if (getXRot < 0 && isSomeKindOfPiston(stack)) 1.82 else 0
        setPos(getX, getY - fakeEyeHeight, getZ)
        Player.setPlayerInventoryItems(this)
        val state = level.getBlockState(pos)
        val traceEndPos = new Vec3(pos.getX + hitX, pos.getY + hitY, pos.getZ + hitZ)
        val traceCtx = if (state.isAir()) BlockHitResult.miss(traceEndPos, side, pos) else new BlockHitResult(traceEndPos, side, pos, false)
        val didPlace = stack.useOn(new UseOnContext(level, this, InteractionHand.OFF_HAND, stack, traceCtx))
        Player.detectPlayerInventoryChanges(this)
        setPos(getX, getY + fakeEyeHeight, getZ)
        if (didPlace.consumesAction) {
          NeoForge.EVENT_BUS.post(new RobotPlaceBlockEvent.Post(agent, stack, level, pos))
        }
        didPlace.consumesAction
      }
    }
  }

  private def isSomeKindOfPiston(stack: ItemStack) =
    stack.getItem match {
      case itemBlock: BlockItem =>
        val block = itemBlock.getBlock
        block != null && block.isInstanceOf[PistonBaseBlock]
      case _ => false
    }

  // ----------------------------------------------------------------------- //

  override def causeFoodExhaustion(amount: Float): Unit = {
    if (Settings.get.robotExhaustionCost > 0) {
      agent.machine.node match {
        case connector: Connector => connector.changeBuffer(-Settings.get.robotExhaustionCost * amount)
        case _ => // This shouldn't happen... oh well.
      }
    }
    NeoForge.EVENT_BUS.post(new RobotExhaustionEvent(agent, amount))
  }

  override def closeContainer(): Unit = {}

  override def swing(hand: InteractionHand): Unit = {}

  override protected def getPermissionLevel: Int = {
    val config = server.getPlayerList
    if (config.isOp(getGameProfile)) {
      config.getOps.get(getGameProfile) match {
        case opEntry: ServerOpListEntry => opEntry.getLevel
        case _ => server.getOperatorUserPermissionLevel
      }
    }
    else 0
  }

  override def canHarmPlayer(player: PlayerEntity): Boolean = Settings.get.canAttackPlayers

  override def canEat(value: Boolean) = false

  override def canBeAffected(effect: MobEffectInstance) = false

  override def doHurtTarget(entity: Entity) = false

  override def hurt(source: DamageSource, damage: Float) = false

  override def heal(amount: Float): Unit = {}

  override def setHealth(value: Float): Unit = {}

  override def remove(reason: RemovalReason): Unit = super.remove(RemovalReason.KILLED)

  override def aiStep(): Unit = {}

  override def take(entity: Entity, count: Int): Unit = {}

  override def setLastHurtByMob(entity: LivingEntity): Unit = {}

  override def setLastHurtMob(entity: Entity): Unit = {}

  override def startRiding(entityIn: Entity, force: Boolean): Boolean = false

  override def startSleepInBed(bedLocation: BlockPos) = Either.left[BedStatus, net.minecraft.util.Unit](BedStatus.OTHER_PROBLEM)

  override def sendSystemMessage(message: Component): Unit = {}

  override def openCommandBlock(commandBlock: CommandBlockEntity): Unit = {}

  override def sendMerchantOffers(containerId: Int, offers: MerchantOffers, villagerLevel: Int, villagerXP: Int, showProgress: Boolean, canRestock: Boolean): Unit = {}

  override def openMenu(guiOwner: MenuProvider) = util.OptionalInt.empty

  override def openMinecartCommandBlock(thing: BaseCommandBlock): Unit = {}

  override def openTextEdit(signTile: SignBlockEntity, isFront: Boolean): Unit = {}

  // ----------------------------------------------------------------------- //

  class DamageOverTime(val player: Player, val pos: BlockPos, val side: Direction, val ticksTotal: Int) {
    val level: Level = player.level
    var ticks = 0
    var lastDamageSent = 0

    def tick(): Unit = {
      // Cancel if the agent stopped or our action is invalidated some other way.
      if (level != player.level || !level.isLoaded(pos) || level.isEmptyBlock(pos) || !player.agent.machine.isRunning) {
        player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, side, player.level.getMaxBuildHeight, 0)
        return
      }

      val damage = 10 * ticks / Math.max(ticksTotal, 1)
      if (damage < 10) {
        ticks += 1
        if (damage != lastDamageSent) {
          lastDamageSent = damage
          if (!PlayerInteractionManagerHelper.updateBlockRemoving(player))
            return
        }
        EventHandler.scheduleServer(() => tick())
      }
      else {
        callUsingItemInSlot(player.agent.equipmentInventory(), 0, _ => {
          this.player.setPos(this.player.getX - side.getStepX / 2.0, this.player.getY, this.player.getZ - side.getStepZ / 2.0)
          val expGained: Int = PlayerInteractionManagerHelper.blockRemoving(player, pos)
          this.player.setPos(this.player.getX + side.getStepX / 2.0, this.player.getY, this.player.getZ + side.getStepZ / 2.0)
          if (expGained >= 0) {
            NeoForge.EVENT_BUS.post(new RobotBreakBlockEvent.Post(agent, expGained))
          }
        })
      }
    }
  }
}
