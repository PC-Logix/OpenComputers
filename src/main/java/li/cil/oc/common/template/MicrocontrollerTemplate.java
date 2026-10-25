package li.cil.oc.common.template;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.util.ItemUtils;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import scala.Function1;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;

public class MicrocontrollerTemplate extends Template {
    @Override public Class<? extends li.cil.oc.api.network.EnvironmentHost> hostClass() { return li.cil.oc.api.internal.Microcontroller.class; }

    @SuppressWarnings("unchecked")
    @Override public Tuple2<String, Function1<Container, Object>>[] suggestedComponents() {
        return new Tuple2[]{new Tuple2<>("BIOS", (Function1<Container, Object>) inventory -> hasComponent("eeprom", inventory))};
    }

    public static boolean selectTier1(ItemStack stack) { return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.MicrocontrollerCaseTier1()); }
    public static boolean selectTier2(ItemStack stack) { return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.MicrocontrollerCaseTier2()); }
    public static boolean selectTier3(ItemStack stack) { return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.MicrocontrollerCaseTier3()); }
    public static boolean selectTierCreative(ItemStack stack) { return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.MicrocontrollerCaseCreative()); }
    public static Object[] validate(Container inventory) { return new MicrocontrollerTemplate().validateComputer(inventory); }

    public static Object[] assemble(Container inventory) {
        MicrocontrollerData data = new MicrocontrollerData();
        data.tier = new MicrocontrollerTemplate().caseTier(inventory);
        List<ItemStack> components = new ArrayList<>();
        for (int slot = 1; slot < inventory.getContainerSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!item.isEmpty()) components.add(item);
        }
        data.components = components.toArray(ItemStack[]::new);
        data.storedEnergy = (int) Settings.get().bufferMicrocontroller();
        ItemStack stack = data.createItemStack();
        double energy = Settings.get().microcontrollerBaseCost() + new MicrocontrollerTemplate().complexity(inventory) * Settings.get().microcontrollerComplexityCost();
        return new Object[]{stack, energy};
    }

    public static boolean selectDisassembler(ItemStack stack) { return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName$.MODULE$.Microcontroller()); }

    public static ItemStack[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        MicrocontrollerData info = new MicrocontrollerData(stack);
        String itemName = Constants.ItemName$.MODULE$.MicrocontrollerCase(info.tier);
        List<ItemStack> result = new ArrayList<>();
        result.add(li.cil.oc.api.Items.get(itemName).createItemStack(1));
        for (ItemStack component : info.components) if (!component.isEmpty()) result.add(component);
        return result.toArray(ItemStack[]::new);
    }

    public static void register() {
        register("Tier 1", "selectTier1", null, new int[]{Tier.Two}, new Object[][]{{Slot.Card, Tier.One}, {Slot.Card, Tier.One}, null, {Slot.CPU, Tier.One}, {Slot.Memory, Tier.One}, null, {Slot.EEPROM, Tier.Any}});
        register("Tier 2", "selectTier2", null, new int[]{Tier.Three}, new Object[][]{{Slot.Card, Tier.Two}, {Slot.Card, Tier.One}, null, {Slot.CPU, Tier.One}, {Slot.Memory, Tier.One}, {Slot.Memory, Tier.One}, {Slot.EEPROM, Tier.Any}});
        register("Tier 3", "selectTier3", null, new int[]{Tier.Four}, new Object[][]{{Slot.Card, Tier.Two}, {Slot.Card, Tier.Two}, null, {Slot.CPU, Tier.One}, {Slot.Memory, Tier.Two}, {Slot.Memory, Tier.One}, {Slot.EEPROM, Tier.Any}});
        register("Creative", "selectTierCreative", new int[]{Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four}, new int[]{Tier.Four}, new Object[][]{{Slot.Card, Tier.Four}, {Slot.Card, Tier.Four}, {Slot.Card, Tier.Four}, {Slot.CPU, Tier.Four}, {Slot.Memory, Tier.Four}, {Slot.Memory, Tier.Four}, {Slot.EEPROM, Tier.Any}});
        li.cil.oc.api.IMC.registerDisassemblerTemplate("Microcontroller", "li.cil.oc.common.template.MicrocontrollerTemplate.selectDisassembler", "li.cil.oc.common.template.MicrocontrollerTemplate.disassemble");
    }

    private static void register(String tierName, String selector, int[] required, int[] limits, Object[][] slots) {
        li.cil.oc.api.IMC.registerAssemblerTemplate("Microcontroller (" + tierName + ")",
                "li.cil.oc.common.template.MicrocontrollerTemplate." + selector,
                "li.cil.oc.common.template.MicrocontrollerTemplate.validate",
                "li.cil.oc.common.template.MicrocontrollerTemplate.assemble",
                li.cil.oc.api.internal.Microcontroller.class, required, limits, constraints(slots));
    }

    private static List<Pair<String, Integer>> constraints(Object[][] slots) {
        List<Pair<String, Integer>> result = new ArrayList<>(slots.length);
        for (Object[] slot : slots) result.add(slot == null ? null : Pair.of((String) slot[0], (Integer) slot[1]));
        return result;
    }

    @Override public int maxComplexity(Container inventory) {
        int tier = caseTier(inventory);
        if (tier == Tier.Two) return 5;
        if (tier == Tier.Five) return 9001;
        return 4;
    }

    @Override public int caseTier(Container inventory) { return ItemUtils.caseTier(inventory.getItem(0)); }
}