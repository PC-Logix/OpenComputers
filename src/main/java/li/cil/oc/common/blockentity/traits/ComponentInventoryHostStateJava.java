package li.cil.oc.common.blockentity.traits;

import net.minecraft.world.item.ItemStack;

/** Host-owned component slots and coalesced client inventory notifications. */
public final class ComponentInventoryHostStateJava {
    public final li.cil.oc.common.container.ComponentInventory.State inventory =
        new li.cil.oc.common.container.ComponentInventory.State();
    public final ClientInventoryChanges<ItemStack> clientChanges = new ClientInventoryChanges<>();
}
