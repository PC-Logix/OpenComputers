package li.cil.oc.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

// for f**king stupid scala compiler
public final class BlockPosHelper {
    public static BlockPos relative(BlockPos pos, Direction direction) {
        return pos.relative(direction);
    }

    public static BlockPos offset(BlockPos pos, int x, int y, int z) {
        return pos.offset(x, y, z);
    }
}
