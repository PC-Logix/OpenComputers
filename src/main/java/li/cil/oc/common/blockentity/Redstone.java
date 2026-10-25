package li.cil.oc.common.blockentity;

import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.blockentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.server.component.RedstoneVanilla;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class Redstone extends RedstoneEnvironmentBlockEntity {
    private static final String REDSTONE_TAG = Settings.namespace() + "redstone";

    private final RedstoneVanilla instance;
    private final Component node;
    private final Node dummyNode;

    public Redstone(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.REDSTONE_IO.get(), pos, state);
        instance = BundledRedstone.isAvailable()
                ? new li.cil.oc.server.component.Redstone.Bundled(this)
                : new li.cil.oc.server.component.Redstone.Vanilla(this);
        instance.wakeNeighborsOnly_$eq(false);
        node = instance.node();
        if (node != null) {
            node.setVisibility(Visibility.Network);
            enableRedstoneOutputWithoutNotification();
            dummyNode = Network.newNode(this, Visibility.None).create();
        } else {
            dummyNode = null;
        }
    }

    public RedstoneVanilla instance() {
        return instance;
    }

    @Override
    public Component node() {
        return node;
    }

    public Node dummyNode() {
        return dummyNode;
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        try {
            instance.loadData(nbt.getCompound(REDSTONE_TAG), provider);
        } catch (UnrecoverablePersistanceException e) {
            throw new IllegalStateException("Unable to restore redstone component state", e);
        }
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        CompoundTag redstoneTag = new CompoundTag();
        instance.saveData(redstoneTag, provider);
        nbt.put(REDSTONE_TAG, redstoneTag);
    }

    @Override
    public void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        super.onRedstoneInputChanged(args);
        if (node != null && node.network() != null) {
            node.connect(dummyNode);
            dummyNode.sendToNeighbors("redstone.changed", args);
        }
    }
}
