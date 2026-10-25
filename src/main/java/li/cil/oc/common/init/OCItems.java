package li.cil.oc.common.init;

import li.cil.oc.Constants;
import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.common.item.AudioCard;
import li.cil.oc.common.item.Tape;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Item catalog entry point. Deferred suppliers and catalog order match the original registry. */
public final class OCItems {
    private OCItems() { }
    private static final DeferredRegister.Items ITEMS = OCItemsRegistryJava.items();
    static {
        OCItemsMaterialsCatalogJava.initialize();
        OCItemsHardwareCatalogJava.initialize();
    }
    public static final DeferredItem<AudioCard> AudioCardTier1 = registerItem(() -> new AudioCard(new Item.Properties()), Constants.ItemName.AudioCardTier1, Constants.SectionName.Component);
    public static final DeferredItem<Tape> Tape = tape(Constants.ItemName.Tape, 1024 * 1024);
    public static final DeferredItem<Tape> TapeCopper = tape(Constants.ItemName.TapeCopper, 4096 * 60 * 4);
    public static final DeferredItem<Tape> TapeGold = tape(Constants.ItemName.TapeGold, 4096 * 60 * 8);
    public static final DeferredItem<Tape> TapeDiamond = tape(Constants.ItemName.TapeDiamond, 4096 * 60 * 16);
    public static final DeferredItem<Tape> TapeNetherStar = tape(Constants.ItemName.TapeNetherStar, 4096 * 60 * 32);
    public static final DeferredItem<Tape> TapeSteel = tape(Constants.ItemName.TapeSteel, 4096 * 60 * 8);
    static {
        OCItemsUpgradesCatalogJava.initialize();
        OCItemsStorageCatalogJava.initialize();
        OCItemsSpecialCatalogJava.initialize();
    }
    public static final ItemAPI INSTANCE = new ItemAPI() {
        @Override public ItemInfo get(String name) { return OCItems.get(name); }
        @Override public ItemInfo get(ItemStack stack) { return OCItems.get(stack); }
        @Override public ItemStack registerFloppy(String displayName, String name, ResourceLocation location, DyeColor color, Callable<FileSystem> factory, boolean cycling) {
            return OCItems.registerFloppy(displayName, name, location, color, factory, cycling);
        }
        @Override public ItemStack registerEEPROM(String name, byte[] code, byte[] data, boolean readonly) { return OCItems.registerEEPROM(name, code, data, readonly); }
        @Override public ItemStack registerStack(ItemStack stack, String name, String section) { return OCItems.registerStack(stack, name, section); }
    };
    private static DeferredItem<Tape> tape(String id, int capacity) {
        return registerItem(() -> new Tape(new Item.Properties().stacksTo(1), capacity), id, Constants.SectionName.Component);
    }
    public static DeferredRegister.Items ITEMS() { return ITEMS; }
    public static ItemInfo get(String name) { return OCItemsApi.get(name); }
    public static ItemInfo get(ItemStack stack) { return OCItemsApi.get(stack); }
    public static <T extends Block> T registerBlockOnly(T block, String id) { return OCItemsRegistration.registerBlockOnly(block, id); }
    public static <T extends Block> T registerBlock(T block, String id, Item.Properties properties) { return registerBlock(block, id, properties, null); }
    public static <T extends Block> T registerBlock(T block, String id, Item.Properties properties, String section) { return OCItemsRegistration.registerBlock(block, id, properties, section); }
    public static <T extends Item> DeferredItem<T> registerItem(Supplier<T> factory, String id) { return registerItem(factory, id, null); }
    public static <T extends Item> DeferredItem<T> registerItem(Supplier<T> factory, String id, String section) { return OCItemsRegistration.registerItem(factory, id, section); }
    public static ItemStack registerStack(ItemStack stack, String id) { return registerStack(stack, id, null); }
    public static ItemStack registerStack(ItemStack stack, String id, String section) { return OCItemsRegistration.registerStack(stack, id, section); }
    public static ItemStack registerFloppy(String displayName, String name, ResourceLocation location, DyeColor color, Callable<FileSystem> factory, boolean cycling) { return OCItemsApi.registerFloppy(displayName, name, location, color, factory, cycling); }
    public static ItemStack registerEEPROM(String name, byte[] code, byte[] data, boolean readonly) { return OCItemsApi.registerEEPROM(name, code, data, readonly); }
    public static ItemStack createEEPROM(String name, byte[] code, byte[] data, boolean readonly) { return OCItemsFactories.createEEPROM(name, code, data, readonly); }
    public static ItemStack createConfiguredDrone() { return OCItemsFactories.createConfiguredDrone(); }
    public static ItemStack createConfiguredMicrocontroller() { return OCItemsFactories.createConfiguredMicrocontroller(); }
    public static ItemStack createConfiguredRobot() { return OCItemsFactories.createConfiguredRobot(); }
    public static ItemStack createConfiguredTablet() { return OCItemsFactories.createConfiguredTablet(); }
    public static ItemStack createChargedHoverBoots() { return OCItemsFactories.createChargedHoverBoots(); }
    public static void init(IEventBus bus) { ITEMS.register(bus); }
    public static void decorateCreativeTab(Consumer<ItemStack> display, Consumer<ItemStack> search) { decorateCreativeTab(display, search, Collections.emptyList(), Collections.emptySet()); }
    public static void decorateCreativeTab(Consumer<ItemStack> display, Consumer<ItemStack> search, Collection<ItemStack> extraDisplay, Collection<ItemStack> extraSearch) { OCItemsCreativeTab.decorate(display, search, extraDisplay, extraSearch); }

    // Read-through views keep older Scala integrations using this internal catalog facade working.
    public static scala.collection.mutable.Map<String, String> ITEM_TO_SECTION() { return scala.jdk.javaapi.CollectionConverters.asScala(OCItemsRegistryJava.itemToSection()); }
    public static scala.collection.mutable.Map<String, Integer> SECTION_Y_VALUES() { return scala.jdk.javaapi.CollectionConverters.asScala(OCItemsRegistryJava.sectionYValues()); }
    public static scala.collection.mutable.Map<String, ItemInfo> descriptors() { return scala.jdk.javaapi.CollectionConverters.asScala(OCItemsRegistryJava.descriptors()); }
    public static scala.collection.mutable.Map<Object, String> names() { return scala.jdk.javaapi.CollectionConverters.asScala(OCItemsRegistryJava.names()); }
    public static scala.collection.immutable.Map<String, String> aliases() { return scala.collection.immutable.Map$.MODULE$.from(scala.jdk.javaapi.CollectionConverters.asScala(OCItemsRegistryJava.aliases())); }
}
