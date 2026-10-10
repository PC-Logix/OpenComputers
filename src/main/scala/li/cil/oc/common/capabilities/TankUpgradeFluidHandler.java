package li.cil.oc.common.capabilities;

import li.cil.oc.api.ImmutableFluidStack;
import li.cil.oc.common.datacomponents.OCComponents$;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Item fluid capability backed by the installed tank upgrade's data component. */
public final class TankUpgradeFluidHandler implements IFluidHandlerItem {
    private final ItemStack stack;
    private final FluidTank tank = new FluidTank(16 * FluidType.BUCKET_VOLUME);

    public TankUpgradeFluidHandler(ItemStack stack) {
        this.stack = stack;
        ImmutableFluidStack stored = stack.get(OCComponents$.MODULE$.TANK().get());
        tank.setFluid(stored == null ? FluidStack.EMPTY : stored.mutableCopy());
    }

    private void save() {
        stack.set(OCComponents$.MODULE$.TANK().get(), ImmutableFluidStack.copyOf(tank.getFluid()));
    }

    @Override
    public ItemStack getContainer() {
        return stack;
    }

    @Override
    public int getTanks() {
        return tank.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int index) {
        return tank.getFluidInTank(index);
    }

    @Override
    public int getTankCapacity(int index) {
        return tank.getTankCapacity(index);
    }

    @Override
    public boolean isFluidValid(int index, FluidStack fluid) {
        return tank.isFluidValid(index, fluid);
    }

    @Override
    public int fill(FluidStack fluid, FluidAction action) {
        int amount = tank.fill(fluid, action);
        if (action.execute() && amount > 0) {
            save();
        }
        return amount;
    }

    @Override
    public FluidStack drain(FluidStack fluid, FluidAction action) {
        FluidStack drained = tank.drain(fluid, action);
        if (action.execute() && drained != null && !drained.isEmpty()) {
            save();
        }
        return drained;
    }

    @Override
    public FluidStack drain(int amount, FluidAction action) {
        FluidStack drained = tank.drain(amount, action);
        if (action.execute() && drained != null && !drained.isEmpty()) {
            save();
        }
        return drained;
    }
}
