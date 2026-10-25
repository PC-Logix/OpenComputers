package li.cil.oc.common.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.internal.Server;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.Lifecycle;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.Tier;
import li.cil.oc.common.datacomponents.CompoundStorage;
import li.cil.oc.common.datacomponents.TerminalReference;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedDataComponentHolderSupportJava;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.collection.Seq;
import scala.collection.immutable.List;
import scala.collection.immutable.List$;
import scala.collection.mutable.ListBuffer;
import scala.collection.mutable.ListBuffer$;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

public class RackKVM implements Environment, EnvironmentHost, Analyzable, RackMountable, Lifecycle, DeviceInfo, RemoteTerminalHost {
    private final Rack rack;
    private final int slot;
    private final Node node;
    private final double range;
    private final ListBuffer<String> keys = ListBuffer$.MODULE$.empty();
    private final int[] consoleSlots;
    private int selectedSlot = -1;
    private int lastServerMask = -1;
    private final Node[] attachedServers = new Node[3];
    private TextBuffer[] buffers;
    private Keyboard[] keyboards;

    public RackKVM(Rack rack, int slot) {
        this.rack = rack;
        this.slot = slot;
        this.node = Network.newNode(this, Visibility.None).create();
        this.range = Settings.get().maxWirelessRange()[Tier.Two];
        this.consoleSlots = new int[3];
        for (int rackSlot = 0, index = 0; rackSlot < 4; rackSlot++) {
            if (rackSlot != slot) consoleSlots[index++] = rackSlot;
        }
    }

    @Override public Rack rack() { return rack; }
    @Override public int slot() { return slot; }
    @Override public Node node() { return node; }
    @Override public double range() { return range; }
    public ListBuffer<String> keys() { return keys; }
    public int[] consoleSlots() { return consoleSlots; }

    public synchronized TextBuffer[] buffers() {
        if (buffers == null) {
            buffers = new TextBuffer[3];
            for (int i = 0; i < buffers.length; i++) {
                ItemStack screenItem = Items.get(Constants.BlockName$.MODULE$.ScreenTier1()).createItemStack(1);
                TextBuffer buffer = (TextBuffer) li.cil.oc.api.Driver.driverFor(screenItem, getClass()).createEnvironment(screenItem, this);
                scala.Tuple2<Object, Object> resolution = Settings.get().screenResolutionsByTier()[Tier.Four];
                buffer.setMaximumResolution(((Number) resolution._1()).intValue(), ((Number) resolution._2()).intValue());
                buffer.setMaximumColorDepth(Settings.get().screenDepthsByTier()[Tier.Four]);
                buffers[i] = buffer;
            }
        }
        return buffers;
    }

    public synchronized Keyboard[] keyboards() {
        if (keyboards == null) {
            keyboards = new Keyboard[3];
            for (int i = 0; i < keyboards.length; i++) {
                final int index = i;
                ItemStack keyboardItem = Items.get(Constants.BlockName$.MODULE$.Keyboard()).createItemStack(1);
                Keyboard keyboard = (Keyboard) li.cil.oc.api.Driver.driverFor(keyboardItem, getClass()).createEnvironment(keyboardItem, this);
                keyboard.setUsableOverride((candidate, player) -> selectedRackSlot() == consoleSlots[index] && isRemoteUsable(player));
                keyboards[i] = keyboard;
            }
        }
        return keyboards;
    }

    @Override public TextBuffer buffer() {
        int index = consoleIndex(selectedRackSlot());
        return buffers()[index >= 0 ? index : 0];
    }

    @Override public Seq<String> sidedKeys() {
        if (!rack.getEnvironmentLevel().isClientSide()) return keys;
        var clientKeys = ExtendedDataComponentHolderSupportJava.convert(rack.getMountableData(slot))
            .getComponentOptional((DataComponentType<List<String>>) (DataComponentType<?>) OCComponentsJava.KEYS.get());
        return clientKeys.orElse(List$.MODULE$.empty());
    }

    public int selectedRackSlot() {
        if (!rack.getEnvironmentLevel().isClientSide()) return selectedSlot;
        return (Integer) rack.getMountableData(slot).getOrDefault((DataComponentType<Integer>) (DataComponentType<?>) OCComponentsJava.RACK_KVM_SELECTED_SLOT.get(), selectedSlot);
    }

