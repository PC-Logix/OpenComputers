package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.component.Screen;
import li.cil.oc.common.component.TextBuffer;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverScreen implements Item, HostAware {
    public static final DriverScreen INSTANCE = new DriverScreen();
    public static final EnvironmentProvider PROVIDER = stack ->
            INSTANCE.worksWith(stack) ? Screen.class : null;

    private DriverScreen() {}

    @Override
    public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.BlockName$.MODULE$.ScreenTier1());
        return item != null && Objects.equals(Items.get(stack), item);
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return host instanceof li.cil.oc.common.blockentity.Screen screen && screen.tier() > 0
                ? new Screen(screen) : new TextBuffer(host);
    }

    @Override
    public String slot(ItemStack stack) { return Slot.Upgrade; }
}
