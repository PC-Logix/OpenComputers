package li.cil.oc.client;

import li.cil.oc.api.API;
import li.cil.oc.client.gui.GuiTypes;
import li.cil.oc.client.renderer.HighlightRenderer;
import li.cil.oc.client.renderer.MFUTargetRenderer;
import li.cil.oc.client.renderer.PetRenderer;
import li.cil.oc.client.renderer.TabletRenderer;
import li.cil.oc.client.renderer.WirelessNetworkDebugRenderer;
import li.cil.oc.client.renderer.block.ModelInitialization;
import li.cil.oc.client.renderer.block.NetSplitterModel;
import li.cil.oc.client.renderer.entity.DroneRenderer;
import li.cil.oc.client.renderer.entity.ModelQuadcopter;
import li.cil.oc.client.renderer.entity.TrainRobotRenderer;
import li.cil.oc.client.renderer.tileentity.AdapterRenderer;
import li.cil.oc.client.renderer.tileentity.AssemblerRenderer;
import li.cil.oc.client.renderer.tileentity.CaseRenderer;
import li.cil.oc.client.renderer.tileentity.ChargerRenderer;
import li.cil.oc.client.renderer.tileentity.DisassemblerRenderer;
import li.cil.oc.client.renderer.tileentity.DiskDriveRenderer;
import li.cil.oc.client.renderer.tileentity.HologramRenderer;
import li.cil.oc.client.renderer.tileentity.MicrocontrollerRenderer;
import li.cil.oc.client.renderer.tileentity.NetSplitterRenderer;
import li.cil.oc.client.renderer.tileentity.PowerDistributorRenderer;
import li.cil.oc.client.renderer.tileentity.ProjectorRenderer;
import li.cil.oc.client.renderer.tileentity.PrinterRenderer;
import li.cil.oc.client.renderer.tileentity.RackRenderer;
import li.cil.oc.client.renderer.tileentity.RaidRenderer;
import li.cil.oc.client.renderer.tileentity.RelayRenderer;
import li.cil.oc.client.renderer.tileentity.RobotRenderer;
import li.cil.oc.client.renderer.tileentity.ScreenRenderer;
import li.cil.oc.client.renderer.tileentity.TransposerRenderer;
import li.cil.oc.common.PacketHandler;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.component.TextBufferEvents;
import li.cil.oc.common.entity.EntityTypes;
import li.cil.oc.common.event.NanomachinesHandler;
import li.cil.oc.common.event.RackMountableRenderHandler;
import li.cil.oc.util.Audio;
import li.cil.oc.util.Audio;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class Proxy extends li.cil.oc.common.Proxy {
    public Proxy(IEventBus modBus) {
        super(modBus);
        NeoForge.EVENT_BUS.addListener(CommandHandler::onRegisterCommands);
        modBus.register(GuiTypes.class);
        modBus.register(ModelInitialization.INSTANCE);
        modBus.register(NetSplitterModel.INSTANCE);
        modBus.register(Textures.INSTANCE);
    }

    @Override
    public void preInit() {
        super.preInit();
        API.manual = Manual.API;
    }

    @Override
    public void init(FMLCommonSetupEvent event) {
        super.init(event);
        PacketHandler.clientHandler = li.cil.oc.client.PacketHandler.INSTANCE;
        NeoForge.EVENT_BUS.register(HighlightRenderer.INSTANCE);
        NeoForge.EVENT_BUS.register(NanomachinesHandler.Client.INSTANCE);
        NeoForge.EVENT_BUS.register(PetRenderer.INSTANCE);
        NeoForge.EVENT_BUS.register(RackMountableRenderHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(Sound.INSTANCE);
        NeoForge.EVENT_BUS.register(TextBufferEvents.INSTANCE);
        NeoForge.EVENT_BUS.register(MFUTargetRenderer.INSTANCE);
        NeoForge.EVENT_BUS.register(WirelessNetworkDebugRenderer.INSTANCE);
        NeoForge.EVENT_BUS.register(Audio.INSTANCE);
        NeoForge.EVENT_BUS.register(HologramRenderer.INSTANCE);
        NeoForge.EVENT_BUS.register(ProjectorRenderer.INSTANCE);
        NeoForge.EVENT_BUS.register(ScreenRenderer.INSTANCE);
        NeoForge.EVENT_BUS.register(TabletRenderer.INSTANCE);
        NeoForge.EVENT_BUS.register(TabletClientEvents.INSTANCE);
    }

    @SubscribeEvent
    public void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModelQuadcopter.LAYER_LOCATION, ModelQuadcopter::createLayer);
    }

    @SubscribeEvent
    public void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.extendedTooltip);
        event.register(KeyBindings.analyzeCopyAddr);
        event.register(KeyBindings.clipboardPaste);
    }

    @SubscribeEvent
    public void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypes.DRONE.get(), DroneRenderer::new);
        event.registerEntityRenderer(EntityTypes.TRAIN_ROBOT.get(), TrainRobotRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.ADAPTER.get(), AdapterRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.ASSEMBLER.get(), AssemblerRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.CASE.get(), CaseRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.CHARGER.get(), ChargerRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.DISASSEMBLER.get(), DisassemblerRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.DISK_DRIVE.get(), DiskDriveRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.HOLOGRAM.get(), HologramRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.PROJECTOR.get(), ProjectorRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.MICROCONTROLLER.get(), MicrocontrollerRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.NET_SPLITTER.get(), NetSplitterRenderer::new);
        BlockEntityRenderers.register(BlockEntityTypes.POWER_DISTRIBUTOR.get(), PowerDistributorRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.PRINTER.get(), PrinterRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.RAID.get(), RaidRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.RACK.get(), RackRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.RELAY.get(), RelayRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.ROBOT.get(), RobotRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.SCREEN.get(), ScreenRenderer.INSTANCE);
        BlockEntityRenderers.register(BlockEntityTypes.TRANSPOSER.get(), TransposerRenderer.INSTANCE);
    }

    @SubscribeEvent
    public void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        registerPacket(event);
    }
}
