package li.cil.oc.common.item;

import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Node;
import li.cil.oc.client.gui.Screen;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.common.item.data.TabletData;
import li.cil.oc.common.item.traits.Chargeable;
import li.cil.oc.common.item.traits.ChargeableUtils;
import li.cil.oc.util.Rarity;
import li.cil.oc.util.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.List;
import scala.Tuple5;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;

/** Shared Java behavior for the tablet item; Scala retains its trait super-dispatch hooks. */
public class TabletItemBase extends SimpleItemBase implements Chargeable{
    public static final int TIME_TO_ANALYZE = 10;

    public TabletItemBase(Properties properties) {
        super(properties);
    }

    protected void appendTabletTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        if (Tooltip.showExtendedTooltip(flag)) {
            TabletData info = new TabletData(stack);
            if (info.items.length - 1 > 1) {
                Tooltip.add(tooltip, flag, "server.Components");
                for (int index = 1; index < info.items.length; index++) {
                    ItemStack component = info.items[index];
                    if (!component.isEmpty()) {
                        tooltip.add(Component.literal("- " + component.getHoverName().getString()).setStyle(Tooltip.DefaultStyle));
                    }
                }
            }
        }
    }

    public void updateTabletRarityAfterLoad(ItemStack stack) {
        stack.set(DataComponents.RARITY, Rarity.byTier(new TabletData(stack).tier));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) return 13;
        Optional<TabletWrapper> weak = TabletCache.getClientWeakOptional(stack);
        TabletData data = weak.isPresent() ? weak.get().data() : new TabletData(stack);
        double ratio = data.energy / data.maxEnergy;
        return (int) Math.round(ratio * 13.0f);
    }

    public boolean canCharge(ItemStack stack) { return true; }

    public double charge(ItemStack stack, double amount, boolean simulate) {
        if (amount < 0) return amount;
        TabletData data = new TabletData(stack);
        return ChargeableUtils.applyCharge(amount, data.energy, data.maxEnergy, used -> {
            if (!simulate) {
                data.energy += used;
                data.saveData(stack);
            }
        });
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof Player player) {
            if (level.isClientSide && player.getUseItemRemainingTicks() == TIME_TO_ANALYZE &&
                li.cil.oc.api.Items.get(player.getUseItem()) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.Tablet())) {
                li.cil.oc.util.Audio.play((float) player.getX(), (float) player.getY() + 2, (float) player.getZ(), ".", 1000, 200);
            }
            TabletCache.get(stack, player).update(level, player, slot, selected);
        }
    }

    @Deprecated
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int duration) {
        if (!(entity instanceof Player player)) return;
        boolean didAnalyze = getUseDuration(stack, entity) - duration >= TIME_TO_ANALYZE;
        if (didAnalyze) {
            if (!level.isClientSide) {
                Optional<?> maybeAnalysis = TabletEvents.currentlyAnalyzingOptional();
                if (maybeAnalysis.isPresent()) {
                    try {
                        Tuple5<?, ?, ?, ?, ?> analysis = (Tuple5<?, ?, ?, ?, ?>) maybeAnalysis.get();
                        Machine computer = TabletCache.get(stack, player).machine();
                        if (computer.isRunning()) {
                            CompoundTag data = new CompoundTag();
                            Node node = computer.node();
                            node.sendToReachable("tablet.use", data, stack, player, analysis._1(), analysis._2(),
                                analysis._3(), analysis._4(), analysis._5());
                            if (!data.isEmpty()) computer.signal("tablet_use", data);
                        }
                    }
                    catch (Throwable error) {
                        OpenComputers.log().warn("Block analysis on tablet right click failed gloriously!", error);
                    }
                }
            }
            return;
        }

        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                MenuTypes.openTabletGui(serverPlayer, TabletCache.get(stack, player));
            }
            return;
        }

        if (!level.isClientSide) {
            TabletWrapper tablet = TabletCache.get(stack, player);
            Machine computer = tablet.machine();
            computer.start();
            tablet.syncRunningState();
            String message = computer.lastError();
            if (message != null) player.sendSystemMessage(Localization.Analyzer.LastError(message));
        }
        else {
            for (Optional<li.cil.oc.api.network.ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(TabletCache.get(stack, player))) {
                if (component.isPresent() && component.get() instanceof TextBuffer buffer) {
                    showGui(buffer);
                    break;
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void showGui(TextBuffer buffer) {
        Minecraft.getInstance().pushGuiLayer(new Screen(buffer, true, () -> true, buffer::isRenderingEnabled));
    }

    @Override
    public double maxCharge(ItemStack stack) { return new TabletData(stack).maxEnergy; }

    @Override
    public double getCharge(ItemStack stack) { return new TabletData(stack).energy; }

    @Override
    public void setCharge(ItemStack stack, double amount) {
        TabletData data = new TabletData(stack);
        data.energy = Math.min(Math.max(amount, 0.0), maxCharge(stack));
        data.saveData(stack);
    }
}
