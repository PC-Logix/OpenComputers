package li.cil.oc.integration.opencomputers;

import com.google.common.base.Strings;
import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.common.item.data.NanomachineData;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public final class ConverterNanomachines implements Converter {
    public static final ConverterNanomachines INSTANCE = new ConverterNanomachines();

    private ItemInfo nanomachines;

    private ConverterNanomachines() {}

    private synchronized ItemInfo nanomachines() {
        if (nanomachines == null) nanomachines = Items.get(Constants.ItemName$.MODULE$.Nanomachines());
        return nanomachines;
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (!(value instanceof ItemStack stack)) return;
        if (Items.get(stack) != nanomachines()) return;

        var data = new NanomachineData(stack);
        if (!Strings.isNullOrEmpty(data.uuid)) output.put("nanomachines", data.uuid);
    }
}
