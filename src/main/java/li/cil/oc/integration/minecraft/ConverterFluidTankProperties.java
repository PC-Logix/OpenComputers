package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.Converter;
import li.cil.oc.util.TankProperties;

import java.util.Map;

public final class ConverterFluidTankProperties implements Converter {
    public static final ConverterFluidTankProperties INSTANCE = new ConverterFluidTankProperties();

    private ConverterFluidTankProperties() {}

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof TankProperties properties) {
            output.put("capacity", properties.capacity());
            var fluid = properties.contents();
            if (fluid != null) {
                ConverterFluidStack.INSTANCE.convert(fluid, output);
            } else {
                output.put("amount", 0);
            }
        }
    }
}
