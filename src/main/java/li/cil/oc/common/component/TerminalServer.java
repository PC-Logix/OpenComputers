package li.cil.oc.common.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.internal.Rack;
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
import li.cil.oc.common.component.TerminalServer.TerminalServerCache;
import li.cil.oc.common.datacomponents.CompoundStorage;
import li.cil.oc.common.datacomponents.TerminalReference;
import li.cil.oc.util.ExtendedDataComponentHolder;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TerminalServer implements Environment, EnvironmentHost, Analyzable, RackMountable, Lifecycle, DeviceInfo, RemoteTerminalHost {
    public static final TerminalServerCache loaded = new TerminalServerCache();

    private final Rack rack;
    private final int slot;
    private final Node node;
    private final double range;
    private final ListBuffer<String> keys = ListBuffer$.MODULE$.empty();
    private TextBuffer buffer;
    private Keyboard keyboard;

    public TerminalServer(Rack rack, int slot) {
        this.rack = rack;
        this.slot = slot;
        this.node = Network.newNode(this, Visibility.None).create();
        this.range = Settings.get().maxWirelessRange()[Tier.Two];
    }

    @Override public Rack rack() { return rack; }
    @Override public int slot() { return slot; }
    @Override public Node node() { return node; }
    @Override public double range() { return range; }
    public ListBuffer<String> keys() { return keys; }

    @Override public synchronized TextBuffer buffer() {
        if (buffer == null) {
            ItemStack screenItem = Items.get(Constants.BlockName$.MODULE$.ScreenTier1()).createItemStack(1);
            buffer = (TextBuffer) li.cil.oc.api.Driver.driverFor(screenItem, getClass()).createEnvironment(screenItem, this);
            scala.Tuple2<Object, Object> resolution = Settings.get().screenResolutionsByTier()[Tier.Four];
            buffer.setMaximumResolution(((Number) resolution._1()).intValue(), ((Number) resolution._2()).intValue());
            buffer.setMaximumColorDepth(Settings.get().screenDepthsByTier()[Tier.Four]);
        }
        return buffer;
    }

    public synchronized Keyboard keyboard() {
        if (keyboard == null) {
            ItemStack keyboardItem = Items.get(Constants.BlockName$.MODULE$.Keyboard()).createItemStack(1);
            keyboard = (Keyboard) li.cil.oc.api.Driver.driverFor(keyboardItem, getClass()).createEnvironment(keyboardItem, this);
            keyboard.setUsableOverride((candidate, player) -> isRemoteUsable(player));
        }
        return keyboard;
    }

    @Override public Seq<String> sidedKeys() {
        if (!rack.getEnvironmentLevel().isClientSide()) return keys;
        List<String> clientKeys = (List<String>) rack.getMountableData(slot).get((DataComponentType<List<String>>) (DataComponentType<?>) OCComponentsJava.KEYS.get());
        return clientKeys != null ? clientKeys : List$.MODULE$.empty();
    }

    @Override public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceAttribute.Class, DeviceClass.Generic.toString(),
                DeviceAttribute.Description, "Terminal server",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "RemoteViewing EX");
    }

    @Override public void onConnect(Node connectedNode) {
        if (connectedNode == node) {
            connectedNode.connect(buffer().node());
            connectedNode.connect(keyboard().node());
            buffer().node().connect(keyboard().node());
        }
    }

    @Override public void onDisconnect(Node disconnectedNode) {
        if (disconnectedNode == node) {
            buffer().node().remove();
            keyboard().node().remove();
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
    }

    @Override public int getConnectableCount() { return 0; }
    @Override public RackBusConnectable getConnectableAt(int index) { return null; }

    @Override public boolean onActivate(Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY) {
        if (player.isCrouching() && Items.get(heldItem) == Items.get(Constants.ItemName$.MODULE$.Terminal())) {
            if (!getEnvironmentLevel().isClientSide()) {
                String key = UUID.randomUUID().toString();
                TerminalReference previous = heldItem.get(OCComponentsJava.TERMINAL_REFERENCE.get());
                if (previous != null) keys.$minus$eq(previous.key());
                int maxSize = Settings.get().terminalsPerServer();
                while (keys.size() >= maxSize) keys.remove(0);
                keys.$plus$eq(key);
                heldItem.set(OCComponentsJava.TERMINAL_REFERENCE.get(), new TerminalReference(key, node.address()));
                rack.markChanged(slot);
                player.getInventory().setChanged();
            }
            return true;
        }
        return false;
    }

    @Override public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        if (!rack.getEnvironmentLevel().isClientSide()) node.loadData(holder);
        DataComponentPatch bufferData = holder.get(OCComponentsJava.TERMINAL_SERVER_BUFFER.get());
        if (bufferData != null) buffer().loadData(new CompoundStorage(DataComponentMap.EMPTY).andApply(bufferData));
        else buffer().loadData(holder);
        DataComponentPatch keyboardData = holder.get(OCComponentsJava.TERMINAL_SERVER_KEYBOARD.get());
        if (keyboardData != null) keyboard().loadData(new CompoundStorage(DataComponentMap.EMPTY).andApply(keyboardData));
        else keyboard().loadData(holder);

        keys.clear();
        List<String> loadedKeys = (List<String>) holder.get((DataComponentType<List<String>>) (DataComponentType<?>) OCComponentsJava.KEYS.get());
        if (loadedKeys != null) keys.$plus$plus$eq(loadedKeys);
    }

    @Override public void saveData(MutableDataComponentHolder holder) {
        node.saveData(holder);
        CompoundStorage bufferData = new CompoundStorage(DataComponentMap.EMPTY);
        buffer().saveData(bufferData);
        holder.set(OCComponentsJava.TERMINAL_SERVER_BUFFER.get(), bufferData.toPatch());
        CompoundStorage keyboardData = new CompoundStorage(DataComponentMap.EMPTY);
        keyboard().saveData(keyboardData);
        holder.set(OCComponentsJava.TERMINAL_SERVER_KEYBOARD.get(), keyboardData.toPatch());
        holder.set(OCComponentsJava.KEYS.get(), keys.toList());
    }

    @Override public boolean canUpdate() { return true; }

    @Override public void update() {
        if (getEnvironmentLevel().isClientSide() || (node.address() != null && node.network() != null)) buffer().update();
    }

    @Override public EnumSet<StateAware.State> getCurrentState() {
        return EnumSet.noneOf(StateAware.State.class);
    }

    @Override public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{buffer().node(), keyboard().node()};
    }

    @Override public void onLifecycleStateChange(Lifecycle.LifecycleState state) {
        if (!rack.getEnvironmentLevel().isClientSide()) return;
        if (state == Lifecycle.LifecycleState.Initialized) loaded.add(this);
        else if (state == Lifecycle.LifecycleState.Disposed) loaded.remove(this);
    }

    public static final class TerminalServerCache {
        private final Map<String, RemoteTerminalHost> ready = new HashMap<>();
        private final ArrayList<RemoteTerminalHost> pending = new ArrayList<>();

        private void completePending() {
            ArrayList<RemoteTerminalHost> promoted = new ArrayList<>();
            for (RemoteTerminalHost terminal : pending) if (terminal.hasAddress()) promoted.add(terminal);
            for (RemoteTerminalHost terminal : promoted) {
                pending.remove(terminal);
                String address = terminal.address();
                if (!ready.containsKey(address)) ready.put(address, terminal);
            }
        }

        public boolean add(RemoteTerminalHost terminal) {
            completePending();
            if (terminal.hasAddress()) {
                String address = terminal.address();
                if (ready.containsKey(address)) return false;
                ready.put(address, terminal);
                return true;
            }
            pending.add(terminal);
            return true;
        }

        public boolean remove(RemoteTerminalHost terminal) {
            completePending();
            if (terminal.hasAddress()) return ready.remove(terminal.address()) != null;
            return pending.remove(terminal);
        }

        public void clear() {
            ready.clear();
            pending.clear();
        }

        public Option<RemoteTerminalHost> find(String address) {
            completePending();
            return Option.apply(ready.get(address));
        }
    }
}