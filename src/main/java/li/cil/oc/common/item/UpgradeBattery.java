package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.item.data.NodeData;
import li.cil.oc.common.item.traits.Chargeable;
import li.cil.oc.common.item.traits.ChargeableUtils;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import scala.collection.immutable.Seq;
import scala.jdk.javaapi.CollectionConverters;

import java.util.List;
import java.util.Optional;

public final class UpgradeBattery extends SimpleItemBase implements Chargeable{
    private final int tier;
    private String unlocalizedName;

    public UpgradeBattery(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    public int tier() { return tier; }
    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }

    @Override
    public Seq<Object> tooltipData() {
        return CollectionConverters.asScala(List.<Object>of((int) Settings.get().bufferCapacitorUpgrades()[tier])).toSeq();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable(Settings.namespace() + "tooltip.tier", super.tierFromDriver(stack) + 1)
                .setStyle(Tooltip.DefaultStyle));
        }
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        double ratio = buffer(stack) / maxCharge(stack);
        return (int) Math.round(ratio * 13.0f);
    }

    @Override public boolean canCharge(ItemStack stack) { return true; }

    @Override
    public double charge(ItemStack stack, double amount, boolean simulate) {
        NodeData data = new NodeData(stack);
        double buffer = buffer(data);
        return ChargeableUtils.applyCharge(amount, buffer, maxCharge(stack), used -> {
            if (!simulate) {
                data.buffer_$eq(Optional.of(buffer + (double) used));
                data.saveData(stack);
            }
        });
    }

    @Override public double maxCharge(ItemStack stack) { return Settings.get().bufferCapacitorUpgrades()[tier]; }
    @Override public double getCharge(ItemStack stack) { return buffer(new NodeData(stack)); }

    @Override
    public void setCharge(ItemStack stack, double amount) {
        NodeData data = new NodeData(stack);
        data.buffer_$eq(Optional.of(Math.max(0.0, Math.min(amount, maxCharge(stack)))));
        data.saveData(stack);
    }

    @Override public boolean canExtract(ItemStack stack) { return true; }

    private static double buffer(ItemStack stack) { return buffer(new NodeData(stack)); }
    private static double buffer(NodeData data) { return data.buffer().orElse(0.0); }
}
