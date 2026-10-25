package li.cil.oc.common.recipe;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.common.Loot;
import li.cil.oc.common.item.data.*;
import li.cil.oc.server.machine.luac.LuaStateFactory;
import li.cil.oc.util.ItemUtils;
import li.cil.oc.util.SideTracker;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public final class ExtendedRecipe {
    private ExtendedRecipe() { }

    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> beaconBlocks =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "beacon_base_blocks"));

    private static final class ItemIds {
        static final ItemInfo DRONE = Items.get(Constants.ItemName$.MODULE$.Drone());
        static final ItemInfo EEPROM = Items.get(Constants.ItemName$.MODULE$.EEPROM());
        static final ItemInfo MCU = Items.get(Constants.BlockName$.MODULE$.Microcontroller());
        static final ItemInfo NAVIGATION_UPGRADE = Items.get(Constants.ItemName$.MODULE$.NavigationUpgrade());
        static final ItemInfo LINKED_CARD = Items.get(Constants.ItemName$.MODULE$.LinkedCard());
        static final ItemInfo FLOPPY = Items.get(Constants.ItemName$.MODULE$.Floppy());
        static final ItemInfo[] DRIVES = {
                Items.get(Constants.ItemName$.MODULE$.HDDTier1()), Items.get(Constants.ItemName$.MODULE$.HDDTier2()),
                Items.get(Constants.ItemName$.MODULE$.HDDTier3()), Items.get(Constants.ItemName$.MODULE$.HDDTier4()),
                Items.get(Constants.ItemName$.MODULE$.SSDTier1()), Items.get(Constants.ItemName$.MODULE$.SSDTier2()),
                Items.get(Constants.ItemName$.MODULE$.SSDTier3())};
        static final ItemInfo[] CPUS = {
                Items.get(Constants.ItemName$.MODULE$.CPUTier1()), Items.get(Constants.ItemName$.MODULE$.CPUTier2()),
                Items.get(Constants.ItemName$.MODULE$.CPUTier3()), Items.get(Constants.ItemName$.MODULE$.APUTier1()),
                Items.get(Constants.ItemName$.MODULE$.APUTier2())};
        static final ItemInfo ROBOT = Items.get(Constants.BlockName$.MODULE$.Robot());
        static final ItemInfo TABLET = Items.get(Constants.ItemName$.MODULE$.Tablet());
        static final ItemInfo PRINT = Items.get(Constants.BlockName$.MODULE$.Print());
    }

    public static void initializeStaticResultData(Recipe<?> recipe, ItemStack resultStack) {
        ItemInfo itemName = Items.get(resultStack);
        CompoundTag tag = ItemUtils.getTag(resultStack);
        if (itemName != ItemIds.EEPROM || resultStack.getCount() != 1 || tag == null || recipe.getIngredients().size() != 2) return;

        CompoundTag nbt = tag.getCompound(Settings.namespace() + "data");
        Tag labelTag = nbt.get(Settings.namespace() + "label");
        if (labelTag instanceof StringTag label) resultStack.set(OCComponentsJava.LABEL.get(), label.getAsString());
        if (nbt.contains(Settings.namespace() + "readonly")) {
            resultStack.set(OCComponentsJava.READONLY.get(), nbt.getBoolean(Settings.namespace() + "readonly"));
        }

        Tag codeTag = nbt.get(Settings.namespace() + "eeprom");
        if (codeTag instanceof StringTag code && code.getAsString().equals("bios.lua")) {
            ItemStack defaultEeprom = Loot.defaultEEPROM();
            var component = OCComponentsJava.EEPROM_CODE.get();
            if (!defaultEeprom.isEmpty() && defaultEeprom.has(component)) resultStack.set(component, defaultEeprom.get(component));
        }
        Tag dataTag = nbt.get(Settings.namespace() + "userdata");
        if (dataTag instanceof StringTag data && data.getAsString().equals("bios.lua")) {
            ItemStack defaultEeprom = Loot.defaultEEPROM();
            var component = OCComponentsJava.EEPROM_DATA.get();
            if (!defaultEeprom.isEmpty() && defaultEeprom.has(component)) resultStack.set(component, defaultEeprom.get(component));
        }
        resultStack.remove(DataComponents.CUSTOM_DATA);
    }

    public static ItemStack addNBTToResult(Recipe<?> recipe, ItemStack craftedStack, CraftingInput inventory,
                                           HolderLookup.Provider provider) {
        initializeStaticResultData(recipe, craftedStack);
        ItemInfo craftedItemName = Items.get(craftedStack);

        if (craftedItemName == ItemIds.NAVIGATION_UPGRADE) {
            for (ItemStack stack : getItems(inventory)) {
                if (stack.is(net.minecraft.world.item.Items.FILLED_MAP)) {
                    craftedStack.set(OCComponentsJava.SOURCE_MAP_ITEM.get(), ImmutableItemStack.copyOf(stack));
                }
            }
        }
        if (craftedItemName == ItemIds.LINKED_CARD && SideTracker.isServer()) {
            craftedStack.set(OCComponentsJava.TUNNEL.get(), UUID.randomUUID().toString());
        }
        if (contains(ItemIds.CPUS, craftedItemName)) LuaStateFactory.setDefaultArch(craftedStack);

        if (craftedItemName == ItemIds.FLOPPY || contains(ItemIds.DRIVES, craftedItemName)) {
            CustomData.update(DataComponents.CUSTOM_DATA, craftedStack, nbt -> {
                if (recipe.canCraftInDimensions(1, 1)) {
                    String colorKey = Settings.namespace() + "color";
                    for (ItemStack stack : getItems(inventory)) {
                        ItemInfo input = Items.get(stack);
                        CompoundTag oldData = ItemUtils.getTag(stack);
                        if (input != null && (input == ItemIds.FLOPPY || "lootDisk".equals(input.name())) && oldData != null
                                && oldData.contains(colorKey) && oldData.getInt(colorKey) != DyeColor.LIGHT_GRAY.getId()) {
                            nbt.put(colorKey, oldData.get(colorKey).copy());
                        }
                    }
                } else if (getItems(inventory).stream().allMatch(stack -> Items.get(stack) == ItemIds.FLOPPY)) {
                    for (ItemStack stack : getItems(inventory)) {
                        CompoundTag oldData = ItemUtils.getTag(stack);
                        if (Items.get(stack) == ItemIds.FLOPPY && oldData != null) {
                            for (String key : oldData.getAllKeys()) if (!nbt.contains(key)) nbt.put(key, oldData.get(key).copy());
                        }
                    }
                }
            });
        }

        if (craftedItemName == ItemIds.PRINT && recipe.getIngredients().size() == 2) {
            PrintData data = new PrintData(craftedStack);
            List<ItemStack> inputs = getItems(inventory);
            for (ItemStack stack : inputs) if (Items.get(stack) == ItemIds.PRINT) data.loadData(stack);
            ItemStack glowstoneDust = new ItemStack(net.minecraft.world.item.Items.GLOWSTONE_DUST);
            ItemStack glowstone = new ItemStack(Blocks.GLOWSTONE);
            for (ItemStack stack : inputs) {
                if (stack.is(beaconBlocks)) {
                    if (data.isBeaconBase()) return ItemStack.EMPTY;
                    data.isBeaconBase_$eq(true);
                }
                if (ItemStack.isSameItem(glowstoneDust, stack)) {
                    if (data.lightLevel() == 15) return ItemStack.EMPTY;
                    data.lightLevel_$eq(Math.min(15, data.lightLevel() + 1));
                }
                if (ItemStack.isSameItem(glowstone, stack)) {
                    if (data.lightLevel() == 15) return ItemStack.EMPTY;
                    data.lightLevel_$eq(Math.min(15, data.lightLevel() + 4));
                }
            }
            data.saveData(craftedStack);
        }

        if (craftedItemName == ItemIds.EEPROM && craftedStack.getCount() == 2 && recipe.getIngredients().size() == 2) {
            for (ItemStack stack : getItems(inventory)) {
                CompoundTag copy = ItemUtils.getTag(stack);
                if (Items.get(stack) == ItemIds.EEPROM && copy != null) {
                    copy.getCompound(Settings.namespace() + "data").getCompound("node").remove("address");
                    CustomData.set(DataComponents.CUSTOM_DATA, craftedStack, copy);
                    break;
                }
            }
        }

        recraft(provider, craftedStack, inventory, ItemIds.MCU, MCUDataWrapper::new);
        recraft(provider, craftedStack, inventory, ItemIds.DRONE, DroneDataWrapper::new);
        recraft(provider, craftedStack, inventory, ItemIds.ROBOT, RobotDataWrapper::new);
        recraft(provider, craftedStack, inventory, ItemIds.TABLET, TabletDataWrapper::new);
        return craftedStack;
    }

    private static boolean contains(ItemInfo[] items, ItemInfo value) {
        for (ItemInfo item : items) if (item == value) return true;
        return false;
    }

    private static List<ItemStack> getItems(CraftingInput inventory) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) result.add(stack);
        }
        return result;
    }

    private interface ItemDataWrapper {
        ItemStack[] components();
        void components_$eq(ItemStack[] value);
        void save(ItemStack stack, HolderLookup.Provider provider);
    }

    private static final class MCUDataWrapper implements ItemDataWrapper {
        private final MicrocontrollerData data;
        MCUDataWrapper(ItemStack stack) { data = new MicrocontrollerData(stack); }
        public ItemStack[] components() { return data.components; }
        public void components_$eq(ItemStack[] value) { data.components = value; }
        public void save(ItemStack stack, HolderLookup.Provider provider) { data.saveData(stack); }
    }
    private static final class DroneDataWrapper implements ItemDataWrapper {
        private final DroneData data;
        DroneDataWrapper(ItemStack stack) { data = new DroneData(stack); }
        public ItemStack[] components() { return data.components; }
        public void components_$eq(ItemStack[] value) { data.components = value; }
        public void save(ItemStack stack, HolderLookup.Provider provider) { data.saveData(stack); }
    }
    private static final class RobotDataWrapper implements ItemDataWrapper {
        private final RobotData data;
        RobotDataWrapper(ItemStack stack) { data = new RobotData(stack); }
        public ItemStack[] components() { return data.components; }
        public void components_$eq(ItemStack[] value) { data.components = value; }
        public void save(ItemStack stack, HolderLookup.Provider provider) { data.saveData(stack); }
    }
    private static final class TabletDataWrapper implements ItemDataWrapper {
        private final TabletData data;
        private ItemStack[] components;
        TabletDataWrapper(ItemStack stack) {
            data = new TabletData(stack);
            components = java.util.Arrays.stream(data.items).filter(item -> !item.isEmpty()).toArray(ItemStack[]::new);
        }
        public ItemStack[] components() { return components; }
        public void components_$eq(ItemStack[] value) { components = value; }
        public void save(ItemStack stack, HolderLookup.Provider provider) {
            data.items = components.clone();
            data.saveData(stack);
        }
    }

    private static void recraft(HolderLookup.Provider provider, ItemStack result, CraftingInput inventory,
                                ItemInfo descriptor, Function<ItemStack, ItemDataWrapper> factory) {
        if (Items.get(result) != descriptor) return;
        ItemStack old = getItems(inventory).stream().filter(stack -> Items.get(stack) == descriptor).findFirst().orElse(null);
        if (old == null) return;
        ItemDataWrapper data = factory.apply(old);
        ItemStack[] components = data.components();
        List<ItemStack> updated = new ArrayList<>();
        for (ItemStack component : components) if (Items.get(component) != ItemIds.EEPROM) updated.add(component);
        for (ItemStack stack : getItems(inventory)) if (Items.get(stack) == ItemIds.EEPROM) updated.add(stack.copyWithCount(1));
        data.components_$eq(updated.toArray(ItemStack[]::new));
        data.save(result, provider);
    }
}
