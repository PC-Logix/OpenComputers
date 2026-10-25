package li.cil.oc.common.blockentity;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import dan200.computercraft.api.peripheral.IComputerAccess;
import com.google.common.base.Charsets;
import li.cil.oc.Settings;
import li.cil.oc.SettingsAccessJava;
import li.cil.oc.api.Driver;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.blockentity.traits.Hub;
import li.cil.oc.integration.Mods;
import li.cil.oc.server.ActivityPacketSender;
import li.cil.oc.server.network.QuantumNetwork;
import li.cil.oc.util.ExtendedCompoundTag;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import scala.Option;
import scala.Option$;
import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.ArrayBuffer$;
import scala.collection.mutable.Map;
import scala.collection.mutable.Map$;
import scala.collection.mutable.Set;
import scala.collection.mutable.Set$;
import scala.runtime.BoxedUnit;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/** Multi-port relay with optional wireless, tunnel, and ComputerCraft forwarding. */
public class Relay extends RelayBlockEntity {
    @Override public Node node() { return null; }
    private final li.cil.oc.common.blockentity.traits.HubStateJava hubState =
        new li.cil.oc.common.blockentity.traits.HubStateJava(this);
    @Override public li.cil.oc.common.blockentity.traits.HubStateJava hubState() { return hubState; }
    @Override public boolean powerLifecycleAfterEnvironment() { return true; }
    @Override public boolean powerLifecycleAfterHub() { return true; }
    private final ItemInfo WirelessNetworkCardTier1 = li.cil.oc.api.Items.get(li.cil.oc.Constants.ItemName$.MODULE$.WirelessNetworkCardTier1());
    private final ItemInfo WirelessNetworkCardTier2 = li.cil.oc.api.Items.get(li.cil.oc.Constants.ItemName$.MODULE$.WirelessNetworkCardTier2());
    private final ItemInfo LinkedCard = li.cil.oc.api.Items.get(li.cil.oc.Constants.ItemName$.MODULE$.LinkedCard());
    private double strength;
    private boolean isRepeater = true;
    private int wirelessTier = -1;
    private boolean isLinkedEnabled;
    private String tunnel = "creative";
    private final Component[] componentNodes = new Component[6];
    private final Map<Object, Set<Object>> openPorts = Map$.MODULE$.empty();
    private long lastMessage;
    private final ArrayBuffer<Object> computers = ArrayBuffer$.MODULE$.empty();

    private static final String StrengthTag = Settings.namespace() + "strength";
    private static final String IsRepeaterTag = Settings.namespace() + "isRepeater";
    private static final String ComponentNodesTag = Settings.namespace() + "componentNodes";

    public Relay(BlockPos pos, BlockState state) {
        super(pos, state);
        strength = maxWirelessRange();
        for (int i = 0; i < componentNodes.length; i++) {
            componentNodes[i] = li.cil.oc.api.Network.newNode(this, Visibility.Network).withComponent("relay").create();
        }
    }

    public ItemInfo WirelessNetworkCardTier1() { return WirelessNetworkCardTier1; }
    public ItemInfo WirelessNetworkCardTier2() { return WirelessNetworkCardTier2; }
    public ItemInfo LinkedCard() { return LinkedCard; }
    public Level getWirelessLevel() { return getLevel(); }
    public double strength() { return strength; }
    public void strength_$eq(double value) { strength = value; }
    public boolean isRepeater() { return isRepeater; }
    public void isRepeater_$eq(boolean value) { isRepeater = value; }
    public int wirelessTier() { return wirelessTier; }
    public void wirelessTier_$eq(int value) { wirelessTier = value; }
    public boolean isWirelessEnabled() { return wirelessTier >= Tier.One; }
    public boolean isLinkedEnabled() { return isLinkedEnabled; }
    public void isLinkedEnabled_$eq(boolean value) { isLinkedEnabled = value; }
    public String tunnel() { return tunnel; }
    public void tunnel_$eq(String value) { tunnel = value; }
    public Component[] componentNodes() { return componentNodes; }
    public Map<Object, Set<Object>> openPorts() { return openPorts; }
    public long lastMessage() { return lastMessage; }
    public void lastMessage_$eq(long value) { lastMessage = value; }
    public ArrayBuffer<Object> computers() { return computers; }
    public double maxWirelessRange() {
        return wirelessTier == Tier.One || wirelessTier == Tier.Two ? Settings.get().maxWirelessRange()[wirelessTier] : 0;
    }
    public double wirelessCostPerRange() {
        return wirelessTier == Tier.One || wirelessTier == Tier.Two ? Settings.get().wirelessCostPerRange()[wirelessTier] : 0;
    }

