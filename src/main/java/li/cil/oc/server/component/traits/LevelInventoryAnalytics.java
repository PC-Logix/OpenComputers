package li.cil.oc.server.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.ItemStackArrayValue;
import li.cil.oc.util.BlockInventorySource;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.DatabaseAccess;
import li.cil.oc.util.EntityInventorySource;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.InventorySource;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.InventoryAccessJava;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.Objects;
import java.util.function.Function;

public interface LevelInventoryAnalytics extends LevelAware, SideRestricted, NetworkAware {
    @Callback(doc = "function(side:number):number -- Get the number of slots in the inventory on the specified side of the device.")
    default Object[] getInventorySize(Context context, Arguments args) {
        return withInventory(checkSideForAction(args, 0), inventory -> ResultWrapper.result(inventory.getSlots()));
    }

    @Callback(doc = "function(side:number, slot:number):number -- Get number of items in the specified slot of the inventory on the specified side of the device.")
    default Object[] getSlotStackSize(Context context, Arguments args) {
        return withInventory(checkSideForAction(args, 0), inventory -> {
            ItemStack stack = inventory.getStackInSlot(slot(args, inventory, 1));
            return ResultWrapper.result(stack.isEmpty() ? 0 : stack.getCount());
        });
    }

    @Callback(doc = "function(side:number, slot:number):number -- Get the maximum number of items in the specified slot of the inventory on the specified side of the device.")
    default Object[] getSlotMaxStackSize(Context context, Arguments args) {
        return withInventory(checkSideForAction(args, 0), inventory -> {
            ItemStack stack = inventory.getStackInSlot(slot(args, inventory, 1));
            return ResultWrapper.result(stack.isEmpty() ? 0 : stack.getMaxStackSize());
        });
    }

    @Callback(doc = "function(side:number, slotA:number, slotB:number[, checkNBT:boolean=false]):boolean -- Get whether the items in the two specified slots of the inventory on the specified side of the device are of the same type.")
    default Object[] compareStacks(Context context, Arguments args) {
        return withInventory(checkSideForAction(args, 0), inventory -> {
            ItemStack first = inventory.getStackInSlot(slot(args, inventory, 1));
            ItemStack second = inventory.getStackInSlot(slot(args, inventory, 2));
            return ResultWrapper.result(Objects.equals(first, second)
                || InventoryUtils.haveSameItemType(first, second, args.optBoolean(3, false)));
        });
    }

    @Callback(doc = "function(side:number, slot:number, dbAddress:string, dbSlot:number[, checkNBT:boolean=false]):boolean -- Compare an item in the specified slot in the inventory on the specified side with one in the database with the specified address.")
    default Object[] compareStackToDatabase(Context context, Arguments args) {
        return withInventory(checkSideForAction(args, 0), inventory -> {
            int selected = slot(args, inventory, 1);
            String address = args.checkString(2);
            ItemStack stack = inventory.getStackInSlot(selected);
            return DatabaseAccess.withDatabase(node(), address, database -> {
                int databaseSlot = new ExtendedArgumentsJava(args).checkSlot(database.data(), 3);
                return ResultWrapper.result(InventoryUtils.haveSameItemType(
                    stack, database.getStackInSlot(databaseSlot), args.optBoolean(4, false)));
            });
        });
    }

    @Callback(doc = "function(side:number, slotA:number, slotB:number):boolean -- Get whether the items in the two specified slots of the inventory on the specified side of the device are equivalent (have shared OreDictionary IDs).")
    default Object[] areStacksEquivalent(Context context, Arguments args) {
        return withInventory(checkSideForAction(args, 0), inventory -> {
            ItemStack first = inventory.getStackInSlot(slot(args, inventory, 1));
            ItemStack second = inventory.getStackInSlot(slot(args, inventory, 2));
            return ResultWrapper.result(Objects.equals(first, second)
                || (!first.isEmpty() && !second.isEmpty() && first.getTags().anyMatch(second::is)));
        });
    }

    @Callback(doc = "function(side:number, slot:number):table -- Get a description of the stack in the inventory on the specified side of the device.")
    default Object[] getStackInSlot(Context context, Arguments args) {
        if (!Settings.get().allowItemStackInspection()) return ResultWrapper.result(null, "not enabled in config");
        return withInventory(checkSideForAction(args, 0), inventory ->
            ResultWrapper.result(inventory.getStackInSlot(slot(args, inventory, 1))));
    }

    @Callback(doc = "function(side:number, slot:number):table -- Alias for getStackInSlot.")
    default Object[] getItem(Context context, Arguments args) {
        return getStackInSlot(context, args);
    }

    @Callback(doc = "function(side:number):userdata -- Get a description of all stacks in the inventory on the specified side of the device.")
    default Object[] getAllStacks(Context context, Arguments args) {
        if (!Settings.get().allowItemStackInspection()) return ResultWrapper.result(null, "not enabled in config");
        return withInventory(checkSideForAction(args, 0), inventory -> {
            ItemStack[] stacks = new ItemStack[inventory.getSlots()];
            for (int index = 0; index < stacks.length; index++) stacks[index] = inventory.getStackInSlot(index);
            return ResultWrapper.result(new ItemStackArrayValue(stacks));
        });
    }

    @Callback(doc = "function(side:number):string -- Get the the name of the inventory on the specified side of the device.")
    default Object[] getInventoryName(Context context, Arguments args) {
        if (!Settings.get().allowItemStackInspection()) return ResultWrapper.result(null, "not enabled in config");
        return withInventorySource(checkSideForAction(args, 0), source -> {
            if (source instanceof BlockInventorySource block) {
                BlockPosition location = block.position();
                if (location.worldOptional().isPresent() && location.worldOptional().orElseThrow().isLoaded(location.toBlockPos())) {
                    var type = location.worldOptional().orElseThrow().getBlockState(location.toBlockPos()).getBlock();
                    return ResultWrapper.result(BuiltInRegistries.BLOCK.getKey(type).toString());
                }
            }
            else if (source instanceof EntityInventorySource entity) {
                return ResultWrapper.result(BuiltInRegistries.ENTITY_TYPE.getKey(entity.entity().getType()).toString());
            }
            return ResultWrapper.result(null, "Unknown");
        });
    }

    @Callback(doc = "function(side:number, slot:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack description in the specified slot of the database with the specified address.")
    default Object[] store(Context context, Arguments args) {
        Direction side = checkSideForAction(args, 0);
        String address = args.checkString(2);
        return withInventory(side, inventory -> {
            ItemStack stack = inventory.getStackInSlot(slot(args, inventory, 1));
            return DatabaseAccess.withDatabase(node(), address, database -> {
                int databaseSlot = new ExtendedArgumentsJava(args).checkSlot(database.data(), 3);
                boolean nonEmpty = database.getStackInSlot(databaseSlot) != ItemStack.EMPTY;
                database.setStackInSlot(databaseSlot, stack.copy());
                return ResultWrapper.result(nonEmpty);
            });
        });
    }

    private int slot(Arguments args, IItemHandler inventory, int index) {
        return new ExtendedArgumentsJava(args).checkSlot(inventory, index);
    }

    private Object[] withInventorySource(Direction side, Function<InventorySource, Object[]> action) {
        var source = InventoryAccessJava.inventorySourceAt(position().offset(side), side.getOpposite());
        if (source.isPresent() && mayInteract(source.get())) return action.apply(source.get());
        return ResultWrapper.result(null, "no inventory");
    }

    private Object[] withInventory(Direction side, Function<IItemHandler, Object[]> action) {
        return withInventorySource(side, source -> action.apply(source.inventory()));
    }
}
