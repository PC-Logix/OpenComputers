package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.server.component.LinkedCard;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverLinkedCard implements Item {
    public static final DriverLinkedCard INSTANCE = new DriverLinkedCard();
    public static final EnvironmentProvider PROVIDER = stack -> INSTANCE.worksWith(stack) ? LinkedCard.class : null;
    private DriverLinkedCard() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.LinkedCard());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide() ? null : new LinkedCard();
    }
    @Override public String slot(ItemStack stack) { return Slot.Card; }
    @Override public int tier(ItemStack stack) { return Tier.Three; }
}
