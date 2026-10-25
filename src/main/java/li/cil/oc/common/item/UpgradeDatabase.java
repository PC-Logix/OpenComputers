package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.container.DatabaseInventory;
import li.cil.oc.common.container.ItemStackInventory;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import scala.collection.immutable.Seq;
import scala.jdk.javaapi.CollectionConverters;

import java.util.List;

public final class UpgradeDatabase extends SimpleItemBase {
    private final int tier;
    private String unlocalizedName;

    public UpgradeDatabase(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    public int tier() { return tier; }
    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }

    @Override
    public Seq<Object> tooltipData() {
        return CollectionConverters.asScala(List.<Object>of(Settings.get().databaseEntriesPerTier()[tier])).toSeq();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable(Settings.namespace() + "tooltip.tier", super.tierFromDriver(stack) + 1)
                .setStyle(Tooltip.DefaultStyle));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isCrouching()) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                MenuTypes.openDatabaseGui(serverPlayer, new DatabaseInventory() {
                    private final ItemStack[] items = ItemStackInventory.newItems(this);
                    @Override public ItemStack[] items() { return items; }
                    @Override public ItemStack container() { return stack; }
                    @Override public boolean stillValid(Player candidate) { return candidate == serverPlayer; }
                    { DatabaseInventory.initialize(this); }
                });
            }
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                data -> data.remove(Settings.namespace() + "items"));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
