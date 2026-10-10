package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.ImmutableFluidStack;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.datacomponents.OCComponents$;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.Map;

public class UpgradeTank extends AbstractManagedEnvironment implements IFluidTank, DeviceInfo {
    private final EnvironmentHost owner;
    private final int capacity;
    public final FluidTank tank;
    private Map<String, String> deviceInfo;

    public UpgradeTank(EnvironmentHost owner, int capacity) {
        this.owner = owner;
        this.capacity = capacity;
        setNode(Network.newNode(this, Visibility.None).create());
        tank = new FluidTank(capacity);
    }

    public EnvironmentHost owner() { return owner; }
    public int capacity() { return capacity; }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Tank upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "Superblubb V10",
                DeviceAttribute.Capacity, Integer.toString(capacity));
        }
        return deviceInfo;
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        ImmutableFluidStack saved = holder.get(OCComponents$.MODULE$.TANK().get());
        tank.setFluid(saved == null ? FluidStack.EMPTY : saved.mutableCopy());
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        holder.set(OCComponents$.MODULE$.TANK().get(), ImmutableFluidStack.copyOf(tank.getFluid()));
    }

    @Override public FluidStack getFluid() { return tank.getFluid(); }
    @Override public int getFluidAmount() { return tank.getFluidAmount(); }
    @Override public int getCapacity() { return tank.getCapacity(); }
    @Override public boolean isFluidValid(FluidStack stack) { return tank.isFluidValid(stack); }

    @Override
    public int fill(FluidStack stack, FluidAction action) {
        int amount = tank.fill(stack, action);
        if (action.execute() && amount > 0) {
            node().sendToVisible("computer.signal", "tank_changed", tankIndex(), amount);
        }
        return amount;
    }

    @Override
    public FluidStack drain(FluidStack stack, FluidAction action) {
        FluidStack amount = tank.drain(stack, action);
        if (action.execute() && amount != null && amount.getAmount() > 0) {
            node().sendToVisible("computer.signal", "tank_changed", tankIndex(), -amount.getAmount());
        }
        return amount;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack amount = tank.drain(maxDrain, action);
        if (action.execute() && amount != null && amount.getAmount() > 0) {
            node().sendToVisible("computer.signal", "tank_changed", tankIndex(), -amount.getAmount());
        }
        return amount;
    }

    private int tankIndex() {
        if (owner instanceof Agent agent && agent.tank() != null) {
            for (int index = 0; index < agent.tank().tankCount(); index++) {
                if (agent.tank().getFluidTank(index) == this) return index + 1;
            }
        }
        return 1;
    }
}
