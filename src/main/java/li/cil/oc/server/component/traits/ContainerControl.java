package li.cil.oc.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.world.item.ItemStack;

public interface ContainerControl extends ContainerAware {
    @Callback(doc = "function():number -- The size of this device's internal inventory.")
    default Object[] inventorySize(Context context, Arguments args) {
        return ResultWrapper.result(inventory().getContainerSize());
    }

    @Callback(doc = "function([slot:number]):number -- Get the currently selected slot; set the selected slot if specified.")
    default Object[] select(Context context, Arguments args) {
        int slot = optSlot(args, 0);
        if (slot != selectedSlot()) selectedSlot_$eq(slot);
        return ResultWrapper.result(selectedSlot() + 1);
    }

    @Callback(direct = true, doc = "function([slot:number]):number -- Get the number of items in the specified slot, otherwise in the selected slot.")
    default Object[] count(Context context, Arguments args) {
        ItemStack stack = inventory().getItem(optSlot(args, 0));
        return ResultWrapper.result(stack.isEmpty() ? 0 : stack.getCount());
    }

    @Callback(direct = true, doc = "function([slot:number]):number -- Get the remaining space in the specified slot, otherwise in the selected slot.")
    default Object[] space(Context context, Arguments args) {
        ItemStack stack = inventory().getItem(optSlot(args, 0));
        return ResultWrapper.result(stack.isEmpty() ? inventory().getMaxStackSize()
            : Math.min(inventory().getMaxStackSize(), stack.getMaxStackSize()) - stack.getCount());
    }

    @Callback(doc = "function(otherSlot:number[, checkNBT:boolean=false]):boolean -- Compare the contents of the selected slot to the contents of the specified slot.")
    default Object[] compareTo(Context context, Arguments args) {
        int slot = new ExtendedArgumentsJava(args).checkSlot(inventory(), 0);
        ItemStack first = inventory().getItem(selectedSlot());
        ItemStack second = inventory().getItem(slot);
        if (first.isEmpty() || second.isEmpty()) return ResultWrapper.result(first.isEmpty() && second.isEmpty());
        return ResultWrapper.result(InventoryUtils.haveSameItemType(first, second, args.optBoolean(1, false)));
    }

    @Callback(doc = "function(toSlot:number[, amount:number]):boolean -- Move up to the specified amount of items from the selected slot into the specified slot.")
    default Object[] transferTo(Context context, Arguments args) {
        var extended = new ExtendedArgumentsJava(args);
        int slot = extended.checkSlot(inventory(), 0);
        int count = extended.optItemCount(1, extended.optItemCount$default$2());
        if (slot == selectedSlot() || count == 0) return ResultWrapper.result(true);

        ItemStack from = inventory().getItem(selectedSlot());
        ItemStack to = inventory().getItem(slot);
        if (from.isEmpty()) return ResultWrapper.result(false);
        if (!to.isEmpty()) {
            if (InventoryUtils.haveSameItemType(from, to, true)) {
                int space = Math.min(inventory().getMaxStackSize(), to.getMaxStackSize()) - to.getCount();
                int amount = Math.min(count, Math.min(space, from.getCount()));
                if (amount <= 0) return ResultWrapper.result(false);
                from.shrink(amount);
                to.grow(amount);
                assert from.getCount() >= 0;
                if (from.getCount() == 0) inventory().setItem(selectedSlot(), ItemStack.EMPTY);
                inventory().setChanged();
                return ResultWrapper.result(true);
            }
            if (count < from.getCount()) return ResultWrapper.result(false);
            inventory().setItem(slot, from);
            inventory().setItem(selectedSlot(), to);
            return ResultWrapper.result(true);
        }
        inventory().setItem(slot, inventory().removeItem(selectedSlot(), count));
        return ResultWrapper.result(true);
    }
}
