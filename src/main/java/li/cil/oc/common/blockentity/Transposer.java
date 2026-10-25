package li.cil.oc.common.blockentity;

import li.cil.oc.api.network.Node;
import li.cil.oc.api.UnrecoverablePersistanceException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class Transposer extends EnvironmentBlockEntity {
    private final li.cil.oc.server.component.Transposer.Block transposer =
            new li.cil.oc.server.component.Transposer.Block(this);
    private long lastOperation;

    public Transposer(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.TRANSPOSER.get(), pos, state);
    }

    public li.cil.oc.server.component.Transposer.Block transposer() {
        return transposer;
    }

    @Override
    public Node node() {
        return transposer.node();
    }

    public long lastOperation() {
        return lastOperation;
    }

    public void lastOperation_$eq(long value) {
        lastOperation = value;
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        try {
            transposer.loadData(nbt, provider);
        } catch (UnrecoverablePersistanceException e) {
            throw new IllegalStateException("Unable to restore transposer state", e);
        }
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        transposer.saveData(nbt, provider);
    }
}
