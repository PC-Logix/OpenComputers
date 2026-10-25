package li.cil.oc.common.init;

import li.cil.oc.Constants;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.*;
import li.cil.oc.util.Rarity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/** Deferred upgrade item registrations. */
public final class OCItemsUpgrades {
    private OCItemsUpgrades() { }


    public static final DeferredItem<Item> AngelUpgrade = basic(Constants.ItemName.AngelUpgrade,
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "upgradeangel");
    public static final DeferredItem<UpgradeBattery> BatteryUpgradeTier1 = upgrade(Constants.ItemName.BatteryUpgradeTier1,
            () -> new UpgradeBattery(properties(), Tier.One));
    public static final DeferredItem<UpgradeBattery> BatteryUpgradeTier2 = upgrade(Constants.ItemName.BatteryUpgradeTier2,
            () -> new UpgradeBattery(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<UpgradeBattery> BatteryUpgradeTier3 = upgrade(Constants.ItemName.BatteryUpgradeTier3,
            () -> new UpgradeBattery(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<Item> ChunkloaderUpgrade = basic(Constants.ItemName.ChunkloaderUpgrade,
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), "upgradechunkloader");
    public static final DeferredItem<UpgradeContainerCard> CardContainerTier1 = upgrade(Constants.ItemName.CardContainerTier1,
            () -> new UpgradeContainerCard(properties(), Tier.One));
    public static final DeferredItem<UpgradeContainerCard> CardContainerTier2 = upgrade(Constants.ItemName.CardContainerTier2,
            () -> new UpgradeContainerCard(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<UpgradeContainerCard> CardContainerTier3 = upgrade(Constants.ItemName.CardContainerTier3,
            () -> new UpgradeContainerCard(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<UpgradeContainerUpgrade> UpgradeContainerTier1 = upgrade(Constants.ItemName.UpgradeContainerTier1,
            () -> new UpgradeContainerUpgrade(properties(), Tier.One));
    public static final DeferredItem<UpgradeContainerUpgrade> UpgradeContainerTier2 = upgrade(Constants.ItemName.UpgradeContainerTier2,
            () -> new UpgradeContainerUpgrade(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<UpgradeContainerUpgrade> UpgradeContainerTier3 = upgrade(Constants.ItemName.UpgradeContainerTier3,
            () -> new UpgradeContainerUpgrade(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<Item> CraftingUpgrade = basic(Constants.ItemName.CraftingUpgrade,
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "upgradecrafting");
    public static final DeferredItem<UpgradeDatabase> DatabaseUpgradeTier1 = upgrade(Constants.ItemName.DatabaseUpgradeTier1,
            () -> new UpgradeDatabase(properties(), Tier.One));
    public static final DeferredItem<UpgradeDatabase> DatabaseUpgradeTier2 = upgrade(Constants.ItemName.DatabaseUpgradeTier2,
            () -> new UpgradeDatabase(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<UpgradeDatabase> DatabaseUpgradeTier3 = upgrade(Constants.ItemName.DatabaseUpgradeTier3,
            () -> new UpgradeDatabase(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<UpgradeExperience> ExperienceUpgrade = upgrade(Constants.ItemName.ExperienceUpgrade,
            () -> new UpgradeExperience(properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<UpgradeGenerator> GeneratorUpgrade = upgrade(Constants.ItemName.GeneratorUpgrade,
            () -> new UpgradeGenerator(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<Item> InventoryUpgrade = basic(Constants.ItemName.InventoryUpgrade, properties(), "upgradeinventory");
    public static final DeferredItem<Item> InventoryControllerUpgrade = basic(Constants.ItemName.InventoryControllerUpgrade,
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "upgradeinventorycontroller");
    public static final DeferredItem<Item> NavigationUpgrade = basic(Constants.ItemName.NavigationUpgrade,
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "upgradenavigation");
    public static final DeferredItem<Item> PistonUpgrade = basic(Constants.ItemName.PistonUpgrade,
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "upgradepiston");
    public static final DeferredItem<Item> SignUpgrade = basic(Constants.ItemName.SignUpgrade,
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "upgradesign");
    public static final DeferredItem<UpgradeSolarGenerator> SolarGeneratorUpgrade = upgrade(Constants.ItemName.SolarGeneratorUpgrade,
            () -> new UpgradeSolarGenerator(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<UpgradeTank> TankUpgrade = upgrade(Constants.ItemName.TankUpgrade,
            () -> new UpgradeTank(properties()));
    public static final DeferredItem<Item> TankControllerUpgrade = basic(Constants.ItemName.TankControllerUpgrade,
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "upgradetankcontroller");
    public static final DeferredItem<Item> TractorBeamUpgrade = basic(Constants.ItemName.TractorBeamUpgrade,
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), "upgradetractorbeam");
    public static final DeferredItem<Item> LeashUpgrade = basic(Constants.ItemName.LeashUpgrade, properties(), "upgradeleash");

    public static final DeferredItem<UpgradeHover> HoverUpgradeTier1 = upgrade(Constants.ItemName.HoverUpgradeTier1,
            () -> new UpgradeHover(properties(), Tier.One));
    public static final DeferredItem<UpgradeHover> HoverUpgradeTier2 = upgrade(Constants.ItemName.HoverUpgradeTier2,
            () -> new UpgradeHover(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<Item> TradingUpgrade = basic(Constants.ItemName.TradingUpgrade,
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "upgradetrading");
    public static final DeferredItem<UpgradeMF> MFU = upgrade(Constants.ItemName.MFU,
            () -> new UpgradeMF(properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<Item> StickyPistonUpgrade = basic(Constants.ItemName.StickyPistonUpgrade,
            properties(), "upgradestickypiston");

    private static Item.Properties properties() {
        return new Item.Properties();
    }

    private static <T extends Item> DeferredItem<T> register(String id, Supplier<T> factory) {
        return OCItemsRegistration.registerItem(factory, id, Constants.SectionName.Upgrade);
    }

    private static <T extends Item> DeferredItem<T> upgrade(String id, Supplier<T> factory) {
        return register(id, factory);
    }

    private static DeferredItem<Item> basic(String id, Item.Properties properties, String itemId) {
        return register(id, () -> new BasicTieredItem(properties, itemId));
    }
}
