package li.cil.oc.data;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.common.init.OCItems;
import li.cil.oc.common.openprinter.OpenPrinter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Objects;

public class OCItemModelProvider extends ItemModelProvider {
    public OCItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, OpenComputers.ID(), existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Crafting materials.
        basicItem(OCItems.CuttingWire().get());
        basicItem(OCItems.Acid().get());
        basicItem(OCItems.RawCircuitBoard().get());
        basicItem(OCItems.CircuitBoard().get());
        basicItem(OCItems.PrintedCircuitBoard().get());
        basicItem(OCItems.Card().get());
        basicItem(OCItems.Transistor().get());
        basicItem(OCItems.ChipTier1().get());
        basicItem(OCItems.ChipTier2().get());
        basicItem(OCItems.ChipTier3().get());
        basicItem(OCItems.ChipTier4().get());
        basicItem(OCItems.Alu().get());
        basicItem(OCItems.Disk().get());
        basicItem(OCItems.Interweb().get());
        basicItem(OCItems.ButtonGroup().get());
        basicItem(OCItems.ArrowKeys().get());
        basicItem(OCItems.NumPad().get());
        basicItem(OCItems.TabletCaseTier1().get());
        basicItem(OCItems.TabletCaseTier2().get());
        basicItem(OCItems.TabletCaseTier3().get());
        basicItem(OCItems.TabletCaseCreative().get());
        basicItem(OCItems.MicrocontrollerCaseTier1().get());
        basicItem(OCItems.MicrocontrollerCaseTier2().get());
        basicItem(OCItems.MicrocontrollerCaseTier3().get());
        basicItem(OCItems.MicrocontrollerCaseCreative().get());
        basicItem(OCItems.DroneCaseTier1().get());
        basicItem(OCItems.DroneCaseTier2().get());
        basicItem(OCItems.DroneCaseTier3().get());
        basicItem(OCItems.DroneCaseCreative().get());
        basicItem(OCItems.InkCartridgeEmpty().get());
        basicItem(OCItems.InkCartridge().get());
        basicItem(OCItems.Chamelium().get());
        basicItem(OCItems.NetheriteSilicon().get());

        // All kinds of tools.
        basicItem(OCItems.Analyzer().get());
        basicItem(OCItems.Debugger().get());
        basicItem(OCItems.DiamondChip().get());
        basicItem(OCItems.Terminal().get());
        basicItem(OCItems.TexturePicker().get());
        basicItem(OCItems.Manual().get());
        basicItem(OCItems.Wrench().get());
        basicItem(OCItems.Nanomachines().get());

        // General purpose components.
        basicItem(OCItems.CPUTier1().get());
        basicItem(OCItems.CPUTier2().get());
        basicItem(OCItems.CPUTier3().get());
        basicItem(OCItems.CPUTier4().get());
        basicItem(OCItems.ComponentBusTier1().get());
        basicItem(OCItems.ComponentBusTier2().get());
        basicItem(OCItems.ComponentBusTier3().get());
        basicItem(OCItems.ComponentBusTier4().get());
        basicItem(OCItems.RAMTier1().get());
        basicItem(OCItems.RAMTier2().get());
        basicItem(OCItems.RAMTier3().get());
        basicItem(OCItems.RAMTier4().get());
        basicItem(OCItems.RAMTier5().get());
        basicItem(OCItems.RAMTier6().get());
        basicItem(OCItems.RAMTier7().get());
        basicItem(OCItems.RAMTier8().get());
        basicItem(OCItems.ServerCreative().get());
        basicItem(OCItems.ServerTier1().get());
        basicItem(OCItems.ServerTier2().get());
        basicItem(OCItems.ServerTier3().get());
        basicItem(OCItems.ServerTier4().get());
        basicItem(OCItems.APUTier1().get());
        basicItem(OCItems.APUTier2().get());
        basicItem(OCItems.APUTier3().get());
        basicItem(OCItems.APUCreative().get());
        basicItem(OCItems.TerminalServer().get());
        basicItem(OCItems.DiskDriveMountable().get());
        basicItem(OCItems.RAMCreative().get());
        basicItem(OCItems.CapacitorMountable().get());

        // Card components.
        basicItem(OCItems.DebugCard().get());
        basicItem(OCItems.GraphicsCardTier1().get());
        basicItem(OCItems.GraphicsCardTier2().get());
        basicItem(OCItems.GraphicsCardTier3().get());
        basicItem(OCItems.GraphicsCardTier4().get());
        basicItem(OCItems.RedstoneCardTier1().get());
        basicItem(OCItems.RedstoneCardTier2().get());
        basicItem(OCItems.NetworkCard().get());
        basicItem(OCItems.WirelessNetworkCardTier2().get());
        basicItem(OCItems.InternetCard().get());
        basicItem(OCItems.LinkedCard().get());
        basicItem(OCItems.DataCardTier1().get());
        basicItem(OCItems.DataCardTier2().get());
        basicItem(OCItems.DataCardTier3().get());

