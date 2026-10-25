package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.Map;

public class UpgradeGenerator extends AbstractManagedEnvironment implements DeviceInfo {
    public final Agent host;
    private ItemStack inventory = ItemStack.EMPTY;
    private int remainingTicks;

    public UpgradeGenerator(Agent host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
            .withComponent("generator", Visibility.Neighbors).withConnector().create());
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Power,
            DeviceAttribute.Description, "Generator",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "Portagen 2.0 (Rev. 3)",
            DeviceAttribute.Capacity, "1");
    }

    @Callback(doc = "function([count:number]):boolean -- Tries to insert fuel from the selected slot into the generator's queue.")
    public Object[] insert(Context context, Arguments args) {
        int count = args.optInteger(0, 64);
        ItemStack stack = host.mainInventory().getItem(host.selectedSlot());
        if (stack.isEmpty()) return ResultWrapper.result(null, "selected slot is empty");
        if (stack.getBurnTime(null) <= 0) return ResultWrapper.result(null, "selected slot does not contain fuel");

        ItemStack container = stack.getCraftingRemainingItem();
        ItemStack inQueue = !inventory.isEmpty() && inventory.getCount() > 0 ? inventory : ItemStack.EMPTY;
        if (!inQueue.isEmpty() && !ItemStack.matches(inQueue, stack)) {
            return ResultWrapper.result(null, "different fuel type already queued");
        }
        int space = inQueue.isEmpty() ? stack.getMaxStackSize() : inQueue.getMaxStackSize() - inQueue.getCount();
        if (space == 0) return ResultWrapper.result(null, "queue is full");

        ItemStack previousSelectedFuel = stack.copy();
        int insertLimit = Math.min(stack.getCount(), Math.min(space, count));
        ItemStack fuelToInsert = stack.split(insertLimit);
        host.mainInventory().setItem(host.selectedSlot(), stack.getCount() == 0 ? ItemStack.EMPTY : stack);

        if (!container.isEmpty()) {
            container.grow(fuelToInsert.getCount() - 1);
            if (!host.player().getInventory().add(container)) {
                host.mainInventory().setItem(host.selectedSlot(), previousSelectedFuel);
                return ResultWrapper.result(false, "no space in inventory for fuel containers");
            }
            else if (container.getCount() > 0) {
                host.player().spawnAtLocation(container.copy(), -0.25f);
            }
        }

        fuelToInsert.grow(inQueue.getCount());
        inventory = fuelToInsert.isEmpty() ? ItemStack.EMPTY : fuelToInsert;
        return ResultWrapper.result(true, insertLimit);
    }

    @Callback(doc = "function():number -- Get the size of the item stack in the generator's queue.")
    public Object[] count(Context context, Arguments args) {
        return inventory.isEmpty()
            ? ResultWrapper.result(0)
            : ResultWrapper.result(inventory.getCount(), inventory.getItem().getName(inventory).getString());
    }

    @Callback(doc = "function([count:number]):boolean -- Tries to remove items from the generator's queue.")
    public Object[] remove(Context context, Arguments args) {
        int count = args.optInteger(0, Integer.MAX_VALUE);
        if (count <= 0) return ResultWrapper.result(true);
        ItemStack inQueue = !inventory.isEmpty() && inventory.getCount() > 0 ? inventory : ItemStack.EMPTY;
        if (inQueue.isEmpty()) return ResultWrapper.result(false, "queue is empty");

        ItemStack previousSelectedItem = host.mainInventory().getItem(host.selectedSlot()).copy();
        ItemStack requiredContainer = inQueue.getCraftingRemainingItem();
        ItemStack emptyContainer;
        if (!requiredContainer.isEmpty() && requiredContainer.getCount() > 0) {
            if (previousSelectedItem.isEmpty() || previousSelectedItem.getItem() != requiredContainer.getItem() ||
                !ItemStack.isSameItemSameComponents(previousSelectedItem, requiredContainer)) {
                return ResultWrapper.result(false, "removing this fuel requires the appropriate container in the selected slot");
            }
            emptyContainer = previousSelectedItem.copy();
        }
        else emptyContainer = ItemStack.EMPTY;

        int removeLimit = Math.min(inQueue.getCount(), emptyContainer.isEmpty() ? count : emptyContainer.getCount());
        ItemStack previousQueue = inQueue.copy();
        ItemStack forUser = inQueue.split(removeLimit);
        if (!emptyContainer.isEmpty()) {
            emptyContainer.split(removeLimit);
            if (emptyContainer.isEmpty()) host.mainInventory().setItem(host.selectedSlot(), ItemStack.EMPTY);
            else host.mainInventory().removeItem(host.selectedSlot(), removeLimit);
        }
        if (!host.player().getInventory().add(forUser)) {
            host.mainInventory().setItem(host.selectedSlot(), previousSelectedItem);
            inventory = previousQueue;
            return ResultWrapper.result(false, "no inventory space available for fuel");
        }
        int actualRemoval = removeLimit - forUser.getCount();
        previousQueue.shrink(actualRemoval);
        inventory = previousQueue.isEmpty() ? ItemStack.EMPTY : previousQueue;
        return ResultWrapper.result(true, actualRemoval);
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (remainingTicks <= 0 && !inventory.isEmpty()) {
            remainingTicks = inventory.getBurnTime(null);
            if (remainingTicks > 0) {
                updateClient();
                inventory.shrink(1);
                if (inventory.getCount() <= 0) inventory = ItemStack.EMPTY;
            }
        }
        if (remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks == 0 && inventory.isEmpty()) updateClient();
            ((Connector) node()).changeBuffer(Settings.get().generatorEfficiency());
        }
    }

    private void updateClient() {
        if (host instanceof Robot robot) robot.synchronizeSlot(robot.componentSlot(node().address()));
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == node()) {
            if (!inventory.isEmpty()) {
                var level = host.getEnvironmentLevel();
                ItemEntity entity = new ItemEntity(level, host.xPosition(), host.yPosition(), host.zPosition(), inventory.copy());
                entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.04, 0));
                entity.setPickUpDelay(5);
                level.addFreshEntity(entity);
                inventory = ItemStack.EMPTY;
            }
            remainingTicks = 0;
        }
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        ImmutableItemStack saved = holder.get(OCComponentsJava.FUEL_INVENTORY.get());
        inventory = saved == null ? ItemStack.EMPTY : saved.mutableCopy();
        Object ticks = holder.get(OCComponentsJava.FUEL_TICKS_REMAINING.get());
        remainingTicks = ticks == null ? 0 : ((Number) ticks).intValue();
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        if (inventory.isEmpty()) holder.remove(OCComponentsJava.FUEL_INVENTORY.get());
        else holder.set(OCComponentsJava.FUEL_INVENTORY.get(), ImmutableItemStack.copyOf(inventory));
        if (remainingTicks > 0) holder.set(OCComponentsJava.FUEL_TICKS_REMAINING.get(), remainingTicks);
        else holder.remove(OCComponentsJava.FUEL_TICKS_REMAINING.get());
    }
}
