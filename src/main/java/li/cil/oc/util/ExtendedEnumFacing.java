package li.cil.oc.util;

import net.minecraft.core.Direction;

public final class ExtendedEnumFacing {
    // Copied from old Forge's ForgeDirection; the Minecraft equivalent is client only.
    private static final int[][] ROTATION_MATRIX = {
        {0, 1, 4, 5, 3, 2, 6},
        {0, 1, 5, 4, 2, 3, 6},
        {5, 4, 2, 3, 0, 1, 6},
        {4, 5, 2, 3, 1, 0, 6},
        {2, 3, 1, 0, 4, 5, 6},
        {3, 2, 0, 1, 4, 5, 6},
        {0, 1, 2, 3, 4, 5, 6}
    };

    private ExtendedEnumFacing() {
    }

    public static Direction getRotation(Direction facing, Direction axis) {
        return Direction.from3DDataValue(ROTATION_MATRIX[axis.ordinal()][facing.ordinal()]);
    }
}
