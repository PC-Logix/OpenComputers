package li.cil.oc.common.item.traits;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Localization;
import li.cil.oc.client.gui.Drive;
import li.cil.oc.common.item.data.DriveData;
import li.cil.oc.util.ItemUtils;
import li.cil.oc.util.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public final class FileSystemLikeJava {
    private FileSystemLikeJava() {}

    public static void appendHoverText(FileSystemLike item, ItemStack stack, TooltipContext context,
                                       List<Component> tooltip, TooltipFlag flag) {
        SimpleItemBehavior simpleItem = (SimpleItemBehavior) item;
        String label = stack.get(OCComponentsJava.LABEL.get());
        if (label != null) tooltip.add(Component.literal(label).setStyle(Tooltip.DefaultStyle));

        if (flag.isAdvanced()) {
            var fsNbt = stack.get(OCComponentsJava.FILESYSTEM_DATA.get());
            if (fsNbt != null && fsNbt.contains("capacity.used")) {
                long used = fsNbt.getLong("capacity.used");
                tooltip.add(Component.literal(Localization.Tooltip.DiskUsage(used, item.kiloBytes() * 1024))
                        .setStyle(Tooltip.DefaultStyle));
            }
        }

        DriveData data = new DriveData(stack);
        tooltip.add(Component.literal(Localization.Tooltip.DiskMode(data.isUnmanaged())).setStyle(Tooltip.DefaultStyle));
        if (data.isLocked()) tooltip.add(Component.literal(Localization.Tooltip.DiskLock(data.lockInfo())).setStyle(Tooltip.DefaultStyle));

        SimpleItemJava.appendHoverText(simpleItem, stack, context, tooltip, flag);
    }

    public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        var tag = ItemUtils.getTag(stack);
        if (!player.isCrouching() && (tag == null || !stack.has(OCComponentsJava.LOOT_DISK.get()))) {
            if (level.isClientSide()) showGui(stack, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private static void showGui(ItemStack stack, Player player) {
        Minecraft.getInstance().pushGuiLayer(new Drive(player.getInventory(), () -> stack));
    }
}
