package li.cil.oc.common.init;

import li.cil.oc.Constants;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.*;
import li.cil.oc.util.Rarity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/** Deferred tool and general component registrations. */
public final class OCItemsHardware {
    private OCItemsHardware() { }


    public static final DeferredItem<Analyzer> Analyzer = register(Constants.ItemName.Analyzer, Constants.SectionName.Tools,
            () -> new Analyzer(properties()));
    public static final DeferredItem<Debugger> Debugger = register(Constants.ItemName.Debugger, Constants.SectionName.Tools,
            () -> new Debugger(properties()));
    public static final DeferredItem<Terminal> Terminal = register(Constants.ItemName.Terminal, Constants.SectionName.Tools,
            () -> new Terminal(properties().stacksTo(1)));
    public static final DeferredItem<TexturePicker> TexturePicker = register(Constants.ItemName.TexturePicker, Constants.SectionName.Tools,
            () -> new TexturePicker(properties()));
    public static final DeferredItem<Manual> Manual = register(Constants.ItemName.Manual, Constants.SectionName.Tools,
            () -> new Manual(properties()));
    public static final DeferredItem<Wrench> Wrench = register(Constants.ItemName.Wrench, Constants.SectionName.Tools,
            () -> new Wrench(properties().stacksTo(1)));
    public static final DeferredItem<HoverBoots> HoverBoots = register(Constants.ItemName.HoverBoots, Constants.SectionName.Tools,
            () -> new HoverBoots(properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON).setNoRepair()));
    public static final DeferredItem<Nanomachines> Nanomachines = register(Constants.ItemName.Nanomachines, Constants.SectionName.Tools,
            () -> new Nanomachines(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final DeferredItem<CPU> CPUTier1 = component(Constants.ItemName.CPUTier1, () -> new CPU(properties(), Tier.One));
    public static final DeferredItem<CPU> CPUTier2 = component(Constants.ItemName.CPUTier2,
            () -> new CPU(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<CPU> CPUTier3 = component(Constants.ItemName.CPUTier3,
            () -> new CPU(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<CPU> CPUTier4 = component(Constants.ItemName.CPUTier4,
            () -> new CPU(properties().rarity(Rarity.LEGENDARY), Tier.Four));

    public static final DeferredItem<ComponentBus> ComponentBusTier1 = component(Constants.ItemName.ComponentBusTier1,
            () -> new ComponentBus(properties(), Tier.One));
    public static final DeferredItem<ComponentBus> ComponentBusTier2 = component(Constants.ItemName.ComponentBusTier2,
            () -> new ComponentBus(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<ComponentBus> ComponentBusTier3 = component(Constants.ItemName.ComponentBusTier3,
            () -> new ComponentBus(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<ComponentBus> ComponentBusTier4 = component(Constants.ItemName.ComponentBusTier4,
            () -> new ComponentBus(properties().rarity(Rarity.LEGENDARY), Tier.Four));
    public static final DeferredItem<ComponentBus> ComponentBusCreative = component(Constants.ItemName.ComponentBusCreative,
            () -> new ComponentBus(properties().rarity(net.minecraft.world.item.Rarity.EPIC), Tier.Five));

    public static final DeferredItem<Memory> RAMTier1 = component(Constants.ItemName.RAMTier1, () -> new Memory(properties(), Tier.One));
    public static final DeferredItem<Memory> RAMTier2 = component(Constants.ItemName.RAMTier2, () -> new Memory(properties(), Tier.Two));
    public static final DeferredItem<Memory> RAMTier3 = component(Constants.ItemName.RAMTier3,
            () -> new Memory(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Three));
    public static final DeferredItem<Memory> RAMTier4 = component(Constants.ItemName.RAMTier4,
            () -> new Memory(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Four));
    public static final DeferredItem<Memory> RAMTier5 = component(Constants.ItemName.RAMTier5,
            () -> new Memory(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Five));
    public static final DeferredItem<Memory> RAMTier6 = component(Constants.ItemName.RAMTier6,
            () -> new Memory(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Six));
    public static final DeferredItem<Memory> RAMTier7 = component(Constants.ItemName.RAMTier7,
            () -> new Memory(properties().rarity(Rarity.LEGENDARY), Tier.Seven));
    public static final DeferredItem<Memory> RAMTier8 = component(Constants.ItemName.RAMTier8,
            () -> new Memory(properties().rarity(Rarity.LEGENDARY), Tier.Eight));
    public static final DeferredItem<Item> RAMCreative = registerBasic(Constants.ItemName.RAMCreative,
            properties().rarity(net.minecraft.world.item.Rarity.EPIC));

    public static final DeferredItem<Server> ServerTier1 = component(Constants.ItemName.ServerTier1,
            () -> new Server(properties().stacksTo(1), Tier.One));
    public static final DeferredItem<Server> ServerTier2 = component(Constants.ItemName.ServerTier2,
            () -> new Server(properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<Server> ServerTier3 = component(Constants.ItemName.ServerTier3,
            () -> new Server(properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<Server> ServerTier4 = component(Constants.ItemName.ServerTier4,
            () -> new Server(properties().stacksTo(1).rarity(Rarity.LEGENDARY), Tier.Four));
    public static final DeferredItem<Server> ServerCreative = component(Constants.ItemName.ServerCreative,
            () -> new Server(properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC), Tier.Five));

    public static final DeferredItem<APU> APUTier1 = component(Constants.ItemName.APUTier1,
            () -> new APU(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.One));
    public static final DeferredItem<APU> APUTier2 = component(Constants.ItemName.APUTier2,
            () -> new APU(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Two));
    public static final DeferredItem<APU> APUTier3 = component(Constants.ItemName.APUTier3,
            () -> new APU(properties().rarity(Rarity.LEGENDARY), Tier.Three));
    public static final DeferredItem<APU> APUCreative = component(Constants.ItemName.APUCreative,
            () -> new APU(properties().rarity(net.minecraft.world.item.Rarity.EPIC), Tier.Five));

    public static final DeferredItem<TerminalServer> TerminalServer = component(Constants.ItemName.TerminalServer,
            () -> new TerminalServer(properties().stacksTo(1)));
    public static final DeferredItem<Item> RackKVM = registerBasic(Constants.ItemName.RackKVM, properties().stacksTo(1));
    public static final DeferredItem<DiskDriveMountable> DiskDriveMountable = component(Constants.ItemName.DiskDriveMountable,
            () -> new DiskDriveMountable(properties().stacksTo(1)));
    public static final DeferredItem<Item> CapacitorMountable = registerBasic(Constants.ItemName.CapacitorMountable,
            properties().stacksTo(1));

    public static final DeferredItem<DebugCard> DebugCard = component(Constants.ItemName.DebugCard,
            () -> new DebugCard(properties()));
    public static final DeferredItem<GraphicsCard> GraphicsCardTier1 = component(Constants.ItemName.GraphicsCardTier1,
            () -> new GraphicsCard(properties(), Tier.One));
    public static final DeferredItem<GraphicsCard> GraphicsCardTier2 = component(Constants.ItemName.GraphicsCardTier2,
            () -> new GraphicsCard(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<GraphicsCard> GraphicsCardTier3 = component(Constants.ItemName.GraphicsCardTier3,
            () -> new GraphicsCard(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<GraphicsCard> GraphicsCardTier4 = component(Constants.ItemName.GraphicsCardTier4,
            () -> new GraphicsCard(properties().rarity(Rarity.LEGENDARY), Tier.Four));
    public static final DeferredItem<Item> QuadGraphicsCard = component(Constants.ItemName.QuadGraphicsCard,
            () -> new QuadGraphicsCard(properties().rarity(Rarity.LEGENDARY)));
    public static final DeferredItem<RedstoneCard> RedstoneCardTier1 = component(Constants.ItemName.RedstoneCardTier1,
            () -> new RedstoneCard(properties(), Tier.One));
    public static final DeferredItem<RedstoneCard> RedstoneCardTier2 = component(Constants.ItemName.RedstoneCardTier2,
            () -> new RedstoneCard(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<Item> NetworkCard = component(Constants.ItemName.NetworkCard,
            () -> new BasicTieredComponentItem(properties(), "networkcard"));
    public static final DeferredItem<WirelessNetworkCard> WirelessNetworkCardTier2 = component(Constants.ItemName.WirelessNetworkCardTier2,
            () -> new WirelessNetworkCard(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<Item> InternetCard = component(Constants.ItemName.InternetCard,
            () -> new BasicTieredComponentItem(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "internetcard"));
    public static final DeferredItem<LinkedCard> LinkedCard = component(Constants.ItemName.LinkedCard,
            () -> new LinkedCard(properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<DataCard> DataCardTier1 = component(Constants.ItemName.DataCardTier1,
            () -> new DataCard(properties(), Tier.One));
    public static final DeferredItem<DataCard> DataCardTier2 = component(Constants.ItemName.DataCardTier2,
            () -> new DataCard(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<DataCard> DataCardTier3 = component(Constants.ItemName.DataCardTier3,
            () -> new DataCard(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<Item> NavigationCard = component(Constants.ItemName.NavigationCard,
            () -> new BasicTieredComponentItem(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "navigationcard"));
    public static final DeferredItem<WirelessNetworkCard> WirelessNetworkCardTier1 = component(Constants.ItemName.WirelessNetworkCardTier1,
            () -> new WirelessNetworkCard(properties(), Tier.One));

    private static Item.Properties properties() {
        return new Item.Properties();
    }

    private static <T extends Item> DeferredItem<T> register(String id, String section, Supplier<T> factory) {
        return OCItemsRegistration.registerItem(factory, id, section);
    }

    private static <T extends Item> DeferredItem<T> component(String id, Supplier<T> factory) {
        return register(id, Constants.SectionName.Component, factory);
    }

    private static DeferredItem<Item> registerBasic(String id, Item.Properties properties) {
        return register(id, Constants.SectionName.Component, () -> new BasicItem(properties, id));
    }
}
