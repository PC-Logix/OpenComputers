package li.cil.oc.common.item;

import li.cil.oc.api.Nanomachines;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class Acid extends SimpleItemBase {
    private String unlocalizedName;

    public Acid(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;
        if (!level.isClientSide()) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 1200));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2000));
            Nanomachines.uninstallController(player);
        }
        stack.shrink(1);
        return stack.getCount() > 0 ? stack : ItemStack.EMPTY;
    }
}
