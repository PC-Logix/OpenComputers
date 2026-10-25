package li.cil.oc.common.item;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Settings;
import li.cil.oc.common.datacomponents.MFCoords;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public final class UpgradeMF extends SimpleItemBase {
    private String unlocalizedName;

    public UpgradeMF(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var level = context.getLevel();
        if (!level.isClientSide() && context.isSecondaryUseActive()) {
            stack.set(OCComponentsJava.MF_COORD.get(),
                new MFCoords(level.dimension().location(), context.getClickedPos(), context.getClickedFace()));
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.onItemUseFirst(stack, context);
    }

    @Override
    public void tooltipExtended(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        String state = stack.has(OCComponentsJava.MF_COORD.get()) ? "Linked" : "Unlinked";
        tooltip.add(Component.translatable(Settings.namespace() + "tooltip.upgrademf." + state)
            .setStyle(Tooltip.DefaultStyle));
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
