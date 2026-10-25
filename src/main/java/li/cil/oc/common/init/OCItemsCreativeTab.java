package li.cil.oc.common.init;

import li.cil.oc.Constants;
import li.cil.oc.common.ContentVisibility;
import li.cil.oc.common.Loot;
import li.cil.oc.integration.opencomputers.ModOpenComputers;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/** Java implementation of the creative tab's item grouping and spacing. */
public final class OCItemsCreativeTab {
    private OCItemsCreativeTab() { }

    public static void decorate(Consumer<ItemStack> displayItems, Consumer<ItemStack> searchItems,
                                Collection<ItemStack> additionalDisplayItems,
                                Collection<ItemStack> additionalSearchItems) {
        // Assembled devices are not usable without their component data. Their
        // configured creative variants are added explicitly below.
        Set<String> excluded = new HashSet<>(List.of(
                Constants.BlockName.Microcontroller,
                Constants.BlockName.Print,
                Constants.BlockName.RobotAfterimage,
                Constants.BlockName.Robot,
                Constants.ItemName.Drone,
                Constants.ItemName.Tablet));
        if (!ModOpenComputers.hasRedstoneCardT2) excluded.add(Constants.ItemName.RedstoneCardTier2);

        Map<String, List<ItemStack>> sectionMap = new LinkedHashMap<>();
        List<ItemStack> sectionStacks = new ArrayList<>();
        OCItemsRegistration.forEachDescriptor((id, info) -> {
            if (!excluded.contains(id)) {
                String configuredSection = OCItemsRegistration.itemSection(id);
                String section = configuredSection != null ? configuredSection : Constants.SectionName.Misc;
                addToSection(sectionMap, sectionStacks, section, info.createItemStack(1));
            }
        });

        addStacks(sectionMap, sectionStacks, Constants.SectionName.Misc, Loot.disksForClient.iterator());
        addStacks(sectionMap, sectionStacks, Constants.SectionName.Component, Loot.eepromsForClient.iterator());
        for (ItemStack stack : additionalDisplayItems) {
            addToSection(sectionMap, sectionStacks, Constants.SectionName.Misc, stack);
        }

        for (int i = 0; i < 9; i++) displayItems.accept(ItemStack.EMPTY);

        OCItemsRegistration.clearSectionYValues();
        int y = 0;
        TreeSet<String> sortedSectionKeys = new TreeSet<>();
        for (String key : sectionMap.keySet()) {
            if (key != null) sortedSectionKeys.add(key);
        }
        List<String> sectionKeys = new ArrayList<>(sortedSectionKeys);
        for (int sectionIndex = 0; sectionIndex < sectionKeys.size(); sectionIndex++) {
            String key = sectionKeys.get(sectionIndex);
            int itemCount = 0;
            for (ItemStack stack : sectionMap.get(key)) {
                if (!stack.isEmpty()) {
                    displayItems.accept(stack);
                    searchItems.accept(stack);
                    itemCount++;
                }
            }
            OCItemsRegistration.putSectionYValue(key, y);
            y += (int) Math.ceil(itemCount / 9.0f) + 1;
            if (sectionIndex < sectionKeys.size() - 1) {
                int padding = 9 - itemCount % 9;
                if (padding < 9) padding += 9;
                for (int i = 0; i < padding; i++) displayItems.accept(ItemStack.EMPTY);
            }
        }

        for (ItemStack stack : additionalSearchItems) {
            if (!ContentVisibility.isHidden(stack)) searchItems.accept(stack);
        }

        displayItems.accept(OCItems.createConfiguredDrone());
        displayItems.accept(OCItems.createConfiguredMicrocontroller());
        displayItems.accept(OCItems.createConfiguredRobot());
        displayItems.accept(OCItems.createConfiguredTablet());
    }

    private static void addStacks(Map<String, List<ItemStack>> sections, List<ItemStack> added,
                                  String section, scala.collection.Iterator<ItemStack> stacks) {
        while (stacks.hasNext()) addToSection(sections, added, section, stacks.next());
    }

    private static void addToSection(Map<String, List<ItemStack>> sections, List<ItemStack> added,
                                     String section, ItemStack stack) {
        if (stack.isEmpty() || ContentVisibility.isHidden(stack)) return;
        for (ItemStack existing : added) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) return;
        }
        sections.computeIfAbsent(section, ignored -> new LinkedList<>()).add(stack);
        added.add(stack);
    }
}
