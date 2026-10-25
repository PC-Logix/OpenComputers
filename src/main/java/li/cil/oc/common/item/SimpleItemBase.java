package li.cil.oc.common.item;

import li.cil.oc.common.item.traits.SimpleItemBehavior;
import li.cil.oc.common.item.traits.SimpleItemJava;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.LevelReader;

import java.util.List;

/** Java-owned base for ordinary OpenComputers items. */
public class SimpleItemBase extends Item implements SimpleItemBehavior {
    private String unlocalizedName;

    public SimpleItemBase(Properties properties) {
        this(properties, null);
    }

    public SimpleItemBase(Properties properties, String name) {
        super(properties);
        this.unlocalizedName = name == null ? getClass().getSimpleName().toLowerCase() : name;
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public String getDescriptionId() {
        return "item.oc." + unlocalizedName;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return SimpleItemJava.doesSneakBypassUse(level, pos,
                () -> super.doesSneakBypassUse(stack, level, pos, player));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SimpleItemJava.appendHoverText(this, stack, context, tooltip, flag);
    }
}
