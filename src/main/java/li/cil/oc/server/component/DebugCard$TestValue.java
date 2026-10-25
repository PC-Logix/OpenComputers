package li.cil.oc.server.component;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Test userdata used by the debug card's value conversion callback. */
public class DebugCard$TestValue extends AbstractValue {
    private static final String VALUE_TAG = "value";

    private String value = "hello";

    public String value() {
        return value;
    }

    public void value_$eq(String value) {
        this.value = value;
    }

    @Override
    public Object apply(Context context, Arguments arguments) {
        OpenComputers.log().info("TestValue.apply(" + formatArguments(arguments) + ")");
        return value;
    }

    @Override
    public void unapply(Context context, Arguments arguments) {
        OpenComputers.log().info("TestValue.unapply(" + formatArguments(arguments) + ")");
        value = arguments.checkString(1);
    }

    @Override
    public Object[] call(Context context, Arguments arguments) {
        OpenComputers.log().info("TestValue.call(" + formatArguments(arguments) + ")");
        return li.cil.oc.util.ResultWrapper.result(arguments.toArray());
    }

    @Override
    public void dispose(Context context) {
        super.dispose(context);
        OpenComputers.log().info("TestValue.dispose()");
    }

    @Override
    public void loadData(DataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadData(holder, nbt, provider);
        value = nbt.getString(VALUE_TAG);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveData(holder, nbt, provider);
        nbt.putString(VALUE_TAG, value);
    }

    private static String formatArguments(Arguments arguments) {
        return Arrays.stream(arguments.toArray()).map(String::valueOf).collect(Collectors.joining(", "));
    }
}
