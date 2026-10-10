package li.cil.oc.common.menu;

import li.cil.oc.common.item.TabletWrapper;
import li.cil.oc.integration.opencomputers.DriverScreen;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public class Tablet extends AbstractMenu {
    private final ItemStack stack;
    private final DataSlot runningData;

    public Tablet(int id, Inventory playerInventory, ItemStack stack, Container tablet, String slot1, int tier1) {
        super(MenuTypes.TABLET.get(), id, playerInventory, tablet);
        this.stack = stack;

        addSlot(new StaticComponentSlot(this, otherInventory(), otherInventory().getContainerSize() - 1,
            90, 35, getHostClass(), slot1, tier1) {
            @Override
            public boolean mayPlace(ItemStack item) {
                return !DriverScreen.worksWith(item, getHostClass()) && super.mayPlace(item);
            }
        });
        addPlayerInventorySlots(8, 84);

        if (tablet instanceof TabletWrapper wrapper) {
            runningData = addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return wrapper.machine().isRunning() ? 1 : 0;
                }

                @Override
                public void set(int value) {
                }
            });
        } else {
            runningData = addDataSlot(DataSlot.standalone());
        }
    }

    public ItemStack stack() {
        return stack;
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return TabletWrapper.class;
    }

    public boolean isRunning() {
        return runningData.get() != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == playerInventory().player;
    }
}
