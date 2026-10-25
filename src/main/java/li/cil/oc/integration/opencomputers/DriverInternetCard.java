package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.server.component.InternetCard;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverInternetCard implements Item {
    public static final DriverInternetCard INSTANCE = new DriverInternetCard();
    public static final EnvironmentProvider PROVIDER = stack -> INSTANCE.worksWith(stack) ? InternetCard.class : null;
    private DriverInternetCard() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.InternetCard());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide() ? null : new InternetCard();
    }
    @Override public String slot(ItemStack stack) { return Slot.Card; }
    @Override public int tier(ItemStack stack) { return Tier.Two; }
}
