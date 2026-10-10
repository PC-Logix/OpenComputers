package li.cil.oc.common.blockentity.traits;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface PlayerInputAware extends Container {
    void onSetInventorySlotContents(Player player, int slot, ItemStack stack);
}
