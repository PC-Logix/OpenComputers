package li.cil.oc.client;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers$;
import li.cil.oc.Settings;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Textures {
    public static final Textures INSTANCE = new Textures();
    private Textures() {}
    public static final class Font extends SimpleTextureBundle {
        public static final Font INSTANCE = new Font();
        private Font() {}
        @Override protected String basePath() { return "font/%s"; }
        public static final ResourceLocation Aliased = INSTANCE.L("chars_aliased");
        public static final ResourceLocation AntiAliased = INSTANCE.L("chars");
    }
    public static final class GUI {
        private GUI() {}
        private static ResourceLocation L(String name) { return ResourceLocation.fromNamespaceAndPath(OpenComputers$.MODULE$.ID(), "textures/gui/" + name + ".png"); }
        public static final ResourceLocation Background = GUI.L("background");
        public static final ResourceLocation Borders = GUI.L("borders");
        public static final ResourceLocation Computer = GUI.L("computer");
        public static final ResourceLocation Database = GUI.L("database");
        public static final ResourceLocation Database1 = GUI.L("database1");
        public static final ResourceLocation Database2 = GUI.L("database2");
        public static final ResourceLocation Disassembler = GUI.L("disassembler");
        public static final ResourceLocation Drive = GUI.L("drive");
        public static final ResourceLocation Drone = GUI.L("drone");
        public static final ResourceLocation Manual = GUI.L("manual");
        public static final ResourceLocation Nanomachines = GUI.L("nanomachines_power");
        public static final ResourceLocation NanomachinesBar = GUI.L("nanomachines_power_bar");
        public static final ResourceLocation Printer = GUI.L("printer");
        public static final ResourceLocation Rack = GUI.L("rack");
        public static final ResourceLocation Raid = GUI.L("raid");
        public static final ResourceLocation Range = GUI.L("range");
        public static final ResourceLocation Robot = GUI.L("robot");
        public static final ResourceLocation RobotAssembler = GUI.L("robot_assembler");
        public static final ResourceLocation RobotNoScreen = GUI.L("robot_noscreen");
        public static final ResourceLocation Server = GUI.L("server");
        public static final ResourceLocation Slot = GUI.L("slot");
        public static final ResourceLocation TapePlayer = GUI.L("tape_player");
        public static final ResourceLocation Waypoint = GUI.L("waypoint");
    }
    public static final class GUISprites {
        private GUISprites() {}
        private static ResourceLocation L(String name) { return ResourceLocation.fromNamespaceAndPath(OpenComputers$.MODULE$.ID(), name); }
        private static WidgetSprites simpleButton(String name) { return new WidgetSprites(L(name), L(name).withSuffix("_highlight")); }
        public static final ResourceLocation Bar = GUISprites.L("bar");
        public static final ResourceLocation KeyboardMissing = GUISprites.L("keyboard_missing");
        public static final ResourceLocation ManualMissingItem = GUISprites.L("manual_missing_item");
        public static final ResourceLocation PrinterInk = GUISprites.L("printer_ink");
        public static final ResourceLocation PrinterMaterial = GUISprites.L("printer_material");
        public static final ResourceLocation PrinterProgress = GUISprites.L("printer_progress");
        public static final ResourceLocation RobotSelection = GUISprites.L("robot_selection");
        public static final ResourceLocation UpgradeTab = GUISprites.L("upgrade_tab");
        public static final ResourceLocation ManualHome = GUISprites.L("manual_home");
        public static final WidgetSprites ButtonDriveMode = new WidgetSprites(L("button_drive_mode"),L("button_drive_mode_disabled"),L("button_drive_mode_highlight"));
        public static final WidgetSprites ButtonPower = new WidgetSprites(L("button_power_on"),L("button_power_off"),L("button_power_on_highlight"),L("button_power_off_highlight"));
        public static final WidgetSprites ButtonRelay = simpleButton("button_relay");
        public static final WidgetSprites ButtonRun = new WidgetSprites(L("button_run"),L("button_run_disabled"),L("button_run_highlight"));
        public static final WidgetSprites ButtonScroll = simpleButton("button_scroll");
        public static final WidgetSprites ManualTab = simpleButton("manual_tab");
    }
    public static final class Icons extends SimpleTextureBundle {
        public static final Icons INSTANCE = new Icons();
        private final Map<String,ResourceLocation> forSlotType=new HashMap<>();
        private final Map<Integer,ResourceLocation> forTier=new HashMap<>();
        private Icons(){for(String n:Slot.All)forSlotType.put(n,L(n));forTier.put(Tier.None,L("na"));for(int t=Tier.One;t<=Tier.Four;t++)forTier.put(t,L("tier"+t));}
        public static ResourceLocation get(String slotType){return INSTANCE.forSlotType.get(slotType);}
        public static ResourceLocation get(int tier){return INSTANCE.forTier.get(tier);}
        @Override protected String basePath(){return "icons/%s";}
    }
    public static final class Model extends SimpleTextureBundle {
        public static final Model INSTANCE=new Model();
        private Model(){}
        @Override protected String basePath(){return "model/%s";}
        public static final ResourceLocation UpgradeCrafting = INSTANCE.L("crafting_upgrade");
        public static final ResourceLocation UpgradeGenerator = INSTANCE.L("generator_upgrade");
        public static final ResourceLocation UpgradeInventory = INSTANCE.L("inventory_upgrade");
        public static final ResourceLocation HologramEffect = INSTANCE.L("hologram_effect");
        public static final ResourceLocation Drone = INSTANCE.L("drone");
        public static final ResourceLocation Robot = INSTANCE.L("robot");
        public static final ResourceLocation RobotProgressFlag = INSTANCE.L("robot_progress_flag");
        public static final ResourceLocation RobotLesbianFlag = INSTANCE.L("robot_lesbian_flag");
        public static final ResourceLocation RobotBisexualFlag = INSTANCE.L("robot_bisexual_flag");
        public static final ResourceLocation RobotPansexualFlag = INSTANCE.L("robot_pansexual_flag");
        public static final ResourceLocation RobotAsexualFlag = INSTANCE.L("robot_asexual_flag");
        public static final ResourceLocation RobotRainbowFlag = INSTANCE.L("robot_rainbow_flag");
        public static final ResourceLocation RobotTransFlag = INSTANCE.L("robot_trans_flag");
    }
    public static final class Item {
        private Item(){}
        public static final ResourceLocation DroneItem=ResourceLocation.fromNamespaceAndPath(OpenComputers$.MODULE$.ID(),"item/drone");
        public static final ResourceLocation Robot=ResourceLocation.fromNamespaceAndPath(OpenComputers$.MODULE$.ID(),"item/robot");
        private static final ResourceLocation LocationTerminalOn=ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(),Constants.ItemName.Terminal+"_on");
        private static final ResourceLocation LocationTerminalOff=ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(),Constants.ItemName.Terminal+"_off");
        public static final ModelResourceLocation TerminalOn=new ModelResourceLocation(LocationTerminalOn,"inventory");
        public static final ModelResourceLocation TerminalOff=new ModelResourceLocation(LocationTerminalOff,"inventory");
    }
    public static final class Block {
        private Block(){}
        private static ResourceLocation blockLocation(String name){return ResourceLocation.fromNamespaceAndPath(OpenComputers$.MODULE$.ID(),"block/"+name);}
        public static final ResourceLocation AdapterOn = Block.blockLocation("overlay/adapter_on");
        public static final ResourceLocation AssemblerSideAssembling = Block.blockLocation("overlay/assembler_side_assembling");
        public static final ResourceLocation AssemblerSideOn = Block.blockLocation("overlay/assembler_side_on");
        public static final ResourceLocation AssemblerTopOn = Block.blockLocation("overlay/assembler_top_on");
        public static final ResourceLocation CaseFrontActivity = Block.blockLocation("overlay/case_front_activity");
        public static final ResourceLocation CaseFrontError = Block.blockLocation("overlay/case_front_error");
        public static final ResourceLocation CaseFrontOn = Block.blockLocation("overlay/case_front_on");
        public static final ResourceLocation ChargerFrontOn = Block.blockLocation("overlay/charger_front_on");
        public static final ResourceLocation ChargerSideOn = Block.blockLocation("overlay/charger_side_on");
        public static final ResourceLocation DisassemblerSideOn = Block.blockLocation("overlay/disassembler_side_on");
        public static final ResourceLocation DisassemblerTopOn = Block.blockLocation("overlay/disassembler_top_on");
        public static final ResourceLocation DiskDriveFrontActivity = Block.blockLocation("overlay/diskdrive_front_activity");
        public static final ResourceLocation GeolyzerTopOn = Block.blockLocation("overlay/geolyzer_top_on");
        public static final ResourceLocation MicrocontrollerFrontLight = Block.blockLocation("overlay/microcontroller_front_light");
        public static final ResourceLocation MicrocontrollerFrontOn = Block.blockLocation("overlay/microcontroller_front_on");
        public static final ResourceLocation MicrocontrollerFrontError = Block.blockLocation("overlay/microcontroller_front_error");
        public static final ResourceLocation NetSplitterOn = Block.blockLocation("overlay/netsplitter_on");
        public static final ResourceLocation PowerDistributorSideOn = Block.blockLocation("overlay/powerdistributor_side_on");
        public static final ResourceLocation PowerDistributorTopOn = Block.blockLocation("overlay/powerdistributor_top_on");
        public static final ResourceLocation RackDiskDrive = Block.blockLocation("rack_disk_drive");
        public static final ResourceLocation RackDiskDriveActivity = Block.blockLocation("overlay/rack_disk_drive_activity");
        public static final ResourceLocation RackCapacitor = Block.blockLocation("rack_capacitor");
        public static final ResourceLocation RackCapacitorOn = Block.blockLocation("overlay/rack_capacitor_on");
        public static final ResourceLocation RackServer = Block.blockLocation("rack_server");
        public static final ResourceLocation RackServerActivity = Block.blockLocation("overlay/rack_server_activity");
        public static final ResourceLocation RackServerOn = Block.blockLocation("overlay/rack_server_on");
        public static final ResourceLocation RackServerError = Block.blockLocation("overlay/rack_server_error");
        public static final ResourceLocation RackServerNetworkActivity = Block.blockLocation("overlay/rack_server_network_activity");
        public static final ResourceLocation RackTerminalServer = Block.blockLocation("rack_terminal_server");
        public static final ResourceLocation RackTerminalServerOn = Block.blockLocation("overlay/rack_terminal_server_on");
        public static final ResourceLocation RackTerminalServerPresence = Block.blockLocation("overlay/rack_terminal_server_presence");
        public static final ResourceLocation RackKVM = Block.blockLocation("rack_kvm");
        public static final ResourceLocation RackKVMPresence = Block.blockLocation("overlay/rack_kvm_presence");
        public static final ResourceLocation RaidFrontActivity = Block.blockLocation("overlay/raid_front_activity");
        public static final ResourceLocation RaidFrontError = Block.blockLocation("overlay/raid_front_error");
        public static final ResourceLocation ScreenUpIndicator = Block.blockLocation("overlay/screen_up_indicator");
        public static final ResourceLocation SwitchSideOn = Block.blockLocation("overlay/switch_side_on");
        public static final ResourceLocation TransposerOn = Block.blockLocation("overlay/transposer_on");
        public static final ResourceLocation GenericTop = Block.blockLocation("generic_top");
        public static final ResourceLocation NetSplitterSide = Block.blockLocation("netsplitter_side");
        public static final ResourceLocation NetSplitterTop = Block.blockLocation("netsplitter_top");
        public static final ResourceLocation RackFront = Block.blockLocation("rack_front");
        public static final ResourceLocation RackSide = Block.blockLocation("rack_side");
        public static final class Screen {
            public static final Screen INSTANCE = new Screen();
            private Screen(){}
            private static ResourceLocation L(String name){return Block.blockLocation(name);}
            public static final ResourceLocation[] Single = new ResourceLocation[] { L("screen/b"), L("screen/b"), L("screen/b2"), L("screen/b2"), L("screen/b2"), L("screen/b2") };
            public static final ResourceLocation[] SingleFront = new ResourceLocation[] { L("screen/f"), L("screen/f2") };
            public static final ResourceLocation[][][] Horizontal = new ResourceLocation[][][] { { { L("screen/bht"), L("screen/bhb"), L("screen/bht2"), L("screen/bht2"), L("screen/b2"), L("screen/b2") }, { L("screen/bhm"), L("screen/bhm"), L("screen/bhm2"), L("screen/bhm2"), L("screen/b"), L("screen/b") }, { L("screen/bhb"), L("screen/bht"), L("screen/bhb2"), L("screen/bhb2"), L("screen/b2"), L("screen/b2") } }, { { L("screen/bhb2"), L("screen/bht2"), L("screen/bht"), L("screen/bhb"), L("screen/b2"), L("screen/b2") }, { L("screen/bhm2"), L("screen/bhm2"), L("screen/bhm"), L("screen/bhm"), L("screen/b"), L("screen/b") }, { L("screen/bht2"), L("screen/bhb2"), L("screen/bhb"), L("screen/bht"), L("screen/b2"), L("screen/b2") } } };
            public static final ResourceLocation[][] HorizontalFront = new ResourceLocation[][] { { L("screen/fhb2"), L("screen/fhm2"), L("screen/fht2") }, { L("screen/fhb"), L("screen/fhm"), L("screen/fht") } };
            public static final ResourceLocation[][][] Vertical = new ResourceLocation[][][] { { { L("screen/b"), L("screen/b"), L("screen/bvt"), L("screen/bvt"), L("screen/bvt"), L("screen/bvt") }, { L("screen/b"), L("screen/b"), L("screen/bvm"), L("screen/bvm"), L("screen/bvm"), L("screen/bvm") }, { L("screen/b"), L("screen/b"), L("screen/bvb2"), L("screen/bvb2"), L("screen/bvb2"), L("screen/bvb2") } }, { { L("screen/b2"), L("screen/b2"), L("screen/bvt"), L("screen/bvt"), L("screen/bht2"), L("screen/bhb2") }, { L("screen/b"), L("screen/b"), L("screen/bvm"), L("screen/bvm"), L("screen/bhm2"), L("screen/bhm2") }, { L("screen/b2"), L("screen/b2"), L("screen/bvb"), L("screen/bvb"), L("screen/bhb2"), L("screen/bht2") } } };
            public static final ResourceLocation[][] VerticalFront = new ResourceLocation[][] { { L("screen/fvt"), L("screen/fvm"), L("screen/fvb2") }, { L("screen/fvt"), L("screen/fvm"), L("screen/fvb") } };
            public static final ResourceLocation[][][][] Multi = new ResourceLocation[][][][] { { { { L("screen/bht"), L("screen/bhb"), L("screen/btl"), L("screen/btr"), L("screen/bvb"), L("screen/bvt") }, { L("screen/bhm"), L("screen/bhm"), L("screen/btm"), L("screen/btm"), L("screen/b"), L("screen/b") }, { L("screen/bhb"), L("screen/bht"), L("screen/btr"), L("screen/btl"), L("screen/bvt"), L("screen/bvb") } }, { { L("screen/b"), L("screen/b"), L("screen/bml"), L("screen/bmr"), L("screen/bvm"), L("screen/bvm") }, { L("screen/b"), L("screen/b"), L("screen/bmm"), L("screen/bmm"), L("screen/b"), L("screen/b") }, { L("screen/b"), L("screen/b"), L("screen/bmr"), L("screen/bml"), L("screen/bvm"), L("screen/bvt") } }, { { L("screen/bht"), L("screen/bhb"), L("screen/bbl2"), L("screen/bbr2"), L("screen/bvt"), L("screen/bvb2") }, { L("screen/bhm"), L("screen/bhm"), L("screen/bbm2"), L("screen/bbm2"), L("screen/b"), L("screen/b") }, { L("screen/bhb"), L("screen/bht"), L("screen/bbr2"), L("screen/bbl2"), L("screen/bvb2"), L("screen/bvt") } } }, { { { L("screen/bhb2"), L("screen/bht2"), L("screen/btl"), L("screen/btr"), L("screen/bht2"), L("screen/bhb2") }, { L("screen/bhm2"), L("screen/bhm2"), L("screen/btm"), L("screen/btm"), L("screen/b"), L("screen/b") }, { L("screen/bht2"), L("screen/bhb2"), L("screen/btr"), L("screen/btl"), L("screen/bht2"), L("screen/bhb2") } }, { { L("screen/b"), L("screen/b"), L("screen/bml"), L("screen/bml"), L("screen/bhm2"), L("screen/bhm2") }, { L("screen/b"), L("screen/b"), L("screen/bmm"), L("screen/bmm"), L("screen/b"), L("screen/b") }, { L("screen/b"), L("screen/b"), L("screen/bmr"), L("screen/bmr"), L("screen/bhm2"), L("screen/bhm2") } }, { { L("screen/bhb2"), L("screen/bht2"), L("screen/bbl"), L("screen/bbr"), L("screen/bhb2"), L("screen/bht2") }, { L("screen/bhm2"), L("screen/bhm2"), L("screen/bbm"), L("screen/bbm"), L("screen/b"), L("screen/b") }, { L("screen/bht2"), L("screen/bhb2"), L("screen/bbr"), L("screen/bbl"), L("screen/bhb2"), L("screen/bht2") } } } };
            public static final ResourceLocation[][][] MultiFront = new ResourceLocation[][][] { { { L("screen/ftr"), L("screen/ftm"), L("screen/ftl") }, { L("screen/fmr"), L("screen/fmm"), L("screen/fml") }, { L("screen/fbr2"), L("screen/fbm2"), L("screen/fbl2") } }, { { L("screen/ftr"), L("screen/ftm"), L("screen/ftl") }, { L("screen/fmr"), L("screen/fmm"), L("screen/fml") }, { L("screen/fbr"), L("screen/fbm"), L("screen/fbl") } } };
            public static void makeSureThisIsInitialized(){}
        }
        static { Screen.makeSureThisIsInitialized(); }
        public static void bind(){Textures.bind(InventoryMenu.BLOCK_ATLAS);}
    }
    public static TextureAtlasSprite getSprite(ResourceLocation location){return Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(location);}
    public static void bind(ResourceLocation location){if(location!=null)RenderSystem.setShaderTexture(0,location);else RenderSystem.setShaderTexture(0,0);}
    @SubscribeEvent
    public void onRegisterReloadListeners(RegisterClientReloadListenersEvent event){
        event.registerReloadListener(new ResourceManagerReloadListener(){
            @Override public void onResourceManagerReload(ResourceManager manager){
                TextureManager tm=Minecraft.getInstance().getTextureManager();
                register(tm,Font.INSTANCE);register(tm,Icons.INSTANCE);register(tm,Model.INSTANCE);
            }
        });
    }
    private static void register(TextureManager tm,SimpleTextureBundle bundle){
        for(ResourceLocation loc:bundle.locations)tm.register(loc,new SimpleTexture(ResourceLocation.fromNamespaceAndPath(loc.getNamespace(),"textures/"+loc.getPath()+".png")));
    }
    public abstract static class SimpleTextureBundle {
        private final List<ResourceLocation> locations=new ArrayList<>();
        protected ResourceLocation L(String name){ResourceLocation loc=ResourceLocation.fromNamespaceAndPath(OpenComputers$.MODULE$.ID(),String.format(basePath(),name));locations.add(loc);return loc;}
        protected abstract String basePath();
    }
}