package li.cil.oc.common

import li.cil.oc.common.datacomponents.OCComponents
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.client.event.{ClientPlayerNetworkEvent, ClientTickEvent}
import net.neoforged.neoforge.common.util.FakePlayer
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent._
import net.neoforged.neoforge.event.level.{BlockEvent, ChunkEvent, LevelEvent}
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.server.ServerLifecycleHooks

import java.util.Calendar

import li.cil.oc._
import li.cil.oc.api.Network
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.api.internal.{Rack, Server}
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.common.blockentity.Robot
import li.cil.oc.common.component.TerminalServer
import li.cil.oc.common.item.data.{MicrocontrollerData, RobotData, TabletData}
import li.cil.oc.integration.util
import li.cil.oc.server.component.Keyboard
import li.cil.oc.server.machine.luac.LuaStateFactory
import li.cil.oc.server.machine.{Callbacks, Machine}
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedLevel._
import li.cil.oc.util.ExtendedDataComponentHolder._
import li.cil.oc.util.StackOption._
import li.cil.oc.util._
import net.minecraft.server.level.{ChunkHolder, ServerLevel, ServerPlayer}
import net.minecraft.sounds.{SoundEvents, SoundSource}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.api.distmarker.{Dist, OnlyIn}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

object EventHandler {
  private var serverTicks = 0L
  private val pendingServerTimed = mutable.PriorityQueue.empty[(Long, () => Unit)](Ordering.by(x => -x._1))

  private val pendingServer = mutable.Buffer.empty[() => Unit]

  private val pendingClient = mutable.Buffer.empty[() => Unit]

  private val runningRobots = mutable.Set.empty[Robot]

  private val keyboards = java.util.Collections.newSetFromMap[Keyboard](new java.util.WeakHashMap[Keyboard, java.lang.Boolean])

  private val machines = mutable.Set.empty[Machine]

  def onRobotStart(robot: Robot): Unit = runningRobots += robot

  def onRobotStopped(robot: Robot): Unit = runningRobots -= robot

  def addKeyboard(keyboard: Keyboard): Unit = keyboards.asScala += keyboard

  def scheduleClose(machine: Machine): Unit = machines += machine

  def unscheduleClose(machine: Machine): Unit = machines -= machine

  def scheduleServer(tileEntity: BlockEntity): Unit = {
    if (SideTracker.isServer) pendingServer.synchronized {
      pendingServer += (() => Network.joinOrCreateNetwork(tileEntity))
    }
  }

  def scheduleServer(f: () => Unit): Unit = {
    pendingServer.synchronized {
      pendingServer += f
    }
  }

  def scheduleServer(f: () => Unit, delay: Int): Unit = {
    pendingServerTimed.synchronized {
      pendingServerTimed += (serverTicks + (delay max 0)) -> f
    }
  }

  def scheduleClient(f: () => Unit): Unit = {
    pendingClient.synchronized {
      pendingClient += f
    }
  }

  def scheduleWirelessRedstone(rs: server.component.RedstoneWireless): Unit = {
    if (SideTracker.isServer) pendingServer.synchronized {
      pendingServer += (() => if (rs.node.network != null) {
        util.WirelessRedstone.addReceiver(rs)
        util.WirelessRedstone.updateOutput(rs)
      })
    }
  }

  def onRegisterCapabilities(event: RegisterCapabilitiesEvent): Unit = {
    // FUCK YOU SCALA
    // ITS FUCKING SHITTY LANGUAGE EVER
    EventHandlerHelper.registerCapabilities(event)

    integration.neoforge.EventHandlerNeoForge.onRegisterCapabilities(event)
  }

  @SubscribeEvent
  def onServerTickPre(e: ServerTickEvent.Pre): Any = {
    pendingServer.synchronized {
      val adds = pendingServer.toArray
      pendingServer.clear()
      adds
    } foreach (callback => {
      try callback() catch {
        case t: Throwable => OpenComputers.log.warn("Error in scheduled tick action.", t)
      }
    })

    serverTicks += 1
    while (pendingServerTimed.nonEmpty && pendingServerTimed.head._1 < serverTicks) {
      val (_, callback) = pendingServerTimed.dequeue()
      try callback() catch {
        case t: Throwable => OpenComputers.log.warn("Error in scheduled tick action.", t)
      }
    }

    val invalid = mutable.ArrayBuffer.empty[Robot]
    runningRobots.foreach(robot => {
      if (robot.isRemoved) invalid += robot
      else if (robot.getEnvironmentLevel != null) robot.machine.update()
    })
    runningRobots --= invalid
  }

