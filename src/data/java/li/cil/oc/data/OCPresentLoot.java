package li.cil.oc.data;

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

        add(pool, OCItems.ArrowKeys().get(), 520);
        add(pool, OCItems.ButtonGroup().get(), 460);
        add(pool, OCItems.NumPad().get(), 410);
        add(pool, OCItems.Disk().get(), 370);
        add(pool, OCItems.Transistor().get(), 350);
        add(pool, OCItems.Floppy().get(), 340);
        add(pool, OCItems.PrintedCircuitBoard().get(), 320);
        add(pool, OCItems.ChipTier1().get(), 290);
        add(pool, OCItems.EEPROM().get(), 250);
        add(pool, OCItems.Interweb().get(), 220);
        add(pool, OCItems.Card().get(), 190);
        add(pool, OCItems.Analyzer().get(), 170);
        add(pool, OCItems.SignUpgrade().get(), 150);
        add(pool, OCItems.InventoryUpgrade().get(), 130);
        add(pool, OCItems.CraftingUpgrade().get(), 110);
        add(pool, OCItems.TankUpgrade().get(), 90);
        add(pool, OCItems.PistonUpgrade().get(), 80);
        add(pool, OCItems.LeashUpgrade().get(), 70);
        add(pool, OCItems.AngelUpgrade().get(), 55);
        add(pool, OCItems.RedstoneCardTier1().get(), 50);
        add(pool, OCItems.RAMTier1().get(), 48);
        add(pool, OCItems.ControlUnit().get(), 46);
        add(pool, OCItems.Alu().get(), 45);
        add(pool, OCItems.BatteryUpgradeTier1().get(), 43);
        add(pool, OCItems.NetworkCard().get(), 38);
        add(pool, OCItems.WirelessNetworkCardTier1().get(), 37);
        add(pool, OCItems.HDDTier1().get(), 36);
        add(pool, OCItems.GeneratorUpgrade().get(), 35);
        add(pool, OCItems.CPUTier1().get(), 31);
        add(pool, OCItems.MicrocontrollerCaseTier1().get(), 30);
        add(pool, OCItems.DroneCaseTier1().get(), 25);
        add(pool, OCItems.UpgradeContainerTier1().get(), 23);
        add(pool, OCItems.CardContainerTier1().get(), 23);
        add(pool, OCItems.GraphicsCardTier1().get(), 19);
        add(pool, OCItems.RedstoneCardTier2().get(), 17);
        add(pool, OCItems.RAMTier2().get(), 15);
        add(pool, OCItems.DatabaseUpgradeTier1().get(), 15);
        add(pool, OCItems.ChipTier2().get(), 15);
        add(pool, OCItems.ComponentBusTier1().get(), 13);
        add(pool, OCItems.BatteryUpgradeTier2().get(), 12);
        add(pool, OCItems.WirelessNetworkCardTier2().get(), 11);
        add(pool, OCItems.RAMTier3().get(), 10);
        add(pool, OCItems.ServerTier1().get(), 10);
        add(pool, OCItems.InternetCard().get(), 9);
        add(pool, OCItems.Terminal().get(), 9);
        add(pool, OCItems.SolarGeneratorUpgrade().get(), 9);
        add(pool, OCItems.HDDTier2().get(), 7);
        add(pool, OCItems.NavigationUpgrade().get(), 7);
        add(pool, OCItems.InventoryControllerUpgrade().get(), 7);
        add(pool, OCItems.TankControllerUpgrade().get(), 7);
        add(pool, OCItems.CPUTier2().get(), 6);
        add(pool, OCItems.MicrocontrollerCaseTier2().get(), 6);
        add(pool, OCItems.ComponentBusTier2().get(), 6);
        add(pool, OCItems.TabletCaseTier1().get(), 5);
        add(pool, OCItems.UpgradeContainerTier2().get(), 5);
        add(pool, OCItems.CardContainerTier2().get(), 5);
        add(pool, OCItems.GraphicsCardTier2().get(), 4);
        add(pool, OCItems.RAMTier4().get(), 4);
        add(pool, OCItems.DroneCaseTier2().get(), 4);
        add(pool, OCItems.DatabaseUpgradeTier2().get(), 4);
        add(pool, OCItems.ServerTier2().get(), 4);
        add(pool, OCItems.ChipTier3().get(), 3);
        add(pool, OCItems.ComponentBusTier3().get(), 3);
        add(pool, OCItems.TractorBeamUpgrade().get(), 3);
        add(pool, OCItems.BatteryUpgradeTier3().get(), 3);
        add(pool, OCItems.ExperienceUpgrade().get(), 2);
        add(pool, OCItems.RAMTier5().get(), 2);
        add(pool, OCItems.UpgradeContainerTier3().get(), 2);
        add(pool, OCItems.CardContainerTier3().get(), 2);
        add(pool, OCItems.TabletCaseTier2().get(), 1);
        add(pool, OCItems.HDDTier3().get(), 1);
        add(pool, OCItems.ChunkloaderUpgrade().get(), 1);
        add(pool, OCItems.CPUTier3().get(), 1);
        add(pool, OCItems.GraphicsCardTier3().get(), 1);
        add(pool, OCItems.ServerTier3().get(), 1);
        add(pool, OCItems.DatabaseUpgradeTier3().get(), 1);
        add(pool, OCItems.RAMTier6().get(), 1);

        output.accept(Present.LOOT_TABLE(), LootTable.lootTable().withPool(pool));
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
