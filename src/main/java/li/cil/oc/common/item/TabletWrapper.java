package li.cil.oc.common.item;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Machine;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.item.Container;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.data.TabletData;
import li.cil.oc.common.menu.Tablet;
import li.cil.oc.integration.opencomputers.DriverScreen;
import li.cil.oc.util.RotationHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;

import java.util.Optional;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;

/** Java implementation of the tablet's live inventory and machine host. */
public final class TabletWrapper extends TabletWrapperBase {
    public ItemStack stack;
    public Player player;
    private final Level environmentLevel;
    public final TabletData data;
    public final li.cil.oc.api.machine.Machine machine;
    public final li.cil.oc.server.component.Tablet tablet;
    private final TextBuffer.ColorDepth colorDepth;
    private boolean isInitialized;
    public int timesChanged;
    public boolean isDirty = true;
    private boolean lastRunning;
    public boolean autoSave = true;

    public TabletWrapper(ItemStack stack, Player player) throws UnrecoverablePersistanceException {
        super(stack, player);
        this.stack = stack;
        this.player = player;
        this.environmentLevel = player.level();
        this.data = new TabletData();
        this.machine = environmentLevel.isClientSide() ? null : Machine.create(this);
        this.colorDepth = data.tier >= Tier.Three ? TextBuffer.ColorDepth.EightBit : TextBuffer.ColorDepth.FourBit;
        this.tablet = environmentLevel.isClientSide() ? null : new li.cil.oc.server.component.Tablet(this);
        this.isInitialized = !environmentLevel.isClientSide();
        readFromNBT(player.registryAccess());
        if (!environmentLevel.isClientSide()) {
            li.cil.oc.api.Network.joinNewNetwork(machine.node());
            double charge = Math.max(0, data.energy - tablet.node().globalBuffer());
            tablet.node().changeBuffer(charge);
            writeToNBT(player.registryAccess());
        }
    }

    public Level getEnvironmentLevel() { return environmentLevel; }
    public ItemStack stack() { return stack; }
    public void stack_$eq(ItemStack stack) { this.stack = stack; }
    public li.cil.oc.api.machine.Machine machine() { return machine; }
    public TabletData data() { return data; }
    public li.cil.oc.server.component.Tablet tablet() { return tablet; }
    public Player player() { return player; }
    public TextBuffer.ColorDepth colorDepth() { return colorDepth; }
    public boolean isCreative() { return data.tier == Tier.Five; }
    public ItemStack[] items() { return data.items; }

    @Override
    public Direction facing() { return RotationHelper.fromYaw(player.getYRot()); }

    @Override
    public Direction toLocal(Direction value) { return RotationHelper.toLocal(Direction.NORTH, facing(), value); }

    @Override
    public Direction toGlobal(Direction value) { return RotationHelper.toGlobal(Direction.NORTH, facing(), value); }

    public void readFromNBT(HolderLookup.Provider provider) throws UnrecoverablePersistanceException {
        loadData(stack);
        if (!environmentLevel.isClientSide()) {
            tablet.loadData(stack);
            machine.loadData(stack);
        }
    }

    public void writeToNBT(HolderLookup.Provider provider) {
        saveData(stack);
        if (!environmentLevel.isClientSide()) {
            tablet.saveData(stack);
            machine.saveData(stack);
        }
    }

