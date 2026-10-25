package li.cil.oc.integration.opencomputers;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.datacomponents.MFCoords;
import li.cil.oc.server.component.UpgradeMF;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Objects;

public final class DriverUpgradeMF implements Item, HostAware {
    public static final DriverUpgradeMF INSTANCE = new DriverUpgradeMF();
    public static final EnvironmentProvider PROVIDER = stack ->
            INSTANCE.worksWith(stack) ? UpgradeMF.class : null;
    private DriverUpgradeMF() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.MFU());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return worksWith(stack) && host != null && Adapter.class.isAssignableFrom(host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() == null || host.getEnvironmentLevel().isClientSide()) return null;
        MFCoords coords = stack.get(OCComponentsJava.MF_COORD.get());
        if (coords == null) return null;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        ServerLevel world = server.getLevel(ResourceKey.create(Registries.DIMENSION, coords.dimension()));
        return world == null ? null : new UpgradeMF(host,
                BlockPositionsJava.apply(coords.blockPos(), world), coords.side());
    }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
    @Override public int tier(ItemStack stack) { return Tier.Three; }
}
