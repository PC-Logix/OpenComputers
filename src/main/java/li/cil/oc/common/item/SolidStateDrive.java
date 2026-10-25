package li.cil.oc.common.item;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Settings;
import li.cil.oc.common.item.traits.FileSystemLike;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;

import java.util.List;

public final class SolidStateDrive extends SimpleItemBase implements FileSystemLike{
    private final int tier;
    private final int kiloBytes;
    private String unlocalizedName;

    public SolidStateDrive(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        kiloBytes = Settings.get().ssdSizes()[tier - 1];
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    public int tier() { return tier; }
    @Override public int kiloBytes() { return kiloBytes; }
    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        String size = kiloBytes >= 1024 ? " (" + kiloBytes / 1024 + "MB)" : " (" + kiloBytes + "KB)";
        return super.getName(stack).copy().append(Component.literal(size));
    }

    public void li$cil$oc$common$item$traits$FileSystemLike$$super$appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        appendSimpleTooltip(stack, tooltip, flag);
        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable(Settings.namespace() + "tooltip.tier", tierFromDriver(stack) + 1)
                .setStyle(Tooltip.DefaultStyle));
        }
    }

    private void appendSimpleTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        Tooltip.add(tooltip, flag, getClass().getSimpleName().toLowerCase());
        String address = stack.get(OCComponentsJava.ADDRESS.get());
        if (address != null) {
            String shortened = address.length() > 13 ? address.substring(0, 13) + "..." : address;
            tooltip.add(Component.literal("§8" + shortened + "§7"));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FileSystemLike.super.appendHoverText(stack, context, tooltip, flag);
    }
}
