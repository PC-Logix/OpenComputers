package li.cil.oc.common;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.common.item.data.TabletData;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.StackOption;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.util.PlayerUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import java.util.Calendar;
import java.util.function.Function;

/** Handles player item lifecycle, crafting, and pickup events. */
final class PlayerItemEvents {
    private static final String receivedManualKey = Settings.namespace() + "receivedManual";

    private static final class ItemInfoHolder {
        private static final ItemInfo drone = Items.get(Constants.ItemName.Drone);
        private static final ItemInfo eeprom = Items.get(Constants.ItemName.EEPROM);
        private static final ItemInfo microcontroller = Items.get(Constants.BlockName.Microcontroller);
        private static final ItemInfo navigationUpgrade = Items.get(Constants.ItemName.NavigationUpgrade);
        private static final ItemInfo robot = Items.get(Constants.BlockName.Robot);
        private static final ItemInfo tablet = Items.get(Constants.ItemName.Tablet);
    }

    private PlayerItemEvents() {}

    static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!Settings.get().giveManualToNewPlayers() || event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player) || player instanceof FakePlayer) return;

        var persistedData = PlayerUtils.persistedData(player);
        if (!persistedData.getBoolean(receivedManualKey)) {
            persistedData.putBoolean(receivedManualKey, true);
            player.getInventory().add(Items.get(Constants.ItemName.Manual).createItemStack(1));
        }
    }

    static void onCrafting(ItemCraftedEvent event) {
        boolean didRecraft = recraft(event, ItemInfoHolder.navigationUpgrade, stack -> {
            ImmutableItemStack map = stack.get(OCComponentsJava.SOURCE_MAP_ITEM.get());
            return map == null ? StackOption.empty() : StackOption.apply(map.mutableCopy());
        });
        didRecraft = recraft(event, ItemInfoHolder.microcontroller,
                stack -> findComponent(new MicrocontrollerData(stack).components, ItemInfoHolder.eeprom, false)) || didRecraft;
        didRecraft = recraft(event, ItemInfoHolder.drone,
                stack -> findComponent(new MicrocontrollerData(stack).components, ItemInfoHolder.eeprom, false)) || didRecraft;

        boolean swapsRobotEeprom = false;
        for (int slot = 0; slot < event.getInventory().getContainerSize(); slot++) {
            if (Items.get(event.getInventory().getItem(slot)) == ItemInfoHolder.eeprom) {
                swapsRobotEeprom = true;
                break;
            }
        }
        if (swapsRobotEeprom) {
            didRecraft = recraft(event, ItemInfoHolder.robot,
                    stack -> findComponent(new RobotData(stack).components, ItemInfoHolder.eeprom, false)) || didRecraft;
        }

        didRecraft = recraft(event, ItemInfoHolder.tablet,
                stack -> findComponent(new TabletData(stack).items, ItemInfoHolder.eeprom, true)) || didRecraft;

        Player player = event.getEntity();
        if (!(player instanceof FakePlayer) && player instanceof ServerPlayer serverPlayer
                && serverPlayer.level() != null && !serverPlayer.level().isClientSide()
                && Settings.get().presentChance() > 0 && !didRecraft
                && Items.get(event.getCrafting()) != null
                && player.getRandom().nextFloat() < Settings.get().presentChance()
                && timeForPresents()) {
            ItemStack present = Items.get(Constants.ItemName.Present).createItemStack(1);
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 0.2f, 1f);
            InventoryUtils.addToPlayerInventory(present, player);
        }

        Advancement.onCraft(event.getCrafting(), player);
    }

    private static StackOption findComponent(ItemStack[] components, ItemInfo item, boolean skipEmpty) {
        for (ItemStack component : components) {
            if ((!skipEmpty || !component.isEmpty()) && Items.get(component) == item) return StackOption.apply(component);
        }
        return StackOption.empty();
    }

    private static boolean recraft(ItemCraftedEvent event, ItemInfo item, Function<ItemStack, StackOption> callback) {
        if (Items.get(event.getCrafting()) != item) return false;
        for (int slot = 0; slot < event.getInventory().getContainerSize(); slot++) {
            ItemStack stack = event.getInventory().getItem(slot);
            if (Items.get(stack) == item) {
                StackOption extra = callback.apply(stack);
                if (!extra.isEmpty()) InventoryUtils.addToPlayerInventory(extra.get(), event.getEntity());
            }
        }
        return true;
    }

    private static boolean timeForPresents() {
        Calendar now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH);
        int dayOfMonth = now.get(Calendar.DAY_OF_MONTH);
        return (month == Calendar.DECEMBER && dayOfMonth > 24)
                || (month == Calendar.JANUARY && dayOfMonth < 7)
                || (month == Calendar.FEBRUARY && dayOfMonth == 14)
                || (month == Calendar.APRIL && dayOfMonth == 22)
                || (month == Calendar.MAY && dayOfMonth == 1)
                || (month == Calendar.OCTOBER && dayOfMonth == 3)
                || (month == Calendar.DECEMBER && dayOfMonth == 14);
    }

    static void onItemEntityPickup(ItemEntityPickupEvent.Post event) {
        var entity = event.getItemEntity();
        if (entity == null || entity.getItem() == null) return;
        var stack = entity.getItem();
        Advancement.onAssemble(stack, event.getPlayer());
        Advancement.onCraft(stack, event.getPlayer());
    }
}
