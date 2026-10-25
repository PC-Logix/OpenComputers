package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class MovingBlockEntityStateJavaTest {
    @Test
    public void movingPositionAndRotationDriveBlockCoordinatesAndNeighbors() {
        MovingBlockEntityStateJava state = new MovingBlockEntityStateJava();
        BlockPos original = new BlockPos(1, 2, 3);
        assertFalse(state.isMoving());
        assertEquals(original, state.blockPos(original));
        assertEquals(Direction.EAST, state.direction(Direction.EAST));

        state.begin(null, new Vec3(10.2, 5.8, -4.1),
            vector -> new Vec3(-vector.z, vector.y, vector.x), null);
        assertTrue(state.isMoving());
        assertEquals(new BlockPos(10, 5, -5), state.blockPos(original));
        assertEquals(Direction.SOUTH, state.direction(Direction.EAST));
        assertEquals(new BlockPos(10, 5, -4), state.neighbor(original, Direction.EAST));

        state.end();
        assertFalse(state.isMoving());
        assertEquals(original, state.blockPos(original));
        assertNull(state.level());
    }
}
