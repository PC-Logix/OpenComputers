package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.Converter;
import net.neoforged.neoforge.fluids.IFluidTank;

import java.util.Map;

public final class ConverterFluidTankInfo implements Converter {
    public static final ConverterFluidTankInfo INSTANCE = new ConverterFluidTankInfo();

    private ConverterFluidTankInfo() {}

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof IFluidTank tank) {
            output.put("capacity", tank.getCapacity());
            if (!tank.getFluid().isEmpty()) {
                ConverterFluidStack.INSTANCE.convert(tank.getFluid(), output);
            } else {
                output.put("amount", 0);
            }
        }
    }
}