        // Computronics content-only port.
        basicItem(OCItems.ComputronicsTape().get());
        basicItem(OCItems.ComputronicsTapeGold().get());
        basicItem(OCItems.ComputronicsTapeDiamond().get());
        basicItem(OCItems.ComputronicsTapeNetherStar().get());
        basicItem(OCItems.ComputronicsTapeCopper().get());
        basicItem(OCItems.ComputronicsTapeSteel().get());
        basicItem(OCItems.ComputronicsTapeGreg().get());
        basicItem(OCItems.ComputronicsTapeIndustrial().get());
        basicItem(OCItems.ComputronicsPortableTapeDrive().get());
        basicItem(OCItems.ComputronicsTapeTrack().get());
        basicItem(OCItems.ComputronicsCameraUpgrade().get());
        basicItem(OCItems.ComputronicsChatUpgrade().get());
        basicItem(OCItems.ComputronicsRadarUpgrade().get());
        basicItem(OCItems.ComputronicsParticleCard().get());
        basicItem(OCItems.ComputronicsSpoofingCard().get());
        basicItem(OCItems.ComputronicsBeepCard().get());
        basicItem(OCItems.ComputronicsSelfDestructingCard().get());
        basicItem(OCItems.ComputronicsColorfulUpgrade().get());
        basicItem(OCItems.ComputronicsNoiseCard().get());
        basicItem(OCItems.ComputronicsSoundCard().get());
        basicItem(OCItems.ComputronicsLightBoard().get());
        basicItem(OCItems.ComputronicsServerSelfDestructor().get());
        basicItem(OCItems.ComputronicsRackCapacitor().get());
        basicItem(OCItems.ComputronicsSwitchBoard().get());
        basicItem(OCItems.ComputronicsSpeechUpgrade().get());
        basicItem(OCItems.ComputronicsMagicalMemory().get());

        // Upgrade components.
        basicItem(OCItems.AngelUpgrade().get());
        basicItem(OCItems.BatteryUpgradeTier1().get());
        basicItem(OCItems.BatteryUpgradeTier2().get());
        basicItem(OCItems.BatteryUpgradeTier3().get());
        basicItem(OCItems.ChunkloaderUpgrade().get());
        basicItem(OCItems.CardContainerTier1().get());
        basicItem(OCItems.CardContainerTier2().get());
        basicItem(OCItems.CardContainerTier3().get());
        basicItem(OCItems.UpgradeContainerTier1().get());
        basicItem(OCItems.UpgradeContainerTier2().get());
        basicItem(OCItems.UpgradeContainerTier3().get());
        basicItem(OCItems.CraftingUpgrade().get());
        basicItem(OCItems.ControlUnit().get());
        basicItem(OCItems.DatabaseUpgradeTier1().get());
        basicItem(OCItems.DatabaseUpgradeTier2().get());
        basicItem(OCItems.DatabaseUpgradeTier3().get());
        basicItem(OCItems.ExperienceUpgrade().get());
        basicItem(OCItems.GeneratorUpgrade().get());
        basicItem(OCItems.InventoryUpgrade().get());
        basicItem(OCItems.InventoryControllerUpgrade().get());
        basicItem(OCItems.NavigationUpgrade().get());
        basicItem(OCItems.PistonUpgrade().get());
        basicItem(OCItems.SignUpgrade().get());
        basicItem(OCItems.SolarGeneratorUpgrade().get());
        basicItem(OCItems.TankUpgrade().get());
        basicItem(OCItems.TankControllerUpgrade().get());
        basicItem(OCItems.TractorBeamUpgrade().get());
        basicItem(OCItems.LeashUpgrade().get());
        basicItem(OCItems.HoverUpgradeTier1().get());
        basicItem(OCItems.HoverUpgradeTier2().get());
        basicItem(OCItems.TradingUpgrade().get());
        basicItem(OCItems.MFU().get());
        basicItem(OCItems.WirelessNetworkCardTier1().get());
        basicItem(OCItems.ComponentBusCreative().get());
        basicItem(OCItems.StickyPistonUpgrade().get());

        // Storage media of all kinds.
        basicItem(OCItems.EEPROM().get());
        basicItem(OCItems.HDDTier1().get());
        basicItem(OCItems.HDDTier2().get());
        basicItem(OCItems.HDDTier3().get());
        basicItem(OCItems.HDDTier4().get());
        basicItem(OCItems.SSDTier1().get());
        basicItem(OCItems.SSDTier2().get());
        basicItem(OCItems.SSDTier3().get());

