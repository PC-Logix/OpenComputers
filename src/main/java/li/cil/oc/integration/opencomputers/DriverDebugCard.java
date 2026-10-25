package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.server.component.DebugCard;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverDebugCard implements Item {
    public static final DriverDebugCard INSTANCE = new DriverDebugCard();
    public static final EnvironmentProvider PROVIDER = stack -> INSTANCE.worksWith(stack) ? DebugCard.class : null;
    private DriverDebugCard() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.DebugCard());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide() ? null : new DebugCard(host);
    }
    @Override public String slot(ItemStack stack) { return Slot.Card; }
}
