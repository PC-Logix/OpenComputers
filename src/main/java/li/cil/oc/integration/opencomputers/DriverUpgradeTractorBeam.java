package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.entity.Drone;
import li.cil.oc.common.item.TabletWrapper;
import li.cil.oc.server.component.UpgradeTractorBeam;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverUpgradeTractorBeam implements Item, HostAware {
    public static final DriverUpgradeTractorBeam INSTANCE = new DriverUpgradeTractorBeam();
    public static final EnvironmentProvider PROVIDER = stack ->
            INSTANCE.worksWith(stack) ? UpgradeTractorBeam.Common.class : null;

    private DriverUpgradeTractorBeam() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.TractorBeamUpgrade());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        if (host instanceof Drone drone) return new UpgradeTractorBeam.Drone(drone);
        if (host instanceof Robot robot) return new UpgradeTractorBeam.Player(host, robot::player);
        if (host instanceof TabletWrapper tablet) return new UpgradeTractorBeam.Player(host, tablet::player);
        return null;
    }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
    @Override public int tier(ItemStack stack) { return Tier.Three; }
}
