package li.cil.oc.common.blockentity;

import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.client.renderer.block.NetSplitterModel;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.blockentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.server.BlockSyncPacketSender;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import scala.collection.mutable.HashMap;
import scala.collection.mutable.Map;

import java.io.IOException;
import java.io.UncheckedIOException;

public class NetSplitter extends NetworkSwitchBlockEntity {
    private final li.cil.oc.common.blockentity.traits.OpenSidesJava.State openSidesState =
        new li.cil.oc.common.blockentity.traits.OpenSidesJava.State(SideCount(), this::defaultState);
    @Override public li.cil.oc.common.blockentity.traits.OpenSidesJava.State openSidesState() { return openSidesState; }
    private static final String INVERTED_TAG = Settings.namespace() + "isInverted";

    private final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("net_splitter", Visibility.Network).create();
    private java.util.Map<String, String> deviceInfo;
    private boolean inverted;

    public NetSplitter(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.NET_SPLITTER.get(), pos, state);
    }

    @Override
    public java.util.Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = BlockEntityDeviceInfo.netSplitter();
        return deviceInfo;
    }

    @Override
    public Node node() {
        return node;
    }

    public boolean isInverted() {
        return inverted;
    }

    public void isInverted_$eq(boolean value) {
        inverted = value;
    }

    @Override
    public boolean isSideOpen(Direction side) {
        return inverted != super.isSideOpen(side);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(NetSplitterModel.NET_SPLITTER_PROPERTY, this).build();
    }

    @Override
    public void setSideOpen(Direction side, boolean value) {
        boolean previous = isSideOpen(side);
        super.setSideOpen(side, value);
        if (previous != isSideOpen(side)) {
            if (isServer()) {
                node.remove();
                Network.joinOrCreateNetwork(this);
                sendState();
                getLevel().playSound(null, getBlockPos(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS,
                        0.5f, getLevel().random.nextFloat() * 0.25f + 0.7f);
                getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            } else {
                requestModelDataUpdate();
                getLevel().sendBlockUpdated(getBlockPos(), getLevel().getBlockState(getBlockPos()),
                        getLevel().getBlockState(getBlockPos()), 3);
            }
        }
    }

    @Override
    public Node sidedNode(Direction side) {
        return isSideOpen(side) ? node : null;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean canConnect(Direction side) {
        return isSideOpen(side);
    }

    @Override
    public void initialize() {
        super.initialize();
        EventHandler.scheduleServer(this);
    }

    @Override
    public void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        super.onRedstoneInputChanged(args);
        boolean old = inverted;
        inverted = args.newValue() > 0;
        if (inverted != old) {
            if (isServer()) {
                node.remove();
                Network.joinOrCreateNetwork(this);
                sendState();
                getLevel().playSound(null, getBlockPos(), SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS,
                        0.5f, getLevel().random.nextFloat() * 0.25f + 0.7f);
            } else {
                requestModelDataUpdate();
                getLevel().sendBlockUpdated(getBlockPos(), getLevel().getBlockState(getBlockPos()),
                        getLevel().getBlockState(getBlockPos()), 3);
            }
        }
    }

    private void sendState() {
        try {
            BlockSyncPacketSender.sendNetSplitter(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        li.cil.oc.common.blockentity.traits.OpenSidesJava.loadServerHost(this, nbt);
        inverted = nbt.getBoolean(INVERTED_TAG);
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        li.cil.oc.common.blockentity.traits.OpenSidesJava.saveHost(this, nbt);
        nbt.putBoolean(INVERTED_TAG, inverted);
    }

    @Override
    public void loadForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForClient(nbt, provider);
        li.cil.oc.common.blockentity.traits.OpenSidesJava.loadClientHost(this, nbt);
        inverted = nbt.getBoolean(INVERTED_TAG);
        requestModelDataUpdate();
    }

    @Override
    public void saveForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForClient(nbt, provider);
        li.cil.oc.common.blockentity.traits.OpenSidesJava.saveHost(this, nbt);
        nbt.putBoolean(INVERTED_TAG, inverted);
    }

    public Map<Integer, Boolean> currentStatus() {
        Map<Integer, Boolean> status = new HashMap<>();
        for (Direction side : Direction.values()) status.put(side.ordinal(), isSideOpen(side));
        return status;
    }

    public boolean setSide(Direction side, boolean state) {
        boolean previous = isSideOpen(side);
        setSideOpen(side, inverted ? !state : state);
        return previous != state;
    }

    @Callback(doc = "function(settings:table):table -- set open state (true/false) of all sides in an array; index by direction. Returns previous states")
    public Object[] setSides(Context context, Arguments args) {
        java.util.Map<?, ?> settings = args.checkTable(0);
        Map<Integer, Boolean> previous = currentStatus();
        for (Direction side : Direction.values()) {
            Object value = settings.get(side.ordinal());
            setSide(side, value instanceof Boolean flag && flag);
        }
        return ResultWrapper.result(previous);
    }

    @Callback(direct = true, doc = "function():table -- Returns current open/close state of all sides in an array, indexed by direction.")
    public Object[] getSides(Context context, Arguments args) {
        return ResultWrapper.result(currentStatus());
    }

    public Object[] setSideHelper(Arguments args, boolean value) {
        int index = args.checkInteger(0);
        if (index < 0 || index > 5) return ResultWrapper.result(ResultWrapper.unit, "invalid direction");
        return ResultWrapper.result(setSide(Direction.from3DDataValue(index), value));
    }

    @Callback(doc = "function(side: number):boolean -- Open the side, returns true if it changed to open.")
    public Object[] open(Context context, Arguments args) {
        return setSideHelper(args, true);
    }

    @Callback(doc = "function(side: number):boolean -- Close the side, returns true if it changed to close.")
    public Object[] close(Context context, Arguments args) {
        return setSideHelper(args, false);
    }
}
