package li.cil.oc.server.component;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.prefab.AbstractValue;

import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.items.IItemHandler;

public class Trade extends AbstractValue {
    private final TradeInfo info;

    public Trade(TradeInfo info) {
        this.info = info;
    }

    public Trade() {
        this(new TradeInfo());
    }

    public Trade(UpgradeTrading upgrade, Merchant merchant, int recipeID, int merchantID) {
        this(new TradeInfo(upgrade.host(), merchant, recipeID, merchantID));
    }

    public double maxRange() {
        return Settings.get().tradingRange();
    }

    public boolean isInRange() {
        Merchant merchant = info.merchant();
        EnvironmentHost host = info.host();
        if (merchant instanceof Entity entity && host != null) {
            return entity.distanceToSqr(host.xPosition(), host.yPosition(), host.zPosition()) < maxRange() * maxRange();
        }
        return false;
    }

    @Override
    public void loadData(DataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        // Loading needs a world, which is not available at this point in the load lifecycle.
        li.cil.oc.common.EventHandler.scheduleServer(() -> {
            info.loadData(nbt);
            return scala.runtime.BoxedUnit.UNIT;
        });
    }

    @Override
    public void saveData(MutableDataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        info.saveData(nbt);
    }

    @Callback(doc = "function():number -- Returns a sort index of the merchant that provides this trade")
    public Object[] getMerchantId(Context context, Arguments arguments) {
        return ResultWrapper.result(info.merchantID());
    }

    @Callback(doc = "function():table, table -- Returns the items the merchant wants for this trade.")
    public Object[] getInput(Context context, Arguments arguments) {
        MerchantOffer recipe = info.recipe();
        return ResultWrapper.result(
                recipe == null ? null : recipe.getCostA().copy(),
                recipe != null && !recipe.getCostB().isEmpty() ? recipe.getCostB().copy() : null);
    }

    @Callback(doc = "function():table -- Returns the item the merchant offers for this trade.")
    public Object[] getOutput(Context context, Arguments arguments) {
        MerchantOffer recipe = info.recipe();
        return ResultWrapper.result(recipe == null ? null : recipe.getResult().copy());
    }

    @Callback(doc = "function():boolean -- Returns whether the merchant currently wants to trade this.")
    public Object[] isEnabled(Context context, Arguments arguments) {
        Merchant merchant = info.merchant();
        MerchantOffer recipe = info.recipe();
        return ResultWrapper.result(merchant != null && (recipe == null || !recipe.isOutOfStock()));
    }

    @Callback(doc = "function():boolean, string -- Returns true when trade succeeds and nil, error when not.")
    public Object[] trade(Context context, Arguments arguments) {
        Container inventory = info.inventory();
        if (inventory == null) {
            return ResultWrapper.result(false, "trading requires an inventory upgrade to be installed");
        }

        Merchant merchant = info.merchant();
        if (!(merchant instanceof Entity entity) || !entity.isAlive() || !isInRange()) {
            return ResultWrapper.result(false, "trade has become invalid");
        }
        if (!entity.isAlive()) {
            return ResultWrapper.result(false, "trader died");
        }
        if (!isInRange()) {
            return ResultWrapper.result(false, "out of range");
        }

        MerchantOffer recipe = info.recipe();
        if (recipe == null) {
            return ResultWrapper.result(false, "trade has become invalid");
        }
        if (recipe.isOutOfStock()) {
            return ResultWrapper.result(false, "trade is disabled");
        }
        if (!hasRoomForRecipe(inventory, recipe)) {
            return ResultWrapper.result(false, "not enough inventory space to trade");
        }
        if (completeTrade(inventory, recipe, true)) {
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(false, "not enough items to trade");
    }

    public boolean hasRoomForRecipe(Container inventory, MerchantOffer recipe) {
        ItemStack remainder = recipe.getResult().copy();
        InventoryUtils.insertIntoInventory(
                remainder,
                InventoryUtils.asItemHandler(inventory),
                remainder.getCount(),
                true,
                scala.Option.empty());
        return remainder.getCount() == 0;
    }

    public boolean completeTrade(Container inventory, MerchantOffer recipe, boolean exact) {
        Merchant merchant = info.merchant();
        if (merchant == null) return false;

        ItemStack firstInputStack = recipe.getCostA();
        ItemStack secondInputStack = recipe.getCostB().isEmpty() ? null : recipe.getCostB();
        IItemHandler handler = InventoryUtils.asItemHandler(inventory);

        if (InventoryUtils.extractFromInventory(firstInputStack, handler, true, exact).getCount() != 0
                || (secondInputStack != null
                && InventoryUtils.extractFromInventory(secondInputStack, handler, true, exact).getCount() != 0)) {
            return false;
        }

        ItemStack outputStack = recipe.getResult().copy();
        InventoryUtils.extractFromInventory(firstInputStack, handler, false, exact);
        if (secondInputStack != null) {
            InventoryUtils.extractFromInventory(secondInputStack, handler, false, exact);
        }
        InventoryUtils.insertIntoInventory(
                outputStack, handler, outputStack.getCount(), false, scala.Option.empty());
        merchant.notifyTrade(recipe);
        return true;
    }
}
