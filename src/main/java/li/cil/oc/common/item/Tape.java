package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.util.ItemUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.extensions.IItemExtension;

import java.util.List;

/** A cassette stores arbitrary bytes; the tape drive interprets them as DFPWM when played. */
public class Tape extends Item implements IItemExtension {
    private static final int BYTES_PER_SECOND = 6000;
    private final int capacity;

    public Tape(Properties properties, int capacity) { super(properties); this.capacity = capacity; }
    public int capacity() { return capacity; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        var tag = ItemUtils.getTag(stack);
        if (tag != null && tag.contains(Settings.namespace() + "tapeLabel")) {
            var label = tag.getString(Settings.namespace() + "tapeLabel");
            if (!label.isEmpty()) tooltip.add(Component.literal(label).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));
        }
        tooltip.add(Component.translatable(Settings.namespace() + "tooltip.tape_length", capacity / (BYTES_PER_SECOND * 60)).withStyle(ChatFormatting.GRAY));
    }
}
