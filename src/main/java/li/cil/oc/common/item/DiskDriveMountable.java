package li.cil.oc.common.item;

import li.cil.oc.common.container.DiskDriveMountableInventory;
import li.cil.oc.common.menu.DiskDrive;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DiskDriveMountable extends SimpleItemBase {
    private String unlocalizedName;

    public DiskDriveMountable(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            var container = new DiskDriveMountableInventory(player, hand);
            player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new DiskDrive(id, inventory, container), container.getDisplayName()));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
