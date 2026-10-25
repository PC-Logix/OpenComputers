package li.cil.oc.server.agent;

import li.cil.oc.Settings;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.util.BlockPositionsJava;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import scala.Function1;

import java.util.ArrayList;
import java.util.List;

/** Handles the item inventory transaction performed while an agent acts as a fake player. */
public final class PlayerInventoryActions {
    private PlayerInventoryActions() {}

    public static <T> T callUsingItemInSlot(Player player, Agent agent, Container inventory, int slot,
                                             Function1<ItemStack, T> action, boolean repair) {
        List<ItemEntity> itemsBefore = adjacentItems(player, agent);
        ItemStack stack = inventory.getItem(slot);
        ItemStack oldStack = stack.copy();
        net.minecraft.world.entity.player.Inventory fakeInventory = player.getInventory();
        fakeInventory.selected = inventory == agent.mainInventory() ? slot : ~slot;
        fakeInventory.offhand.set(0, inventory.getItem(slot));
        try {
            return action.apply(stack);
        } finally {
            fakeInventory.selected = 0;
            inventory.setItem(slot, fakeInventory.offhand.get(0));
            fakeInventory.offhand.set(0, ItemStack.EMPTY);
            ItemStack newStack = inventory.getItem(slot);
            // This is only possible if the action modified the stack object in-place (e.g. IC2).
            if (ItemStack.matches(oldStack, newStack) && !ItemStack.matches(oldStack, stack)) {
                inventory.setItem(slot, stack);
            }
            if (!newStack.isEmpty()) {
                if (newStack.getCount() <= 0) inventory.setItem(slot, ItemStack.EMPTY);
                if (repair) {
                    if (newStack.getCount() > 0) tryRepair(agent, newStack, oldStack);
                    else inventory.setItem(slot, ItemStack.EMPTY);
                }
            }
            collectDroppedItems(player, agent, itemsBefore);
        }
    }

    private static List<ItemEntity> adjacentItems(Player player, Agent agent) {
        return player.level().getEntitiesOfClass(ItemEntity.class,
                BlockPositionsJava.apply(agent).bounds().inflate(2, 2, 2), item -> true);
    }

    private static void collectDroppedItems(Player player, Agent agent, List<ItemEntity> itemsBefore) {
        List<ItemEntity> itemsDropped = new ArrayList<>(adjacentItems(player, agent));
        itemsDropped.removeAll(itemsBefore);
        for (ItemEntity drop : itemsDropped) {
            drop.setDefaultPickUpDelay();
            drop.playerTouch(player);
        }
    }

    private static void tryRepair(Agent agent, ItemStack stack, ItemStack oldStack) {
        // Only if the underlying type didn't change.
        if (!stack.isEmpty() && !oldStack.isEmpty() && stack.getItem() == oldStack.getItem()) {
            RobotUsedToolEvent.ComputeDamageRate damageRate = new RobotUsedToolEvent.ComputeDamageRate(
                    agent, oldStack, stack, Settings.get().itemDamageRate());
            NeoForge.EVENT_BUS.post(damageRate);
            if (damageRate.getDamageRate() < 1) {
                NeoForge.EVENT_BUS.post(new RobotUsedToolEvent.ApplyDamageRate(
                        agent, oldStack, stack, damageRate.getDamageRate()));
            }
        }
    }
}