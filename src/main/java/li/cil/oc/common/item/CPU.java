package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.item.traits.CPULike;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import scala.collection.immutable.Seq;

import java.util.List;

public final class CPU extends ComponentItemBase implements CPULike{
    private final int tier;
    private String unlocalizedName;

    public CPU(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    public int tier() { return tier; }
    @Override public int cpuTier() { return tier; }
    @Override public Seq<Object> tooltipData() { return li.cil.oc.common.item.traits.CPULikeJava.tooltipData(this); }
    @Override public void tooltipExtended(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        li.cil.oc.common.item.traits.CPULikeJava.tooltipExtended(this, stack, tooltip, flag);
    }
    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return li.cil.oc.common.item.traits.CPULikeJava.use(this, level, player, hand);
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
