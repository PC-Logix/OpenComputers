package li.cil.oc.common.blockentity;

import li.cil.oc.api.network.Node;
import li.cil.oc.api.UnrecoverablePersistanceException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class Geolyzer extends EnvironmentBlockEntity {
    private final li.cil.oc.server.component.Geolyzer geolyzer =
            new li.cil.oc.server.component.Geolyzer(this);

    public Geolyzer(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.GEOLYZER.get(), pos, state);
    }

    public li.cil.oc.server.component.Geolyzer geolyzer() {
        return geolyzer;
    }

    @Override
    public Node node() {
        return geolyzer.node();
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        try {
            geolyzer.loadData(nbt, provider);
        } catch (UnrecoverablePersistanceException e) {
            throw new IllegalStateException("Unable to restore geolyzer state", e);
        }
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        geolyzer.saveData(nbt, provider);
    }
}
