package li.cil.oc.data;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.common.init.OCItemsMaterials;
import li.cil.oc.common.init.OCItemsHardware;
import li.cil.oc.common.init.OCItemsUpgrades;
import li.cil.oc.common.init.OCItemsStorage;
import li.cil.oc.common.init.OCItemsSpecial;
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
        basicItem(OCItemsMaterials.CuttingWire.get());
        basicItem(OCItemsMaterials.Acid.get());
        basicItem(OCItemsMaterials.RawCircuitBoard.get());
        basicItem(OCItemsMaterials.CircuitBoard.get());
        basicItem(OCItemsMaterials.PrintedCircuitBoard.get());
        basicItem(OCItemsMaterials.Card.get());
        basicItem(OCItemsMaterials.Transistor.get());
        basicItem(OCItemsMaterials.ChipTier1.get());
        basicItem(OCItemsMaterials.ChipTier2.get());
        basicItem(OCItemsMaterials.ChipTier3.get());
        basicItem(OCItemsMaterials.ChipTier4.get());
        basicItem(OCItemsMaterials.Alu.get());
        basicItem(OCItemsMaterials.Disk.get());
        basicItem(OCItemsMaterials.Interweb.get());
        basicItem(OCItemsMaterials.ButtonGroup.get());
        basicItem(OCItemsMaterials.ArrowKeys.get());
        basicItem(OCItemsMaterials.NumPad.get());
        basicItem(OCItemsMaterials.TabletCaseTier1.get());
        basicItem(OCItemsMaterials.TabletCaseTier2.get());
        basicItem(OCItemsMaterials.TabletCaseTier3.get());
        basicItem(OCItemsMaterials.TabletCaseCreative.get());
        basicItem(OCItemsMaterials.MicrocontrollerCaseTier1.get());
        basicItem(OCItemsMaterials.MicrocontrollerCaseTier2.get());
        basicItem(OCItemsMaterials.MicrocontrollerCaseTier3.get());
        basicItem(OCItemsMaterials.MicrocontrollerCaseCreative.get());
        basicItem(OCItemsMaterials.DroneCaseTier1.get());
        basicItem(OCItemsMaterials.DroneCaseTier2.get());
        basicItem(OCItemsMaterials.DroneCaseTier3.get());
        basicItem(OCItemsMaterials.DroneCaseCreative.get());
        basicItem(OCItemsMaterials.InkCartridgeEmpty.get());
        basicItem(OCItemsMaterials.InkCartridge.get());
        basicItem(OCItemsMaterials.Chamelium.get());
        basicItem(OCItemsMaterials.NetheriteSilicon.get());

        // All kinds of tools.
        basicItem(OCItemsHardware.Analyzer.get());
        basicItem(OCItemsHardware.Debugger.get());
        basicItem(OCItemsMaterials.DiamondChip.get());
        basicItem(OCItemsHardware.Terminal.get());
        basicItem(OCItemsHardware.TexturePicker.get());
        basicItem(OCItemsHardware.Manual.get());
        basicItem(OCItemsHardware.Wrench.get());
        basicItem(OCItemsHardware.Nanomachines.get());

        // General purpose components.
        basicItem(OCItemsHardware.CPUTier1.get());
        basicItem(OCItemsHardware.CPUTier2.get());
        basicItem(OCItemsHardware.CPUTier3.get());
        basicItem(OCItemsHardware.CPUTier4.get());
        basicItem(OCItemsHardware.ComponentBusTier1.get());
        basicItem(OCItemsHardware.ComponentBusTier2.get());
        basicItem(OCItemsHardware.ComponentBusTier3.get());
        basicItem(OCItemsHardware.ComponentBusTier4.get());
        basicItem(OCItemsHardware.RAMTier1.get());
        basicItem(OCItemsHardware.RAMTier2.get());
        basicItem(OCItemsHardware.RAMTier3.get());
        basicItem(OCItemsHardware.RAMTier4.get());
        basicItem(OCItemsHardware.RAMTier5.get());
        basicItem(OCItemsHardware.RAMTier6.get());
        basicItem(OCItemsHardware.RAMTier7.get());
        basicItem(OCItemsHardware.RAMTier8.get());
        basicItem(OCItemsHardware.ServerCreative.get());
        basicItem(OCItemsHardware.ServerTier1.get());
        basicItem(OCItemsHardware.ServerTier2.get());
        basicItem(OCItemsHardware.ServerTier3.get());
        basicItem(OCItemsHardware.ServerTier4.get());
        basicItem(OCItemsHardware.APUTier1.get());
        basicItem(OCItemsHardware.APUTier2.get());
        basicItem(OCItemsHardware.APUTier3.get());
        basicItem(OCItemsHardware.APUCreative.get());
        basicItem(OCItemsHardware.TerminalServer.get());
        basicItem(OCItemsHardware.DiskDriveMountable.get());
        basicItem(OCItemsHardware.RAMCreative.get());
        basicItem(OCItemsHardware.CapacitorMountable.get());

        // Card components.
        basicItem(OCItemsHardware.DebugCard.get());
        basicItem(OCItemsHardware.GraphicsCardTier1.get());
        basicItem(OCItemsHardware.GraphicsCardTier2.get());
        basicItem(OCItemsHardware.GraphicsCardTier3.get());
        basicItem(OCItemsHardware.GraphicsCardTier4.get());
        basicItem(OCItemsHardware.RedstoneCardTier1.get());
        basicItem(OCItemsHardware.RedstoneCardTier2.get());
        basicItem(OCItemsHardware.NetworkCard.get());
        basicItem(OCItemsHardware.WirelessNetworkCardTier2.get());
        basicItem(OCItemsHardware.InternetCard.get());
        basicItem(OCItemsHardware.LinkedCard.get());
        basicItem(OCItemsHardware.DataCardTier1.get());
        basicItem(OCItemsHardware.DataCardTier2.get());
        basicItem(OCItemsHardware.DataCardTier3.get());
        basicItem(OCItemsHardware.NavigationCard.get());
        basicItem(OCItems.AudioCardTier1.get());

        // Upgrade components.
        basicItem(OCItemsUpgrades.AngelUpgrade.get());
        basicItem(OCItemsUpgrades.BatteryUpgradeTier1.get());
        basicItem(OCItemsUpgrades.BatteryUpgradeTier2.get());
        basicItem(OCItemsUpgrades.BatteryUpgradeTier3.get());
        basicItem(OCItemsUpgrades.ChunkloaderUpgrade.get());
        basicItem(OCItemsUpgrades.CardContainerTier1.get());
        basicItem(OCItemsUpgrades.CardContainerTier2.get());
        basicItem(OCItemsUpgrades.CardContainerTier3.get());
        basicItem(OCItemsUpgrades.UpgradeContainerTier1.get());
        basicItem(OCItemsUpgrades.UpgradeContainerTier2.get());
        basicItem(OCItemsUpgrades.UpgradeContainerTier3.get());
        basicItem(OCItemsUpgrades.CraftingUpgrade.get());
        basicItem(OCItemsMaterials.ControlUnit.get());
        basicItem(OCItemsUpgrades.DatabaseUpgradeTier1.get());
        basicItem(OCItemsUpgrades.DatabaseUpgradeTier2.get());
        basicItem(OCItemsUpgrades.DatabaseUpgradeTier3.get());
        basicItem(OCItemsUpgrades.ExperienceUpgrade.get());
        basicItem(OCItemsUpgrades.GeneratorUpgrade.get());
        basicItem(OCItemsUpgrades.InventoryUpgrade.get());
        basicItem(OCItemsUpgrades.InventoryControllerUpgrade.get());
        basicItem(OCItemsUpgrades.NavigationUpgrade.get());
        basicItem(OCItemsUpgrades.PistonUpgrade.get());
        basicItem(OCItemsUpgrades.SignUpgrade.get());
        basicItem(OCItemsUpgrades.SolarGeneratorUpgrade.get());
        basicItem(OCItemsUpgrades.TankUpgrade.get());
        basicItem(OCItemsUpgrades.TankControllerUpgrade.get());
        basicItem(OCItemsUpgrades.TractorBeamUpgrade.get());
        basicItem(OCItemsUpgrades.LeashUpgrade.get());
        basicItem(OCItemsUpgrades.HoverUpgradeTier1.get());
        basicItem(OCItemsUpgrades.HoverUpgradeTier2.get());
        basicItem(OCItemsUpgrades.TradingUpgrade.get());
        basicItem(OCItemsUpgrades.MFU.get());
        basicItem(OCItemsHardware.WirelessNetworkCardTier1.get());
        basicItem(OCItemsHardware.ComponentBusCreative.get());
        basicItem(OCItemsUpgrades.StickyPistonUpgrade.get());

        // Storage media of all kinds.
        basicItem(OCItemsStorage.EEPROM.get());
        basicItem(OCItemsStorage.HDDTier1.get());
        basicItem(OCItemsStorage.HDDTier2.get());
        basicItem(OCItemsStorage.HDDTier3.get());
        basicItem(OCItemsStorage.HDDTier4.get());
        basicItem(OCItemsStorage.SSDTier1.get());
        basicItem(OCItemsStorage.SSDTier2.get());
        basicItem(OCItemsStorage.SSDTier3.get());

        // Special purpose items that don't fit into any other category.
        basicItem(OCItemsSpecial.Tablet.get());
        basicItem(OCItemsSpecial.Present.get());

        // Open Printers
        basicItem(OpenPrinter.BLACK_INK.get());
        basicItem(OpenPrinter.COLOR_INK.get());
        basicItem(OpenPrinter.PAPER_SHREDS.get());
        // TODO: This feels like it should have a custom full/empty texture?
        basicItem(OpenPrinter.FOLDER.get(), "folder_empty");
    }

    private ItemModelBuilder basicItem(Item item, String texture) {
        return getBuilder(Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)).toString())
            .parent(new ModelFile.UncheckedModelFile("item/generated"))
            .texture("layer0", ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "item/" + texture));
    }
}
