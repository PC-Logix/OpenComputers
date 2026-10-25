package li.cil.oc.integration.mekanism;

import li.cil.oc.Settings;
import li.cil.oc.api.driver.Converter;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;

import java.util.Map;

public final class ConverterChemicalStack implements Converter {
    public static final ConverterChemicalStack INSTANCE = new ConverterChemicalStack();

    private ConverterChemicalStack() {
    }

    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (!(value instanceof ChemicalStack)) {
            return;
        }
        final ChemicalStack stack = (ChemicalStack) value;
        if (Settings.get().insertIdsInConverters()) {
            output.put("id", MekanismAPI.CHEMICAL_REGISTRY.getId(stack.getChemical()));
        }
        output.put("amount", stack.getAmount());
        final var chemical = stack.getChemical();
        if (chemical != null) {
            output.put("name", chemical.getRegistryName().toString());
            output.put("label", chemical.getTextComponent().getString());
        }
    }
}