        // Special purpose items that don't fit into any other category.
        basicItem(OCItems.Tablet().get());
        basicItem(OCItems.Present().get());

        // Open Printers
        basicItem(OpenPrinter.BLACK_INK.get());
        basicItem(OpenPrinter.COLOR_INK.get());
        basicItem(OpenPrinter.PAPER_SHREDS.get(), "shredded_paper");
        // TODO: This feels like it should have a custom full/empty texture?
        basicItem(OpenPrinter.FOLDER.get(), "folder_empty");

        simpleBlockItem(OCBlocks.Adapter().get());
        simpleBlockItem(OCBlocks.Assembler().get());
        simpleBlockItem(OCBlocks.Capacitor().get());
        simpleBlockItem(OCBlocks.CarpetedCapacitor().get());
        simpleBlockItem(OCBlocks.ChameliumBlock().get());
        simpleBlockItem(OCBlocks.Charger().get());
        simpleBlockItem(OCBlocks.Disassembler().get());
        simpleBlockItem(OCBlocks.DiskDrive().get());
        simpleBlockItem(OCBlocks.FlatScreenBackTier1().get());
        simpleBlockItem(OCBlocks.FlatScreenBackTier2().get());
        simpleBlockItem(OCBlocks.FlatScreenBackTier3().get());
        simpleBlockItem(OCBlocks.FlatScreenBackTier4().get());
        simpleBlockItem(OCBlocks.FlatScreenFrontTier1().get());
        simpleBlockItem(OCBlocks.FlatScreenFrontTier2().get());
        simpleBlockItem(OCBlocks.FlatScreenFrontTier3().get());
        simpleBlockItem(OCBlocks.FlatScreenFrontTier4().get());
        simpleBlockItem(OCBlocks.Geolyzer().get());
        simpleBlockItem(OCBlocks.HoloScreenTier1().get());
        simpleBlockItem(OCBlocks.HoloScreenTier2().get());
        simpleBlockItem(OCBlocks.HoloScreenTier3().get());
        simpleBlockItem(OCBlocks.HoloScreenTier4().get());
        simpleBlockItem(OCBlocks.HologramTier1().get());
        simpleBlockItem(OCBlocks.HologramTier2().get());
        simpleBlockItem(OCBlocks.HologramTier3().get());
        simpleBlockItem(OCBlocks.Keyboard().get());
        simpleBlockItem(OCBlocks.Microcontroller().get());
        simpleBlockItem(OCBlocks.MotionSensor().get());
        simpleBlockItem(OCBlocks.PowerConverter().get());
        simpleBlockItem(OCBlocks.PowerDistributor().get());
        simpleBlockItem(OCBlocks.Printer().get());
        simpleBlockItem(OCBlocks.Rack().get());
        simpleBlockItem(OCBlocks.Raid().get());
        simpleBlockItem(OCBlocks.Redstone().get());
        simpleBlockItem(OCBlocks.Relay().get());
        simpleBlockItem(OCBlocks.Transposer().get());
        simpleBlockItem(OCBlocks.Waypoint().get());

        simpleBlockItem(OpenPrinter.BRIEFCASE.get());
        simpleBlockItem(OpenPrinter.FILE_CABINET.get());
        simpleBlockItem(OpenPrinter.PRINTER.get());
        simpleBlockItem(OpenPrinter.SHREDDER.get());

        simpleBlockItem(OCBlocks.ComputronicsIronNote().get());
        simpleBlockItem(OCBlocks.ComputronicsAudioCable().get());
        simpleBlockItem(OCBlocks.ComputronicsSpeaker().get());
        simpleBlockItem(OCBlocks.ComputronicsTapeReader().get());
        simpleBlockItem(OCBlocks.ComputronicsCamera().get());
        simpleBlockItem(OCBlocks.ComputronicsChatBox().get());
        simpleBlockItem(OCBlocks.ComputronicsCipher().get());
        simpleBlockItem(OCBlocks.ComputronicsCipherAdvanced().get());
        simpleBlockItem(OCBlocks.ComputronicsRadar().get());
        simpleBlockItem(OCBlocks.ComputronicsColorfulLamp().get());
        simpleBlockItem(OCBlocks.ComputronicsSpeechBox().get());
    }

    private ItemModelBuilder basicItem(Item item, String texture) {
        return getBuilder(Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)).toString())
            .parent(new ModelFile.UncheckedModelFile("item/generated"))
            .texture("layer0", ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "item/" + texture));
    }
}