    public void onSwitchActivity() {
        long now = System.currentTimeMillis();
        if (now - lastMessage >= (relayDelay() - 1) * 50L) {
            lastMessage = now;
            try {
                ActivityPacketSender.sendSwitchActivity(this, Settings.get().maxNetworkClientEffectPacketDistance());
            } catch (java.io.IOException error) {
                Relay.<RuntimeException>throwUnchecked(error);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override public boolean hasConnector(Direction side) { return true; }
    @Override
    public java.util.Optional<Connector> connectorOptional(Direction side) {
        Node node = sidedNode(side);
        return java.util.Optional.ofNullable(node instanceof Connector connector ? connector : null);
    }
    @Override public double energyThroughput() { return Settings.get().accessPointRate(); }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        if (!isWirelessEnabled()) return null;
        player.sendSystemMessage(li.cil.oc.Localization.Analyzer.WirelessStrength(strength));
        return new Node[]{componentNodes[side.get3DDataValue()]};
    }

    @Callback(direct = true, doc = "function():number -- Get the signal strength (range) used when relaying messages.")
    public synchronized Object[] getStrength(Context context, Arguments args) { return ResultWrapper.result(strength); }
    @Callback(doc = "function(strength:number):number -- Set the signal strength (range) used when relaying messages.")
    public synchronized Object[] setStrength(Context context, Arguments args) {
        strength = Math.max(0, Math.min(args.checkDouble(0), maxWirelessRange())); return ResultWrapper.result(strength);
    }
    @Callback(direct = true, doc = "function():boolean -- Get whether the access point currently acts as a repeater (resend received wireless packets wirelessly).")
    public synchronized Object[] isRepeater(Context context, Arguments args) { return ResultWrapper.result(isRepeater); }
    @Callback(doc = "function(enabled:boolean):boolean -- Set whether the access point should act as a repeater.")
    public synchronized Object[] setRepeater(Context context, Arguments args) {
        isRepeater = args.checkBoolean(0); return ResultWrapper.result(isRepeater);
    }

    private void queueComputerCraftMessage(String source, String destination, int port, int answerPort, Object[] args) {
        for (Iterator<Object> iterator = computers.iterator(); iterator.hasNext();) {
            Object value = iterator.next();
            if (!(value instanceof IComputerAccess computer)) continue;
            String address = "cc" + computer.getID() + "_" + computer.getAttachmentName();
            if (source.equals(address) || (destination != null && !destination.equals(address))) continue;
            Object ports = openPorts.apply(computer);
            if (!(ports instanceof scala.collection.mutable.Set<?>)) continue;
            @SuppressWarnings("unchecked") Set<Object> open = (Set<Object>) ports;
            if (!open.contains(Integer.valueOf(port))) continue;
            Object[] payload = new Object[args.length];
            for (int i = 0; i < args.length; i++) payload[i] = args[i] instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : args[i];
            Object data = payload.length > 1 ? payload : payload[0];
            computer.queueEvent("modem_message", computer.getAttachmentName(), port, answerPort, data);
        }
    }

    @Override
    public void receivePacket(Packet packet, li.cil.oc.api.network.WirelessEndpoint source) {
        if (isWirelessEnabled()) tryEnqueuePacketJava(Optional.empty(), packet);
    }
    @Override
    public void receivePacket(Packet packet) {
        if (isLinkedEnabled) tryEnqueuePacketJava(Optional.empty(), packet);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean tryEnqueuePacketJava(Optional<Direction> sourceSide, Packet packet) {
        if (Mods.ComputerCraft().isModAvailable()) {
            Object[] data = packet.data();
            if (data.length > 0 && data[0] instanceof Double answerPort)
                queueComputerCraftMessage(packet.source(), packet.destination(), packet.port(), answerPort.intValue(), Arrays.copyOfRange(data, 1, data.length));
            else queueComputerCraftMessage(packet.source(), packet.destination(), packet.port(), -1, data);
        }
        return super.tryEnqueuePacketJava(sourceSide, packet);
    }

    @Override
    public void relayPacketJava(Optional<Direction> sourceSide, Packet packet) {
        super.relayPacketJava(sourceSide, packet);
        java.util.function.DoublePredicate tryChangeBuffer = sourceSide.isPresent()
                ? amount -> ((Connector) plugs()[sourceSide.get().ordinal()].node()).tryChangeBuffer(amount)
                : amount -> {
                    for (Hub.Plug plug : plugs()) if (((Connector) plug.node()).tryChangeBuffer(amount)) return true;
                    return false;
                };
        if (isWirelessEnabled() && strength > 0 && (sourceSide.isPresent() || isRepeater)) {
            if (tryChangeBuffer.test(-strength * wirelessCostPerRange())) li.cil.oc.api.Network.sendWirelessPacket(this, strength, packet);
        }
        if (isLinkedEnabled && sourceSide.isPresent()) {
            double cost = packet.size() / 32.0 + wirelessCostPerRange() * maxWirelessRange() * 5;
            if (tryChangeBuffer.test(-cost)) {
                Iterator<QuantumNetwork.QuantumNode> endpoints = QuantumNetwork.getEndpoints(tunnel).iterator();
                while (endpoints.hasNext()) {
                    QuantumNetwork.QuantumNode endpoint = endpoints.next();
                    if (endpoint != this) endpoint.receivePacket(packet);
                }
            }
        }
        onSwitchActivity();
    }

    @Override
    public Connector createNode(Hub.Plug plug) {
        return (Connector) li.cil.oc.api.Network.newNode(plug, Visibility.Network)
                .withConnector((double) Math.round(Settings.get().bufferAccessPoint())).create();
    }

    @Override
    public void onPlugConnect(Hub.Plug plug, Node node) {
        super.onPlugConnect(plug, node);
        if (node == plug.node()) li.cil.oc.api.Network.joinWirelessNetwork(this);
        if (plug.isPrimary()) plug.node().connect(componentNodes[plug.side().ordinal()]);
        else componentNodes[plug.side().ordinal()].remove();
    }

    @Override
    public void onPlugDisconnect(Hub.Plug plug, Node node) {
        super.onPlugDisconnect(plug, node);
        if (node == plug.node()) li.cil.oc.api.Network.leaveWirelessNetwork(this);
        if (plug.isPrimary() && node != plug.node()) plug.node().connect(componentNodes[plug.side().ordinal()]);
        else componentNodes[plug.side().ordinal()].remove();
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) { super.onItemAdded(slot, stack); updateLimits(slot, stack); }
    private void updateLimits(int slot, ItemStack stack) {
        var driver = Driver.driverFor(stack, getClass());
        if (driver == null) return;
        String slotType = driver.slot(stack);
        if (Slot.CPU.equals(slotType)) relayDelay_$eq(Math.max(1, SettingsAccessJava.get().switchDefaultRelayDelay() -
                (int) ((driver.tier(stack) + 1) * SettingsAccessJava.get().switchRelayDelayUpgrade())));
        else if (Slot.Memory.equals(slotType)) {
            int bonus;
            if (stack.getItem() instanceof li.cil.oc.common.item.Memory memory) bonus = (memory.tier() + 1) * SettingsAccessJava.get().switchRelayAmountUpgrade();
            else bonus = (driver.tier(stack) + 1) * (SettingsAccessJava.get().switchRelayAmountUpgrade() * 2);
            relayAmount_$eq(Math.max(1, SettingsAccessJava.get().switchDefaultRelayAmount() + bonus));
        }
        else if (Slot.HDD.equals(slotType)) maxQueueSize_$eq(Math.max(1,
                SettingsAccessJava.get().switchDefaultMaxQueueSize() + (driver.tier(stack) + 1) * SettingsAccessJava.get().switchQueueSizeUpgrade()));
        else if (Slot.Card.equals(slotType)) {
            ItemInfo descriptor = li.cil.oc.api.Items.get(stack);
            if (descriptor == WirelessNetworkCardTier1 || descriptor == WirelessNetworkCardTier2)
                wirelessTier = descriptor == WirelessNetworkCardTier1 ? Tier.One : Tier.Two;
            if (descriptor == LinkedCard) {
                var component = stack.get(OCComponentsJava.TUNNEL.get());
                if (component != null) { tunnel = component; isLinkedEnabled = true; QuantumNetwork.add(this); }
            }
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        super.onItemRemoved(slot, stack);
        var driver = Driver.driverFor(stack, getClass());
        String slotType = driver.slot(stack);
        if (Slot.CPU.equals(slotType)) relayDelay_$eq(SettingsAccessJava.get().switchDefaultRelayDelay());
        else if (Slot.Memory.equals(slotType)) relayAmount_$eq(SettingsAccessJava.get().switchDefaultRelayAmount());
        else if (Slot.HDD.equals(slotType)) maxQueueSize_$eq(SettingsAccessJava.get().switchDefaultMaxQueueSize());
        else if (Slot.Card.equals(slotType)) {
            wirelessTier = -1; isLinkedEnabled = false; QuantumNetwork.remove(this);
        }
    }

    @Override public int getContainerSize() { return InventorySlots.relay.length; }
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        var driver = Driver.driverFor(stack, getClass());
        if (driver == null) return false;
        InventorySlots.InventorySlot provided = InventorySlots.relay[slot];
        boolean tierSatisfied = driver.slot(stack).equals(provided.slot()) && driver.tier(stack) <= provided.tier();
        boolean cardTypeSatisfied = !Slot.Card.equals(provided.slot()) ||
                li.cil.oc.api.Items.get(stack) == WirelessNetworkCardTier1 ||
                li.cil.oc.api.Items.get(stack) == WirelessNetworkCardTier2 || li.cil.oc.api.Items.get(stack) == LinkedCard;
        return tierSatisfied && cardTypeSatisfied;
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.menu.Relay(id, playerInventory, this);
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        for (int slot = 0; slot < items().length; slot++) if (!items()[slot].isEmpty()) updateLimits(slot, items()[slot]);
        if (nbt.contains(StrengthTag)) strength = Math.max(0, Math.min(nbt.getDouble(StrengthTag), maxWirelessRange()));
        if (nbt.contains(IsRepeaterTag)) isRepeater = nbt.getBoolean(IsRepeaterTag);
        ListTag list = nbt.getList(ComponentNodesTag, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), componentNodes.length); i++) {
            try { componentNodes[i].loadData(list.getCompound(i), provider); }
            catch (li.cil.oc.api.UnrecoverablePersistanceException error) { Relay.<RuntimeException>throwUnchecked(error); }
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E { throw (E) error; }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        nbt.putDouble(StrengthTag, strength); nbt.putBoolean(IsRepeaterTag, isRepeater);
        ListTag list = new ListTag();
        for (Component component : componentNodes) {
            CompoundTag tag = new CompoundTag();
            if (component instanceof Node node) node.saveData(tag, provider);
            list.add(tag);
        }
        nbt.put(ComponentNodesTag, list);
    }
}


