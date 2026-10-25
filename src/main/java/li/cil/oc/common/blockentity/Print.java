package li.cil.oc.common.blockentity;

import li.cil.oc.Settings;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.client.renderer.block.PrintModel;
import li.cil.oc.common.blockentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.item.data.PrintShape;
import li.cil.oc.util.ExtendedAABB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.ScheduledTick;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import scala.Function0;
import scala.Function1;
import scala.Option;
import scala.Option$;
import scala.runtime.BoxedUnit;

import java.util.HashMap;
import java.util.Map;

public class Print extends PrintBlockEntity {
    private final li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava rotationState =
        new li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava();
    @Override public li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava rotationState() { return rotationState; }
    private final Option<Function0<Boolean>> canToggle;
    private final Option<Function1<Object, BoxedUnit>> scheduleUpdate;
    private final Option<Function0<BoxedUnit>> onStateChange;
    private final PrintData data = new PrintData();
    private VoxelShape shapeOff = Shapes.block();
    private VoxelShape shapeOn = Shapes.block();
    private boolean state;

    public Print(BlockPos pos, BlockState blockState) {
        this(pos, blockState, Option$.MODULE$.empty(), Option$.MODULE$.empty(), Option$.MODULE$.empty());
    }

    public Print(BlockPos pos, BlockState blockState, Function0<Boolean> canToggle,
                 Function1<Object, BoxedUnit> scheduleUpdate, Function0<BoxedUnit> onStateChange) {
        this(pos, blockState, Option$.MODULE$.apply(canToggle), Option$.MODULE$.apply(scheduleUpdate),
                Option$.MODULE$.apply(onStateChange));
    }

    public Print(BlockPos pos, BlockState blockState, Option<Function0<Boolean>> canToggle,
                 Option<Function1<Object, BoxedUnit>> scheduleUpdate, Option<Function0<BoxedUnit>> onStateChange) {
        super(BlockEntityTypes.PRINT.get(), pos, blockState);
        this.canToggle = canToggle;
        this.scheduleUpdate = scheduleUpdate;
        this.onStateChange = onStateChange;
        enableRedstoneOutput();
    }

    public Option<Function0<Boolean>> canToggle() { return canToggle; }
    public Option<Function1<Object, BoxedUnit>> scheduleUpdate() { return scheduleUpdate; }
    public Option<Function0<BoxedUnit>> onStateChange() { return onStateChange; }
    public PrintData data() { return data; }
    public VoxelShape shapeOff() { return shapeOff; }
    public void shapeOff_$eq(VoxelShape value) { shapeOff = value; }
    public VoxelShape shapeOn() { return shapeOn; }
    public void shapeOn_$eq(VoxelShape value) { shapeOn = value; }
    public boolean state() { return state; }
    public void state_$eq(boolean value) { state = value; }
    public VoxelShape shape() { return state ? shapeOn : shapeOff; }
    public boolean noclip() { return state ? data.noclipOn() : data.noclipOff(); }
    public scala.collection.mutable.Set<PrintShape> shapes() { return state ? data.stateOn() : data.stateOff(); }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(PrintModel.PRINT_PROPERTY, this).build();
    }

    public boolean activate() {
        if (data.hasActiveState() && (!state || !data.isButtonMode())) {
            toggleState();
            return true;
        }
        return false;
    }

    private Map<Object, Object> buildValueSet(int value) {
        Map<Object, Object> values = new HashMap<>();
        for (Direction side : Direction.values()) values.put(side.ordinal(), value);
        return values;
    }

    public void toggleState() {
        if (canToggle.isDefined() && !canToggle.get().apply()) return;
        state = !state;
        getLevel().playSound(null, getBlockPos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, state ? 0.6F : 0.5F);
        getLevel().sendBlockUpdated(getBlockPos(), getLevel().getBlockState(getBlockPos()),
                getLevel().getBlockState(getBlockPos()), 3);
        updateRedstone();
        if (state && data.isButtonMode()) {
            var block = OCBlocks.Print.get();
            int delay = block.tickRate(getLevel());
            if (scheduleUpdate.isDefined()) scheduleUpdate.get().apply(delay);
            else if (!getLevel().isClientSide()) {
                ServerLevel serverLevel = (ServerLevel) getLevel();
                long triggerTime = serverLevel.getGameTime() + delay;
                serverLevel.getBlockTicks().schedule(new ScheduledTick<>(block, getBlockPos(),
                        triggerTime, serverLevel.getGameTime()));
            }
        }
        if (onStateChange.isDefined()) onStateChange.get().apply();
    }

    private VoxelShape convertShape(scala.collection.Iterable<PrintShape> shapes) {
        if (shapes.isEmpty()) return Shapes.block();
        VoxelShape result = Shapes.empty();
        var iterator = shapes.iterator();
        while (iterator.hasNext()) {
            VoxelShape voxel = Shapes.create(ExtendedAABB.rotateTowards(iterator.next().bounds(), facing()));
            result = Shapes.joinUnoptimized(result, voxel, BooleanOp.OR);
        }
        return result.optimize();
    }

    public void updateShape() {
        shapeOff = convertShape(data.stateOff());
        shapeOn = convertShape(data.stateOn());
    }

    public void updateRedstone() {
        if (data.emitRedstone()) setOutput(buildValueSet(data.emitRedstone(state) ? data.redstoneLevel() : 0));
    }

    @Override
    public void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        boolean newState = args.newValue() > 0;
        if (!data.emitRedstone() && data.hasActiveState() && state != newState) toggleState();
    }

    @Override
    public void onRotationChanged() {
        super.onRotationChanged();
        updateShape();
    }

    private static final String DATA_TAG = Settings.namespace() + "data";
    private static final String STATE_TAG = Settings.namespace() + "state";

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava.loadForServer(this, nbt);
        try {
            data.loadData(nbt.getCompound(DATA_TAG), provider);
        } catch (UnrecoverablePersistanceException e) {
            throw new IllegalStateException("Unable to restore print state", e);
        }
        state = nbt.getBoolean(STATE_TAG);
        updateShape();
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava.saveForServer(this, nbt);
        CompoundTag dataTag = new CompoundTag();
        data.saveData(dataTag, provider);
        nbt.put(DATA_TAG, dataTag);
        nbt.putBoolean(STATE_TAG, state);
    }

    @Override
    public void loadForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForClient(nbt, provider);
        li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava.loadForClient(this, nbt);
        try {
            data.loadData(nbt.getCompound(DATA_TAG), provider);
        } catch (UnrecoverablePersistanceException e) {
            throw new IllegalStateException("Unable to restore print state", e);
        }
        state = nbt.getBoolean(STATE_TAG);
        updateShape();
        if (getLevel() != null) {
            getLevel().sendBlockUpdated(getBlockPos(), getLevel().getBlockState(getBlockPos()),
                    getLevel().getBlockState(getBlockPos()), 3);
            if (data.emitLight()) getLevel().getLightEngine().checkBlock(getBlockPos());
        }
    }

    @Override
    public void saveForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForClient(nbt, provider);
        li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava.saveForClient(this, nbt);
        CompoundTag dataTag = new CompoundTag();
        data.saveData(dataTag, provider);
        nbt.put(DATA_TAG, dataTag);
        nbt.putBoolean(STATE_TAG, state);
    }

    @Deprecated @Override public boolean hasProperty(ModelProperty<?> property) { return false; }
    @Deprecated @Override public <T> T getData(ModelProperty<T> property) { return null; }
    @Deprecated @Override public <T> T setData(ModelProperty<T> property, T value) { return null; }
}

