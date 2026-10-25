package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.Optional;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/** Array and map operations for bundled redstone state. */
public final class BundledRedstoneAwareJava {
    private BundledRedstoneAwareJava() {}

    public static void clearOutputs(int[][] output) {
        for (int[] side : output) java.util.Arrays.fill(side, 0);
    }

    public static int[][] getInput(int[][] bundled, int[][] rednet) {
        int[][] result = new int[bundled.length][16];
        for (int side = 0; side < bundled.length; side++) {
            for (int color = 0; color < result[side].length; color++) {
                result[side][color] = Math.max(Math.max(bundled[side][color], rednet[side][color]), 0);
            }
        }
        return result;
    }

    public static int[] getInput(int[][] bundled, int[][] rednet, Direction side) {
        int sideIndex = checkSide(side);
        int[] result = new int[bundled[sideIndex].length];
        for (int color = 0; color < result.length; color++) {
            result[color] = Math.max(Math.max(bundled[sideIndex][color], rednet[sideIndex][color]), 0);
        }
        return result;
    }

    public static int getInput(int[][] bundled, int[][] rednet, Direction side, int color) {
        int sideIndex = checkSide(side);
        int colorIndex = checkColor(color);
        return Math.max(Math.max(bundled[sideIndex][colorIndex], rednet[sideIndex][colorIndex]), 0);
    }

    public static void setInput(int[][] inputs, Direction side, int color, int newValue,
                                Consumer<RedstoneChangedEventArgs> onChanged) {
        int sideIndex = checkSide(side);
        int colorIndex = checkColor(color);
        int oldValue = inputs[sideIndex][colorIndex];
        if (oldValue != newValue) {
            inputs[sideIndex][colorIndex] = newValue;
            if (oldValue != -1) onChanged.accept(new RedstoneChangedEventArgs(side, oldValue, newValue, colorIndex));
        }
    }

    public static void setInput(int[][] inputs, Direction side, int[] newValues,
                                Consumer<RedstoneChangedEventArgs> onChanged) {
        for (int color = 0; color < 16; color++) {
            int value = newValues == null || color >= newValues.length ? 0 : newValues[color];
            setInput(inputs, side, color, value, onChanged);
        }
    }

    public static int[][] getOutput(int[][] input) {
        return input;
    }

    public static int[] getOutput(int[][] output, Direction side,
                                  Function<Direction, Direction> toLocal) {
        return output[checkSide(toLocal.apply(side))];
    }

    public static int getOutput(int[][] output, Direction side, int color,
                                 Function<Direction, Direction> toLocal) {
        return getOutput(output, side, toLocal)[checkColor(color)];
    }

    public static boolean setOutput(int[][] output, Direction side, int color, int value,
                                    Function<Direction, Direction> toLocal,
                                    Consumer<Direction> onChanged) {
        int[] values = getOutput(output, side, toLocal);
        int colorIndex = checkColor(color);
        if (value == values[colorIndex]) return false;
        values[colorIndex] = value;
        onChanged.accept(side);
        return true;
    }

    public static boolean setOutput(int[][] output, Direction side, Map<?, ?> values,
                                    Function<Direction, Direction> toLocal,
                                    Consumer<Direction> onChanged) {
        int sideIndex = toLocal.apply(side).ordinal();
        boolean changed = false;
        for (int color = 0; color < 16; color++) {
            Optional<Object> value = RedstoneAwareJava.getObjectFuzzyOptional(values, color);
            if (value.isPresent() && value.get() instanceof Integer newValue
                    && newValue != getOutput(output, side, color, toLocal)) {
                output[sideIndex][color] = newValue;
                changed = true;
            }
        }
        if (changed) onChanged.accept(side);
        return changed;
    }

    public static boolean setOutput(int[][] output, Map<?, ?> values,
                                    Function<Direction, Direction> toLocal,
                                    Consumer<Direction> onChanged) {
        boolean changed = false;
        for (Direction side : Direction.values()) {
            int sideIndex = toLocal.apply(side).ordinal();
            Optional<Object> child = RedstoneAwareJava.getObjectFuzzyOptional(values, sideIndex);
            if (child.isPresent() && child.get() instanceof Map<?, ?> childMap
                    && setOutput(output, side, childMap, toLocal, onChanged)) {
                changed = true;
            }
        }
        return changed;
    }

    public static void loadServer(CompoundTag nbt, String inputTag, String outputTag, String rednetTag,
                                  int[][] bundledInput, int[][] bundledOutput, int[][] rednetInput) {
        loadList(nbt.getList(inputTag, Tag.TAG_INT_ARRAY), bundledInput);
        loadList(nbt.getList(outputTag, Tag.TAG_INT_ARRAY), bundledOutput);
        loadList(nbt.getList(rednetTag, Tag.TAG_INT_ARRAY), rednetInput);
    }

    public static void saveServer(CompoundTag nbt, String inputTag, String outputTag, String rednetTag,
                                  int[][] bundledInput, int[][] bundledOutput, int[][] rednetInput) {
        nbt.put(inputTag, toTagList(bundledInput));
        nbt.put(outputTag, toTagList(bundledOutput));
        nbt.put(rednetTag, toTagList(rednetInput));
    }

    private static void loadList(ListTag tags, int[][] target) {
        int sides = Math.min(tags.size(), target.length);
        for (int side = 0; side < sides; side++) {
            int[] data = ((IntArrayTag) tags.get(side)).getAsIntArray();
            System.arraycopy(data, 0, target[side], 0, Math.min(data.length, target[side].length));
        }
    }

    private static ListTag toTagList(int[][] values) {
        ListTag tags = new ListTag();
        for (int[] value : values) tags.add(new IntArrayTag(value));
        return tags;
    }

    private static int checkSide(Direction side) {
        int index = side.ordinal();
        if (index >= 6) throw new IndexOutOfBoundsException("Bad side " + side);
        return index;
    }

    private static int checkColor(int color) {
        if (color < 0 || color >= 16) throw new IndexOutOfBoundsException("Bad color " + color);
        return color;
    }
}
