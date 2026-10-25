package li.cil.oc.common.item.traits;

import li.cil.oc.common.item.traits.ComponentItemHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/** Java implementation of the double-sneak component identity reset gesture. */
public final class ComponentItemJava {
    private ComponentItemJava() { }

    public static InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand,
                                                          Supplier<InteractionResultHolder<ItemStack>> fallback) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return fallback.get();

        ItemStack resultStack = stack;
        if (!world.isClientSide()) {
            if (ComponentItemHooks.registerSneakClick(player, hand, stack.getItem(), world.getGameTime())) {
                resultStack = new ItemStack(stack.getItem(), stack.getCount());
                player.setItemInHand(hand, resultStack);
                player.displayClientMessage(Component.literal("Item reset."), true);
            } else {
                player.displayClientMessage(Component.literal("Double click quickly to reset"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(resultStack, world.isClientSide());
    }
}
