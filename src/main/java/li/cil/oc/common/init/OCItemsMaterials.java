package li.cil.oc.common.init;

import li.cil.oc.Constants;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.Acid;
import li.cil.oc.common.item.BasicItem;
import li.cil.oc.common.item.Chamelium;
import li.cil.oc.common.item.DroneCase;
import li.cil.oc.common.item.InkCartridge;
import li.cil.oc.common.item.Microchip;
import li.cil.oc.common.item.MicrocontrollerCase;
import li.cil.oc.common.item.TabletCase;
import li.cil.oc.util.Rarity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/** Deferred registrations for crafting materials. */
public final class OCItemsMaterials {
    private OCItemsMaterials() { }


    public static final DeferredItem<Item> CuttingWire = basic(Constants.ItemName.CuttingWire);
    public static final DeferredItem<Acid> Acid = register(Constants.ItemName.Acid, () -> new Acid(properties()));
    public static final DeferredItem<Item> RawCircuitBoard = basic(Constants.ItemName.RawCircuitBoard);
    public static final DeferredItem<Item> CircuitBoard = basic(Constants.ItemName.CircuitBoard);
    public static final DeferredItem<Item> PrintedCircuitBoard = basic(Constants.ItemName.PrintedCircuitBoard);
    public static final DeferredItem<Item> Card = register(Constants.ItemName.Card, () -> new BasicItem(properties(), "cardbase"));
    public static final DeferredItem<Item> Transistor = basic(Constants.ItemName.Transistor);
    public static final DeferredItem<Microchip> ChipTier1 = register(Constants.ItemName.ChipTier1,
            () -> new Microchip(properties(), Tier.One));
    public static final DeferredItem<Microchip> ChipTier2 = register(Constants.ItemName.ChipTier2,
            () -> new Microchip(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<Microchip> ChipTier3 = register(Constants.ItemName.ChipTier3,
            () -> new Microchip(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<Microchip> ChipTier4 = register(Constants.ItemName.ChipTier4,
            () -> new Microchip(properties().rarity(Rarity.LEGENDARY), Tier.Four));
    public static final DeferredItem<Item> Alu = basic(Constants.ItemName.Alu);
    public static final DeferredItem<Item> ControlUnit = register(Constants.ItemName.ControlUnit,
            () -> new BasicItem(properties(), "controlunit"));
    public static final DeferredItem<Item> Disk = basic(Constants.ItemName.Disk);
    public static final DeferredItem<Item> Interweb = basic(Constants.ItemName.Interweb);
    public static final DeferredItem<Item> ButtonGroup = register(Constants.ItemName.ButtonGroup,
            () -> new BasicItem(properties(), Constants.ItemName.ButtonGroup));
    public static final DeferredItem<Item> ArrowKeys = basic(Constants.ItemName.ArrowKeys);
    public static final DeferredItem<Item> NumPad = basic(Constants.ItemName.NumPad);

    public static final DeferredItem<TabletCase> TabletCaseTier1 = register(Constants.ItemName.TabletCaseTier1,
            () -> new TabletCase(properties(), Tier.One));
    public static final DeferredItem<TabletCase> TabletCaseTier2 = register(Constants.ItemName.TabletCaseTier2,
            () -> new TabletCase(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<TabletCase> TabletCaseTier3 = register(Constants.ItemName.TabletCaseTier3,
            () -> new TabletCase(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<TabletCase> TabletCaseCreative = register(Constants.ItemName.TabletCaseCreative,
            () -> new TabletCase(properties().rarity(net.minecraft.world.item.Rarity.EPIC), Tier.Five));
    public static final DeferredItem<MicrocontrollerCase> MicrocontrollerCaseTier1 = register(Constants.ItemName.MicrocontrollerCaseTier1,
            () -> new MicrocontrollerCase(properties(), Tier.One));
    public static final DeferredItem<MicrocontrollerCase> MicrocontrollerCaseTier2 = register(Constants.ItemName.MicrocontrollerCaseTier2,
            () -> new MicrocontrollerCase(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<MicrocontrollerCase> MicrocontrollerCaseTier3 = register(Constants.ItemName.MicrocontrollerCaseTier3,
            () -> new MicrocontrollerCase(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<MicrocontrollerCase> MicrocontrollerCaseCreative = register(Constants.ItemName.MicrocontrollerCaseCreative,
            () -> new MicrocontrollerCase(properties().rarity(net.minecraft.world.item.Rarity.EPIC), Tier.Five));
    public static final DeferredItem<DroneCase> DroneCaseTier1 = register(Constants.ItemName.DroneCaseTier1,
            () -> new DroneCase(properties(), Tier.One));
    public static final DeferredItem<DroneCase> DroneCaseTier2 = register(Constants.ItemName.DroneCaseTier2,
            () -> new DroneCase(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<DroneCase> DroneCaseTier3 = register(Constants.ItemName.DroneCaseTier3,
            () -> new DroneCase(properties().rarity(Rarity.LEGENDARY), Tier.Three));
    public static final DeferredItem<DroneCase> DroneCaseCreative = register(Constants.ItemName.DroneCaseCreative,
            () -> new DroneCase(properties().rarity(net.minecraft.world.item.Rarity.EPIC), Tier.Five));

    public static final DeferredItem<Item> InkCartridgeEmpty = register(Constants.ItemName.InkCartridgeEmpty,
            () -> new BasicItem(properties().stacksTo(1), Constants.ItemName.InkCartridgeEmpty));
    public static final DeferredItem<InkCartridge> InkCartridge = register(Constants.ItemName.InkCartridge,
            () -> new InkCartridge(properties().stacksTo(1)));
    public static final DeferredItem<Chamelium> Chamelium = register(Constants.ItemName.Chamelium,
            () -> new Chamelium(properties()));

    public static final DeferredItem<Item> DiamondChip = register(Constants.ItemName.DiamondChip,
            () -> new BasicItem(properties(), "diamondchip"));
    public static final DeferredItem<Item> NetheriteSilicon = basic(Constants.ItemName.NetheriteSilicon);

    private static Item.Properties properties() {
        return new Item.Properties();
    }

    private static DeferredItem<Item> basic(String id) {
        return register(id, () -> new BasicItem(properties(), id));
    }

    private static <T extends Item> DeferredItem<T> register(String id, Supplier<T> factory) {
        return OCItemsRegistration.registerItem(factory, id, Constants.SectionName.Materials);
    }
}
