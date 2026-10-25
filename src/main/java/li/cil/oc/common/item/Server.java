package li.cil.oc.common.item;

import li.cil.oc.common.container.ServerInventory;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Server extends SimpleItemBase {
    private final int tier;
    private String unlocalizedName;

    public Server(final Properties properties, final int tier) {
        super(properties);
        this.tier = tier;
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    public int tier() { return tier; }
    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(final String value) { unlocalizedName = value; }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }

    @Override
    public void tooltipExtended(final ItemStack stack, final List<Component> tooltip, final TooltipFlag flag) {
        if (!Tooltip.showExtendedTooltip(flag)) return;

        final ServerInventory inventory = ServerInventoryHelper.tooltipInventory(stack);
        final Map<String, Integer> counts = new TreeMap<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack item = inventory.getItem(slot);
            if (!item.isEmpty()) counts.merge(item.getHoverName().getString(), 1, Integer::sum);
        }

        if (!counts.isEmpty()) {
            Tooltip.add(tooltip, flag, "server.Components");
            for (final var entry : counts.entrySet()) {
                tooltip.add(Component.literal("- " + entry.getValue() + "x " + entry.getKey()).setStyle(Tooltip.DefaultStyle));
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!player.isCrouching() && !level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            final ServerInventory inventory = ServerInventoryHelper.guiInventory(stack, serverPlayer);
            MenuTypes.openServerGui(serverPlayer, inventory, -1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
