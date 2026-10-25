package li.cil.oc.common.item.traits;

import net.minecraft.world.item.ItemStack;

public interface Chargeable extends li.cil.oc.api.driver.item.Chargeable {
    double maxCharge(ItemStack stack);

    double getCharge(ItemStack stack);

    void setCharge(ItemStack stack, double amount);

    default boolean canExtract(ItemStack stack) {
        return false;
    }
}
