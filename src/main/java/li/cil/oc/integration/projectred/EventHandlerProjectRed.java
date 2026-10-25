package li.cil.oc.integration.projectred;

import mrtjp.projectred.api.IScrewdriver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class EventHandlerProjectRed {
    private EventHandlerProjectRed() {}

    public static boolean useWrench(Player player, BlockPos pos, boolean changeDurability) {
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof IScrewdriver wrench) {
            if (changeDurability) wrench.damageScrewdriver(player, InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.getItem() instanceof IScrewdriver;
    }
}
