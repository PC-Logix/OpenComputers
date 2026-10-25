package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.util.Tooltip;
import li.cil.oc.SettingsAccessJava;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;

import java.util.List;
import scala.collection.immutable.Seq;
import scala.jdk.javaapi.CollectionConverters;

public final class GraphicsCard extends ComponentItemBase {
    private final int tier;
    private String unlocalizedName;

    public GraphicsCard(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    public int tier() { return tier; }
    public int gpuTier() { return tier; }

    @Override
    public Seq<Object> tooltipData() {
        return gpuTooltipData(gpuTier());
    }

    static Seq<Object> gpuTooltipData(int tier) {
        int width = SettingsAccessJava.screenWidthAtTier(tier);
        int height = SettingsAccessJava.screenHeightAtTier(tier);
        int depth = switch (SettingsAccessJava.screenDepthAtTier(tier)) {
            case OneBit -> 1;
            case FourBit -> 4;
            case EightBit -> 8;
            case SixteenBit -> 16;
        };
        String memory = switch (tier) {
            case 0 -> "1/1/4/2/2";
            case 1 -> "2/4/8/4/4";
            case 2 -> "4/8/16/8/8";
            case 3 -> "8/16/32/16/16";
            default -> throw new IllegalArgumentException("Unsupported GPU tier: " + tier);
        };
        return CollectionConverters.asScala(List.<Object>of(width, height, depth, memory)).toSeq();
    }
    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
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
