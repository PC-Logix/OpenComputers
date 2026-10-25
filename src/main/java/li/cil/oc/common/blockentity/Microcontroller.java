package li.cil.oc.common.blockentity;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.blockentity.traits.Hub.Plug;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.Network;
import li.cil.oc.api.Items;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.Tier;
import li.cil.oc.common.datacomponents.CompoundStorage;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.ExtendedDataComponentHolderSupportJava;
import li.cil.oc.util.StackOption;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Option$;
import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;

import java.util.Optional;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;

public class Microcontroller extends MicrocontrollerBlockEntity {
    private final li.cil.oc.common.blockentity.traits.ComputerStateJava computerState =
        new li.cil.oc.common.blockentity.traits.ComputerStateJava();
    @Override public li.cil.oc.common.blockentity.traits.ComputerStateJava computerState() { return computerState; }
    private final li.cil.oc.common.blockentity.traits.HubStateJava hubState =
        new li.cil.oc.common.blockentity.traits.HubStateJava(this);
    @Override public li.cil.oc.common.blockentity.traits.HubStateJava hubState() { return hubState; }
    private static final String INFO_TAG = Settings.namespace() + "info";
    private static final String OUTPUTS_TAG = Settings.namespace() + "outputs";
    private static final String COMPONENT_NODES_TAG = Settings.namespace() + "componentNodes";
    private static final String SNOOPER_TAG = Settings.namespace() + "snooper";

    private final MicrocontrollerData info = new MicrocontrollerData();
    private final boolean[] outputSides = {true, true, true, true, true, true};
    private final ComponentConnector snooperNode = Network.newNode(this, Visibility.Network)
            .withComponent("microcontroller").withConnector(Settings.get().bufferMicrocontroller()).create();
    private final Component[] componentNodes;
    private Map<String, String> deviceInfo;

