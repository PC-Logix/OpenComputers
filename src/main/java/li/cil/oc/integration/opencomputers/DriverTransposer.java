package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.server.component.Transposer;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverTransposer implements Item, HostAware {
    public static final DriverTransposer INSTANCE = new DriverTransposer();
    public static final EnvironmentProvider PROVIDER = stack ->
            INSTANCE.worksWith(stack) ? Transposer.Upgrade.class : null;

    private DriverTransposer() {}

    @Override
    public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.BlockName$.MODULE$.Transposer());
        return item != null && Objects.equals(Items.get(stack), item);
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()
                ? null : new Transposer.Upgrade(host);
    }

    @Override
    public String slot(ItemStack stack) { return Slot.Upgrade; }
}
