package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.server.component.GraphicsCard;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverGraphicsCard implements Item, HostAware {
    public static final DriverGraphicsCard INSTANCE = new DriverGraphicsCard();
    public static final EnvironmentProvider PROVIDER = stack -> INSTANCE.worksWith(stack) ? GraphicsCard.class : null;
    private DriverGraphicsCard() {}

    @Override public boolean worksWith(ItemStack stack) {
        var items = new li.cil.oc.api.detail.ItemInfo[] {
                Items.get(Constants.ItemName$.MODULE$.GraphicsCardTier1()),
                Items.get(Constants.ItemName$.MODULE$.GraphicsCardTier2()),
                Items.get(Constants.ItemName$.MODULE$.GraphicsCardTier3()),
                Items.get(Constants.ItemName$.MODULE$.GraphicsCardTier4())
        };
        var item = Items.get(stack);
        for (var info : items) if (info != null && Objects.equals(item, info)) return true;
        return false;
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) { return Item.super.worksWith(stack, host); }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        int tier = tier(stack);
        return tier >= Tier.One && tier <= Tier.Four
                ? new GraphicsCard(tier, scala.Option.empty(), li.cil.oc.api.network.Visibility.Neighbors) : null;
    }
    @Override public String slot(ItemStack stack) { return Slot.Card; }
    @Override public int tier(ItemStack stack) {
        if (matches(stack, Constants.ItemName$.MODULE$.GraphicsCardTier4())) return Tier.Four;
        if (matches(stack, Constants.ItemName$.MODULE$.GraphicsCardTier3())) return Tier.Three;
        if (matches(stack, Constants.ItemName$.MODULE$.GraphicsCardTier2())) return Tier.Two;
        return Tier.One;
    }
    private static boolean matches(ItemStack stack, String itemName) {
        var info = Items.get(itemName);
        return info != null && Objects.equals(Items.get(stack), info);
    }
}
