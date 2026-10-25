package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Set;

public class UpgradeCrafting extends AbstractManagedEnvironment implements DeviceInfo {
    private final Robot host;
    private final Node node;
    private final CraftingContainer craftingContainer;

    public UpgradeCrafting(Robot host) {
        this.host = host;
        this.node = Network.newNode(this, Visibility.Network).withComponent("crafting").create();
        this.craftingContainer = new CraftingContainer();
    }

    public Robot host() { return host; }
    @Override public Node node() { return node; }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceAttribute.Class, DeviceClass.Generic.toString(),
                DeviceAttribute.Description, "Assembly controller",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "MultiCombinator-9S");
    }

    @Callback(doc = "function([count:number]):number -- Tries to craft the specified number of items in the top left area of the inventory.")
    public Object[] craft(Context context, Arguments args) {
        int count = Math.max(0, Math.min(64, args.optInteger(0, 64)));
        var result = craftingContainer.craft(count);
        return ResultWrapper.result(result[0], result[1]);
    }

    private final class CraftingContainer extends TransientCraftingContainer {
        private CraftingContainer() {
            super(new AbstractContainerMenu(null, 0) {
                @Override public boolean stillValid(Player player) { return true; }
                @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            }, 3, 3);
        }

        private Object[] craft(int wantedCount) {
            Player player = host.player();
            copyItemsFromHost(player.getInventory());
            int countCrafted = 0;
            List<ItemStack> craftedStacks = new ArrayList<>();
            var level = host.getEnvironmentLevel();
            var manager = level.getRecipeManager();
            var initialCraft = manager.getRecipeFor(RecipeType.CRAFTING, asCraftInput(), level);
            if (initialCraft.isPresent()) {
                while (countCrafted < wantedCount) {
                    var craft = manager.getRecipeFor(RecipeType.CRAFTING, asCraftInput(), level);
                    if (!craft.equals(initialCraft)) break;

                    ResultContainer craftResult = new ResultContainer();
                    ResultSlot craftingSlot = new ResultSlot(player, this, craftResult, 0, 0, 0);
                    ItemStack craftedResult = craft.get().value().assemble(asCraftInput(), player.registryAccess());
                    craftResult.setItem(0, craftedResult);
                    if (!craftingSlot.hasItem()) break;

                    ItemStack stack = craftingSlot.remove(1);
                    if (stack.isEmpty()) break;
                    countCrafted += stack.getCount();
                    craftingSlot.onTake(player, stack);
                    copyItemsToHost(player.getInventory());
                    copyItemsFromHost(player.getInventory());
                    craftedStacks.add(stack);
                }
            }

            Set<Integer> craftingSlots = new HashSet<>();
            for (int slot = 0; slot < getContainerSize(); slot++) craftingSlots.add(toParentSlot(slot));
            List<Integer> outputSlots = new ArrayList<>();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (!craftingSlots.contains(slot)) outputSlots.add(slot);
            }
            for (ItemStack stack : craftedStacks) addCraftedStack(stack, player, outputSlots);
            return new Object[]{countCrafted > 0, countCrafted};
        }

        private void addCraftedStack(ItemStack stack, Player player, List<Integer> outputSlots) {
            if (stack.isEmpty()) return;
            Container inventory = player.getInventory();
            IItemHandler itemHandler = InventoryUtils.asItemHandler(inventory);
            InventoryUtils.insertIntoInventory(stack, itemHandler, 64, false, java.util.Optional.of(outputSlots));
            if (stack.getCount() > 0) player.drop(stack, false, false);
            inventory.setChanged();
            if (player.containerMenu != null) player.containerMenu.broadcastChanges();
        }

        private void copyItemsFromHost(Container inventory) {
            for (int slot = 0; slot < getContainerSize(); slot++) setItem(slot, inventory.getItem(toParentSlot(slot)));
        }

        private void copyItemsToHost(Container inventory) {
            for (int slot = 0; slot < getContainerSize(); slot++) inventory.setItem(toParentSlot(slot), getItem(slot));
        }

        private int toParentSlot(int slot) {
            int col = slot % 3;
            int row = slot / 3;
            return row * 4 + col;
        }
    }
}
