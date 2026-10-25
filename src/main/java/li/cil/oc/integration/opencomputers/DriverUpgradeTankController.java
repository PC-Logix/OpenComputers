package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.blockentity.Robot;
import li.cil.oc.common.entity.Drone;
import li.cil.oc.server.component.UpgradeTankController;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverUpgradeTankController implements Item, HostAware {
    public static final DriverUpgradeTankController INSTANCE = new DriverUpgradeTankController();
    public static final EnvironmentProvider PROVIDER = stack ->
            INSTANCE.worksWith(stack) ? UpgradeTankController.Robot.class : null;

    private DriverUpgradeTankController() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.TankControllerUpgrade());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        if (host instanceof Adapter) return new UpgradeTankController.Adapter(host);
        if (host instanceof Drone drone) return new UpgradeTankController.Drone(drone);
        if (host instanceof Robot robot) return new UpgradeTankController.Robot(robot);
        return null;
    }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
    @Override public int tier(ItemStack stack) { return Tier.Two; }
}
