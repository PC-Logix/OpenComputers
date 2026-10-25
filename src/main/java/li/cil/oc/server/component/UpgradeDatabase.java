package li.cil.oc.server.component;

import com.google.common.hash.Hashing;
import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.DatabaseAccess;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.ItemUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Map;

public class UpgradeDatabase extends AbstractManagedEnvironment implements Database, DeviceInfo {
    private final Container data;
    private Map<String, String> deviceInfo;

    public UpgradeDatabase(Container data) {
        this.data = data;
        setNode(Network.newNode(this, Visibility.Network).withComponent("database").create());
    }

    public Container data() { return data; }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = Map.of(
            DeviceAttribute.Class, DeviceClass.Generic,
            DeviceAttribute.Description, "Object catalogue",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "iCatalogue (patent pending)",
            DeviceAttribute.Capacity, Integer.toString(size()));
        return deviceInfo;
    }

    @Override public int size() { return data.getContainerSize(); }
    @Override public void setStackInSlot(int slot, ItemStack stack) { data.setItem(slot, stack); }
    @Override public int findStackWithHash(String hash) { return indexOf(hash, 0); }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack stack = data.getItem(slot);
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    @Callback(doc = "function(slot:number):table -- Get the representation of the item stack stored in the specified slot.")
    public Object[] get(Context context, Arguments args) {
        return ResultWrapper.result(data.getItem(new ExtendedArgumentsJava(args).checkSlot(data, 0)));
    }

    @Callback(doc = "function(slot:number):string -- Computes a hash value for the item stack in the specified slot.")
    public Object[] computeHash(Context context, Arguments args) {
        ItemStack stack = data.getItem(new ExtendedArgumentsJava(args).checkSlot(data, 0));
        return stack == null ? null : ResultWrapper.result(hash(stack));
    }

    @Callback(doc = "function(hash:string):number -- Get the index of an item stack with the specified hash. Returns a negative value if no such stack was found.")
    public Object[] indexOf(Context context, Arguments args) { return ResultWrapper.result(indexOf(args.checkString(0), 1)); }

    @Callback(doc = "function(slot:number):boolean -- Clears the specified slot. Returns true if there was something in the slot before.")
    public Object[] clear(Context context, Arguments args) {
        int slot = new ExtendedArgumentsJava(args).checkSlot(data, 0);
        boolean nonEmpty = data.getItem(slot) != ItemStack.EMPTY;
        data.setItem(slot, ItemStack.EMPTY);
        return ResultWrapper.result(nonEmpty);
    }

    @Callback(doc = "function(fromSlot:number, toSlot:number[, address:string]):boolean -- Copies an entry to another slot, optionally to another database. Returns true if something was overwritten.")
    public Object[] copy(Context context, Arguments args) {
        int from = new ExtendedArgumentsJava(args).checkSlot(data, 0);
        ItemStack entry = data.getItem(from);
        if (args.count() > 2) {
            return DatabaseAccess.withDatabase(node(), args.checkString(2), database -> copyEntry(entry, database.data(), args));
        }
        return copyEntry(entry, data, args);
    }

    private Object[] copyEntry(ItemStack entry, Container target, Arguments args) {
        int to = new ExtendedArgumentsJava(args).checkSlot(target, 1);
        boolean nonEmpty = target.getItem(to) != ItemStack.EMPTY;
        target.setItem(to, entry.copy());
        return ResultWrapper.result(nonEmpty);
    }

    @Callback(doc = "function(address:string):number -- Copies the data stored in this database to another database with the specified address.")
    public Object[] clone(Context context, Arguments args) {
        return DatabaseAccess.withDatabase(node(), args.checkString(0), database -> {
            int count = Math.min(data.getContainerSize(), database.data().getContainerSize());
            for (int slot = 0; slot < count; slot++) database.data().setItem(slot, data.getItem(slot).copy());
            context.pause(0.25);
            return ResultWrapper.result(count);
        });
    }

    private int indexOf(String needle, int offset) {
        for (int slot = 0; slot < data.getContainerSize(); slot++) {
            ItemStack stack = data.getItem(slot);
            if (stack != null && hash(stack).equals(needle)) return slot + offset;
        }
        return -1;
    }

    private String hash(ItemStack stack) {
        return Hashing.sha256().hashBytes(ItemUtils.saveStack(stack, ServerLifecycleHooks.getCurrentServer().registryAccess())).toString();
    }
}
