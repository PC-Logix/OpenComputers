package li.cil.oc.common.template;

import li.cil.oc.Constants;
import li.cil.oc.common.item.data.NavigationUpgradeData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class NavigationUpgradeTemplate {
    private NavigationUpgradeTemplate() { }

    public static boolean selectDisassembler(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.NavigationUpgrade());
    }

    public static ItemStack[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        NavigationUpgradeData info = new NavigationUpgradeData(stack);
        ItemStack[] result = ingredients.clone();
        for (int i = 0; i < result.length; i++) if (result[i].getItem() == Items.FILLED_MAP) result[i] = info.map;
        return result;
    }

    public static void register() {
        li.cil.oc.api.IMC.registerDisassemblerTemplate("Navigation Upgrade",
                "li.cil.oc.common.template.NavigationUpgradeTemplate.selectDisassembler",
                "li.cil.oc.common.template.NavigationUpgradeTemplate.disassemble");
    }
}