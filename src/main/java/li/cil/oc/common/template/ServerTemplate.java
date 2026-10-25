package li.cil.oc.common.template;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedDataComponentHolderSupportJava;
import li.cil.oc.util.ItemUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import scala.collection.immutable.List;
import scala.collection.immutable.List$;

public final class ServerTemplate {
    private ServerTemplate() { }

    public static boolean selectDisassembler(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.ServerTier1()) ||
                li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.ServerTier2()) ||
                li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.ServerTier3()) ||
                li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.ServerTier4());
    }

    public static Object[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        int tier = Math.max(0, ItemUtils.caseTier(stack));
        int size = InventorySlots.server[tier].length;
        SimpleContainer inventory = new SimpleContainer(size);
        var holder = ExtendedDataComponentHolderSupportJava.convert(stack);
        var components = holder.getComponentOptional(OCComponentsJava.COMPONENTS.get());
        var contents = holder.getComponentOptional(OCComponentsJava.CONTENTS.get());
        List<li.cil.oc.api.ImmutableItemStack> selected;
        if (components.isPresent() && components.get().exists(item -> !item.isEmpty())) selected = components.get();
        else if (contents.isPresent()) selected = contents.get();
        else selected = components.orElseGet(List$.MODULE$::empty);

        int count = Math.min(size, selected.size());
        for (int slot = 0; slot < count; slot++) inventory.setItem(slot, selected.apply(slot).mutableCopy());
        ItemStack[] stored = new ItemStack[size];
        for (int slot = 0; slot < size; slot++) stored[slot] = inventory.getItem(slot);
        return new Object[]{ingredients, stored};
    }

    public static void register() {
        li.cil.oc.api.IMC.registerDisassemblerTemplate("Server",
                "li.cil.oc.common.template.ServerTemplate.selectDisassembler",
                "li.cil.oc.common.template.ServerTemplate.disassemble");
    }
}
