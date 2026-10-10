package li.cil.oc.util;

import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.Map;

public final class RotationHelper {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Map<Direction, Map<Direction, Direction[]>> TRANSLATION_CACHE =
        new EnumMap<>(Direction.class);
    private static final Map<Direction, Map<Direction, Direction[]>> INVERSE_TRANSLATION_CACHE =
        new EnumMap<>(Direction.class);

    /**
     * Translates directions based on the block's pitch and yaw. The base
     * forward direction is south with no pitch. The outer array contains the
     * down, up, and horizontal pitch states; the inner array contains yaw.
     */
    private static final Direction[][][] TRANSLATIONS = {
        { // Pitch = Down
            {Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST},
            {Direction.SOUTH, Direction.NORTH, Direction.DOWN, Direction.UP, Direction.WEST, Direction.EAST},
            {Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN},
            {Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST, Direction.DOWN, Direction.UP}
        },
        { // Pitch = Up
            {Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP, Direction.EAST, Direction.WEST},
            {Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST},
            {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
            {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN}
        },
        { // Pitch = Horizontal
            {Direction.DOWN, Direction.UP, Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST},
            {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST},
            {Direction.DOWN, Direction.UP, Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH},
            {Direction.DOWN, Direction.UP, Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}
        }
    };

    private RotationHelper() {
    }

    public static int getNumDirections() {
        return DIRECTIONS.length;
    }

    public static Direction getFront(int index) {
        return DIRECTIONS[Math.floorMod(index, DIRECTIONS.length)];
    }

    public static Direction fromYaw(float yaw) {
        return switch (Math.round(yaw / 360 * 4) & 3) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    public static Direction toLocal(Direction pitch, Direction yaw, Direction value) {
        return translationFor(pitch, yaw)[value.ordinal()];
    }

    public static Direction toGlobal(Direction pitch, Direction yaw, Direction value) {
        return inverseTranslationFor(pitch, yaw)[value.ordinal()];
    }

    public static Direction[] translationFor(Direction pitch, Direction yaw) {
        synchronized (TRANSLATION_CACHE) {
            return TRANSLATION_CACHE
                .computeIfAbsent(pitch, ignored -> new EnumMap<>(Direction.class))
                .computeIfAbsent(yaw, ignored -> TRANSLATIONS[pitch.ordinal()][yaw.ordinal() - 2]);
        }
    }

    public static Direction[] inverseTranslationFor(Direction pitch, Direction yaw) {
        synchronized (INVERSE_TRANSLATION_CACHE) {
            return INVERSE_TRANSLATION_CACHE
                .computeIfAbsent(pitch, ignored -> new EnumMap<>(Direction.class))
                .computeIfAbsent(yaw, ignored -> {
                    Direction[] translation = translationFor(pitch, yaw);
                    Direction[] inverse = new Direction[translation.length];
                    for (int i = 0; i < inverse.length; i++) {
                        Direction direction = Direction.from3DDataValue(i);
                        for (int j = 0; j < translation.length; j++) {
                            if (translation[j] == direction) {
                                inverse[i] = Direction.from3DDataValue(j);
                                break;
                            }
                        }
                    }
                    return inverse;
                });
        }
    }
}
