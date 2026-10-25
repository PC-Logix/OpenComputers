package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import scala.Option;

import java.util.Arrays;
import java.util.Optional;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import li.cil.oc.server.BlockSyncPacketSender;

import java.io.IOException;

/** Java array operations for redstone input state. */
public final class RedstoneAwareJava {
    private RedstoneAwareJava() { }

    public static void checkInputChanged(RedstoneAware self) {
        if (self instanceof Tickable) {
            self.shouldUpdateInput_$eq(self.isServer());
        } else {
            updateAllInputs(self);
        }
    }

    public static void afterUpdateEntity(RedstoneAware self) {
        if (!shouldUpdateAfterTick(self.isServer(), self.shouldUpdateInput())) return;
        self.shouldUpdateInput_$eq(false);
        updateAllInputs(self);
    }

    public static void afterClearRemoved(RedstoneAware self) {
        if (!shouldScheduleAfterRemoval(self instanceof Tickable, self.isServer())) return;
        li.cil.oc.common.EventHandler.scheduleServer(() -> {
            updateAllInputs(self);
            return scala.runtime.BoxedUnit.UNIT;
        });
    }

    static boolean shouldUpdateAfterTick(boolean server, boolean pending) {
        return server && pending;
    }

    static boolean shouldScheduleAfterRemoval(boolean tickable, boolean server) {
        return !tickable && server;
    }

    private static void updateAllInputs(RedstoneAware self) {
        for (Direction side : Direction.values()) self.updateRedstoneInput(side);
    }

    public static void updateRedstoneInput(RedstoneAware self, Direction side) {
        BlockEntity blockEntity = (BlockEntity) self;
        Level level = blockEntity.getLevel();
        BlockPos position = self.isMoving() ? self.movingBlockPos() : blockEntity.getBlockPos();
        Direction inputSide = self.isMoving() ? self.movingDirection(side) : side;
        li.cil.oc.util.BlockPosition inputPosition = li.cil.oc.util.BlockPositionsJava.apply(position, level);
        self.setInput(side, computeInput(inputPosition, inputSide));
    }

    public static void onRedstoneOutputEnabledChanged(RedstoneAware self) {
        BlockEntity blockEntity = (BlockEntity) self;
        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockPos position = self.isMoving() ? self.movingBlockPos() : blockEntity.getBlockPos();
        BlockState state = self.isMoving() ? self.movingBlockState() : blockEntity.getBlockState();
        outputEnabledChanged(level, position, state.getBlock(), () -> sendRedstoneUpdate(self, blockEntity, level));
    }

    public static void onRedstoneOutputChanged(RedstoneAware self, Direction side) {
        BlockEntity blockEntity = (BlockEntity) self;
        Level level = blockEntity.getLevel();
        BlockPos sourcePosition = self.isMoving() ? self.movingBlockPos() : blockEntity.getBlockPos();
        BlockState sourceState = self.isMoving() ? self.movingBlockState() : blockEntity.getBlockState();
        Direction outputSide = self.isMoving() ? self.movingDirection(side) : side;
        BlockPos outputPosition = self.isMoving()
            ? self.movingNeighbor(side)
            : li.cil.oc.util.BlockPosHelper.relative(sourcePosition, side);

        outputChanged(level, outputPosition, sourceState.getBlock(), sourcePosition, outputSide,
            () -> sendRedstoneUpdate(self, blockEntity, level));
    }

