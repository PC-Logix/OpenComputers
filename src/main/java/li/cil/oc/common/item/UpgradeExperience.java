package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import scala.collection.immutable.Seq;

import java.util.List;

public class UpgradeExperience extends SimpleItemBase {
    private String unlocalizedName;

    public UpgradeExperience(Properties properties) {
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
            var nbt = li.cil.oc.integration.opencomputers.Item.dataTag(stack);
            double experience = li.cil.oc.util.UpgradeExperience.getExperience(nbt);
            int level = li.cil.oc.util.UpgradeExperience.calculateLevelFromExperience(experience);
            double reportedLevel = li.cil.oc.util.UpgradeExperience.calculateExperienceLevel(level, experience);
            tooltip.add(Component.translatable(Settings.namespace() + "tooltip.robot_level", reportedLevel)
                .setStyle(Tooltip.DefaultStyle));
        }
    }
}
