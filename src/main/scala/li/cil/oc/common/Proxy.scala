package li.cil.oc.common

import li.cil.oc._
import li.cil.oc.common.blockentity.BlockEntityTypes
import li.cil.oc.common.init.OCItems
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.common.menu.MenuTypes
import li.cil.oc.common.entity.EntityTypes
import li.cil.oc.common.init.{OCBlocks, OCItems}
import li.cil.oc.common.item.RedstoneCard
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.integration.Mods
import li.cil.oc.server._
import li.cil.oc.server.machine.luac.{LuaStateFactory, NativeLua52Architecture, NativeLua53Architecture, NativeLua54Architecture}
import li.cil.oc.server.machine.luaj.LuaJLuaArchitecture
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.neoforged.bus.api.{IEventBus, SubscribeEvent}
import net.neoforged.fml.event.lifecycle.{FMLCommonSetupEvent, FMLLoadCompleteEvent}
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.registration.PayloadRegistrar

import scala.jdk.CollectionConverters._

class Proxy(val modBus: IEventBus) {

  def preInit(): Unit = {
    OpenComputers.log.info("Initializing OpenComputers API.")

    api.CreativeTab.instance = CreativeTab.MAIN.getKey
    api.API.driver = driver.Registry
    api.API.fileSystem = fs.FileSystem
    api.API.items = OCItems
    api.API.machine = machine.Machine
    api.API.nanomachines = nanomachines.Nanomachines
    api.API.network = network.Network

    api.API.config = Settings.get.config

    if (LuaStateFactory.isAvailable) {
      if (LuaStateFactory.include53) {
        api.Machine.add(classOf[NativeLua53Architecture])
      }
      if (LuaStateFactory.include54) {
        api.Machine.add(classOf[NativeLua54Architecture])
      }
      if (LuaStateFactory.include52) {
        api.Machine.add(classOf[NativeLua52Architecture])
      }
    }
    if (LuaStateFactory.includeLuaJ) {
      api.Machine.add(classOf[LuaJLuaArchitecture])
    }

    api.Machine.LuaArchitecture =
      if (Settings.get.forceLuaJ) classOf[LuaJLuaArchitecture]
      else api.Machine.architectures.asScala.head
  }

  def init(e: FMLCommonSetupEvent): Unit = {
    e.enqueueWork((() => {
      CommonPacketHandler.serverHandler = server.PacketHandler

      OpenComputers.log.debug("Initializing mod integration.")
      Mods.init()

      api.API.isPowerEnabled = !Settings.get.ignorePower
    }): Runnable)
  }

  def registerPacket(event: RegisterPayloadHandlersEvent): Unit = {
    val registrar: PayloadRegistrar = event.registrar(OpenComputers.ID).versioned("1")

    registrar.playBidirectional(
      PacketPayload.TYPE,
      PacketPayload.STREAM_CODEC,
      (payload: PacketPayload, context) => {
        // manual Runnable instantiation for scala's shitty type system
        context.enqueueWork(new Runnable {
          override def run(): Unit = CommonPacketHandler.handlePacket(context.flow.isClientbound, payload.data, context.player())
        })
      }
    )
  }
}

class ServerProxy(modBus: IEventBus) extends Proxy(modBus) {
  @SubscribeEvent
  def postInit(e: FMLLoadCompleteEvent): Unit = {
    // Don't allow driver registration after this point, to avoid issues.
    driver.Registry.locked = true
  }

  @SubscribeEvent
  def onRegisterPayloads(event: RegisterPayloadHandlersEvent): Unit = {
    registerPacket(event)
  }
}
