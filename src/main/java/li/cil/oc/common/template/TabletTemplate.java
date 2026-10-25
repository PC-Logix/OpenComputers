package li.cil.oc.common.template;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.data.TabletData;
import li.cil.oc.util.ItemUtils;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import scala.Function1;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabletTemplate extends Template {
    @Override public Class<? extends li.cil.oc.api.network.EnvironmentHost> hostClass() { return li.cil.oc.api.internal.Tablet.class; }

    @SuppressWarnings("unchecked")
    @Override public Tuple2<String, Function1<Container, Object>>[] suggestedComponents() {
        return new Tuple2[]{
                new Tuple2<>("BIOS", (Function1<Container, Object>) inventory -> hasComponent(Constants.ItemName$.MODULE$.EEPROM(), inventory)),
                new Tuple2<>("Keyboard", (Function1<Container, Object>) inventory -> hasComponent(Constants.BlockName$.MODULE$.Keyboard(), inventory)),
                new Tuple2<>("GraphicsCard", (Function1<Container, Object>) inventory -> Arrays.stream(new String[]{
                        Constants.ItemName$.MODULE$.APUCreative(), Constants.ItemName$.MODULE$.APUTier1(), Constants.ItemName$.MODULE$.APUTier2(),
                        Constants.ItemName$.MODULE$.GraphicsCardTier1(), Constants.ItemName$.MODULE$.GraphicsCardTier2(), Constants.ItemName$.MODULE$.GraphicsCardTier3()
                }).anyMatch(name -> hasComponent(name, inventory))),
                new Tuple2<>("OS", (Function1<Container, Object>) this::hasFileSystem)
        };
    }

    public static boolean selectTier1(ItemStack stack) { return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.TabletCaseTier1()); }
    public static boolean selectTier2(ItemStack stack) { return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.TabletCaseTier2()); }
    public static boolean selectTier3(ItemStack stack) { return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.TabletCaseTier3()); }
    public static boolean selectCreative(ItemStack stack) { return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.TabletCaseCreative()); }
    public static Object[] validate(Container inventory) { return new TabletTemplate().validateComputer(inventory); }

    public static Object[] assemble(Container inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 1; slot < inventory.getContainerSize(); slot++) items.add(inventory.getItem(slot));
        TabletData data = new TabletData();
        data.tier = ItemUtils.caseTier(inventory.getItem(0));
        data.container = items.isEmpty() ? ItemStack.EMPTY : items.get(0);
        List<ItemStack> contents = new ArrayList<>();
        contents.add(li.cil.oc.api.Items.get(Constants.BlockName$.MODULE$.ScreenTier1()).createItemStack(1));
        for (int i = data.tier == Tier.One ? 0 : 1; i < items.size(); i++) if (!items.get(i).isEmpty()) contents.add(items.get(i));
        data.items = contents.toArray(ItemStack[]::new);
        data.energy = Settings.get().bufferTablet();
        data.maxEnergy = data.energy;
        ItemStack stack = li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.Tablet()).createItemStack(1);
        data.saveData(stack);
        double energy = Settings.get().tabletBaseCost() + new TabletTemplate().complexity(inventory) * Settings.get().tabletComplexityCost();
        return new Object[]{stack, energy};
    }

    public static boolean selectDisassembler(ItemStack stack) { return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.Tablet()); }

    public static ItemStack[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        TabletData info = new TabletData(stack);
        String itemName = Constants.ItemName$.MODULE$.TabletCase(info.tier);
        List<ItemStack> result = new ArrayList<>();
        result.add(li.cil.oc.api.Items.get(itemName).createItemStack(1));
        result.add(info.container);
        for (int i = 1; i < info.items.length; i++) if (!info.items[i].isEmpty()) result.add(info.items[i]);
        return result.stream().filter(item -> !item.isEmpty()).toArray(ItemStack[]::new);
    }

    public static void register() {
        register("Tier 1", "selectTier1", null, new int[]{Tier.Three, Tier.Two, Tier.One}, new Object[][]{{Slot.Card, Tier.Two}, {Slot.Card, Tier.Two}, null, {Slot.CPU, Tier.Two}, {Slot.Memory, Tier.Two}, {Slot.Memory, Tier.Two}, {Slot.EEPROM, Tier.Any}, {Slot.HDD, Tier.Two}});
        register("Tier 2", "selectTier2", new int[]{Tier.Two}, new int[]{Tier.Three, Tier.Two, Tier.Two}, new Object[][]{{Slot.Card, Tier.Three}, {Slot.Card, Tier.Two}, null, {Slot.CPU, Tier.Three}, {Slot.Memory, Tier.Two}, {Slot.Memory, Tier.Two}, {Slot.EEPROM, Tier.Any}, {Slot.HDD, Tier.Two}});
        register("Tier 3", "selectTier3", new int[]{Tier.Three}, new int[]{Tier.Four, Tier.Three, Tier.Three}, new Object[][]{{Slot.Card, Tier.Four}, {Slot.Card, Tier.Three}, null, {Slot.CPU, Tier.Four}, {Slot.Memory, Tier.Three}, {Slot.Memory, Tier.Three}, {Slot.EEPROM, Tier.Any}, {Slot.HDD, Tier.Three}});
        register("Creative", "selectCreative", new int[]{Tier.Four}, new int[]{Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four}, new Object[][]{{Slot.Card, Tier.Four}, {Slot.Card, Tier.Four}, {Slot.Card, Tier.Four}, {Slot.CPU, Tier.Four}, {Slot.Memory, Tier.Four}, {Slot.Memory, Tier.Four}, {Slot.EEPROM, Tier.Any}, {Slot.HDD, Tier.Four}});
        li.cil.oc.api.IMC.registerDisassemblerTemplate("Tablet", "li.cil.oc.common.template.TabletTemplate.selectDisassembler", "li.cil.oc.common.template.TabletTemplate.disassemble");
    }

    private static void register(String tierName, String selector, int[] required, int[] limits, Object[][] slots) {
        li.cil.oc.api.IMC.registerAssemblerTemplate("Tablet (" + tierName + ")", "li.cil.oc.common.template.TabletTemplate." + selector,
                "li.cil.oc.common.template.TabletTemplate.validate", "li.cil.oc.common.template.TabletTemplate.assemble",
                li.cil.oc.api.internal.Tablet.class, required, limits, constraints(slots));
    }

    private static List<Pair<String, Integer>> constraints(Object[][] slots) {
        List<Pair<String, Integer>> result = new ArrayList<>(slots.length);
        for (Object[] slot : slots) result.add(slot == null ? null : Pair.of((String) slot[0], (Integer) slot[1]));
        return result;
    }

    @Override public int maxComplexity(Container inventory) { return super.maxComplexity(inventory) / 2 + 5; }
    @Override public int caseTier(Container inventory) { return ItemUtils.caseTier(inventory.getItem(0)); }
}