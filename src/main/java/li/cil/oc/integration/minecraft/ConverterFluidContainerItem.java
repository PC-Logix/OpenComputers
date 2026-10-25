package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.Converter;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class ConverterFluidContainerItem implements Converter {
    public static final ConverterFluidContainerItem INSTANCE = new ConverterFluidContainerItem();

    private ConverterFluidContainerItem() {}

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (!(value instanceof ItemStack stack)) return;
        if (!(stack.getCapability(Capabilities.FluidHandler.ITEM) instanceof IFluidHandlerItem handler)) return;

        int capacity = 0;
        for (int i = 0; i < handler.getTanks(); i++) capacity += handler.getTankCapacity(i);
        output.put("capacity", capacity);

        if (handler.getTanks() > 1) {
            var tanks = new ArrayList<Map<Object, Object>>(handler.getTanks());
            for (int i = 0; i < handler.getTanks(); i++) {
                Map<Object, Object> tank = new HashMap<>();
                tank.put("capacity", handler.getTankCapacity(i));
                var fluid = handler.getFluidInTank(i);
                if (fluid != null) {
                    ConverterFluidStack.INSTANCE.convert(fluid, tank);
                } else {
                    tank.put("amount", 0);
                }
                tanks.add(tank);
            }
            output.put("fluid", tanks);
        } else {
            output.put("fluid", handler.getFluidInTank(0));
        }
    }
}
