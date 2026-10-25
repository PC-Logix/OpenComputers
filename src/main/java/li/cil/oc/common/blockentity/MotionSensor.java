package li.cil.oc.common.blockentity;

import li.cil.oc.api.network.Node;
import li.cil.oc.api.UnrecoverablePersistanceException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class MotionSensor extends TickingEnvironmentBlockEntity {
    private final li.cil.oc.server.component.MotionSensor motionSensor =
            new li.cil.oc.server.component.MotionSensor(this);

    public MotionSensor(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.MOTION_SENSOR.get(), pos, state);
    }

    public li.cil.oc.server.component.MotionSensor motionSensor() {
        return motionSensor;
    }

    @Override
    public Node node() {
        return motionSensor.node();
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isServer()) {
            motionSensor.update();
        }
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        try {
            motionSensor.loadData(nbt, provider);
        } catch (UnrecoverablePersistanceException e) {
            throw new IllegalStateException("Unable to restore motion sensor state", e);
        }
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        motionSensor.saveData(nbt, provider);
    }
}
