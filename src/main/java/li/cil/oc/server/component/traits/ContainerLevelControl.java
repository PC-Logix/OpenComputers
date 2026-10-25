package li.cil.oc.server.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.InventoryAccessJava;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

import java.util.List;

public interface ContainerLevelControl extends ContainerAware, LevelAware, SideRestricted {
    @Override
    net.minecraft.world.entity.player.Player fakePlayer();

    @Callback(doc = "function(side:number[, fuzzy:boolean=false]):boolean -- Compare the block on the specified side with the one in the selected slot. Returns true if equal.")
    default Object[] compare(Context context, Arguments args) {
        Direction side = checkSideForAction(args, 0);
        ItemStack stack = inventory().getItem(selectedSlot());
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return ResultWrapper.result(false);
        var state = world().getBlockState(position().offset(side).toBlockPos());
        boolean matches = item.getBlock() == state.getBlock();
        if (args.optBoolean(1, false)) return ResultWrapper.result(matches);
        var itemState = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
            .apply(item.getBlock().defaultBlockState());
        return ResultWrapper.result(matches && itemState.equals(state));
    }

    @Callback(doc = "function(side:number[, count:number=64]):boolean -- Drops items from the selected slot towards the specified side.")
    default Object[] drop(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        var extended = new ExtendedArgumentsJava(args);
        int count = extended.optItemCount(1, extended.optItemCount$default$2());
        ItemStack stack = inventory().getItem(selectedSlot());
        if (stack.isEmpty() || stack.getCount() <= 0) return ResultWrapper.result(false);

        var source = InventoryAccessJava.inventorySourceAt(position().offset(facing), facing.getOpposite());
        if (source.isPresent() && mayInteract(source.get())) {
            if (!InventoryUtils.insertIntoInventory(stack, source.get().inventory(), count, false)) {
                return ResultWrapper.result(false, "inventory full");
            }
            if (stack.getCount() == 0) inventory().setItem(selectedSlot(), ItemStack.EMPTY);
            else inventory().setChanged();
        }
        else {
            ItemStack dropped = inventory().removeItem(selectedSlot(), count);
            if (!dropped.isEmpty()) {
                java.util.function.Predicate<ItemEntity> validator = item -> {
                    var event = new ItemTossEvent(item, fakePlayer());
                    NeoForge.EVENT_BUS.post(event);
                    return !event.isCanceled();
                };
                if (InventoryUtils.spawnStackInWorld(position(), dropped, java.util.Optional.of(facing), java.util.Optional.of(validator)) == null) {
                    fakePlayer().getInventory().add(dropped);
                }
            }
        }
        context.pause(Settings.get().dropDelay());
        return ResultWrapper.result(true);
    }

    default int suckFromItems(Direction facing) {
        for (ItemEntity entity : suckableItems(facing)) {
            if (!entity.isAlive() || entity.hasPickUpDelay()) continue;
            ItemStack stack = entity.getItem();
            int size = stack.getCount();
            onSuckCollect(entity);
            if (stack.getCount() < size) return size - stack.getCount();
            if (!entity.isAlive()) return size;
        }
        return 0;
    }

    @Callback(doc = "function(side:number[, count:number=64]):boolean -- Suck up items from the specified side.")
    default Object[] suck(Context context, Arguments args) {
        Direction facing = checkSideForAction(args, 0);
        var extended = new ExtendedArgumentsJava(args);
        int count = extended.optItemCount(1, extended.optItemCount$default$2());
        var source = InventoryAccessJava.inventorySourceAt(position().offset(facing), facing.getOpposite());
        int extracted = 0;
        if (source.isPresent()) {
            mayInteract(source.get());
            extracted = InventoryAccessJava.extractAnyFromInventory((stack, simulate) -> {
                InventoryUtils.insertIntoInventory(stack, InventoryUtils.asItemHandler(inventory()), 64,
                    simulate, java.util.Optional.of(insertionSlots()));
            }, source.get().inventory(), count);
        }
        if (extracted <= 0) extracted = suckFromItems(facing);
        if (extracted <= 0) return ResultWrapper.result(false);
        context.pause(Settings.get().suckDelay());
        return ResultWrapper.result(extracted);
    }

    default List<ItemEntity> suckableItems(Direction side) {
        return entitiesOnSide(ItemEntity.class, side);
    }

    default void onSuckCollect(ItemEntity entity) {
        entity.playerTouch(fakePlayer());
    }
}
