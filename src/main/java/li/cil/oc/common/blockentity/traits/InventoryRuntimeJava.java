package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

/** Owns lazy inventory storage and interaction-distance checks for inventory block entities. */
public final class InventoryRuntimeJava {
    private ItemStack[] items;

    public synchronized ItemStack[] items(int size, ItemStack emptyValue) {
        if (items == null) {
            items = new ItemStack[size];
            Arrays.fill(items, emptyValue);
        }
        return items;
    }

    public static boolean stillValid(Vec3 playerPosition, boolean moving, Vec3 movingPosition, BlockPos blockPosition) {
        Vec3 target = moving ? movingPosition : Vec3.atCenterOf(blockPosition);
        return playerPosition.distanceToSqr(target) <= 64;
    }
}
