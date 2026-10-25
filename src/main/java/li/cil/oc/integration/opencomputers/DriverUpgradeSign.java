package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.server.component.UpgradeSign;
import li.cil.oc.server.component.UpgradeSignInAdapter;
import li.cil.oc.server.component.UpgradeSignInRotatable;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverUpgradeSign implements Item, HostAware {
    public static final DriverUpgradeSign INSTANCE = new DriverUpgradeSign();
    public static final EnvironmentProvider PROVIDER = stack ->
            INSTANCE.worksWith(stack) ? UpgradeSign.class : null;

    private DriverUpgradeSign() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.SignUpgrade());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        if (host instanceof Rotatable) return new UpgradeSignInRotatable(host);
        if (host instanceof Adapter) return new UpgradeSignInAdapter(host);
        return null;
    }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
}