  @SubscribeEvent
  def onServerTickPost(e: ServerTickEvent.Post): Unit = {
    // Clean up machines *after* a tick, to allow stuff to be saved, first.
    val closed = mutable.ArrayBuffer.empty[Machine]
    machines.foreach(machine => if (machine.tryClose()) {
      closed += machine
      if (machine.host.getEnvironmentLevel == null || !machine.host.getEnvironmentLevel.blockExists(BlockPosition(machine.host))) {
        if (machine.node != null) machine.node.remove()
      }
    })
    machines --= closed
  }

  @SubscribeEvent
  def onClientTick(e: ClientTickEvent.Pre): Unit = {
    pendingClient.synchronized {
      val adds = pendingClient.toArray
      pendingClient.clear()
      adds
    } foreach (callback => {
      try callback() catch {
        case t: Throwable => OpenComputers.log.warn("Error in scheduled tick action.", t)
      }
    })
  }

  @SubscribeEvent
  def onPlayerLoggedIn(e: PlayerLoggedInEvent): Unit = {
    if (SideTracker.isServer) e.getEntity match {
      case _: FakePlayer => // Nope
      case player: ServerPlayer =>
        if (!LuaStateFactory.isAvailable && !LuaStateFactory.luajRequested) {
          player.sendSystemMessage(Localization.Chat.WarningLuaFallback)
        }
        // Defer these packets until the client has a world and player instance.
        EventHandler.scheduleServer(() => {
          ServerPacketSender.sendPetVisibility(None, Some(player))
          ServerPacketSender.sendLootDisks(player)
          ServerPacketSender.sendLootEEPROMs(player)
        })
        // Do update check in local games and for OPs.
        val server = ServerLifecycleHooks.getCurrentServer
        if (server.getPlayerList.isOp(player.getGameProfile)) {
          Future {
            UpdateCheck.info foreach {
              case Some(release) => player.sendSystemMessage(Localization.Chat.InfoNewVersion(release.tag_name))
              case _ =>
            }
          }
        }
      case _ =>
    }
  }

  @SubscribeEvent
  @OnlyIn(Dist.CLIENT)
  def clientLoggedIn(e: ClientPlayerNetworkEvent.LoggingIn): Unit = {
    PetRenderer.isInitialized = false
    PetRenderer.hidden.clear()
    Loot.resetDisksForClient()
    Loot.disksForCyclingClient.clear()
    Loot.disksForCyclingClient ++= Loot.disksForCyclingServer.map(_.copy())
    Loot.eepromsForClient.clear()

    client.Sound.stopAll()
  }

  @SubscribeEvent
  def onBlockBreak(e: BlockEvent.BreakEvent): Unit = {
    e.getLevel.getBlockEntity(e.getPos) match {
      case c: blockentity.Case =>
        if (c.isCreative && (!e.getPlayer.isCreative || !c.canInteract(e.getPlayer.getName.getString))) {
          e.setCanceled(true)
        }
      case r: blockentity.RobotProxy =>
        val robot = r.robot
        if (robot.isCreative && (!e.getPlayer.isCreative || !robot.canInteract(e.getPlayer.getName.getString))) {
          e.setCanceled(true)
        }
      case _ =>
    }
  }

  @SubscribeEvent
  def onPlayerRespawn(e: PlayerRespawnEvent): Unit = {
    keyboards.asScala.foreach(_.releasePressedKeys(e.getEntity))
  }

  @SubscribeEvent
  def onPlayerChangedDimension(e: PlayerChangedDimensionEvent): Unit = {
    keyboards.asScala.foreach(_.releasePressedKeys(e.getEntity))
  }

  @SubscribeEvent
  def onPlayerLogout(e: PlayerLoggedOutEvent): Unit = {
    keyboards.asScala.foreach(_.releasePressedKeys(e.getEntity))
  }

