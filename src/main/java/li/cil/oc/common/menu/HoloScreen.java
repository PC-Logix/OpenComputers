package li.cil.oc.common.menu;

import li.cil.oc.api.Items;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class HoloScreen extends AbstractMenu {
    public HoloScreen(int id, Inventory playerInventory, Container screen) {
        super(MenuTypes.HOLO_SCREEN.get(), id, playerInventory, screen);
        addSlot(new Slot(otherInventory(), 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return Objects.equals(Items.get(stack), Items.get("keyboard")) && super.mayPlace(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addPlayerInventorySlots(8, 84);
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.HoloScreen.class;
    }
}
