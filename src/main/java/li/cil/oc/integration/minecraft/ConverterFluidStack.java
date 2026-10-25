package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.Converter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Map;

public final class ConverterFluidStack implements Converter {
    public static final ConverterFluidStack INSTANCE = new ConverterFluidStack();

    private ConverterFluidStack() {}

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof FluidStack stack) {
            output.put("amount", stack.getAmount());
            output.put("hasTag", stack.has(DataComponents.CUSTOM_DATA));
            var fluid = stack.getFluid();
            output.put("name", BuiltInRegistries.FLUID.getKey(fluid).toString());
            output.put("label", fluid.getFluidType().getDescription(stack).getString());
        }
    }
}