    private static void sendRedstoneUpdate(RedstoneAware self, BlockEntity blockEntity, Level level) {
        if (self.isServer()) {
            try {
                BlockSyncPacketSender.sendRedstone(self);
            } catch (IOException error) {
                RedstoneAwareJava.<RuntimeException>throwUnchecked(error);
            }
        } else {
            BlockPos position = blockEntity.getBlockPos();
            BlockState state = level.getBlockState(position);
            level.sendBlockUpdated(position, state, state, 3);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable error) throws T {
        throw (T) error;
    }

    public static int[] getInput(int[] input) {
        return Arrays.stream(input).map(value -> Math.max(value, 0)).toArray();
    }

    public static int getInput(int[] input, Direction side) {
        return Math.max(input[side.ordinal()], 0);
    }

    public static int maxInput(int[] input) {
        return Arrays.stream(input).map(value -> Math.max(value, 0)).max().orElseThrow();
    }

    public static void setInput(int[] input, Direction side, int newInput,
                                Consumer<RedstoneChangedEventArgs> onChanged) {
        int index = side.ordinal();
        int oldInput = input[index];
        input[index] = newInput;
        if (oldInput >= 0 && newInput != oldInput) {
            onChanged.accept(new RedstoneChangedEventArgs(side, oldInput, newInput));
        }
    }

    public static int[] getOutput(int[] output, Function<Direction, Direction> toLocal) {
        Direction[] directions = Direction.values();
        int[] result = new int[directions.length];
        for (int index = 0; index < directions.length; index++) {
            result[index] = output[toLocal.apply(directions[index]).ordinal()];
        }
        return result;
    }

    public static int getOutput(int[] output, Direction side, Function<Direction, Direction> toLocal) {
        int index = toLocal.apply(side).ordinal();
        return output != null && output.length > index ? output[index] : 0;
    }

    public static boolean setOutput(int[] output, Direction side, int value,
                                    Function<Direction, Direction> toLocal,
                                    Consumer<Direction> onChanged) {
        if (value == getOutput(output, side, toLocal)) return false;
        output[toLocal.apply(side).ordinal()] = value;
        onChanged.accept(side);
        return true;
    }

    public static void setInput(int[] input, int[] values, BiConsumer<Direction, Integer> setter) {
        for (Direction side : Direction.values()) {
            int index = side.ordinal();
            int value = index <= values.length ? values[index] : 0;
            setter.accept(side, value);
        }
    }

    public static boolean setOutputEnabled(boolean current, boolean value, int[] output,
                                           Consumer<Boolean> setState, Runnable onChanged) {
        if (value != current) {
            setState.accept(value);
            if (!value) Arrays.fill(output, 0);
            onChanged.run();
        }
        return value;
    }

    public static void setOutputEnabled(RedstoneAware self, boolean value) {
        setOutputEnabled(self._isOutputEnabled(), value, self._output(), self::_isOutputEnabled_$eq,
                self::onRedstoneOutputEnabledChanged);
    }

    public static boolean setOutput(int[] output, Map<?, ?> values,
                                    Function<Direction, Direction> toLocal,
                                    Function<Object, Optional<Integer>> valueToInt,
                                    BiPredicate<Direction, Integer> setOutput) {
        boolean changed = false;
        for (Direction side : Direction.values()) {
            int sideIndex = toLocal.apply(side).ordinal();
            Object rawValue = getObjectFuzzyOptional(values, sideIndex).orElse(null);
            Optional<Integer> value = valueToInt.apply(rawValue);
            if (value.isPresent() && setOutput.test(side, value.get())) changed = true;
        }
        return changed;
    }

    public static void loadServer(CompoundTag nbt, String inputTag, String outputTag,
                                  int[] input, int[] output) {
        copy(nbt.getIntArray(inputTag), input);
        copy(nbt.getIntArray(outputTag), output);
    }

    public static void saveServer(CompoundTag nbt, String inputTag, String outputTag,
                                  int[] input, int[] output) {
        nbt.putIntArray(inputTag, input);
        nbt.putIntArray(outputTag, output);
    }

    public static boolean loadClient(CompoundTag nbt, int[] output) {
        copy(nbt.getIntArray("output"), output);
        return nbt.getBoolean("isOutputEnabled");
    }

    public static void saveClient(CompoundTag nbt, boolean outputEnabled, int[] output) {
        nbt.putBoolean("isOutputEnabled", outputEnabled);
        nbt.putIntArray("output", output);
    }

    private static void copy(int[] source, int[] target) {
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }

    public static int computeInput(li.cil.oc.util.BlockPosition position, Direction side) {
        return li.cil.oc.integration.util.BundledRedstone.computeInput(position, side);
    }

    public static void outputEnabledChanged(Level level, BlockPos blockPos, Block block, Runnable sendUpdate) {
        if (level != null) {
            level.updateNeighborsAt(blockPos, block);
            sendUpdate.run();
        }
    }

    public static void outputChanged(Level level, BlockPos blockPos, Block sourceBlock, BlockPos sourcePos,
                                     Direction outputSide, Runnable sendUpdate) {
        level.neighborChanged(blockPos, sourceBlock, sourcePos);
        level.updateNeighborsAtExceptFromFacing(blockPos, level.getBlockState(blockPos).getBlock(),
                outputSide.getOpposite());
        sendUpdate.run();
    }

    public static Optional<Object> getObjectFuzzyOptional(Map<?, ?> map, int key) {
        if (map.containsKey(key)) return Optional.ofNullable(map.get(key));
        double decimalKey = key * 1.0;
        if (map.containsKey(decimalKey)) return Optional.ofNullable(map.get(decimalKey));
        return Optional.empty();
    }

    public static Optional<Integer> valueToIntOptional(Object value) {
        if (value instanceof Option<?> option && option.isDefined() && option.get() instanceof Number number) {
            return Optional.of(number.intValue());
        }
        return Optional.empty();
    }
}
