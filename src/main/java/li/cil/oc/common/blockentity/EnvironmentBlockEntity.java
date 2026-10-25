package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java network block-entity base for new blocks and existing device capabilities. */
public abstract class EnvironmentBlockEntity extends JavaBlockEntity implements NetworkBlockEntity {
    private final EnvironmentRuntimeStateJava environmentState = new EnvironmentRuntimeStateJava();
    protected EnvironmentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override public boolean isChangeScheduledForJava() { return environmentState.isChangeScheduled(); }
    @Override public void setChangeScheduledForJava(boolean value) { environmentState.setChangeScheduled(value); }
    @Override public void saveMovingState(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        NetworkBlockEntity.super.saveMovingState(tag, provider);
    }
    @Override public void disposeMoving() { NetworkBlockEntity.super.disposeMoving(); }
    protected boolean isConnected() {
        return this instanceof Hub hub ? HubNetworkJava.isConnected(hub.plugNodes()) : EnvironmentJava.isConnected(node());
    }
    protected Object[] result(Object... args) { return li.cil.oc.util.ResultWrapper.result(args); }
}