    public int serverMask() {
        if (!rack.getEnvironmentLevel().isClientSide()) return computeServerMask();
        return (Integer) rack.getMountableData(slot).getOrDefault((DataComponentType<Integer>) (DataComponentType<?>) OCComponentsJava.RACK_KVM_SERVER_MASK.get(), 0);
    }

    public Option<TextBuffer> bufferAtRackSlot(int rackSlot) {
        int index = consoleIndex(rackSlot);
        return index >= 0 ? Option.apply(buffers()[index]) : Option.empty();
    }

    public boolean selectRackSlot(int rackSlot) {
        if (rack.getEnvironmentLevel().isClientSide() || !isServerAt(rackSlot)) return false;
        if (selectedSlot != rackSlot) {
            selectedSlot = rackSlot;
            rack.markChanged(slot);
            rack.markChanged();
        }
        return true;
    }

    @Override public boolean isBufferUsable(TextBuffer candidate, Player player) {
        return bufferAtRackSlot(selectedRackSlot()).contains(candidate) && isRemoteUsable(player);
    }

    private int consoleIndex(int rackSlot) {
        for (int i = 0; i < consoleSlots.length; i++) if (consoleSlots[i] == rackSlot) return i;
        return -1;
    }

    private boolean isServerAt(int rackSlot) {
        return rackSlot >= 0 && rackSlot < rack.getContainerSize() && rack.getMountable(rackSlot) instanceof Server;
    }

    private int computeServerMask() {
        int mask = 0;
        for (int i = 0; i < consoleSlots.length; i++) if (isServerAt(consoleSlots[i])) mask |= 1 << i;
        return mask;
    }

