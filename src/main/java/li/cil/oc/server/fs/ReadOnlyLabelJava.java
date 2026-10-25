package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Label;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

/** Java implementation of the read-only filesystem label component. */
public class ReadOnlyLabelJava implements Label {
    private String label;

    public ReadOnlyLabelJava(String label) {
        this.label = label;
    }

    @Override
    public void setLabel(String value) {
        throw new IllegalArgumentException("label is read only");
    }

    @Override public String getLabel(HolderLookup.Provider provider) { return label; }

    @Override
    public void loadData(DataComponentHolder holder) {
        String value = holder.get(OCComponentsJava.LABEL.get());
        if (value != null) label = value;
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        if (label != null) holder.set(OCComponentsJava.LABEL.get(), label);
    }
}
