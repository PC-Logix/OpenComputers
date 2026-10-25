package li.cil.oc.common.template;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.util.ItemUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RobotTemplate extends Template {
    @Override
    public Class<? extends EnvironmentHost> hostClass() {
        return li.cil.oc.api.internal.Robot.class;
    }

    public static boolean selectTier1(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName$.MODULE$.CaseTier1());
    }

    public static boolean selectTier2(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName$.MODULE$.CaseTier2());
    }

    public static boolean selectTier3(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName$.MODULE$.CaseTier3());
    }

    public static boolean selectCreative(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName$.MODULE$.CaseCreative());
    }

    public static Object[] validate(Container inventory) {
        return new RobotTemplate().validateComputer(inventory);
    }

    public static Object[] assemble(Container inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 1; i < inventory.getContainerSize(); i++) items.add(inventory.getItem(i));

        RobotData data = new RobotData();
        data.tier_$eq(new RobotTemplate().caseTier(inventory));
        data.name_$eq(Component.literal(RobotData.randomName()));
        data.robotEnergy_$eq((int) Settings.get().bufferRobot());
        data.totalEnergy_$eq(data.robotEnergy());
        data.containers_$eq(items.subList(0, Math.min(3, items.size())).stream()
                .filter(stack -> !stack.isEmpty()).toArray(ItemStack[]::new));
        data.components_$eq(items.subList(Math.min(3, items.size()), items.size()).stream()
                .filter(stack -> !stack.isEmpty()).toArray(ItemStack[]::new));
        ItemStack stack = data.createItemStack();
        double energy = Settings.get().robotBaseCost() + new RobotTemplate().complexity(inventory) * Settings.get().robotComplexityCost();
        return new Object[]{stack, energy};
    }

    public static boolean selectDisassembler(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.BlockName$.MODULE$.Robot());
    }

    public static ItemStack[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        RobotData data = new RobotData(stack);
        ItemStack casing = li.cil.oc.api.Items.get(Constants.BlockName$.MODULE$.Case(data.tier())).createItemStack(1);
        ItemStack[] result = new ItemStack[1 + data.containers().length + data.components().length];
        result[0] = casing;
        System.arraycopy(data.containers(), 0, result, 1, data.containers().length);
        System.arraycopy(data.components(), 0, result, 1 + data.containers().length, data.components().length);
        return result;
    }

    public static void register() {
        li.cil.oc.api.IMC.registerAssemblerTemplate("Robot (Tier 1)",
                "li.cil.oc.common.template.RobotTemplate.selectTier1", "li.cil.oc.common.template.RobotTemplate.validate",
                "li.cil.oc.common.template.RobotTemplate.assemble", li.cil.oc.api.internal.Robot.class,
                new int[]{Tier.Two, Tier.One, Tier.One}, new int[]{Tier.One, Tier.One, Tier.One},
                constraints(new Object[][]{{Slot.Card, Tier.One}, null, null, {Slot.CPU, Tier.One}, {Slot.Memory, Tier.One},
                        {Slot.Memory, Tier.One}, {Slot.EEPROM, Tier.Any}, {Slot.HDD, Tier.One}}));

        li.cil.oc.api.IMC.registerAssemblerTemplate("Robot (Tier 2)",
                "li.cil.oc.common.template.RobotTemplate.selectTier2", "li.cil.oc.common.template.RobotTemplate.validate",
                "li.cil.oc.common.template.RobotTemplate.assemble", li.cil.oc.api.internal.Robot.class,
                new int[]{Tier.Three, Tier.Two, Tier.One}, new int[]{Tier.Two, Tier.Two, Tier.Two, Tier.One, Tier.One, Tier.One},
                constraints(new Object[][]{{Slot.Card, Tier.Two}, {Slot.Card, Tier.One}, null, {Slot.CPU, Tier.Two},
                        {Slot.Memory, Tier.Two}, {Slot.Memory, Tier.Two}, {Slot.EEPROM, Tier.Any}, {Slot.HDD, Tier.Two}}));

        li.cil.oc.api.IMC.registerAssemblerTemplate("Robot (Tier 3)",
                "li.cil.oc.common.template.RobotTemplate.selectTier3", "li.cil.oc.common.template.RobotTemplate.validate",
                "li.cil.oc.common.template.RobotTemplate.assemble", li.cil.oc.api.internal.Robot.class,
                new int[]{Tier.Three, Tier.Two, Tier.Two},
                new int[]{Tier.Three, Tier.Three, Tier.Three, Tier.Two, Tier.Two, Tier.Two, Tier.One, Tier.One, Tier.One},
                constraints(new Object[][]{{Slot.Card, Tier.Three}, {Slot.Card, Tier.Two}, {Slot.Card, Tier.Two}, {Slot.CPU, Tier.Three},
                        {Slot.Memory, Tier.Three}, {Slot.Memory, Tier.Three}, {Slot.EEPROM, Tier.Any}, {Slot.HDD, Tier.Three},
                        {Slot.HDD, Tier.Two}}));

        li.cil.oc.api.IMC.registerAssemblerTemplate("Robot (Creative)",
                "li.cil.oc.common.template.RobotTemplate.selectCreative", "li.cil.oc.common.template.RobotTemplate.validate",
                "li.cil.oc.common.template.RobotTemplate.assemble", li.cil.oc.api.internal.Robot.class,
                new int[]{Tier.Four, Tier.Four, Tier.Four},
                new int[]{Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four, Tier.Four},
                constraints(new Object[][]{{Slot.Card, Tier.Four}, {Slot.Card, Tier.Four}, {Slot.Card, Tier.Four}, {Slot.CPU, Tier.Four},
                        {Slot.Memory, Tier.Four}, {Slot.Memory, Tier.Four}, {Slot.EEPROM, Tier.Any}, {Slot.HDD, Tier.Four},
                        {Slot.HDD, Tier.Four}}));

        li.cil.oc.api.IMC.registerDisassemblerTemplate("Robot", "li.cil.oc.common.template.RobotTemplate.selectDisassembler",
                "li.cil.oc.common.template.RobotTemplate.disassemble");
    }

    private static List<Pair<String, Integer>> constraints(Object[][] slots) {
        List<Pair<String, Integer>> result = new ArrayList<>(slots.length);
        for (Object[] slot : slots) {
            result.add(slot == null ? null : Pair.of((String) slot[0], (Integer) slot[1]));
        }
        return result;
    }

    @Override
    public int caseTier(Container inventory) {
        return ItemUtils.caseTier(inventory.getItem(0));
    }
}