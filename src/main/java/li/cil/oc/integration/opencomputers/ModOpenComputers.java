package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.IMC;
import li.cil.oc.api.Items;
import li.cil.oc.api.Manual;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.api.internal.Wrench;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.prefab.ItemStackTabIconRenderer;
import li.cil.oc.api.prefab.ResourceContentProvider;
import li.cil.oc.api.prefab.SpriteTabIconRenderer;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.markdown.segment.render.BlockImageProvider;
import li.cil.oc.client.renderer.markdown.segment.render.ItemImageProvider;
import li.cil.oc.client.renderer.markdown.segment.render.TagImageProvider;
import li.cil.oc.client.renderer.markdown.segment.render.TextureImageProvider;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.Loot;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.common.block.SimpleBlock;
import li.cil.oc.common.event.*;
import li.cil.oc.common.item.AnalyzerEvents;
import li.cil.oc.common.item.TabletEvents;
import li.cil.oc.common.nanomachines.provider.*;
import li.cil.oc.common.openprinter.OpenPrinter;
import li.cil.oc.common.template.*;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;
import li.cil.oc.integration.SableTickBridge;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.server.network.Waypoints;
import li.cil.oc.server.network.WirelessNetwork;
import li.cil.oc.util.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Set;
import java.util.Objects;

public final class ModOpenComputers implements ModProxy {
    public static final ModOpenComputers INSTANCE = new ModOpenComputers();
    public static boolean hasRedstoneCardT2;

    private ModOpenComputers() {
    }

    @Override
    public li.cil.oc.integration.Mod getMod() {
        return Mods.OpenComputers();
    }

