package li.cil.oc.common.blockentity;

import li.cil.oc.Settings;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class PowerDistributor extends PowerDistributorBlockEntity {
    private final li.cil.oc.common.blockentity.traits.PowerBalancerStateJava powerState =
        new li.cil.oc.common.blockentity.traits.PowerBalancerStateJava();
    @Override public li.cil.oc.common.blockentity.traits.PowerBalancerStateJava powerBalancerState() { return powerState; }
    @Override public boolean powerNetworkConnected() { return isConnected(); }
    private static final String CONNECTOR_TAG = Settings.namespace() + "connector";

    private final Connector[] nodes = new Connector[Direction.values().length];

    public PowerDistributor(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.POWER_DISTRIBUTOR.get(), pos, state);
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = Network.newNode(this, Visibility.None)
                    .withConnector(Settings.get().bufferDistributor()).create();
        }
    }

    @Override
    public Node node() {
        return null;
    }

    @Override
    public boolean isConnected() {
        for (Connector node : nodes) {
            if (node.address() != null && node.network() != null) return true;
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean canConnect(Direction side) {
        return true;
    }

    @Override
    public Connector sidedNode(Direction side) {
        return nodes[side.ordinal()];
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        ListTag savedNodes = nbt.getList(CONNECTOR_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(savedNodes.size(), nodes.length); i++) {
            try {
                nodes[i].loadData(savedNodes.getCompound(i), provider);
            } catch (UnrecoverablePersistanceException e) {
                throw new IllegalStateException("Unable to restore power distributor connector " + i, e);
            }
        }
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        if (isServer()) {
            ListTag savedNodes = new ListTag();
            for (Connector node : nodes) {
                CompoundTag tag = new CompoundTag();
                node.saveData(tag, provider);
                savedNodes.add(tag);
            }
            nbt.put(CONNECTOR_TAG, savedNodes);
        }
    }
}
