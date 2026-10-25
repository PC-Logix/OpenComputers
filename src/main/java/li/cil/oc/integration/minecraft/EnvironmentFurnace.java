package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public final class EnvironmentFurnace extends ManagedBlockEntityEnvironment<AbstractFurnaceBlockEntity> implements NamedBlock {
    private final String name;

    public EnvironmentFurnace(AbstractFurnaceBlockEntity blockEntity, String name) {
        super(blockEntity, name);
        this.name = name;
    }

    @Override
    public String preferredName() { return name; }

    @Override
    public int priority() { return 0; }

    @Callback(doc = "function():number -- The number of ticks that the furnace will keep burning from the last consumed fuel.")
    public Object[] getBurnTime(Context context, Arguments args) {
        return ResultWrapper.result(blockEntity.litTime);
    }

    @Callback(doc = "function():number -- The number of ticks that the currently burning fuel lasts in total.")
    public Object[] getCurrentItemBurnTime(Context context, Arguments args) {
        return ResultWrapper.result(blockEntity.litDuration);
    }

    @Callback(doc = "function():number -- The number of ticks that the current item has been cooking for.")
    public Object[] getCookTime(Context context, Arguments args) {
        return ResultWrapper.result(blockEntity.cookingProgress);
    }

    @Callback(doc = "function():number -- The number of ticks that the current item needs to cook.")
    public Object[] getTotalCookTime(Context context, Arguments args) {
        return ResultWrapper.result(blockEntity.cookingTotalTime);
    }

    @Callback(doc = "function():boolean -- Get whether the furnace is currently active.")
    public Object[] isBurning(Context context, Arguments args) {
        return ResultWrapper.result(blockEntity.litTime > 0);
    }
}