  @SubscribeEvent
  def onEntityJoinLevel(e: EntityJoinLevelEvent): Unit = {
    if (Settings.get.giveManualToNewPlayers && !e.getLevel.isClientSide) e.getEntity match {
      case player: Player if !player.isInstanceOf[FakePlayer] =>
        val persistedData = PlayerUtils.persistedData(player)
        if (!persistedData.getBoolean(Settings.namespace + "receivedManual")) {
          persistedData.putBoolean(Settings.namespace + "receivedManual", true)
          player.inventory.add(api.Items.get(Constants.ItemName.Manual).createItemStack(1))
        }
      case _ =>
    }
  }

  lazy val drone: ItemInfo = api.Items.get(Constants.ItemName.Drone)
  lazy val eeprom: ItemInfo = api.Items.get(Constants.ItemName.EEPROM)
  lazy val mcu: ItemInfo = api.Items.get(Constants.BlockName.Microcontroller)
  lazy val navigationUpgrade: ItemInfo = api.Items.get(Constants.ItemName.NavigationUpgrade)
  lazy val robot: ItemInfo = api.Items.get(Constants.BlockName.Robot)
  lazy val tablet: ItemInfo = api.Items.get(Constants.ItemName.Tablet)

  @SubscribeEvent
  def onCrafting(e: ItemCraftedEvent): Unit = {
    var didRecraft = false

    didRecraft = recraft(e, navigationUpgrade, stack => {
      // Restore the map currently used in the upgrade.
      stack.getComponent(OCComponents.SOURCE_MAP_ITEM) match {
        case Some(map) => StackOption(map.mutableCopy())
        case _ => EmptyStack
      }
    }) || didRecraft

    didRecraft = recraft(e, mcu, stack => {
      // Restore EEPROM currently used in microcontroller.
      new MicrocontrollerData(stack).components.find(api.Items.get(_) == eeprom).asStackOption
    }) || didRecraft

    didRecraft = recraft(e, drone, stack => {
      // Restore EEPROM currently used in drone.
      new MicrocontrollerData(stack).components.find(api.Items.get(_) == eeprom).asStackOption
    }) || didRecraft

    // The robot recrafting recipe swaps an EEPROM and returns the old one.
    // Do not treat unrelated robot-to-robot recipes (such as cosmetics) as
    // EEPROM swaps merely because their input and output items match.
    val swapsRobotEeprom = (0 until e.getInventory.getContainerSize).exists(slot =>
      api.Items.get(e.getInventory.getItem(slot)) == eeprom)
    if (swapsRobotEeprom) {
      didRecraft = recraft(e, robot, stack => {
        // Restore EEPROM currently used in robot.
        new RobotData(stack).components.find(api.Items.get(_) == eeprom).asStackOption
      }) || didRecraft
    }

    didRecraft = recraft(e, tablet, stack => {
      // Restore EEPROM currently used in tablet.
      new TabletData(stack).items.collect { case item if !item.isEmpty => item }.find(api.Items.get(_) == eeprom).asStackOption
    }) || didRecraft

    // Presents?
    e.getEntity match {
      case _: FakePlayer => // No presents for you, automaton. Such discrimination. Much bad conscience.
      case player: ServerPlayer if player.level != null && !player.level.isClientSide =>
        // Presents!? If we didn't recraft, it's an OC item, and the time is right...
        if (Settings.get.presentChance > 0 && !didRecraft && api.Items.get(e.getCrafting) != null &&
          e.getEntity.getRandom.nextFloat() < Settings.get.presentChance && timeForPresents) {
          // Presents!
          val present = api.Items.get(Constants.ItemName.Present).createItemStack(1)
          e.getEntity.level.playSound(e.getEntity, e.getEntity.getX, e.getEntity.getY, e.getEntity.getZ, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 0.2f, 1f)
          InventoryUtils.addToPlayerInventory(present, e.getEntity)
        }
      case _ => // Nope.
    }

    Advancement.onCraft(e.getCrafting, e.getEntity)
  }

  @SubscribeEvent
  def onItemEntityPickup(e: ItemEntityPickupEvent.Post): Unit = {
    val entity = e.getItemEntity
    Option(entity).flatMap(e => Option(e.getItem)) match {
      case Some(stack) =>
        Advancement.onAssemble(stack, e.getPlayer)
        Advancement.onCraft(stack, e.getPlayer)
      case _ => // Huh.
    }
  }

