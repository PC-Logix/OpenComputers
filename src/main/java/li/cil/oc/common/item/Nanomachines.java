package li.cil.oc.common.item;

import com.google.common.base.Strings;
import li.cil.oc.common.item.data.NanomachineData;
import li.cil.oc.common.nanomachines.ControllerImpl;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

import java.util.List;

public final class Nanomachines extends SimpleItemBase {
    private String unlocalizedName;

    public Nanomachines(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            NanomachineData data = new NanomachineData(stack);
            if (!Strings.isNullOrEmpty(data.uuid)) {
                tooltip.add(Component.literal("§8" + data.uuid.substring(0, 13) + "...§7"));
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.EAT; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;
        if (!level.isClientSide()) {
            NanomachineData data = new NanomachineData(stack);
            li.cil.oc.api.Nanomachines.uninstallController(player);
            var controller = li.cil.oc.api.Nanomachines.installController(player);
            if (controller instanceof ControllerImpl implementation) {
                if (data.configuration.isDefined()) {
                    if (!Strings.isNullOrEmpty(data.uuid)) implementation.uuid_$eq(data.uuid);
                    implementation.configuration().loadData(data.configuration.get());
                } else {
                    implementation.reconfigure();
                }
            } else {
                controller.reconfigure();
            }
        }
        stack.shrink(1);
        return stack.getCount() > 0 ? stack : ItemStack.EMPTY;
    }
}
