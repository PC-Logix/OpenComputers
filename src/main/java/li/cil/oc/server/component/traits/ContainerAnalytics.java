package li.cil.oc.server.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.DatabaseAccess;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.world.item.ItemStack;

public interface ContainerAnalytics extends ContainerAware, NetworkAware {
    @Callback(doc = "function([slot:number]):table -- Get a description of the stack in the specified slot or the selected slot.")
    default Object[] getStackInInternalSlot(Context context, Arguments args) {
        if (!Settings.get().allowItemStackInspection()) return ResultWrapper.result(ResultWrapper.unit, "not enabled in config");
        return ResultWrapper.result(inventory().getItem(optSlot(args, 0)));
    }

    @Callback(doc = "function(otherSlot:number):boolean -- Get whether the stack in the selected slot is equivalent to the item in the specified slot (have shared OreDictionary IDs).")
    default Object[] isEquivalentTo(Context context, Arguments args) {
        int slot = new ExtendedArgumentsJava(args).checkSlot(inventory(), 0);
        ItemStack first = inventory().getItem(selectedSlot());
        ItemStack second = inventory().getItem(slot);
        if (first.isEmpty() || second.isEmpty()) return ResultWrapper.result(first.isEmpty() && second.isEmpty());
        // Keep the original callback's nested result for two non-empty stacks.
        return ResultWrapper.result((Object) ResultWrapper.result(first.equals(second)
            || InventoryUtils.haveSameItemType(first, second, args.optBoolean(3, false))));
    }

    @Callback(doc = "function(slot:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack description in the specified slot of the database with the specified address.")
    default Object[] storeInternal(Context context, Arguments args) {
        var extended = new ExtendedArgumentsJava(args);
        int slot = extended.checkSlot(inventory(), 0);
        String address = args.checkString(1);
        ItemStack stack = inventory().getItem(slot);
        return DatabaseAccess.withDatabase(node(), address, database -> {
            int databaseSlot = extended.checkSlot(database.data(), 2);
            boolean nonEmpty = database.getStackInSlot(databaseSlot) != ItemStack.EMPTY;
            database.setStackInSlot(databaseSlot, stack.copy());
            return ResultWrapper.result(nonEmpty);
        });
    }

    @Callback(doc = "function(slot:number, dbAddress:string, dbSlot:number[, checkNBT:boolean=false]):boolean -- Compare an item in the specified slot with one in the database with the specified address.")
    default Object[] compareToDatabase(Context context, Arguments args) {
        var extended = new ExtendedArgumentsJava(args);
        int slot = extended.checkSlot(inventory(), 0);
        String address = args.checkString(1);
        ItemStack stack = inventory().getItem(slot);
        return DatabaseAccess.withDatabase(node(), address, database -> {
            int databaseSlot = extended.checkSlot(database.data(), 2);
            return ResultWrapper.result(InventoryUtils.haveSameItemType(
                stack, database.getStackInSlot(databaseSlot), args.optBoolean(3, false)));
        });
    }
}
