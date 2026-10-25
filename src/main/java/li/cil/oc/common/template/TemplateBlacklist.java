package li.cil.oc.common.template;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TemplateBlacklist {
    private static final Pattern DESCRIPTOR = Pattern.compile("^([^@]+)(?:@(\\d+))?$");

    private TemplateBlacklist() { }

    public static void register() {
        li.cil.oc.api.IMC.registerAssemblerFilter("li.cil.oc.common.template.TemplateBlacklist.filter");
    }

    public static boolean filter(ItemStack stack) {
        for (ItemStack banned : Blacklist.STACKS) if (ItemStack.isSameItem(banned, stack)) return false;
        return true;
    }

    private static ItemStack[] parseBlacklist() {
        List<ItemStack> stacks = new ArrayList<>();
        for (String descriptor : Settings.get().assemblerBlacklist()) {
            Matcher matcher = DESCRIPTOR.matcher(descriptor);
            if (!matcher.matches()) {
                OpenComputers.log().warn("Bad assembler blacklist entry '" + descriptor + "', invalid format (should be 'id' or 'id@damage').");
                continue;
            }
            String id = matcher.group(1);
            int meta = 0;
            if (matcher.group(2) != null) {
                try {
                    meta = Integer.parseInt(matcher.group(2));
                } catch (NumberFormatException error) {
                    OpenComputers.log().warn("Bad assembler blacklist entry '" + id + "@" + matcher.group(2) + "', invalid damage value.");
                    continue;
                }
            }
            ItemStack stack = parseDescriptor(id, meta);
            if (stack != null) stacks.add(stack);
        }
        return stacks.toArray(ItemStack[]::new);
    }

    private static ItemStack parseDescriptor(String id, int meta) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        var item = location == null ? null : BuiltInRegistries.ITEM.get(location);
        if (item == null) {
            OpenComputers.log().warn("Bad assembler blacklist entry '" + id + "', unknown item id.");
            return null;
        }
        ItemStack stack = new ItemStack(item, 1);
        stack.setDamageValue(meta);
        return stack;
    }

    private static final class Blacklist {
        private static final ItemStack[] STACKS = parseBlacklist();
    }
}