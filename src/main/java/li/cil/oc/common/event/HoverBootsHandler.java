package li.cil.oc.common.event;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.common.item.HoverBoots;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class HoverBootsHandler {
    public static final HoverBootsHandler INSTANCE = new HoverBootsHandler();

    private HoverBootsHandler() {}

    @SubscribeEvent
    public void onLivingUpdate(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || player instanceof FakePlayer) return;
        var data = player.getPersistentData();
        String key = Settings.namespace() + "hasHoverBoots";
        boolean hadHoverBoots = data.getBoolean(key);
        boolean hasHoverBoots = false;
        if (!player.isCrouching()) {
            for (ItemStack stack : player.getInventory().armor) {
                if (stack.isEmpty() || !(stack.getItem() instanceof HoverBoots boots)) continue;
                if (Settings.get().ignorePower()) {
                    hasHoverBoots = true;
                    break;
                }
                if (player.onGround() && !player.isCreative()
                    && player.level().getGameTime() % Settings.get().tickFrequency() == 0
                    && player.getDeltaMovement().lengthSqr() > 0.015f) {
                    boots.charge(stack, -Settings.get().hoverBootMove(), false);
                }
                if (boots.getCharge(stack) > 0) {
                    hasHoverBoots = true;
                    break;
                }
            }
        }
        if (hasHoverBoots != hadHoverBoots) {
            data.putBoolean(key, hasHoverBoots);
            var stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepHeight != null) {
                var id = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "hover_boots_step");
                stepHeight.removeModifier(id);
                if (hasHoverBoots) {
                    stepHeight.addTransientModifier(new AttributeModifier(id, 0.5, AttributeModifier.Operation.ADD_VALUE));
                }
            }
        }
        if (hasHoverBoots && !player.onGround() && player.fallDistance < 5 && player.getDeltaMovement().y < 0) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0.9, 1));
        }
        if (hasHoverBoots && !Settings.get().ignorePower() && player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null) {
            for (ItemStack stack : player.getInventory().armor) {
                if (!stack.isEmpty() && stack.getItem() instanceof HoverBoots boots && boots.getCharge(stack) == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingJump(LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || player instanceof FakePlayer || player.isCrouching()) return;
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isEmpty() || !(stack.getItem() instanceof HoverBoots boots)) continue;
            double cost = -Settings.get().hoverBootJump();
            boolean creative = Settings.get().ignorePower() || player.isCreative();
            if (creative || boots.charge(stack, cost, true) == 0) {
                if (!creative) boots.charge(stack, cost, false);
                var motion = player.getDeltaMovement();
                if (player.isSprinting()) player.push(motion.x * 0.5, 0.4, motion.z * 0.5);
                else player.push(0, 0.4, 0);
            }
            break;
        }
    }

    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        if (event.getDistance() <= 3 || !(event.getEntity() instanceof Player player) || player instanceof FakePlayer) return;
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isEmpty() || !(stack.getItem() instanceof HoverBoots boots)) continue;
            double cost = -Settings.get().hoverBootAbsorb();
            boolean creative = Settings.get().ignorePower() || player.isCreative();
            if (creative || boots.charge(stack, cost, true) == 0) {
                if (!creative) boots.charge(stack, cost, false);
                event.setDistance(event.getDistance() * 0.3f);
            }
            break;
        }
    }
}
