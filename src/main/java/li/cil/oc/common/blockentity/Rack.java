package li.cil.oc.common.blockentity;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.client.renderer.block.ServerRackModel;
import li.cil.oc.common.blockentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.common.Slot;
import li.cil.oc.common.component.RackKVM;
import li.cil.oc.common.component.TerminalServer;
import li.cil.oc.common.datacomponents.CompoundStorage;
import li.cil.oc.server.InventoryPacketSender;
import li.cil.oc.util.ExtendedDataComponentHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Option$;

import java.util.Optional;
import scala.collection.Iterator;
import scala.collection.immutable.List$;
import scala.collection.mutable.ArrayBuffer;
import scala.jdk.javaapi.CollectionConverters;

import java.util.Optional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;

/** Four-slot rack joining mountable components into a managed network. */
public class Rack extends RackBlockEntity {
    @Override public Node node() { return null; }
    private final li.cil.oc.common.blockentity.traits.HubStateJava hubState =
        new li.cil.oc.common.blockentity.traits.HubStateJava(this);
    @Override public li.cil.oc.common.blockentity.traits.HubStateJava hubState() { return hubState; }
    private final li.cil.oc.common.blockentity.traits.PowerBalancerStateJava powerState =
        new li.cil.oc.common.blockentity.traits.PowerBalancerStateJava();
    @Override public li.cil.oc.common.blockentity.traits.PowerBalancerStateJava powerBalancerState() { return powerState; }
    @Override public boolean powerNetworkConnected() { return isConnected(); }
    @Override public boolean powerBalanceAfterHub() { return true; }
    @Override public boolean redstoneAfterInventory() { return true; }
    public boolean isRelayEnabled;
    public final Option<CompoundStorage>[] lastData;
    public final boolean[] hasChanged;
    public final Option<Direction>[][] nodeMapping;
    public final Node[][] snifferNodes;

    @SuppressWarnings("unchecked")
    public Rack(BlockPos pos, BlockState state) {
        super(pos, state);
        lastData = (Option<CompoundStorage>[]) new Option<?>[getContainerSize()];
        hasChanged = new boolean[getContainerSize()];
        nodeMapping = (Option<Direction>[][]) new Option<?>[getContainerSize()][4];
        snifferNodes = new Node[getContainerSize()][3];
        for (int slot = 0; slot < getContainerSize(); slot++) {
            lastData[slot] = Option$.MODULE$.empty();
            hasChanged[slot] = true;
            for (int connection = 0; connection < 4; connection++) nodeMapping[slot][connection] = Option$.MODULE$.empty();
            for (int index = 0; index < 3; index++) snifferNodes[slot][index] = li.cil.oc.api.Network.newNode(this, Visibility.Neighbors).create();
        }
    }

    public boolean isRelayEnabled() { return isRelayEnabled; }
    public void isRelayEnabled_$eq(boolean value) { isRelayEnabled = value; }
    public Option<CompoundStorage>[] lastData() { return lastData; }
    public boolean[] hasChanged() { return hasChanged; }
    public Option<Direction>[][] nodeMapping() { return nodeMapping; }
    public Node[][] snifferNodes() { return snifferNodes; }

    public void connect(int slot, int connectableIndex, Option<Direction> side) {
        Option<Direction> newSide = side.isDefined() && side.get() != Direction.SOUTH ? side : Option$.MODULE$.empty();
        Option<Direction> oldSide = nodeMapping[slot][connectableIndex + 1];
        if (oldSide.equals(newSide)) return;
        RackMountable mountable = getMountable(slot);
        if (mountable != null && oldSide.isDefined()) {
            if (connectableIndex == -1) {
                Node node = mountable.node();
                Node plug = sidedNode(toGlobal(oldSide.get()));
                if (node != null && plug != null) node.disconnect(plug);
            } else if (connectableIndex >= 0) snifferNodes[slot][connectableIndex].remove();
        }
        nodeMapping[slot][connectableIndex + 1] = newSide;
        if (mountable != null && newSide.isDefined()) {
            if (connectableIndex == -1) {
                Node node = mountable.node();
                Node plug = sidedNode(toGlobal(newSide.get()));
                if (node != null && plug != null) node.connect(plug);
            } else if (connectableIndex >= 0 && connectableIndex < mountable.getConnectableCount()) {
                var connectable = mountable.getConnectableAt(connectableIndex);
                if (connectable != null && connectable.node() != null) {
                    if (connectable.node().network() == null) li.cil.oc.api.Network.joinNewNetwork(connectable.node());
                    connectable.node().connect(snifferNodes[slot][connectableIndex]);
                }
            }
        }
    }

    public void connect(int slot, int connectableIndex, Optional<Direction> side) {
        connect(slot, connectableIndex, scala.jdk.javaapi.OptionConverters.toScala(side));
    }

