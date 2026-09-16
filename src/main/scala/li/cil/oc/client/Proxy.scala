package li.cil.oc.client

import li.cil.oc.{api, client}
import li.cil.oc.client.gui.GuiTypes
import li.cil.oc.client.renderer._
import li.cil.oc.client.renderer.block.{ModelInitialization, NetSplitterModel}
import li.cil.oc.client.renderer.entity.{DroneRenderer, ModelQuadcopter, TrainRobotRenderer}
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.common.{PacketHandler => CommonPacketHandler, Proxy => CommonProxy}
import li.cil.oc.common.blockentity.BlockEntityTypes
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.entity.EntityTypes
import li.cil.oc.common.event.{NanomachinesHandler, RackMountableRenderHandler}
import li.cil.oc.util.Audio
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.neoforged.bus.api.{IEventBus, SubscribeEvent}
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.client.event.{EntityRenderersEvent, RegisterKeyMappingsEvent}
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

private[oc] class Proxy(modBus: IEventBus) extends CommonProxy(modBus) {
  NeoForge.EVENT_BUS.addListener(CommandHandler.onRegisterCommands)
  modBus.register(classOf[GuiTypes])
  modBus.register(ModelInitialization)
  modBus.register(NetSplitterModel)
  modBus.register(Textures)

  override def preInit(): Unit = {
    super.preInit()

    api.API.manual = client.Manual
  }

  override def init(e: FMLCommonSetupEvent): Unit = {
    super.init(e)

    CommonPacketHandler.clientHandler = PacketHandler

    NeoForge.EVENT_BUS.register(HighlightRenderer)
    NeoForge.EVENT_BUS.register(NanomachinesHandler.Client)
    NeoForge.EVENT_BUS.register(PetRenderer)
    NeoForge.EVENT_BUS.register(RackMountableRenderHandler)
    NeoForge.EVENT_BUS.register(Sound)
    NeoForge.EVENT_BUS.register(TextBuffer)
    NeoForge.EVENT_BUS.register(MFUTargetRenderer)
    NeoForge.EVENT_BUS.register(WirelessNetworkDebugRenderer)
    NeoForge.EVENT_BUS.register(Audio)
    NeoForge.EVENT_BUS.register(HologramRenderer)
    NeoForge.EVENT_BUS.register(ProjectorRenderer)
    NeoForge.EVENT_BUS.register(ScreenRenderer)
    NeoForge.EVENT_BUS.register(TabletRenderer)
  }

  @SubscribeEvent
  def onRegisterLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions): Unit = {
    event.registerLayerDefinition(ModelQuadcopter.LAYER_LOCATION, () => ModelQuadcopter.createLayer())
  }

  @SubscribeEvent
  def onRegisterKeyMappings(event: RegisterKeyMappingsEvent): Unit = {
    event.register(KeyBindings.extendedTooltip)
    event.register(KeyBindings.analyzeCopyAddr)
    event.register(KeyBindings.clipboardPaste)
  }

  @SubscribeEvent
  def onRegisterRenderers(e: EntityRenderersEvent.RegisterRenderers): Unit = {
    e.registerEntityRenderer(EntityTypes.DRONE.get(), ctx => new DroneRenderer(ctx))
    e.registerEntityRenderer(EntityTypes.TRAIN_ROBOT.get(), ctx => new TrainRobotRenderer(ctx))

    BlockEntityRenderers.register(BlockEntityTypes.ADAPTER.get(), AdapterRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.ASSEMBLER.get(), AssemblerRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.CASE.get(), CaseRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.CHARGER.get(), ChargerRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.DISASSEMBLER.get(), DisassemblerRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.DISK_DRIVE.get(), DiskDriveRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.HOLOGRAM.get(), HologramRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.PROJECTOR.get(), ProjectorRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.MICROCONTROLLER.get(), ctx => new MicrocontrollerRenderer(ctx))
    BlockEntityRenderers.register(BlockEntityTypes.NET_SPLITTER.get(), ctx => new NetSplitterRenderer(ctx))
    BlockEntityRenderers.register(BlockEntityTypes.POWER_DISTRIBUTOR.get(), PowerDistributorRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.PRINTER.get(), PrinterRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.RAID.get(), RaidRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.RACK.get(), RackRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.RELAY.get(), RelayRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.ROBOT.get(), RobotRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.SCREEN.get(), ScreenRenderer)
    BlockEntityRenderers.register(BlockEntityTypes.TRANSPOSER.get(), TransposerRenderer)
  }

  @SubscribeEvent
  def onRegisterPayloads(event: RegisterPayloadHandlersEvent): Unit = {
    registerPacket(event)
  }
}
