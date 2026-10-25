package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverContainerFloppy implements Item, li.cil.oc.api.driver.item.Container {
    public static final DriverContainerFloppy INSTANCE = new DriverContainerFloppy();
    private DriverContainerFloppy() {}
    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.BlockName$.MODULE$.DiskDrive());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, li.cil.oc.api.network.EnvironmentHost host) { return null; }
    @Override public String slot(ItemStack stack) { return Slot.Container; }
    @Override public String providedSlot(ItemStack stack) { return Slot.Floppy; }
    @Override public int providedTier(ItemStack stack) { return Tier.Any; }
}