    private void reconnect(Direction plugSide) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            Option<Direction>[] mapping = nodeMapping[slot];
            if (mapping[0].isDefined() && toGlobal(mapping[0].get()) == plugSide) {
                RackMountable mountable = getMountable(slot);
                Node busNode = sidedNode(plugSide);
                if (busNode != null && mountable != null && mountable.node() != null && busNode != mountable.node()) {
                    li.cil.oc.api.Network.joinNewNetwork(mountable.node());
                    busNode.connect(mountable.node());
                }
            }
            for (int index = 0; index < 3; index++) {
                Option<Direction> mappedSide = mapping[index + 1];
                if (!mappedSide.isDefined() || toGlobal(mappedSide.get()) != plugSide) continue;
                RackMountable mountable = getMountable(slot);
                if (mountable == null || index >= mountable.getConnectableCount()) continue;
                var connectable = mountable.getConnectableAt(index);
                if (connectable != null && connectable.node() != null) {
                    if (connectable.node().network() == null) li.cil.oc.api.Network.joinNewNetwork(connectable.node());
                    connectable.node().connect(snifferNodes[slot][index]);
                }
            }
        }
    }

    public void sendPacketToMountables(Option<Direction> sourceSide, Packet packet) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            RackMountable mountable = getMountable(slot);
            if (mountable == null) continue;
            for (int index = 0; index < 3; index++) {
                Option<Direction> mappedSide = nodeMapping[slot][index + 1];
                if (mappedSide.isDefined() && sourceSide.contains(toGlobal(mappedSide.get())) && index < mountable.getConnectableCount()) {
                    var connectable = mountable.getConnectableAt(index);
                    if (connectable != null) connectable.receivePacket(packet);
                }
            }
        }
    }

    @Override public boolean tryEnqueuePacketJava(Optional<Direction> sourceSide, Packet packet) {
        sendPacketToMountables(scala.jdk.javaapi.OptionConverters.toScala(sourceSide), packet);
        return isRelayEnabled ? super.tryEnqueuePacketJava(sourceSide, packet) : true;
    }

    @Override public void relayPacketJava(Optional<Direction> sourceSide, Packet packet) { if (isRelayEnabled) super.relayPacketJava(sourceSide, packet); }
    @Override public void onPlugConnect(li.cil.oc.common.blockentity.traits.Hub.Plug plug, Node node) { super.onPlugConnect(plug, node); connectComponents(); reconnect(plug.side()); }
    @Override public Node createNode(li.cil.oc.common.blockentity.traits.Hub.Plug plug) { return li.cil.oc.api.Network.newNode(plug, Visibility.Network).withConnector(Settings.get().bufferDistributor()).create(); }

    @Override public void dispose() { super.dispose(); disconnectComponents(); }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        Object[] data = message.data();
        if ("network.message".equals(message.name()) && data.length == 1 && data[0] instanceof Packet packet) relayIfMessageFromConnectable(message, packet);
    }

    private void relayIfMessageFromConnectable(Message message, Packet packet) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            RackMountable mountable = getMountable(slot);
            if (mountable == null) continue;
            for (int index = 0; index < 3; index++) {
                Option<Direction> side = nodeMapping[slot][index + 1];
                if (!side.isDefined() || index >= mountable.getConnectableCount()) continue;
                var connectable = mountable.getConnectableAt(index);
                if (connectable != null && connectable.node() == message.source()) {
                    Node node = sidedNode(toGlobal(side.get()));
                    if (node != null) node.sendToReachable("network.message", packet);
                    relayToConnectablesOnSide(message, packet, side.get());
                    return;
                }
            }
        }
    }

    private void relayToConnectablesOnSide(Message message, Packet packet, Direction sourceSide) {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            RackMountable mountable = getMountable(slot);
            if (mountable == null) continue;
            for (int index = 0; index < 3; index++) {
                Option<Direction> side = nodeMapping[slot][index + 1];
                if (!side.isDefined() || side.get() != sourceSide || index >= mountable.getConnectableCount()) continue;
                var connectable = mountable.getConnectableAt(index);
                if (connectable != null && connectable.node() != message.source()) snifferNodes[slot][index].sendToNeighbors("network.message", packet);
            }
        }
    }

    @Override public boolean canConnect(Direction side) { return side != facing(); }
    @Override public Node sidedNode(Direction side) { return side != facing() ? super.sidedNode(side) : null; }
    @OnlyIn(Dist.CLIENT) @Override public boolean hasConnector(Direction side) { return side != facing(); }
    @Override public java.util.Optional<Connector> connectorOptional(Direction side) { return java.util.Optional.ofNullable(side != facing() && sidedNode(side) instanceof Connector connector ? connector : null); }
    @Override public double energyThroughput() { return Settings.get().serverRackRate(); }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        Option<Integer> slot = slotAt(side, hitX, hitY, hitZ);
        if (slot.isDefined()) {
            Optional<ManagedEnvironment>[] components = ComponentInventorySlotAccessJava.snapshot(this);
            Optional<ManagedEnvironment> component = components[slot.get()];
            if (component.isPresent() && component.get() instanceof Analyzable analyzable) return analyzable.onAnalyze(player, side, hitX, hitY, hitZ);
            return null;
        }
        return new Node[]{sidedNode(side)};
    }

    @Override public int indexOfMountable(RackMountable mountable) {
        Optional<ManagedEnvironment>[] components = ComponentInventorySlotAccessJava.snapshot(this);
        for (int i = 0; i < components.length; i++) if (components[i].isPresent() && components[i].get() == mountable) return i;
        return -1;
    }
    @Override public RackMountable getMountable(int slot) {
        Optional<ManagedEnvironment>[] components = ComponentInventorySlotAccessJava.snapshot(this);
        Optional<ManagedEnvironment> value = components[slot];
        return value.isPresent() && value.get() instanceof RackMountable mountable ? mountable : null;
    }
    @Override public DataComponentHolder getMountableData(int slot) { return lastData[slot].isDefined() ? lastData[slot].get() : CompoundStorage.EMPTY; }
    @Override public void markChanged(int slot) { synchronized (hasChanged) { hasChanged[slot] = true; } setOutputEnabled(hasRedstoneCard()); }

    @Override
    public java.util.EnumSet<StateAware.State> getCurrentState() {
        java.util.EnumSet<StateAware.State> result = java.util.EnumSet.noneOf(StateAware.State.class);
        for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this)) if (component.isPresent() && component.get() instanceof RackMountable mountable) result.addAll(mountable.getCurrentState());
        return result;
    }

    @Override public void onRotationChanged() { super.onRotationChanged(); checkRedstoneInputChanged(); }

    @Override
    public void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        super.onRedstoneInputChanged(args);
        for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this)) if (component.isPresent() && component.get() instanceof RackMountable mountable && mountable.node() != null) {
            RedstoneChangedEventArgs local = new RedstoneChangedEventArgs(toLocal(args.side()), args.oldValue(), args.newValue(), args.color());
            mountable.node().sendToNeighbors("redstone.changed", local);
        }
    }

    @Override public int getContainerSize() { return 4; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { var driver = Driver.driverFor(stack, getClass()); return driver != null && driver.slot(stack) == Slot.RackMountable(); }

    @Override
    public void setChanged() {
        super.setChanged();
        if (isServer()) {
            setOutputEnabled(hasRedstoneCard());
            try { InventoryPacketSender.sendRackInventory(this); }
            catch (IOException error) { Rack.<RuntimeException>throwUnchecked(error); }
        }
        else getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) { return new li.cil.oc.common.menu.Rack(id, playerInventory, this); }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        if (isServer()) {
            for (int index = 0; index < 4; index++) nodeMapping[slot][index] = Option$.MODULE$.empty();
            lastData[slot] = Option$.MODULE$.empty();
            hasChanged[slot] = true;
        }
        super.onItemAdded(slot, stack);
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        if (isServer()) {
            for (int index = 0; index < 4; index++) nodeMapping[slot][index] = Option$.MODULE$.empty();
            lastData[slot] = Option$.MODULE$.empty();
        }
        super.onItemRemoved(slot, stack);
    }

    @Override public void connectItemNode(Node node) { li.cil.oc.api.Network.joinNewNetwork(node); }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isServer() && isConnected()) {
            List<Connector> outside = new ArrayList<>();
            for (Direction direction : Direction.values()) if (sidedNode(direction) instanceof Connector connector) outside.add(connector);
            for (int slot = 0; slot < ComponentInventorySlotAccessJava.snapshot(this).length; slot++) {
                RackMountable mountable = getMountable(slot);
                if (mountable == null) continue;
                if (hasChanged[slot]) {
                    hasChanged[slot] = false;
                    CompoundStorage data = lastData[slot].isDefined() ? lastData[slot].get() : new CompoundStorage();
                    mountable.describeForClient(data);
                    lastData[slot] = Option$.MODULE$.apply(data);
                    try { InventoryPacketSender.sendRackMountableData(this, slot); }
                    catch (IOException error) { Rack.<RuntimeException>throwUnchecked(error); }
                    getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
                    setOutputEnabled(hasRedstoneCard());
                }
                if (mountable.node() instanceof Connector connector) {
                    double remaining = Settings.get().serverRackRate();
                    for (Connector plug : outside) if (remaining > 0) {
                        double received = remaining + plug.changeBuffer(-remaining);
                        double rejected = connector.changeBuffer(received);
                        plug.changeBuffer(rejected);
                        remaining -= received - rejected;
                    }
                }
            }
            updateComponents();
        } else {
            for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this)) {
                if (component.isPresent() && component.get() instanceof TerminalServer terminal) terminal.update();
                else if (component.isPresent() && component.get() instanceof RackKVM kvm) kvm.update();
            }
        }
    }

    @Override
    public void loadComponentsForServer(DataComponentHolder holder) {
        super.loadComponentsForServer(holder);
        isRelayEnabled = holder.has(OCComponentsJava.RELAY_ENABLED.get());
        new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.RACK_NODE_MAPPING.get()).ifPresent(saved -> {
            int slot = 0;
            Iterator<scala.collection.immutable.List<Direction>> mappings = saved.iterator();
            while (mappings.hasNext()) {
                scala.collection.immutable.List<Direction> mapping = mappings.next();
                for (int index = 0; index < Math.min(4, mapping.size()); index++) nodeMapping[slot][index] = mapping.apply(index) == Direction.SOUTH ? Option$.MODULE$.empty() : Option$.MODULE$.apply(mapping.apply(index));
                slot++;
                if (slot >= nodeMapping.length) break;
            }
        });
    }

    @Override
    public void saveComponentsForServer(MutableDataComponentHolder holder) {
        super.saveComponentsForServer(holder);
        new li.cil.oc.util.ExtendedMutableDataComponentHolderJava(holder).setUnitComponent(OCComponentsJava.RELAY_ENABLED, isRelayEnabled);
        List<scala.collection.immutable.List<Direction>> encoded = new ArrayList<>();
        for (Option<Direction>[] mapping : nodeMapping) {
            List<Direction> values = new ArrayList<>();
            for (Option<Direction> value : mapping) {
                if (!value.isDefined()) values.add(Direction.SOUTH);
                else if (value.get() == Direction.SOUTH) { OpenComputers.log().warn("Weird direction value in rack at " + getBlockPos() + "! SOUTH should not be possible?"); values.add(Direction.SOUTH); }
                else values.add(value.get());
            }
            encoded.add(List$.MODULE$.from(CollectionConverters.asScala(values)));
        }
        holder.set(OCComponentsJava.RACK_NODE_MAPPING.get(), List$.MODULE$.from(CollectionConverters.asScala(encoded)));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(ServerRackModel.RACK_PROPERTY, this).build();
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        for (int slot = 0; slot < ComponentInventorySlotAccessJava.snapshot(this).length; slot++) {
            RackMountable mountable = getMountable(slot);
            ItemStack stack = items()[slot];
            if (!stack.isEmpty() && mountable instanceof TerminalServer terminal) terminal.saveData(stack);
            else if (!stack.isEmpty() && mountable instanceof RackKVM kvm) kvm.saveData(stack);
        }
        super.saveForServer(nbt, provider);
    }

    @Override
    public void loadComponentsForClient(DataComponentHolder holder) {
        super.loadComponentsForClient(holder);
        requestModelDataUpdate();
        new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.RACK_DATA.get()).ifPresent(saved -> {
            int index = 0;
            Iterator<Option<CompoundStorage>> values = saved.iterator();
            while (values.hasNext() && index < lastData.length) lastData[index++] = values.next();
        });
        loadData(holder);
        connectComponents();
    }

    @Override
    public void saveComponentsForClient(MutableDataComponentHolder holder) {
        super.saveComponentsForClient(holder);
        holder.set(OCComponentsJava.RACK_DATA.get(), List$.MODULE$.from(CollectionConverters.asScala(java.util.Arrays.asList(lastData))));
        saveData(holder);
    }

    public Option slotAt(Direction side, float hitX, float hitY, float hitZ) {
        if (side != facing()) return Option$.MODULE$.empty();
        int globalY = (int) (hitY * 16);
        int slot = (15 - globalY - 2) * getContainerSize() / (14 - 2);
        return Option$.MODULE$.apply(Math.max(0, Math.min(getContainerSize() - 1, slot)));
    }

    public boolean isWorking(RackMountable mountable) { return mountable.getCurrentState().contains(StateAware.State.IsWorking); }

    public boolean hasRedstoneCard() {
        for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this)) {
            if (!component.isPresent() || !(component.get() instanceof RackMountable mountable) || !(mountable instanceof Container container) || !isWorking(mountable)) continue;
            for (int slot = 0; slot < container.getContainerSize(); slot++) if (li.cil.oc.integration.opencomputers.DriverRedstoneCard.INSTANCE.worksWith(container.getItem(slot), (Class<? extends EnvironmentHost>) mountable.getClass())) return true;
        }
        return false;
    }
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable error) throws T { throw (T) error; }
}



