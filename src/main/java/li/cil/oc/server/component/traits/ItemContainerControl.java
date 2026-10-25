package li.cil.oc.server.component.traits;

import li.cil.oc.api.Driver;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.InventoryAccessJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Function;

public interface ItemContainerControl extends ContainerAware {
    @Callback(doc = "function(slot:number):number -- The size of an item inventory in the specified slot.")
    default Object[] getItemInventorySize(Context context, Arguments args) {
        int slot = new ExtendedArgumentsJava(args).checkSlot(inventory(), 0);
        return withItemInventory(slot, itemInventory -> ResultWrapper.result(itemInventory.getSlots()));
    }

    @Callback(doc = "function(inventorySlot:number, slot:number[, count:number=64]):number -- Drops an item from the selected slot into the specified slot in the item inventory.")
    default Object[] dropIntoItemInventory(Context context, Arguments args) {
        var extended = new ExtendedArgumentsJava(args);
        int inventorySlot = extended.checkSlot(inventory(), 0);
        return withItemInventory(inventorySlot, itemInventory -> {
            int slot = extended.checkSlot(itemInventory, 1);
            int count = extended.optItemCount(2, extended.optItemCount$default$2());
            int extracted = InventoryAccessJava.extractFromInventorySlot((stack, simulate) -> {
                InventoryUtils.insertIntoInventorySlot(stack, itemInventory, slot, 64, simulate);
            }, inventory(), null, selectedSlot(), count);
            return ResultWrapper.result(extracted);
        });
    }

    @Callback(doc = "function(inventorySlot:number, slot:number[, count:number=64]):number -- Sucks an item out of the specified slot in the item inventory.")
    default Object[] suckFromItemInventory(Context context, Arguments args) {
        var extended = new ExtendedArgumentsJava(args);
        int inventorySlot = extended.checkSlot(inventory(), 0);
        return withItemInventory(inventorySlot, itemInventory -> {
            int slot = extended.checkSlot(itemInventory, 1);
            int count = extended.optItemCount(2, extended.optItemCount$default$2());
            int extracted = InventoryAccessJava.extractFromInventorySlot((stack, simulate) -> {
                InventoryUtils.insertIntoInventory(stack, InventoryUtils.asItemHandler(inventory()), 64,
                    simulate, java.util.Optional.of(insertionSlots()));
            }, itemInventory, slot, count);
            return ResultWrapper.result(extracted);
        });
    }

    private Object[] withItemInventory(int slot, Function<IItemHandler, Object[]> action) {
        IItemHandler itemInventory = Driver.itemHandlerFor(inventory().getItem(slot), fakePlayer());
        return itemInventory != null ? action.apply(itemInventory) : ResultWrapper.result(0, "no item inventory");
    }
}
