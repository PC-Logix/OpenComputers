package li.cil.oc.server.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.FluidUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.function.BiFunction;

public interface TankContainerControl extends LevelAware, ContainerAware, TankAware {
    @Override
    net.minecraft.world.entity.player.Player fakePlayer();

    @Callback(doc = "function([slot:number]):number -- Get the amount of fluid in the tank item in the specified slot or the selected slot.")
    default Object[] getTankLevelInSlot(Context context, Arguments args) {
        return withFluidInfo(optSlot(args, 0), (fluid, capacity) -> ResultWrapper.result(fluid == null ? 0 : fluid.getAmount()));
    }

    @Callback(doc = "function([slot:number]):number -- Get the capacity of the tank item in the specified slot of the robot or the selected slot.")
    default Object[] getTankCapacityInSlot(Context context, Arguments args) {
        return withFluidInfo(optSlot(args, 0), (fluid, capacity) -> ResultWrapper.result(capacity));
    }

    @Callback(doc = "function([slot:number]):table -- Get a description of the fluid in the tank item in the specified slot or the selected slot.")
    default Object[] getFluidInTankInSlot(Context context, Arguments args) {
        if (!Settings.get().allowItemStackInspection()) return ResultWrapper.result(ResultWrapper.unit, "not enabled in config");
        return withFluidInfo(optSlot(args, 0), (fluid, capacity) -> ResultWrapper.result(fluid));
    }

    @Callback(doc = "function([tank:number]):table -- Get a description of the fluid in the tank in the specified slot or the selected slot.")
    default Object[] getFluidInInternalTank(Context context, Arguments args) {
        if (!Settings.get().allowItemStackInspection()) return ResultWrapper.result(ResultWrapper.unit, "not enabled in config");
        var selected = tank().getFluidTank(optTank(args, 0));
        return ResultWrapper.result(selected == null ? null : selected.getFluid());
    }

    @Callback(doc = "function([amount:number]):boolean -- Transfers fluid from a tank in the selected inventory slot to the selected tank.")
    default Object[] drain(Context context, Arguments args) {
        int amount = new ExtendedArgumentsJava(args).optFluidCount(0,
            new ExtendedArgumentsJava(args).optFluidCount$default$2());
        var into = tank().getFluidTank(selectedTank());
        if (into == null) return ResultWrapper.result(ResultWrapper.unit, "no tank");
        ItemStack stack = inventory().getItem(selectedSlot());
        if (stack == null) return ResultWrapper.result(ResultWrapper.unit, "nothing selected");
        IFluidHandlerItem handler = FluidUtils.fluidHandlerOf(stack);
        if (handler == null) return ResultWrapper.result(ResultWrapper.unit, "item is not a fluid container");
        FluidStack drained = handler.drain(amount, FluidAction.SIMULATE);
        int transferred = into.fill(drained, FluidAction.EXECUTE);
        if (transferred <= 0) return ResultWrapper.result(ResultWrapper.unit, "incompatible or no fluid");
        handler.drain(transferred, FluidAction.EXECUTE);
        inventory().setItem(selectedSlot(), handler.getContainer());
        return ResultWrapper.result(true, transferred);
    }

    @Callback(doc = "function([amount:number]):boolean -- Transfers fluid from the selected tank to a tank in the selected inventory slot.")
    default Object[] fill(Context context, Arguments args) {
        var extended = new ExtendedArgumentsJava(args);
        int amount = extended.optFluidCount(0, extended.optFluidCount$default$2());
        var from = tank().getFluidTank(selectedTank());
        if (from == null) return ResultWrapper.result(ResultWrapper.unit, "no tank");
        ItemStack stack = inventory().getItem(selectedSlot());
        if (stack == null) return ResultWrapper.result(ResultWrapper.unit, "nothing selected");
        IFluidHandlerItem handler = FluidUtils.fluidHandlerOf(stack);
        if (handler == null) return ResultWrapper.result(ResultWrapper.unit, "item is not a fluid container");
        FluidStack drained = from.drain(amount, FluidAction.SIMULATE);
        int transferred = handler.fill(drained, FluidAction.EXECUTE);
        if (transferred <= 0) return ResultWrapper.result(ResultWrapper.unit, "incompatible or no fluid");
        from.drain(transferred, FluidAction.EXECUTE);
        inventory().setItem(selectedSlot(), handler.getContainer());
        return ResultWrapper.result(true, transferred);
    }

    private Object[] withFluidInfo(int slot, BiFunction<FluidStack, Integer, Object[]> action) {
        IFluidHandlerItem handler = FluidUtils.fluidHandlerOf(inventory().getItem(slot));
        if (handler == null || handler.getTanks() == 0) {
            return ResultWrapper.result(ResultWrapper.unit, "item is not a fluid container");
        }
        return action.apply(handler.getFluidInTank(0), handler.getTankCapacity(0));
    }
}
