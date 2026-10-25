package li.cil.oc.common.item;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Settings;
import li.cil.oc.common.item.traits.FileSystemLike;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

public class FloppyDisk extends SimpleItemBase implements FileSystemLike{
    private final int kiloBytes;
    private String unlocalizedName = "floppydisk";

    public FloppyDisk(Properties properties) {
        super(properties);
        kiloBytes = Settings.get().floppySize();
    }

    @Override public int kiloBytes() { return kiloBytes; }
    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    public void li$cil$oc$common$item$traits$FileSystemLike$$super$appendHoverText(
            ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        Tooltip.add(tooltip, flag, getClass().getSimpleName().toLowerCase());
        String address = stack.get(OCComponentsJava.ADDRESS.get());
        if (address != null) {
            String shortened = address.length() > 13 ? address.substring(0, 13) + "..." : address;
            tooltip.add(Component.literal("§8" + shortened + "§7"));
        }
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        FileSystemLike.super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return FileSystemLike.super.use(level, player, hand);
    }
}