    private static Component[] createComponentNodes(Microcontroller host) {
        Component[] nodes = new Component[6];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = Network.newNode(host, Visibility.Network).withComponent("microcontroller").create();
        }
        return nodes;
    }

    public Microcontroller(net.minecraft.core.BlockPos pos, BlockState state) {
        super(pos, state);
        componentNodes = createComponentNodes(this);
        if (machine() != null) {
            ((Connector) machine().node()).setLocalBufferSize(0);
            machine().setCostPerTick(Settings.get().microcontrollerCost());
        }
    }

    public MicrocontrollerData info() { return info; }
    public boolean[] outputSides() { return outputSides; }
    public ComponentConnector snooperNode() { return snooperNode; }
    public Component[] componentNodes() { return componentNodes; }

    @Override public Node node() { return null; }
    @Override public int tier() { return info.tier; }
    @Override public Option<String> runSound() { return Option$.MODULE$.empty(); }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = BlockEntityDeviceInfo.microcontroller(Integer.toString(getContainerSize()));
        return deviceInfo;
    }

    @OnlyIn(Dist.CLIENT)
    @Override public boolean canConnect(Direction side) { return side != facing(); }

    @Override
    public Node sidedNode(Direction side) {
        return side != facing() ? super.sidedNode(side) : null;
    }

    @OnlyIn(Dist.CLIENT)
    @Override public boolean hasConnector(Direction side) { return side != facing(); }

    @Override
    public java.util.Optional<Connector> connectorOptional(Direction side) {
        return java.util.Optional.ofNullable(side != facing() ? snooperNode : null);
    }

    @Override public double energyThroughput() { return Settings.get().caseRate()[Tier.One]; }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        super.onAnalyze(player, side, hitX, hitY, hitZ);
        return side != facing() ? new Node[]{componentNodes[side.get3DDataValue()]} : new Node[]{machine().node()};
    }

    @Override public java.lang.Iterable<ItemStack> internalComponents() { return Arrays.asList(info.components); }

    @Override
    public int componentSlot(String address) {
        Optional<?>[] slots = ComponentInventorySlotAccessJava.snapshot(this);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].isPresent()) {
                var environment = (li.cil.oc.api.network.ManagedEnvironment) slots[i].get();
                if (environment.node() != null && address.equals(environment.node().address())) return i;
            }
        }
        return -1;
    }

    @Callback(doc = "function():boolean -- Starts the microcontroller. Returns true if the state changed.")
    public Object[] start(Context context, Arguments args) { return new Object[]{!machine().isPaused() && machine().start()}; }

    @Callback(doc = "function():boolean -- Stops the microcontroller. Returns true if the state changed.")
    public Object[] stop(Context context, Arguments args) { return new Object[]{machine().stop()}; }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the microcontroller is running.")
    public Object[] isRunning(Context context, Arguments args) { return new Object[]{machine().isRunning()}; }

    @Callback(direct = true, doc = "function():string -- Returns the reason the microcontroller crashed, if applicable.")
    public Object[] lastError(Context context, Arguments args) { return new Object[]{machine().lastError()}; }

    @Callback(direct = true, doc = "function(side:number):boolean -- Get whether network messages are sent via the specified side.")
    public Object[] isSideOpen(Context context, Arguments args) {
        Direction side = new ExtendedArgumentsJava(args).checkSideExcept(0, facing());
        return new Object[]{outputSides[side.ordinal()]};
    }

    @Callback(doc = "function(side:number, open:boolean):boolean -- Set whether network messages are sent via the specified side.")
    public Object[] setSideOpen(Context context, Arguments args) {
        Direction side = new ExtendedArgumentsJava(args).checkSideExcept(0, facing());
        boolean oldValue = outputSides[side.ordinal()];
        outputSides[side.ordinal()] = args.checkBoolean(1);
        return new Object[]{oldValue};
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isServer() && getLevel().getGameTime() % Settings.get().tickFrequency() == 0) {
            for (Direction side : Direction.values()) {
                if (side == facing()) continue;
                Node sideNode = sidedNode(side);
                if (sideNode instanceof Connector connector) {
                    double demand = snooperNode.globalBufferSize() - snooperNode.globalBuffer();
                    double available = demand + connector.changeBuffer(-demand);
                    snooperNode.changeBuffer(available);
                }
            }
        }
    }

    @Override
    public void connectItemNode(Node node) {
        if (machine() != null && machine().node() != null && node != null) {
            Network.joinNewNetwork(machine().node());
            machine().node().connect(node);
        }
    }

    @Override public Node createNode(Plug plug) {
        return Network.newNode(plug, Visibility.Network).withConnector().create();
    }

    @Override
    public void onPlugConnect(Plug plug, Node node) {
        super.onPlugConnect(plug, node);
        if (node == plug.node()) {
            Network.joinNewNetwork(machine().node());
            machine().node().connect(snooperNode);
            connectComponents();
        }
        if (plug.isPrimary()) plug.node().connect(componentNodes[plug.side().ordinal()]);
        else componentNodes[plug.side().ordinal()].remove();
    }

    @Override
    public void onPlugDisconnect(Plug plug, Node node) {
        super.onPlugDisconnect(plug, node);
        if (plug.isPrimary() && node != plug.node()) plug.node().connect(componentNodes[plug.side().ordinal()]);
        else componentNodes[plug.side().ordinal()].remove();
        if (node == plug.node()) disconnectComponents();
    }

    @Override
    public void onPlugMessage(Plug plug, Message message) {
        if ("network.message".equals(message.name()) && message.source().network() != snooperNode.network()) {
            snooperNode.sendToReachable(message.name(), (Object[]) message.data());
        }
    }

    @Override
    public void onMessage(Message message) {
        if ("network.message".equals(message.name()) && message.source().network() == snooperNode.network()) {
            for (Direction side : Direction.values()) {
                if (outputSides[side.ordinal()] && side != facing()) {
                    Node sideNode = sidedNode(side);
                    if (sideNode != null) sideNode.sendToReachable(message.name(), (Object[]) message.data());
                }
            }
        }
    }

    @Override
    public void loadComponentsForServer(DataComponentHolder holder) {
        try {
            ExtendedDataComponentHolderSupportJava.convert(holder)
                .getComponentOptional(OCComponentsJava.COMPONENT_NODES.get()).ifPresent(savedNodes -> {
                scala.collection.immutable.List<?> nodes = (scala.collection.immutable.List<?>) savedNodes;
                int index = 0;
                for (scala.collection.Iterator<?> it = nodes.iterator(); it.hasNext(); index++) {
                    Option<?> saved = (Option<?>) it.next();
                    if (index < componentNodes.length && saved.isDefined()) {
                        try {
                            componentNodes[index].loadData((CompoundStorage) saved.get());
                        } catch (UnrecoverablePersistanceException e) {
                            throw new IllegalStateException("Unable to restore microcontroller network state", e);
                        }
                    }
                }
            });
            snooperNode.loadData(holder);
        } catch (UnrecoverablePersistanceException e) {
            throw new IllegalStateException("Unable to restore microcontroller network state", e);
        }
        super.loadComponentsForServer(holder);
        Network.joinNewNetwork(machine().node());
        machine().node().connect(snooperNode);
    }

    @Override
    public void saveComponentsForServer(MutableDataComponentHolder holder) {
        super.saveComponentsForServer(holder);
        scala.collection.mutable.ArrayBuffer<Option<CompoundStorage>> saved = scala.collection.mutable.ArrayBuffer$.MODULE$.empty();
        for (Component component : componentNodes) {
            if (component instanceof Node node) {
                CompoundStorage storage = new CompoundStorage();
                node.saveData(storage);
                saved.$plus$eq(Option$.MODULE$.apply(storage));
            } else saved.$plus$eq(Option$.MODULE$.empty());
        }
        ExtendedDataComponentHolderSupportJava.convert(holder).setComponent(
                OCComponentsJava.COMPONENT_NODES.get(), saved.toList());
    }

    @Override public void loadComponentsCommon(DataComponentHolder holder) { super.loadComponentsCommon(holder); info.loadData(holder); }
    @Override public void saveComponentsCommon(MutableDataComponentHolder holder) { super.saveComponentsCommon(holder); info.saveData(holder); }
    @Override public ItemStack[] items() { return info.components; }
    @Override public void updateItems(int slot, ItemStack stack) { info.components[slot] = stack; }
    @Override public int getContainerSize() { return info.components.length; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
    @Override public void setItem(int slot, ItemStack stack) { }
    @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
    @Override public void forAllLoot(Consumer<ItemStack> destination) { }
    @Override public boolean dropSlot(int slot, int count, Option<Direction> direction) { return false; }
    @Override public void dropAllSlots() { }

    public StackOption changeEEPROM(ItemStack newEeprom) {
        int oldIndex = -1;
        for (int i = 0; i < info.components.length; i++) {
            if (Items.get(info.components[i]) == Items.get(Constants.ItemName$.MODULE$.EEPROM())) { oldIndex = i; break; }
        }
        if (oldIndex >= 0) {
            ItemStack oldEeprom = info.components[oldIndex];
            super.setItem(oldIndex, newEeprom);
            return StackOption.apply(oldEeprom);
        }
        assert info.components[getContainerSize() - 1].isEmpty();
        super.setItem(getContainerSize() - 1, newEeprom);
        return StackOption.empty();
    }
}


