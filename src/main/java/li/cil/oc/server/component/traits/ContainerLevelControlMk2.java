package li.cil.oc.server.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.InventoryAccessJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Function;

public interface ContainerLevelControlMk2 extends ContainerAware, LevelAware, SideRestricted {
    @Override
    net.minecraft.world.entity.player.Player fakePlayer();

    @Callback(doc = "function(facing:number, slot:number[, count:number[, fromSide:number]]):boolean -- Drops the selected item stack into the specified slot of an inventory.")
    default Object[] dropIntoSlot(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        var extended = new ExtendedArgumentsJava(args);
        int count = extended.optItemCount(2, extended.optItemCount$default$2());
        Direction fromSide = extended.optSideAny(3, facing.getOpposite());
        ItemStack stack = inventory().getItem(selectedSlot());
        if (stack.isEmpty() || stack.getCount() <= 0) return ResultWrapper.result(false);
        return withInventory(position().offset(facing), fromSide, target -> {
            int slot = extended.checkSlot(target, 1);
            if (!InventoryUtils.insertIntoInventorySlot(stack, target, slot, count, false)) {
                return ResultWrapper.result(false, "inventory full/invalid slot");
            }
            if (stack.getCount() == 0) inventory().setItem(selectedSlot(), ItemStack.EMPTY);
            else inventory().setChanged();
            context.pause(Settings.get().dropDelay());
            return ResultWrapper.result(true);
        });
    }

    @Callback(doc = "function(facing:number, slot:number[, count:number[, fromSide:number]]):boolean -- Sucks items from the specified slot of an inventory.")
    default Object[] suckFromSlot(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        var extended = new ExtendedArgumentsJava(args);
        int count = extended.optItemCount(2, extended.optItemCount$default$2());
        Direction fromSide = extended.optSideAny(3, facing.getOpposite());
        return withInventory(position().offset(facing), fromSide, source -> {
            int slot = extended.checkSlot(source, 1);
            int extracted = InventoryAccessJava.extractFromInventorySlot((stack, simulate) -> {
                InventoryUtils.insertIntoInventory(stack, InventoryUtils.asItemHandler(inventory()), 64,
                    simulate, java.util.Optional.of(insertionSlots()));
            }, source, slot, count);
            if (extracted <= 0) return ResultWrapper.result(false);
            context.pause(Settings.get().suckDelay());
            return ResultWrapper.result(extracted);
        });
    }

    private Object[] withInventory(BlockPosition position, Direction side, Function<IItemHandler, Object[]> action) {
        var source = InventoryAccessJava.inventorySourceAt(position, side);
        return source.isPresent() && mayInteract(source.get())
            ? action.apply(source.get().inventory()) : ResultWrapper.result(ResultWrapper.unit, "no inventory");
    }
}
