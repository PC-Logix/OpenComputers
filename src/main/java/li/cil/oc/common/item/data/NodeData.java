package li.cil.oc.common.item.data;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.network.Visibility;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedMutableDataComponentHolderJava;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.Optional;

public class NodeData extends ItemData {
    private Optional<String> address = Optional.empty();
    private Optional<Double> buffer = Optional.empty();
    private Optional<Visibility> visibility = Optional.empty();

    public NodeData() {
        super(null);
    }

    public NodeData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public Optional<String> address() { return address; }
    public void address_$eq(Optional<String> value) { address = value; }
    public Optional<Double> buffer() { return buffer; }
    public void buffer_$eq(Optional<Double> value) { buffer = value; }
    public Optional<Visibility> visibility() { return visibility; }
    public void visibility_$eq(Optional<Visibility> value) { visibility = value; }

    @Override
    public void loadData(DataComponentHolder holder) {
        ExtendedDataComponentHolder support = new ExtendedDataComponentHolder(holder);
        address = support.getComponentOptional(OCComponentsJava.ADDRESS);
        visibility = support.getComponentOptional(OCComponentsJava.VISIBILITY);
        @SuppressWarnings("unchecked")
        DataComponentType<Object> chargeType = (DataComponentType<Object>) (DataComponentType<?>) OCComponentsJava.CHARGE.get();
        @SuppressWarnings("unchecked")
        Optional<Double> charge = (Optional<Double>) (Optional<?>) support.getComponentOptional(chargeType);
        buffer = charge;
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        ExtendedMutableDataComponentHolderJava support = new ExtendedMutableDataComponentHolderJava(holder);
        support.setComponent(OCComponentsJava.ADDRESS, address);
        support.setComponent(OCComponentsJava.VISIBILITY, visibility);
        @SuppressWarnings("unchecked")
        DataComponentType<Object> chargeType = (DataComponentType<Object>) (DataComponentType<?>) OCComponentsJava.CHARGE.get();
        support.setComponent(chargeType, buffer);
    }
}
