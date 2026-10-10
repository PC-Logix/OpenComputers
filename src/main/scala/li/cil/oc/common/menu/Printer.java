package li.cil.oc.common.menu;

import li.cil.oc.common.Tier;
import li.cil.oc.common.item.data.PrintData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class Printer extends AbstractMenu {
    private final Container printer;

    public Printer(int id, Inventory playerInventory, Container printer) {
        super(MenuTypes.PRINTER.get(), id, playerInventory, printer);
        this.printer = printer;

        addSlot(new StaticComponentSlot(this, otherInventory(), slots.size(), 18, 19, getHostClass(), li.cil.oc.common.Slot.Filtered, Tier.Any) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(getSlotIndex(), stack) && PrintData.materialValue(stack) > 0;
            }
        });
        addSlot(new StaticComponentSlot(this, otherInventory(), slots.size(), 18, 51, getHostClass(), li.cil.oc.common.Slot.Filtered, Tier.Any) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(getSlotIndex(), stack) && PrintData.inkValue(stack) > 0;
            }
        });
        addSlot(new Slot(otherInventory(), slots.size(), 152, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventorySlots(8, 84);
    }

    public Container printer() {
        return printer;
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.Printer.class;
    }

    public double progress() {
        return synchronizedData().getDouble("progress");
    }

    public int maxAmountMaterial() {
        return synchronizedData().getInt("maxAmountMaterial");
    }

    public int amountMaterial() {
        return synchronizedData().getInt("amountMaterial");
    }

    public int maxAmountInk() {
        return synchronizedData().getInt("maxAmountInk");
    }

    public int amountInk() {
        return synchronizedData().getInt("amountInk");
    }

    @Override
    public void detectCustomDataChanges(CompoundTag nbt) {
        if (printer instanceof li.cil.oc.common.blockentity.Printer entity) {
            synchronizedData().putDouble("progress", entity.isPrinting() ? entity.progress() / 100.0 : 0);
            synchronizedData().putInt("maxAmountMaterial", entity.maxAmountMaterial());
            synchronizedData().putInt("amountMaterial", entity.amountMaterial());
            synchronizedData().putInt("maxAmountInk", entity.maxAmountInk());
            synchronizedData().putInt("amountInk", entity.amountInk());
        }
        super.detectCustomDataChanges(nbt);
    }
}
