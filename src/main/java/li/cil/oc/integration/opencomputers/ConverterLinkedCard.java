package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.server.component.LinkedCard;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public final class ConverterLinkedCard implements Converter {
    public static final ConverterLinkedCard INSTANCE = new ConverterLinkedCard();

    private ItemInfo linkedCard;

    private ConverterLinkedCard() {}

    private synchronized ItemInfo linkedCard() {
        if (linkedCard == null) linkedCard = Items.get(Constants.ItemName$.MODULE$.LinkedCard());
        return linkedCard;
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (!(value instanceof ItemStack stack)) return;
        if (Items.get(stack) == linkedCard()) output.put("linkChannel", new LinkedCard().tunnel());
    }
}
