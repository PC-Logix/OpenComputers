package li.cil.oc.common.menu;

import li.cil.oc.common.Tier;
import li.cil.oc.integration.util.ItemCharge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class Charger extends AbstractMenu {
    public Charger(int id, Inventory playerInventory, Container charger) {
        super(MenuTypes.CHARGER.get(), id, playerInventory, charger);
        addSlot(new StaticComponentSlot(this, otherInventory(), slots.size(), 80, 35, getHostClass(), "tablet", Tier.Any) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(getSlotIndex(), stack) && ItemCharge.canCharge(stack);
            }
        });
        addPlayerInventorySlots(8, 84);
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.Charger.class;
    }
}
