package li.cil.oc.data;

import li.cil.oc.common.init.OCItemsMaterials;
import li.cil.oc.common.init.OCItemsStorage;
import li.cil.oc.common.init.OCItemsHardware;
import li.cil.oc.common.init.OCItemsUpgrades;
import li.cil.oc.common.init.OCItems;
import li.cil.oc.common.item.Present;
import li.cil.oc.server.loot.IsCraftable;
import li.cil.oc.util.RarityExt;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.function.BiConsumer;

class OCPresentLoot implements LootTableSubProvider {
    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        var pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1));

        add(pool, OCItemsMaterials.ArrowKeys.get(), 520);
        add(pool, OCItemsMaterials.ButtonGroup.get(), 460);
        add(pool, OCItemsMaterials.NumPad.get(), 410);
        add(pool, OCItemsMaterials.Disk.get(), 370);
        add(pool, OCItemsMaterials.Transistor.get(), 350);
        add(pool, OCItemsStorage.Floppy.get(), 340);
        add(pool, OCItemsMaterials.PrintedCircuitBoard.get(), 320);
        add(pool, OCItemsMaterials.ChipTier1.get(), 290);
        add(pool, OCItemsStorage.EEPROM.get(), 250);
        add(pool, OCItemsMaterials.Interweb.get(), 220);
        add(pool, OCItemsMaterials.Card.get(), 190);
        add(pool, OCItemsHardware.Analyzer.get(), 170);
        add(pool, OCItemsUpgrades.SignUpgrade.get(), 150);
        add(pool, OCItemsUpgrades.InventoryUpgrade.get(), 130);
        add(pool, OCItemsUpgrades.CraftingUpgrade.get(), 110);
        add(pool, OCItemsUpgrades.TankUpgrade.get(), 90);
        add(pool, OCItemsUpgrades.PistonUpgrade.get(), 80);
        add(pool, OCItemsUpgrades.LeashUpgrade.get(), 70);
        add(pool, OCItemsUpgrades.AngelUpgrade.get(), 55);
        add(pool, OCItemsHardware.RedstoneCardTier1.get(), 50);
        add(pool, OCItemsHardware.RAMTier1.get(), 48);
        add(pool, OCItemsMaterials.ControlUnit.get(), 46);
        add(pool, OCItemsMaterials.Alu.get(), 45);
        add(pool, OCItemsUpgrades.BatteryUpgradeTier1.get(), 43);
        add(pool, OCItemsHardware.NetworkCard.get(), 38);
        add(pool, OCItemsHardware.WirelessNetworkCardTier1.get(), 37);
        add(pool, OCItemsStorage.HDDTier1.get(), 36);
        add(pool, OCItemsUpgrades.GeneratorUpgrade.get(), 35);
        add(pool, OCItemsHardware.CPUTier1.get(), 31);
        add(pool, OCItemsMaterials.MicrocontrollerCaseTier1.get(), 30);
        add(pool, OCItemsMaterials.DroneCaseTier1.get(), 25);
        add(pool, OCItemsUpgrades.UpgradeContainerTier1.get(), 23);
        add(pool, OCItemsUpgrades.CardContainerTier1.get(), 23);
        add(pool, OCItemsHardware.GraphicsCardTier1.get(), 19);
        add(pool, OCItemsHardware.RedstoneCardTier2.get(), 17);
        add(pool, OCItemsHardware.RAMTier2.get(), 15);
        add(pool, OCItemsUpgrades.DatabaseUpgradeTier1.get(), 15);
        add(pool, OCItemsMaterials.ChipTier2.get(), 15);
        add(pool, OCItemsHardware.ComponentBusTier1.get(), 13);
        add(pool, OCItemsUpgrades.BatteryUpgradeTier2.get(), 12);
        add(pool, OCItemsHardware.WirelessNetworkCardTier2.get(), 11);
        add(pool, OCItemsHardware.RAMTier3.get(), 10);
        add(pool, OCItemsHardware.ServerTier1.get(), 10);
        add(pool, OCItemsHardware.InternetCard.get(), 9);
        add(pool, OCItemsHardware.Terminal.get(), 9);
        add(pool, OCItemsUpgrades.SolarGeneratorUpgrade.get(), 9);
        add(pool, OCItemsStorage.HDDTier2.get(), 7);
        add(pool, OCItemsUpgrades.NavigationUpgrade.get(), 7);
        add(pool, OCItemsUpgrades.InventoryControllerUpgrade.get(), 7);
        add(pool, OCItemsUpgrades.TankControllerUpgrade.get(), 7);
        add(pool, OCItemsHardware.CPUTier2.get(), 6);
        add(pool, OCItemsMaterials.MicrocontrollerCaseTier2.get(), 6);
        add(pool, OCItemsHardware.ComponentBusTier2.get(), 6);
        add(pool, OCItemsMaterials.TabletCaseTier1.get(), 5);
        add(pool, OCItemsUpgrades.UpgradeContainerTier2.get(), 5);
        add(pool, OCItemsUpgrades.CardContainerTier2.get(), 5);
        add(pool, OCItemsHardware.GraphicsCardTier2.get(), 4);
        add(pool, OCItemsHardware.RAMTier4.get(), 4);
        add(pool, OCItemsMaterials.DroneCaseTier2.get(), 4);
        add(pool, OCItemsUpgrades.DatabaseUpgradeTier2.get(), 4);
        add(pool, OCItemsHardware.ServerTier2.get(), 4);
        add(pool, OCItemsMaterials.ChipTier3.get(), 3);
        add(pool, OCItemsHardware.ComponentBusTier3.get(), 3);
        add(pool, OCItemsUpgrades.TractorBeamUpgrade.get(), 3);
        add(pool, OCItemsUpgrades.BatteryUpgradeTier3.get(), 3);
        add(pool, OCItemsUpgrades.ExperienceUpgrade.get(), 2);
        add(pool, OCItemsHardware.RAMTier5.get(), 2);
        add(pool, OCItemsUpgrades.UpgradeContainerTier3.get(), 2);
        add(pool, OCItemsUpgrades.CardContainerTier3.get(), 2);
        add(pool, OCItemsMaterials.TabletCaseTier2.get(), 1);
        add(pool, OCItemsStorage.HDDTier3.get(), 1);
        add(pool, OCItemsUpgrades.ChunkloaderUpgrade.get(), 1);
        add(pool, OCItemsHardware.CPUTier3.get(), 1);
        add(pool, OCItemsHardware.GraphicsCardTier3.get(), 1);
        add(pool, OCItemsHardware.ServerTier3.get(), 1);
        add(pool, OCItemsUpgrades.DatabaseUpgradeTier3.get(), 1);
        add(pool, OCItemsHardware.RAMTier6.get(), 1);

        output.accept(Present.LOOT_TABLE, LootTable.lootTable().withPool(pool));
    }

    private static void add(LootPool.Builder pool, Item item, int weight) {
        pool.add(LootItem.lootTableItem(item).when(IsCraftable.builder(item)).setWeight(weight).setQuality(getQuality(item)));
    }

    private static int getQuality(Item item) {
        var rarity = new ItemStack(item).getOrDefault(DataComponents.RARITY, Rarity.COMMON);
        if (rarity == Rarity.COMMON) return 0;
        if (rarity == Rarity.UNCOMMON) return 1;
        if (rarity == Rarity.RARE) return 2;
        if (rarity == Rarity.EPIC) return 3;
        if (rarity == RarityExt.LEGENDARY.getValue()) return 4;
        return 0;
    }
}