    @Override
    public void initialize() {
        DroneTemplate.register();
        MicrocontrollerTemplate.register();
        NavigationUpgradeTemplate.register();
        RobotTemplate.register();
        ServerTemplate.register();
        TabletTemplate.register();
        TemplateBlacklist.register();

        IMC.registerWrenchTool("li.cil.oc.integration.opencomputers.ModOpenComputers.useWrench");
        IMC.registerWrenchToolCheck("li.cil.oc.integration.opencomputers.ModOpenComputers.isWrench");
        IMC.registerItemCharge("OpenComputers",
            "li.cil.oc.integration.opencomputers.ModOpenComputers.canCharge",
            "li.cil.oc.integration.opencomputers.ModOpenComputers.charge");
        IMC.registerInkProvider("li.cil.oc.integration.opencomputers.ModOpenComputers.inkCartridgeInkProvider");
        IMC.registerInkProvider("li.cil.oc.integration.opencomputers.ModOpenComputers.dyeInkProvider");

        IMC.registerProgramDiskLabel("build", "builder", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("dig", "dig", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("base64", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("deflate", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("gpg", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("inflate", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("md5sum", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("sha256sum", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("refuel", "generator", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("irc", "irc", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("maze", "maze", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("arp", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("ifconfig", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("ping", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("route", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("opl-flash", "openloader", "Lua 5.2", "Lua 5.3", "LuaJ");
        IMC.registerProgramDiskLabel("oppm", "oppm", "Lua 5.2", "Lua 5.3", "LuaJ");

        OpenComputers.proxy().modBus().addListener(EventHandler::onRegisterCapabilities);
        OpenComputers.proxy().modBus().addListener(ChunkloaderUpgradeHandler.INSTANCE::onRegisterTicketControllers);
        OpenComputers.proxy().modBus().addListener(SableTickBridge::onRegisterTicketControllers);

        NeoForge.EVENT_BUS.register(li.cil.oc.common.EventHandlerListener.INSTANCE);
        NeoForge.EVENT_BUS.register(NanomachinesHandler.Common.INSTANCE);
        NeoForge.EVENT_BUS.register(TabletEvents.INSTANCE);
        NeoForge.EVENT_BUS.register(new AnalyzerEvents());
        NeoForge.EVENT_BUS.register(AngelUpgradeHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(ChunkloaderUpgradeHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(ExperienceUpgradeHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(FileSystemAccessHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(HoverBootsHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(Loot.INSTANCE);
        NeoForge.EVENT_BUS.register(NetworkActivityHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(RobotCommonHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(SaveHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(Waypoints.INSTANCE);
        NeoForge.EVENT_BUS.register(WirelessNetwork.INSTANCE);
        NeoForge.EVENT_BUS.register(WirelessNetworkCardHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(li.cil.oc.client.ComponentTracker.INSTANCE);
        NeoForge.EVENT_BUS.register(li.cil.oc.server.ComponentTracker.INSTANCE);

        Driver.add(ConverterNanomachines.INSTANCE);
        Driver.add(ConverterLinkedCard.INSTANCE);
        Driver.add(DriverAPU.INSTANCE);
        Driver.add(DriverAudioCard.INSTANCE);
        Driver.add(DriverComponentBus.INSTANCE);
        Driver.add(DriverCPU.INSTANCE);
        Driver.add(DriverDataCard.INSTANCE);
        Driver.add(DriverDebugCard.INSTANCE);
        Driver.add(DriverEEPROM.INSTANCE);
        Driver.add(DriverFileSystem.INSTANCE);
        Driver.add(DriverGraphicsCard.INSTANCE);
        Driver.add(DriverQuadGraphicsCard.INSTANCE);
        Driver.add(DriverInternetCard.INSTANCE);
        Driver.add(DriverLinkedCard.INSTANCE);
        Driver.add(DriverNavigationCard.INSTANCE);
        Driver.add(DriverLootDisk.INSTANCE);
        Driver.add(DriverMemory.INSTANCE);
        Driver.add(DriverCreativeMemory.INSTANCE);
        Driver.add(DriverNetworkCard.INSTANCE);
        Driver.add(DriverKeyboard.INSTANCE);
        Driver.add(DriverRedstoneCard.INSTANCE);
        Driver.add(DriverTablet.INSTANCE);
        Driver.add(DriverWirelessNetworkCard.INSTANCE);
        Driver.add(DriverContainerCard.INSTANCE);
        Driver.add(DriverContainerFloppy.INSTANCE);
        Driver.add(DriverContainerUpgrade.INSTANCE);
        Driver.add(DriverGeolyzer.INSTANCE);
        Driver.add(DriverMotionSensor.INSTANCE);
        Driver.add(DriverScreen.INSTANCE);
        Driver.add(DriverTransposer.INSTANCE);
        Driver.add(DriverCapacitorMountable.INSTANCE);
        Driver.add(DriverDiskDriveMountable.INSTANCE);
        Driver.add(DriverServer.INSTANCE);
        Driver.add(DriverTerminalServer.INSTANCE);
        Driver.add(DriverUpgradeAngel.INSTANCE);
        Driver.add(DriverUpgradeBarcodeReader.INSTANCE);
        Driver.add(DriverUpgradeBattery.INSTANCE);
        Driver.add(DriverUpgradeChunkloader.INSTANCE);
        Driver.add(DriverUpgradeCrafting.INSTANCE);
        Driver.add(DriverUpgradeDatabase.INSTANCE);
        Driver.add(DriverUpgradeExperience.INSTANCE);
        Driver.add(DriverUpgradeGenerator.INSTANCE);
        Driver.add(DriverUpgradeHover.INSTANCE);
        Driver.add(DriverUpgradeInventory.INSTANCE);
        Driver.add(DriverUpgradeInventoryController.INSTANCE);
        Driver.add(DriverUpgradeLeash.INSTANCE);
        Driver.add(DriverUpgradeNavigation.INSTANCE);
        Driver.add(DriverUpgradePiston.INSTANCE);
        Driver.add(DriverUpgradeSign.INSTANCE);
        Driver.add(DriverUpgradeSolarGenerator.INSTANCE);
        Driver.add(DriverUpgradeStickyPiston.INSTANCE);
        Driver.add(DriverUpgradeTank.INSTANCE);
        Driver.add(DriverUpgradeTankController.INSTANCE);
        Driver.add(DriverUpgradeTractorBeam.INSTANCE);
        Driver.add(DriverUpgradeTrading.INSTANCE);
        Driver.add(DriverUpgradeMF.INSTANCE);
        Driver.add(DriverAPU.Provider.INSTANCE);
        Driver.add(DriverAudioCard.PROVIDER);
        Driver.add(DriverDataCard.PROVIDER);
        Driver.add(DriverDebugCard.PROVIDER);
        Driver.add(DriverEEPROM.PROVIDER);
        Driver.add(DriverGraphicsCard.PROVIDER);
        Driver.add(DriverQuadGraphicsCard.PROVIDER);
        Driver.add(DriverInternetCard.PROVIDER);
        Driver.add(DriverLinkedCard.PROVIDER);
        Driver.add(DriverNavigationCard.PROVIDER);
        Driver.add(DriverNetworkCard.PROVIDER);
        Driver.add(DriverRedstoneCard.PROVIDER);
        Driver.add(DriverWirelessNetworkCard.PROVIDER);
        Driver.add(DriverGeolyzer.PROVIDER);
        Driver.add(DriverMotionSensor.PROVIDER);
        Driver.add(DriverScreen.PROVIDER);
        Driver.add(DriverTransposer.PROVIDER);
        Driver.add(DriverUpgradeChunkloader.PROVIDER);
        Driver.add(DriverUpgradeCrafting.PROVIDER);
        Driver.add(DriverUpgradeDatabase.PROVIDER);
        Driver.add(DriverUpgradeExperience.PROVIDER);
        Driver.add(DriverUpgradeGenerator.PROVIDER);
        Driver.add(DriverUpgradeInventoryController.PROVIDER);
        Driver.add(DriverUpgradeLeash.PROVIDER);
        Driver.add(DriverUpgradeNavigation.PROVIDER);
        Driver.add(DriverUpgradePiston.PROVIDER);
        Driver.add(DriverUpgradeSign.PROVIDER);
        Driver.add(DriverUpgradeStickyPiston.PROVIDER);
        Driver.add(DriverUpgradeTankController.PROVIDER);
        Driver.add(DriverUpgradeTractorBeam.PROVIDER);
        Driver.add(DriverUpgradeMF.PROVIDER);
        Driver.add(EnvironmentProviderBlocks.INSTANCE);
        Driver.add(InventoryProviderDatabase.INSTANCE);
        Driver.add(InventoryProviderServer.INSTANCE);

        blacklistHost(li.cil.oc.api.internal.Adapter.class,
            Constants.BlockName$.MODULE$.Geolyzer(),
      Constants.BlockName$.MODULE$.MotionSensor(),
      Constants.BlockName$.MODULE$.Keyboard(),
      Constants.BlockName$.MODULE$.ScreenTier1(),
      Constants.BlockName$.MODULE$.Transposer(),
      Constants.BlockName$.MODULE$.CarpetedCapacitor(),
      Constants.ItemName$.MODULE$.Analyzer(),
      Constants.ItemName$.MODULE$.AngelUpgrade(),
      Constants.ItemName$.MODULE$.BatteryUpgradeTier1(),
      Constants.ItemName$.MODULE$.BatteryUpgradeTier2(),
      Constants.ItemName$.MODULE$.BatteryUpgradeTier3(),
      Constants.ItemName$.MODULE$.ChunkloaderUpgrade(),
      Constants.ItemName$.MODULE$.CraftingUpgrade(),
      Constants.ItemName$.MODULE$.ExperienceUpgrade(),
      Constants.ItemName$.MODULE$.GeneratorUpgrade(),
      Constants.ItemName$.MODULE$.HoverUpgradeTier1(),
      Constants.ItemName$.MODULE$.HoverUpgradeTier2(),
      Constants.ItemName$.MODULE$.InventoryUpgrade(),
      Constants.ItemName$.MODULE$.NavigationUpgrade(),
      Constants.ItemName$.MODULE$.PistonUpgrade(),
      Constants.ItemName$.MODULE$.StickyPistonUpgrade(),
      Constants.ItemName$.MODULE$.SolarGeneratorUpgrade(),
      Constants.ItemName$.MODULE$.TankUpgrade(),
      Constants.ItemName$.MODULE$.TractorBeamUpgrade(),
      Constants.ItemName$.MODULE$.LeashUpgrade(),
      Constants.ItemName$.MODULE$.TradingUpgrade());
        blacklistHost(li.cil.oc.api.internal.Drone.class,
            Constants.BlockName$.MODULE$.Keyboard(),
      Constants.BlockName$.MODULE$.ScreenTier1(),
      Constants.BlockName$.MODULE$.Transposer(),
      Constants.BlockName$.MODULE$.CarpetedCapacitor(),
      Constants.ItemName$.MODULE$.Analyzer(),
      Constants.ItemName$.MODULE$.APUTier1(),
      Constants.ItemName$.MODULE$.APUTier2(),
      Constants.ItemName$.MODULE$.GraphicsCardTier1(),
      Constants.ItemName$.MODULE$.GraphicsCardTier2(),
      Constants.ItemName$.MODULE$.GraphicsCardTier3(),
      Constants.ItemName$.MODULE$.NetworkCard(),
      Constants.ItemName$.MODULE$.RedstoneCardTier1(),
      Constants.ItemName$.MODULE$.CraftingUpgrade(),
      Constants.ItemName$.MODULE$.HoverUpgradeTier1(),
      Constants.ItemName$.MODULE$.HoverUpgradeTier2());
        blacklistHost(li.cil.oc.api.internal.Microcontroller.class,
            Constants.BlockName$.MODULE$.Keyboard(),
      Constants.BlockName$.MODULE$.ScreenTier1(),
      Constants.BlockName$.MODULE$.CarpetedCapacitor(),
      Constants.ItemName$.MODULE$.Analyzer(),
      Constants.ItemName$.MODULE$.APUTier1(),
      Constants.ItemName$.MODULE$.APUTier2(),
      Constants.ItemName$.MODULE$.GraphicsCardTier1(),
      Constants.ItemName$.MODULE$.GraphicsCardTier2(),
      Constants.ItemName$.MODULE$.GraphicsCardTier3(),
      Constants.ItemName$.MODULE$.AngelUpgrade(),
      Constants.ItemName$.MODULE$.CraftingUpgrade(),
      Constants.ItemName$.MODULE$.DatabaseUpgradeTier1(),
      Constants.ItemName$.MODULE$.DatabaseUpgradeTier2(),
      Constants.ItemName$.MODULE$.DatabaseUpgradeTier3(),
      Constants.ItemName$.MODULE$.ExperienceUpgrade(),
      Constants.ItemName$.MODULE$.GeneratorUpgrade(),
      Constants.ItemName$.MODULE$.HoverUpgradeTier1(),
      Constants.ItemName$.MODULE$.HoverUpgradeTier2(),
      Constants.ItemName$.MODULE$.InventoryUpgrade(),
      Constants.ItemName$.MODULE$.InventoryControllerUpgrade(),
      Constants.ItemName$.MODULE$.NavigationUpgrade(),
      Constants.ItemName$.MODULE$.TankUpgrade(),
      Constants.ItemName$.MODULE$.TankControllerUpgrade(),
      Constants.ItemName$.MODULE$.TractorBeamUpgrade(),
      Constants.ItemName$.MODULE$.LeashUpgrade(),
      Constants.ItemName$.MODULE$.TradingUpgrade());
        blacklistHost(li.cil.oc.api.internal.Robot.class,
            Constants.BlockName$.MODULE$.Transposer(),
      Constants.BlockName$.MODULE$.CarpetedCapacitor(),
      Constants.ItemName$.MODULE$.Analyzer(),
      Constants.ItemName$.MODULE$.LeashUpgrade());
        blacklistHost(li.cil.oc.api.internal.Tablet.class,
            Constants.BlockName$.MODULE$.ScreenTier1(),
      Constants.BlockName$.MODULE$.Transposer(),
      Constants.BlockName$.MODULE$.CarpetedCapacitor(),
      Constants.ItemName$.MODULE$.NetworkCard(),
      Constants.ItemName$.MODULE$.RedstoneCardTier1(),
      Constants.ItemName$.MODULE$.AngelUpgrade(),
      Constants.ItemName$.MODULE$.ChunkloaderUpgrade(),
      Constants.ItemName$.MODULE$.CraftingUpgrade(),
      Constants.ItemName$.MODULE$.DatabaseUpgradeTier1(),
      Constants.ItemName$.MODULE$.DatabaseUpgradeTier2(),
      Constants.ItemName$.MODULE$.DatabaseUpgradeTier3(),
      Constants.ItemName$.MODULE$.ExperienceUpgrade(),
      Constants.ItemName$.MODULE$.GeneratorUpgrade(),
      Constants.ItemName$.MODULE$.HoverUpgradeTier1(),
      Constants.ItemName$.MODULE$.HoverUpgradeTier2(),
      Constants.ItemName$.MODULE$.InventoryUpgrade(),
      Constants.ItemName$.MODULE$.InventoryControllerUpgrade(),
      Constants.ItemName$.MODULE$.TankUpgrade(),
      Constants.ItemName$.MODULE$.TankControllerUpgrade(),
      Constants.ItemName$.MODULE$.LeashUpgrade(),
      Constants.ItemName$.MODULE$.TradingUpgrade());

        if (BundledRedstone.isAvailable()) {
            OpenComputers.log().info("Found extended redstone mods, enabling tier two redstone card.");
            hasRedstoneCardT2 = true;
        }

        Nanomachines.addProvider(DisintegrationProvider.INSTANCE);
        Nanomachines.addProvider(HungryProvider.INSTANCE);
        Nanomachines.addProvider(ParticleProvider.INSTANCE);
        Nanomachines.addProvider(PotionProvider.INSTANCE);
        Nanomachines.addProvider(MagnetProvider.INSTANCE);

        if (FMLEnvironment.dist.isClient()) initializeClient();
    }

    @OnlyIn(Dist.CLIENT)
    private void initializeClient() {
        Manual.addProvider(DefinitionPathProvider.INSTANCE);
        Manual.addProvider(new ResourceContentProvider(Settings.resourceDomain(), "doc/"));
        Manual.addProvider("", TextureImageProvider.INSTANCE);
        Manual.addProvider("item", ItemImageProvider.INSTANCE);
        Manual.addProvider("block", BlockImageProvider.INSTANCE);
        Manual.addProvider("oredict", TagImageProvider.INSTANCE);
        OpenPrinter.registerManual();

        Manual.addTab(new SpriteTabIconRenderer(Textures.GUISprites.ManualHome),
            "oc:gui.Manual.Home", "%LANGUAGE%/index.md");
        Manual.addTab(new ItemStackTabIconRenderer(Items.get("case1").createItemStack(1)),
            "oc:gui.Manual.Blocks", "%LANGUAGE%/block/index.md");
        Manual.addTab(new ItemStackTabIconRenderer(Items.get("cpu1").createItemStack(1)),
            "oc:gui.Manual.Items", "%LANGUAGE%/item/index.md");
    }

    public static boolean useWrench(Player player, BlockPos pos, boolean changeDurability) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof Wrench wrench &&
            wrench.useWrenchOnBlock(player, player.level(), pos, !changeDurability);
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.getItem() instanceof Wrench;
    }

    public static boolean canCharge(ItemStack stack) {
        return stack.getItem() instanceof Chargeable chargeable && chargeable.canCharge(stack);
    }

    public static double charge(ItemStack stack, double amount, boolean simulate) {
        return stack.getItem() instanceof Chargeable chargeable ? chargeable.charge(stack, amount, simulate) : amount;
    }

    public static int inkCartridgeInkProvider(ItemStack stack) {
        return Objects.equals(Items.get(stack), Items.get(Constants.ItemName$.MODULE$.InkCartridge()))
            ? Settings.get().printInkValue() : 0;
    }

    public static int dyeInkProvider(ItemStack stack) {
        return Color.isDye(stack) ? Settings.get().printInkValue() / 10 : 0;
    }

    private static void blacklistHost(Class<?> host, String... itemNames) {
        for (String itemName : itemNames) {
            try {
                IMC.blacklistHost(itemName, host, Items.get(itemName).createItemStack(1));
            } catch (Throwable exception) {
                OpenComputers.log().warn("Error blacklisting '{}' for '{}'.", itemName, host.getSimpleName(), exception);
            }
        }
    }

    public static final class DefinitionPathProvider implements PathProvider {
        public static final DefinitionPathProvider INSTANCE = new DefinitionPathProvider();
        private static final Set<String> BLACKLIST = Set.of(
            Constants.ItemName$.MODULE$.Debugger(),
            Constants.ItemName$.MODULE$.DiamondChip(),
            Constants.BlockName$.MODULE$.Endstone());

        private DefinitionPathProvider() {
        }

        @Override
        public String pathFor(ItemStack stack) {
            return checkBlacklisted(Items.get(stack));
        }

        @Override
        public String pathFor(Level level, BlockPos pos) {
            return level.getBlockState(pos).getBlock() instanceof SimpleBlock block
                ? checkBlacklisted(Items.get(new ItemStack(block))) : null;
        }

        private String checkBlacklisted(ItemInfo info) {
            if (info == null || BLACKLIST.contains(info.name())) return null;
            return info.item() instanceof BlockItem
                ? "%LANGUAGE%/block/" + info.name() + ".md"
                : "%LANGUAGE%/item/" + info.name() + ".md";
        }
    }
}
