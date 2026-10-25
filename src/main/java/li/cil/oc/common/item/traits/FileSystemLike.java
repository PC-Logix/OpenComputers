package li.cil.oc.common.item.traits;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Shared item behavior for storage media. */
public interface FileSystemLike extends li.cil.oc.api.driver.item.UpgradeRenderer,
        net.neoforged.neoforge.common.extensions.IItemExtension {
    int kiloBytes();

    default void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FileSystemLikeJava.appendHoverText(this, stack, context, tooltip, flag);
    }

    default InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return FileSystemLikeJava.use(level, player, hand);
    }
}
