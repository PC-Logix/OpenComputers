package li.cil.oc.server.component;

import li.cil.oc.api.Machine;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.server.network.Connector;
import li.cil.oc.util.ExtendedDataComponentHolderSupportJava;
import li.cil.oc.util.ExtendedMutableDataComponentHolderJava;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;

import java.util.Optional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;

/** A server machine mounted in a rack slot. */
public final class Server extends ServerComponentBridge {
    private final ItemStack[] itemStacks;
    private li.cil.oc.api.machine.Machine machine;
    private final Node node;
    private boolean wasRunning;
    private boolean hadErrored;
    private long lastFileSystemAccess;
    private long lastNetworkActivity;
    private DataComponentMap pendingMachineData;
    private Map<String, String> deviceInfo;

    public Server(li.cil.oc.api.internal.Rack rack, int slot) {
        super(rack, slot);
        itemStacks = new ItemStack[getContainerSize()];
        Arrays.fill(itemStacks, ItemStack.EMPTY);
        node = rack.getEnvironmentLevel().isClientSide() ? null : machine().node();
        if (container() != null && !container().isEmpty()) reinitialize();
    }

    @Override public ItemStack[] items() { return itemStacks; }
    @Override public li.cil.oc.api.machine.Machine machine() {
        if (machine == null) machine = Machine.create(this);
        return machine;
    }
    @Override public Node node() { return node; }
    @Override public int rackSlot() { return slot(); }
    public long lastFileSystemAccess() { return lastFileSystemAccess; }
    public void lastFileSystemAccess_$eq(long value) { lastFileSystemAccess = value; }
    public long lastNetworkActivity() { return lastNetworkActivity; }
    public void lastNetworkActivity_$eq(long value) { lastNetworkActivity = value; }
    public boolean wasRunning() { return wasRunning; }
    public void wasRunning_$eq(boolean value) { wasRunning = value; }
    public boolean hadErrored() { return hadErrored; }
    public void hadErrored_$eq(boolean value) { hadErrored = value; }
    @Override public li.cil.oc.api.internal.Rack host() { return rack(); }
    @Override public ItemStack container() { return rack().getItem(slot()); }
    @Override public int tier() { return container().getItem() instanceof li.cil.oc.common.item.Server server ? server.tier() : 0; }
    @Override public Level getEnvironmentLevel() { return rack().getEnvironmentLevel(); }
    @Override public double xPosition() { return rack().xPosition(); }
    @Override public double yPosition() { return rack().yPosition(); }
    @Override public double zPosition() { return rack().zPosition(); }
    @Override public void markChanged() { rack().markChanged(); }
    @Override public boolean stillValid(Player player) { return rack().stillValid(player) && rack().indexOfMountable(this) >= 0; }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = ServerComponentDeviceInfo.server(Integer.toString(getContainerSize()));
        return deviceInfo;
    }

    @Override
    public void onConnect(Node connected) {
        if (connected == node) connectComponents();
    }

    @Override
    public void onDisconnect(Node disconnected) {
        if (disconnected == node) disconnectComponents();
    }

    @Override public void onMessage(Message message) { }

    @Override
    public void loadData(DataComponentHolder holder) {
        super.loadData(holder);
        if (!rack().getEnvironmentLevel().isClientSide()) pendingMachineData = holder.getComponents();
    }

    private void loadPendingMachineData() {
        if (pendingMachineData != null) {
            DataComponentMap data = pendingMachineData;
            pendingMachineData = null;
            try {
                machine().loadData(Persistable.holder(data));
            } catch (UnrecoverablePersistanceException error) {
                Server.<RuntimeException>throwUnchecked(error);
            }
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        if (!rack().getEnvironmentLevel().isClientSide()) loadPendingMachineData();
        super.saveData(holder);
        if (!rack().getEnvironmentLevel().isClientSide()) machine().saveData(holder);
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty() && isComponentSlot(i, stack)) result.add(stack);
        }
        return result;
    }

    @Override
    public int componentSlot(String address) {
        Optional<li.cil.oc.api.network.ManagedEnvironment>[] components = ComponentInventorySlotAccessJava.snapshot(this);
        for (int i = 0; i < components.length; i++) {
            if (components[i].isPresent()) {
                Node componentNode = components[i].get().node();
                if (componentNode != null && Objects.equals(address, componentNode.address())) return i;
            }
        }
        return -1;
    }

    @Override public void onMachineConnect(Node connected) { onConnect(connected); }
    @Override public void onMachineDisconnect(Node disconnected) { onDisconnect(disconnected); }

    @Override
    public void connectItemNode(Node componentNode) {
        if (componentNode != null) {
            li.cil.oc.api.Network.joinNewNetwork(machine().node());
            machine().node().connect(componentNode);
        }
    }

    @Override
    public void onItemRemoved(int inventorySlot, ItemStack stack) {
        super.onItemRemoved(inventorySlot, stack);
        if (!rack().getEnvironmentLevel().isClientSide()) {
            if (InventorySlots.server[tier()][inventorySlot].slot().equals(Slot.CPU)) machine().stop();
        }
    }

    @Override
    public void describeForClient(MutableDataComponentHolder holder) {
        ExtendedMutableDataComponentHolderJava data = ExtendedDataComponentHolderSupportJava.convert(holder);
        data.setComponent(OCComponentsJava.IS_RUNNING.get(), wasRunning);
        data.setUnitComponent(OCComponentsJava.IS_ERRORED, hadErrored);
        data.setComponent(OCComponentsJava.NETWORK_LAST_DISK_ACCESS.get(), lastFileSystemAccess);
        data.setComponent(OCComponentsJava.NETWORK_LAST_NETWORK_ACCESS.get(), lastNetworkActivity);
    }

    @Override
    public int getConnectableCount() {
        int count = 0;
        for (Optional<li.cil.oc.api.network.ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this))
            if (component.isPresent() && component.get() instanceof RackBusConnectable) count++;
        return count;
    }

    @Override
    public RackBusConnectable getConnectableAt(int index) {
        for (Optional<li.cil.oc.api.network.ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this)) {
            if (component.isPresent() && component.get() instanceof RackBusConnectable connectable && index-- == 0) return connectable;
        }
        throw new IndexOutOfBoundsException(index);
    }

    @Override
    public boolean onActivate(Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY) {
        if (!player.level().isClientSide()) {
            if (player.isCrouching()) {
                if (!machine().isRunning() && stillValid(player)) {
                    wasRunning = false;
                    hadErrored = false;
                    machine().start();
                }
            } else if (player instanceof ServerPlayer serverPlayer) {
                MenuTypes.openServerGui(serverPlayer, this, slot());
            }
        }
        return true;
    }

    @Override public boolean canUpdate() { return true; }

    @Override
    public void update() {
        if (!rack().getEnvironmentLevel().isClientSide()) {
            loadPendingMachineData();
            machine().update();
            boolean isRunning = machine().isRunning();
            boolean hasErrored = machine().lastError() != null;
            if (isRunning != wasRunning || hasErrored != hadErrored) rack().markChanged(slot());
            wasRunning = isRunning;
            hadErrored = hasErrored;
            if (tier() == Tier.Five) ((Connector) node).changeBuffer(Double.POSITIVE_INFINITY);
        }
        updateComponents();
    }

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        return machine().isRunning() ? EnumSet.of(StateAware.State.IsWorking) : EnumSet.noneOf(StateAware.State.class);
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{machine().node()};
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }
}