    @Override public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceAttribute.Class, DeviceClass.Generic.toString(),
                DeviceAttribute.Description, "Rack KVM",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "RemoteViewing KVM");
    }

    @Override public void onConnect(Node connectedNode) {
        if (connectedNode == node && !getEnvironmentLevel().isClientSide()) reconcileConsoles();
    }

    @Override public void onDisconnect(Node disconnectedNode) {
        if (disconnectedNode == node) {
            for (TextBuffer buffer : buffers()) buffer.node().remove();
            for (Keyboard keyboard : keyboards()) keyboard.node().remove();
            for (int i = 0; i < attachedServers.length; i++) attachedServers[i] = null;
        }
    }

    @Override public void onMessage(Message message) { }
    @Override public net.minecraft.world.level.Level getEnvironmentLevel() { return rack.getEnvironmentLevel(); }
    @Override public double xPosition() { return rack.xPosition(); }
    @Override public double yPosition() { return rack.yPosition(); }
    @Override public double zPosition() { return rack.zPosition(); }
    @Override public void markChanged() { rack.markChanged(); }

    @Override public void describeForClient(MutableDataComponentHolder holder) {
        if (node.address() == null) Network.joinNewNetwork(node);
        holder.set(OCComponentsJava.KEYS.get(), keys.toList());
        holder.set(OCComponentsJava.ADDRESS.get(), node.address());
        holder.set(OCComponentsJava.RACK_KVM_SELECTED_SLOT.get(), selectedSlot);
        holder.set(OCComponentsJava.RACK_KVM_SERVER_MASK.get(), computeServerMask());
    }

    @Override public int getConnectableCount() { return 0; }
    @Override public RackBusConnectable getConnectableAt(int index) { return null; }

    @Override public boolean onActivate(Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY) {
        if (player.isCrouching() && Items.get(heldItem) == Items.get(Constants.ItemName$.MODULE$.Terminal())) {
            if (!getEnvironmentLevel().isClientSide()) {
                String key = UUID.randomUUID().toString();
                keys.clear();
                keys.$plus$eq(key);
                heldItem.set(OCComponentsJava.TERMINAL_REFERENCE.get(), new TerminalReference(key, node.address()));
                rack.markChanged(slot);
                rack.markChanged();
                player.getInventory().setChanged();
            }
            return true;
        }
        return false;
    }

    @Override public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        if (!getEnvironmentLevel().isClientSide()) node.loadData(holder);
        for (int i = 0; i < buffers().length; i++) {
            DataComponentPatch bufferData = holder.get(bufferComponent(i));
            if (bufferData != null) buffers()[i].loadData(new CompoundStorage(DataComponentMap.EMPTY).andApply(bufferData));
            DataComponentPatch keyboardData = holder.get(keyboardComponent(i));
            if (keyboardData != null) keyboards()[i].loadData(new CompoundStorage(DataComponentMap.EMPTY).andApply(keyboardData));
        }
        keys.clear();
        List<String> loadedKeys = (List<String>) holder.get((DataComponentType<List<String>>) (DataComponentType<?>) OCComponentsJava.KEYS.get());
        if (loadedKeys != null) keys.$plus$plus$eq(loadedKeys);
        Integer loadedSlot = (Integer) holder.getOrDefault((DataComponentType<Integer>) (DataComponentType<?>) OCComponentsJava.RACK_KVM_SELECTED_SLOT.get(), -1);
        selectedSlot = loadedSlot;
    }

    @Override public void saveData(MutableDataComponentHolder holder) {
        node.saveData(holder);
        for (int i = 0; i < buffers().length; i++) {
            CompoundStorage bufferData = new CompoundStorage(DataComponentMap.EMPTY);
            buffers()[i].saveData(bufferData);
            holder.set(bufferComponent(i), bufferData.toPatch());
            CompoundStorage keyboardData = new CompoundStorage(DataComponentMap.EMPTY);
            keyboards()[i].saveData(keyboardData);
            holder.set(keyboardComponent(i), keyboardData.toPatch());
        }
        holder.set(OCComponentsJava.KEYS.get(), keys.toList());
        holder.set(OCComponentsJava.RACK_KVM_SELECTED_SLOT.get(), selectedSlot);
    }

    private net.minecraft.core.component.DataComponentType<DataComponentPatch> bufferComponent(int index) {
        return switch (index) {
            case 0 -> OCComponentsJava.RACK_KVM_BUFFER_0.get();
            case 1 -> OCComponentsJava.RACK_KVM_BUFFER_1.get();
            case 2 -> OCComponentsJava.RACK_KVM_BUFFER_2.get();
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    private net.minecraft.core.component.DataComponentType<DataComponentPatch> keyboardComponent(int index) {
        return switch (index) {
            case 0 -> OCComponentsJava.RACK_KVM_KEYBOARD_0.get();
            case 1 -> OCComponentsJava.RACK_KVM_KEYBOARD_1.get();
            case 2 -> OCComponentsJava.RACK_KVM_KEYBOARD_2.get();
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override public boolean canUpdate() { return true; }

    @Override public void update() {
        if (!getEnvironmentLevel().isClientSide()) reconcileConsoles();
        for (TextBuffer buffer : buffers()) buffer.update();
    }

    private void reconcileConsoles() {
        int mask = computeServerMask();
        if (mask != lastServerMask) {
            lastServerMask = mask;
            rack.markChanged(slot);
        }
        if (selectedSlot < 0) {
            for (int rackSlot : consoleSlots) if (isServerAt(rackSlot)) {
                selectedSlot = rackSlot;
                rack.markChanged(slot);
                break;
            }
        }
        for (int i = 0; i < buffers().length; i++) {
            Node screenNode = buffers()[i].node();
            Node keyboardNode = keyboards()[i].node();
            if (screenNode.network() == null) Network.joinNewNetwork(screenNode);
            screenNode.connect(keyboardNode);
            Object mountable = rack.getMountable(consoleSlots[i]);
            Node target = mountable instanceof Server server ? server.machine().node() : null;
            if (attachedServers[i] != target) {
                if (attachedServers[i] != null) screenNode.disconnect(attachedServers[i]);
                attachedServers[i] = target;
                if (target != null) screenNode.connect(target);
            }
        }
    }

    @Override public EnumSet<StateAware.State> getCurrentState() {
        return EnumSet.noneOf(StateAware.State.class);
    }

    @Override public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        Node[] result = new Node[6];
        for (int i = 0; i < 3; i++) {
            result[i] = buffers()[i].node();
            result[i + 3] = keyboards()[i].node();
        }
        return result;
    }

    @Override public void onLifecycleStateChange(Lifecycle.LifecycleState state) {
        if (!rack.getEnvironmentLevel().isClientSide()) return;
        if (state == Lifecycle.LifecycleState.Initialized) TerminalServer.loaded.add(this);
        else if (state == Lifecycle.LifecycleState.Disposed) TerminalServer.loaded.remove(this);
    }
}
