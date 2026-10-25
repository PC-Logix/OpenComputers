package li.cil.oc.client.gui.traits;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public interface LockedHotbar<C extends AbstractContainerMenu> {
    ItemStack lockedStack();
}