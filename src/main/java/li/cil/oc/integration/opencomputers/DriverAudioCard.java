package li.cil.oc.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.server.component.AudioCard;
import net.minecraft.world.item.ItemStack;

public final class DriverAudioCard implements Item, HostAware {
    public static final DriverAudioCard INSTANCE = new DriverAudioCard();
    public static final EnvironmentProvider PROVIDER = stack -> INSTANCE.worksWith(stack) ? AudioCard.class : null;
    private DriverAudioCard() { }
    @Override public boolean worksWith(ItemStack stack) { return isOneOf(stack, li.cil.oc.api.Items.get(li.cil.oc.Constants.ItemName.AudioCardTier1)); }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide() ? null : new AudioCard(host);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) { return Item.super.worksWith(stack, host); }
    @Override public String slot(ItemStack stack) { return Slot.Card; }
    @Override public int tier(ItemStack stack) { return Tier.One; }
}

