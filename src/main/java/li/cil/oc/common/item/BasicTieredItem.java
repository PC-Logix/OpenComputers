package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;

import java.util.List;

public final class BasicTieredItem extends SimpleItemBase {
    public BasicTieredItem(Properties properties, String name) {
        super(properties, name);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable(Settings.namespace() + "tooltip.tier", tierFromDriver(stack) + 1)
                .setStyle(Tooltip.DefaultStyle));
        }
    }
}