    @Override
    public Component getDisplayName() { return getName(); }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new Tablet(id, playerInventory, stack, this, containerSlotType(), containerSlotTier());
    }

    @Override
    public void onConnect(Node node) {
        if (node == node()) {
            connectComponents();
            node.connect(tablet.node());
        } else if (node.host() instanceof TextBuffer buffer) {
            buffer.setMaximumColorDepth(colorDepth);
            buffer.setMaximumResolution(80, 25);
        }
    }

    @Override
    public void connectItemNode(Node node) {
        super.connectItemNode(node);
        if (node == null) return;
        if (node.host() instanceof TextBuffer buffer) {
            for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this)) {
                if (component.isPresent() && component.get() instanceof li.cil.oc.api.internal.Keyboard keyboard) {
                    buffer.node().connect(keyboard.node());
                }
            }
        } else if (node.host() instanceof li.cil.oc.api.internal.Keyboard keyboard) {
            for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this)) {
                if (component.isPresent() && component.get() instanceof TextBuffer buffer) {
                    keyboard.node().connect(buffer.node());
                }
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        if (node == node()) {
            disconnectComponents();
            tablet.node().remove();
        }
    }

    @Override
    public void onMessage(Message message) { }

    @Override
    public TabletWrapper host() { return this; }

    @Override
    public int getContainerSize() { return data.items.length; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != getContainerSize() - 1) return false;
        var driver = Driver.driverFor(stack, getClass());
        return driver != null && driver != DriverScreen.INSTANCE &&
            Objects.equals(driver.slot(stack), containerSlotType()) && driver.tier(stack) <= containerSlotTier();
    }

    @Override
    public boolean stillValid(Player player) { return machine != null && machine.canInteract(player.getName().getString()); }

    @Override
    public void setChanged() {
        data.saveData(stack);
        player.getInventory().setChanged();
    }

    @Override
    public double xPosition() { return player.getX(); }

    @Override
    public double yPosition() { return player.getY() + player.getEyeHeight(); }

    @Override
    public double zPosition() { return player.getZ(); }

    @Override
    public void markChanged() { }

    public String containerSlotType() {
        if (data.container.isEmpty()) return Slot.None;
        var driver = Driver.driverFor(data.container, getClass());
        return driver instanceof Container container ? container.providedSlot(data.container) : Slot.None;
    }

    public int containerSlotTier() {
        if (data.container.isEmpty()) return Tier.None;
        var driver = Driver.driverFor(data.container, getClass());
        return driver instanceof Container container ? container.providedTier(data.container) : Tier.None;
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack item = getItem(slot);
            if (!item.isEmpty() && isComponentSlot(slot, item)) result.add(item);
        }
        return result;
    }

    @Override
    public int componentSlot(String address) {
        Optional<ManagedEnvironment>[] slots = ComponentInventorySlotAccessJava.snapshot(this);
        for (int slot = 0; slot < slots.length; slot++) {
            if (slots[slot].isPresent()) {
                Node node = slots[slot].get().node();
                if (node != null && Objects.equals(address, node.address())) return slot;
            }
        }
        return -1;
    }

    @Override
    public void onMachineConnect(Node node) { onConnect(node); }

    @Override
    public void onMachineDisconnect(Node node) { onDisconnect(node); }

    @Override
    public Node node() { return machine == null ? null : machine.node(); }

    public void syncRunningState() {
        boolean running = machine.isRunning();
        boolean changed = lastRunning != running || data.isRunning != running;
        data.isRunning = running;
        if (changed) {
            lastRunning = running;
            setChanged();
            if (player instanceof ServerPlayer serverPlayer) {
                try {
                    li.cil.oc.server.GeneralPacketSender.sendMachineItemState(serverPlayer, stack, running);
                } catch (IOException error) {
                    throw new UncheckedIOException(error);
                }
            }
            if (running) {
                for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this)) {
                    if (component.isPresent() && component.get() instanceof TextBuffer buffer) buffer.setPowerState(true);
                }
            }
        }
    }

    public void update(Level level, Player player, int slot, boolean selected) {
        this.player = player;
        if (!isInitialized) {
            isInitialized = true;
            connectComponents();
            for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this)) {
                if (component.isPresent() && component.get() instanceof TextBuffer buffer) {
                    buffer.setMaximumColorDepth(colorDepth);
                    buffer.setMaximumResolution(80, 25);
                    if (buffer instanceof li.cil.oc.common.component.TextBuffer concrete) concrete.markInitialized();
                }
            }
            li.cil.oc.client.PacketSender.sendMachineItemStateRequest(stack, level.registryAccess());
        }
        if (!level.isClientSide()) {
            if (isCreative() && level.getGameTime() % li.cil.oc.Settings.get().tickFrequency() == 0) {
                ((Connector) machine.node()).changeBuffer(Double.POSITIVE_INFINITY);
            }
            machine.update();
            updateComponents();
            data.energy = tablet.node().globalBuffer();
            data.maxEnergy = tablet.node().globalBufferSize();
            syncRunningState();
        }
    }

    @Override
    public void loadData(DataComponentHolder holder) { data.loadData(holder); }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        saveComponents();
        data.saveData(holder);
    }
}
