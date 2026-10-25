package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.blockentity.traits.BundledRedstoneAware;
import li.cil.oc.common.blockentity.traits.RedstoneAware;
import li.cil.oc.common.item.RedstoneCard;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.integration.util.WirelessRedstone;
import li.cil.oc.server.component.Redstone;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverRedstoneCard implements Item, HostAware {
    public static final DriverRedstoneCard INSTANCE = new DriverRedstoneCard();
    public static final EnvironmentProvider PROVIDER = stack -> {
        if (!INSTANCE.worksWith(stack)) return null;
        boolean advanced = INSTANCE.tier(stack) == Tier.Two;
        boolean hasBundled = BundledRedstone.isAvailable() && advanced;
        boolean hasWireless = WirelessRedstone.isAvailable() && advanced;
        if (hasBundled) return hasWireless ? Redstone.BundledWireless.class : Redstone.Bundled.class;
        return Redstone.Vanilla.class;
    };
    private DriverRedstoneCard() {}

    @Override public boolean worksWith(ItemStack stack) {
        var items = new li.cil.oc.api.detail.ItemInfo[] {
                Items.get(Constants.ItemName$.MODULE$.RedstoneCardTier1()),
                Items.get(Constants.ItemName$.MODULE$.RedstoneCardTier2())
        };
        var item = Items.get(stack);
        for (var info : items) if (info != null && Objects.equals(item, info)) return true;
        return false;
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        boolean advanced = tier(stack) == Tier.Two;
        boolean hasBundled = BundledRedstone.isAvailable() && advanced;
        boolean hasWireless = WirelessRedstone.isAvailable() && advanced;
        if (host instanceof BundledRedstoneAware && hasBundled) {
            var redstone = (EnvironmentHost & BundledRedstoneAware) host;
            return hasWireless ? new Redstone.BundledWireless(redstone) : new Redstone.Bundled(redstone);
        }
        if (host instanceof RedstoneAware) {
            var redstone = (EnvironmentHost & RedstoneAware) host;
            return hasWireless ? new Redstone.VanillaWireless(redstone) : new Redstone.Vanilla(redstone);
        }
        return hasWireless ? new Redstone.Wireless(host) : null;
    }
    @Override public String slot(ItemStack stack) { return Slot.Card; }
    @Override public int tier(ItemStack stack) {
        return stack.getItem() instanceof RedstoneCard card ? card.tier() : Tier.One;
    }
}
