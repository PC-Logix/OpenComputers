package li.cil.oc.common.item.data;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

public class HoverBootsData extends ItemData {
    public double charge;

    public HoverBootsData() {
        super("hoverboots");
    }

    public HoverBootsData(DataComponentHolder holder) {
        this();
        loadData(holder);
    }

    @Override
    public void loadData(DataComponentHolder holder) {
        Object value = holder.get(OCComponentsJava.CHARGE.get());
        charge = value == null ? 0.0 : (Double) value;
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        holder.set(OCComponentsJava.CHARGE.get(), charge);
    }
}
