package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;

import java.util.List;

public final class Memory extends SimpleItemBase {
    private final int tier;
    private final int kiloBytes;
    private String unlocalizedName;

    public Memory(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        kiloBytes = Settings.get().ramSizes()[tier];
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    public int tier() { return tier; }
    public int kiloBytes() { return kiloBytes; }
    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().append(Component.literal(" (" + kiloBytes + "KB)"));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable(Settings.namespace() + "tooltip.tier", super.tierFromDriver(stack) + 1)
                .setStyle(Tooltip.DefaultStyle));
        }
    }
}
