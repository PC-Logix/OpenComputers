package li.cil.oc.common.blockentity;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.Slot;
import li.cil.oc.server.GeneralPacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import scala.Option;

import java.util.Optional;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;

public class Adapter extends AdapterEnvironmentBlockEntity {
    private final li.cil.oc.common.blockentity.traits.OpenSidesJava.State openSidesState =
        new li.cil.oc.common.blockentity.traits.OpenSidesJava.State(SideCount(), this::defaultState);
    @Override public li.cil.oc.common.blockentity.traits.OpenSidesJava.State openSidesState() { return openSidesState; }
    private static final String BLOCKS_TAG = Settings.namespace() + "adapter.blocks";
    private static final String NAME_TAG = "name";
    private static final String DATA_TAG = "data";

    private final Node node = Network.newNode(this, Visibility.Network).create();
    private final AdaptedBlock[] blocks = new AdaptedBlock[Direction.values().length];
    private final List<ManagedEnvironment> updatingBlocks = new ArrayList<>();
    private final BlockData[] blocksData = new BlockData[Direction.values().length];
    private Map<String, String> deviceInfo;

    public Adapter(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.ADAPTER.get(), pos, state);
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = BlockEntityDeviceInfo.adapter();
        return deviceInfo;
    }

    @Override
    public boolean defaultState() {
        return true;
    }

    @Override
    public void setSideOpen(Direction side, boolean value) {
        super.setSideOpen(side, value);
        if (isServer()) {
            try {
                GeneralPacketSender.sendAdapterState(this);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            getLevel().playSound(null, getBlockPos(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS,
                    0.5f, getLevel().random.nextFloat() * 0.25f + 0.7f);
            getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            neighborChanged(side);
        } else {
            getLevel().sendBlockUpdated(getBlockPos(), getLevel().getBlockState(getBlockPos()),
                    getLevel().getBlockState(getBlockPos()), 3);
        }
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        List<Node> nodes = new ArrayList<>();
        for (AdaptedBlock block : blocks) if (block != null) nodes.add(block.environment.node());
        for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this)) {
            if (component.isPresent()) nodes.add(component.get().node());
        }
        return nodes.toArray(Node[]::new);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isServer()) {
            for (ManagedEnvironment block : updatingBlocks) block.update();
        }
    }

    public void neighborChanged(Direction side) {
        if (node == null || node.network() == null) return;
        BlockPos neighbor = getBlockPos().relative(side);
        if (getLevel().getBlockEntity(neighbor) instanceof li.cil.oc.common.blockentity.traits.NetworkBlockEntity) return;

        int index = side.ordinal();
        DriverBlock newDriver = Driver.driverFor(getLevel(), neighbor, side);
        AdaptedBlock old = blocks[index];
        if (newDriver != null && isSideOpen(side)) {
            if (old != null) {
                if (!newDriver.equals(old.driver)) {
                    blocks[index] = null;
                    updatingBlocks.remove(old.environment);
                    blocksData[index] = null;
                    node.disconnect(old.environment.node());
                    createAdaptedBlock(index, newDriver, neighbor, side, false);
                }
            } else {
                createAdaptedBlock(index, newDriver, neighbor, side, true);
            }
        } else if (old != null) {
            node.disconnect(old.environment.node());
            old.environment.saveData(blocksData[index].data, getLevel().registryAccess());
            if (old.environment.node() != null) old.environment.node().remove();
            blocks[index] = null;
            updatingBlocks.remove(old.environment);
        }
    }

    private void createAdaptedBlock(int index, DriverBlock driver, BlockPos neighbor, Direction side, boolean restore) {
        ManagedEnvironment environment = driver.createEnvironment(getLevel(), neighbor, side);
        if (environment == null) return;
        blocks[index] = new AdaptedBlock(environment, driver);
        if (environment.canUpdate()) updatingBlocks.add(environment);
        if (restore && blocksData[index] != null && blocksData[index].name.equals(environment.getClass().getName())) {
            try {
                environment.loadData(blocksData[index].data, getLevel().registryAccess());
            } catch (UnrecoverablePersistanceException e) {
                throw new IllegalStateException("Unable to restore adapted block state", e);
            }
        }
        blocksData[index] = new BlockData(environment.getClass().getName(), new CompoundTag());
        node.connect(environment.node());
    }

    public void neighborChanged() {
        if (node != null && node.network() != null) {
            for (Direction side : Direction.values()) neighborChanged(side);
        }
    }

    @Override
    public void onConnect(Node connectedNode) {
        super.onConnect(connectedNode);
        if (connectedNode == node) neighborChanged();
    }

    @Override
    public void onDisconnect(Node disconnectedNode) {
        super.onDisconnect(disconnectedNode);
        if (disconnectedNode == node) updatingBlocks.clear();
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 0) return false;
        var driver = Driver.driverFor(stack, getClass());
        return driver != null && Slot.Upgrade.equals(driver.slot(stack));
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.menu.Adapter(id, playerInventory, this);
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        li.cil.oc.common.blockentity.traits.OpenSidesJava.loadServerHost(this, nbt);
        ListTag savedBlocks = nbt.getList(BLOCKS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(savedBlocks.size(), blocksData.length); i++) {
            CompoundTag saved = savedBlocks.getCompound(i);
            if (saved.contains(NAME_TAG) && saved.contains(DATA_TAG)) {
                blocksData[i] = new BlockData(saved.getString(NAME_TAG), saved.getCompound(DATA_TAG));
            }
        }
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        li.cil.oc.common.blockentity.traits.OpenSidesJava.saveHost(this, nbt);
        ListTag savedBlocks = new ListTag();
        for (int i = 0; i < blocks.length; i++) {
            CompoundTag saved = new CompoundTag();
            BlockData data = blocksData[i];
            if (data != null) {
                if (blocks[i] != null) blocks[i].environment.saveData(data.data, provider);
                saved.putString(NAME_TAG, data.name);
                saved.put(DATA_TAG, data.data);
            }
            savedBlocks.add(saved);
        }
        nbt.put(BLOCKS_TAG, savedBlocks);
    }

    private record AdaptedBlock(ManagedEnvironment environment, DriverBlock driver) {
    }

    private record BlockData(String name, CompoundTag data) {
    }
    @Override public void loadForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForClient(nbt, provider);
        li.cil.oc.common.blockentity.traits.OpenSidesJava.loadClientHost(this, nbt);
    }
    @Override public void saveForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForClient(nbt, provider);
        li.cil.oc.common.blockentity.traits.OpenSidesJava.saveHost(this, nbt);
    }
}

