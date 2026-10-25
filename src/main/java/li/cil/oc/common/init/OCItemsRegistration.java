package li.cil.oc.common.init;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.common.block.SimpleBlock;
import li.cil.oc.common.block.Item;
import li.cil.oc.common.item.traits.SimpleItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Java implementations of the item registry's descriptor and deferred registration operations. */
public final class OCItemsRegistration {
    private OCItemsRegistration() { }

    public static <T extends Block> T registerBlockOnly(T instance, String id) {
        var descriptors = OCItemsRegistryJava.descriptors();
        var names = OCItemsRegistryJava.names();
        if (!descriptors.containsKey(id)) {
            if (instance instanceof SimpleBlock simple) simple.setUnlocalizedName("oc." + id);
            descriptors.put(id, new ItemInfo() {
                @Override public String name() { return id; }
                @Override public Block block() { return instance; }
                @Override public net.minecraft.world.item.Item item() { return null; }
                @Override public ItemStack createItemStack(int size) {
                    OpenComputers.log.warn("Attempt to get ItemStack for block " + instance + " without item form");
                    return ItemStack.EMPTY;
                }
            });
            names.put(instance, id);
        }
        return instance;
    }

    public static <T extends Block> T registerBlock(
            T instance, String id, net.minecraft.world.item.Item.Properties itemProps, String sectionId) {
        var items = OCItemsRegistryJava.items();
        var descriptors = OCItemsRegistryJava.descriptors();
        var names = OCItemsRegistryJava.names();
        var itemToSection = OCItemsRegistryJava.itemToSection();
        if (!descriptors.containsKey(id) && instance instanceof SimpleBlock simple) {
            simple.setUnlocalizedName("oc." + id);
            if (sectionId != null) itemToSection.put(id, sectionId);
            DeferredItem<net.minecraft.world.item.Item> registeredItem =
                    items.register(id, () -> new Item(simple, itemProps));
            descriptors.put(id, new ItemInfo() {
                @Override public String name() { return id; }
                @Override public Block block() { return instance; }
                @Override public net.minecraft.world.item.Item item() { return registeredItem.get(); }
                @Override public ItemStack createItemStack(int size) { return simple.createItemStack(size); }
            });
            names.put(instance, id);
        }
        return instance;
    }

    /** Java-friendly entry point that uses the shared Java registry state. */
    public static <T extends net.minecraft.world.item.Item> DeferredItem<T> registerItem(
            Supplier<T> makeItem, String id, String sectionId) {
        var items = OCItemsRegistryJava.items();
        var descriptors = OCItemsRegistryJava.descriptors();
        var names = OCItemsRegistryJava.names();
        var itemToSection = OCItemsRegistryJava.itemToSection();
        if (descriptors.containsKey(id)) throw new IllegalArgumentException("Duplicate item " + id);
        if (sectionId != null) itemToSection.put(id, sectionId);

        // Construct items inside the supplier while the registry is writable.
        DeferredItem<T> registeredItem = items.register(id, () -> {
            T instance = makeItem.get();
            names.put(instance, id);
            return instance;
        });
        descriptors.put(id, new ItemInfo() {
            @Override public String name() { return id; }
            @Override public Block block() { return null; }
            @Override public net.minecraft.world.item.Item item() { return registeredItem.get(); }
            @Override public ItemStack createItemStack(int size) {
                net.minecraft.world.item.Item instance = registeredItem.get();
                return instance instanceof SimpleItemBehavior simple
                        ? simple.createItemStack(size)
                        : new ItemStack(instance, size);
            }
        });
        return registeredItem;
    }

    public static void init(IEventBus bus) {
        // Initialize the ordered catalog before attaching its deferred registry.
        OCItems.ITEMS();
        OCItemsRegistryJava.items().register(bus);
    }

    public static void forEachDescriptor(BiConsumer<String, ItemInfo> action) {
        OCItemsRegistryJava.descriptors().forEach(action);
    }

    public static String itemSection(String id) {
        return OCItemsRegistryJava.itemToSection().get(id);
    }

    public static void clearSectionYValues() {
        OCItemsRegistryJava.sectionYValues().clear();
    }

    public static void putSectionYValue(String section, int y) {
        OCItemsRegistryJava.sectionYValues().put(section, y);
    }

    public static void forEachSectionY(BiConsumer<String, Integer> action) {
        OCItemsRegistryJava.sectionYValues().forEach(action);
    }

    public static ItemStack registerStack(ItemStack stack, String id, String sectionId) {
        var descriptors = OCItemsRegistryJava.descriptors();
        var itemToSection = OCItemsRegistryJava.itemToSection();
        ItemStack immutableStack = stack.copy();
        if (sectionId != null) itemToSection.put(id, sectionId);
        descriptors.put(id, new ItemInfo() {
            @Override public String name() { return id; }
            @Override public Block block() { return null; }
            @Override public net.minecraft.world.item.Item item() { return immutableStack.getItem(); }
            @Override public ItemStack createItemStack(int size) {
                ItemStack copy = immutableStack.copy();
                copy.setCount(size);
                return copy;
            }
        });
        return stack;
    }
}

