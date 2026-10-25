package li.cil.oc.common.item;

import li.cil.oc.common.item.traits.ComponentItemJava;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Java-owned base for component items with the identity-reset gesture. */
public class ComponentItemBase extends SimpleItemBase {
    public ComponentItemBase(Properties properties) {
        super(properties);
    }

    public ComponentItemBase(Properties properties, String name) {
        super(properties, name);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        return ComponentItemJava.use(world, player, hand, () -> super.use(world, player, hand));
    }
}
