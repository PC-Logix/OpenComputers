package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.APU;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverAPU extends DriverCPU implements HostAware {
    public static final DriverAPU INSTANCE = new DriverAPU();
    private DriverAPU() {}

    @Override
    public boolean worksWith(ItemStack stack) {
        ItemInfo item = li.cil.oc.api.Items.get(stack);
        ItemInfo[] supported = {
                li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.APUTier1()),
                li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.APUTier2()),
                li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.APUTier3()),
                li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.APUCreative())
        };
        for (ItemInfo candidate : supported) if (candidate != null && Objects.equals(item, candidate)) return true;
        return false;
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return worksWith(stack);
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        return switch (gpuTier(stack)) {
            case Tier.One -> new li.cil.oc.server.component.APU(Tier.One);
            case Tier.Two -> new li.cil.oc.server.component.APU(Tier.Two);
            case Tier.Three -> new li.cil.oc.server.component.APU(Tier.Three);
            case Tier.Four -> new li.cil.oc.server.component.APU(Tier.Four);
            default -> null;
        };
    }

    @Override
    public int cpuTier(ItemStack stack) {
        return stack.getItem() instanceof APU apu ? apu.cpuTier() : Tier.One;
    }

    public int gpuTier(ItemStack stack) {
        return stack.getItem() instanceof APU apu ? apu.gpuTier() : Tier.One;
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();
        private Provider() {}

        @Override
        public Class<? extends li.cil.oc.api.network.Environment> getEnvironment(ItemStack stack) {
            return DriverAPU.INSTANCE.worksWith(stack) ? li.cil.oc.server.component.GraphicsCard.class : null;
        }
    }
}
