package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.WirelessNetworkCard;
import li.cil.oc.server.component.WirelessNetworkCard.Tier1;
import li.cil.oc.server.component.WirelessNetworkCard.Tier2;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverWirelessNetworkCard implements Item {
    public static final DriverWirelessNetworkCard INSTANCE = new DriverWirelessNetworkCard();
    public static final EnvironmentProvider PROVIDER = stack -> {
        if (!INSTANCE.worksWith(stack)) return null;
        return switch (INSTANCE.tier(stack)) {
            case Tier.One -> Tier1.class;
            case Tier.Two -> Tier2.class;
            default -> null;
        };
    };

    private DriverWirelessNetworkCard() {}

    @Override public boolean worksWith(ItemStack stack) {
        var items = new li.cil.oc.api.detail.ItemInfo[] {
                Items.get(Constants.ItemName$.MODULE$.WirelessNetworkCardTier1()),
                Items.get(Constants.ItemName$.MODULE$.WirelessNetworkCardTier2())
        };
        var item = Items.get(stack);
        for (var info : items) if (info != null && Objects.equals(item, info)) return true;
        return false;
    }

    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        return switch (tier(stack)) {
            case Tier.One -> new Tier1(host);
            case Tier.Two -> new Tier2(host);
            default -> null;
        };
    }

    @Override public String slot(ItemStack stack) { return Slot.Card; }
    @Override public int tier(ItemStack stack) {
        return stack.getItem() instanceof WirelessNetworkCard card ? card.tier() : Tier.One;
    }
}
