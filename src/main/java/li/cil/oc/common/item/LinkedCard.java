package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.util.ItemUtils;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;

import java.util.List;

public final class LinkedCard extends SimpleItemBase {
    private String unlocalizedName;

    public LinkedCard(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable(Settings.namespace() + "tooltip.tier", super.tierFromDriver(stack) + 1)
                .setStyle(Tooltip.DefaultStyle));
        }
        CompoundTag tag = ItemUtils.getTag(stack);
        String dataKey = Settings.namespace() + "data";
        String tunnelKey = Settings.namespace() + "tunnel";
        if (tag != null && tag.contains(dataKey)) {
            CompoundTag data = tag.getCompound(dataKey);
            if (data.contains(tunnelKey)) {
                String channel = data.getString(tunnelKey);
                Tooltip.add(tooltip, flag, unlocalizedName + "_channel",
                    channel.length() > 13 ? channel.substring(0, 13) + "..." : channel);
            }
        }
    }
}
