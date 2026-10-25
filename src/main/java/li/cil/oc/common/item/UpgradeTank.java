package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class UpgradeTank extends SimpleItemBase {
    private String unlocalizedName;

    public UpgradeTank(Properties properties) {
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
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag()
                .getCompound(Settings.namespace() + "data");
            FluidStack.parse(context.level().registryAccess(), tag).ifPresent(fluid ->
                tooltip.add(Component.literal(fluid.getDisplayName().getString() + ": " + fluid.getAmount() + "/16000")
                    .setStyle(Tooltip.DefaultStyle)));
        }
    }
}