  private def timeForPresents = {
    val now = Calendar.getInstance()
    val month = now.get(Calendar.MONTH)
    val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
    // On the 12th day of Christmas, my robot brought to me~
    (month == Calendar.DECEMBER && dayOfMonth > 24) || (month == Calendar.JANUARY && dayOfMonth < 7) ||
      (month == Calendar.FEBRUARY && dayOfMonth == 14) ||
      (month == Calendar.APRIL && dayOfMonth == 22) ||
      (month == Calendar.MAY && dayOfMonth == 1) ||
      (month == Calendar.OCTOBER && dayOfMonth == 3) ||
      (month == Calendar.DECEMBER && dayOfMonth == 14)
  }

  def isItTime: Boolean = {
    val now = Calendar.getInstance()
    val month = now.get(Calendar.MONTH)
    val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
    month == Calendar.APRIL && dayOfMonth == 1
  }

  private def recraft(e: ItemCraftedEvent, item: ItemInfo, callback: ItemStack => StackOption): Boolean = {
    if (api.Items.get(e.getCrafting) == item) {
      for (slot <- 0 until e.getInventory.getContainerSize) {
        val stack = e.getInventory.getItem(slot)
        if (api.Items.get(stack) == item) {
          callback(stack).foreach(extra =>
            InventoryUtils.addToPlayerInventory(extra, e.getEntity))
        }
      }
      true
    }
    else false
  }

  private def getChunks(world: ServerLevel): Iterable[ChunkHolder] = {
    world.getChunkSource.chunkMap.getChunks.asScala
  }

  // This is called from the ServerThread *and* the ClientShutdownThread, which
  // can potentially happen at the same time... for whatever reason. So let's
  // synchronize what we're doing here to avoid race conditions (e.g. when
  // disposing networks, where this actually triggered an assert).
  @SubscribeEvent
  def onWorldUnload(e: LevelEvent.Unload): Unit = this.synchronized {
    val level = e.getLevel

    if (!level.isClientSide) {
      val serverLevel = level.asInstanceOf[ServerLevel]

      val chunkMap = serverLevel.getChunkSource.chunkMap
      chunkMap.getChunks.asScala.foreach { holder =>
        val chunk = holder.getTickingChunk
        if (chunk != null) {
          chunk.getBlockEntities.values().asScala.foreach {
            case te: blockentity.traits.BaseBlockEntity => te.dispose()
            case _ =>
          }
        }
      }

      serverLevel.getAllEntities.asScala.foreach {
        case host: MachineHost => host.machine.stop()
        case _ =>
      }

      Callbacks.clear()
    } else {
      TerminalServer.loaded.clear()
    }
  }

  @SubscribeEvent
  def onChunkUnloaded(e: ChunkEvent.Unload): Unit = {
    val levelAccessor = e.getLevel

    if (!levelAccessor.isClientSide && levelAccessor.isInstanceOf[Level]) {
      val level = levelAccessor.asInstanceOf[Level]

      e.getChunk match {
        case chunk: LevelChunk =>
          chunk.getBlockEntities.values().asScala.foreach {
            case host: MachineHost => host.machine match {
              case machine: Machine => scheduleClose(machine)
              case _ =>
            }
            case rack: Rack =>
              (0 until rack.getContainerSize)
                .map(rack.getMountable)
                .foreach {
                  case server: Server if server.machine != null => server.machine.stop()
                  case _ =>
                }
            case _ =>
          }
          val chunkPos = chunk.getPos
          val aabb = new AABB(
            chunkPos.getMinBlockX, level.getMinBuildHeight, chunkPos.getMinBlockZ,
            chunkPos.getMaxBlockX, level.getMaxBuildHeight, chunkPos.getMaxBlockZ
          )
          level.getEntitiesOfClass(classOf[Entity], aabb).asScala.foreach {
            case host: MachineHost => host.machine match {
              case machine: Machine => scheduleClose(machine)
              case _ =>
            }
            case _ =>
          }

        case _ =>
      }
    }
  }
}
