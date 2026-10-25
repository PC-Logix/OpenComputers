package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.Test;

import static org.junit.Assert.*;

public final class StoredRotationHostTest {
    @Test
    public void persistedRotationRestoresBeforeNotifyingAndKeepsLegacyMissingTagRules() {
        Host original = new Host();
        original.setFromPitchAndYaw(Direction.UP, Direction.EAST);
        CompoundTag tag = new CompoundTag();
        RotatableBaseBlockJava.saveForServer(original, tag);
        assertEquals(Direction.UP.ordinal(), tag.getInt("oc:pitch"));
        assertEquals(Direction.EAST.ordinal(), tag.getInt("oc:yaw"));
        Host restored = new Host();
        RotatableBaseBlockJava.loadForServer(restored, tag);
        assertEquals(Direction.UP, restored.notifiedPitch);
        assertEquals(Direction.EAST, restored.notifiedYaw);
        RotatableBaseBlockJava.loadForServer(restored, new CompoundTag());
        assertEquals(Direction.UP, restored.pitch());
        assertEquals(Direction.EAST, restored.yaw());
        RotatableBaseBlockJava.loadForClient(restored, new CompoundTag());
        assertEquals(Direction.DOWN, restored.pitch());
        assertEquals(Direction.SOUTH, restored.yaw());
    }

    @Test
    public void settersAndTransformsUseHostStateAndNotifyOnlyOnChange() {
        Host host = new Host();
        assertTrue(host.setFromPitchAndYaw(Direction.UP, Direction.EAST));
        assertFalse(host.setFromPitchAndYaw(Direction.UP, Direction.EAST));
        assertEquals(1, host.notifications);
        host.pitch_$eq(Direction.WEST);
        assertEquals(Direction.NORTH, host.pitch());
        host.yaw_$eq(Direction.DOWN);
        assertEquals(Direction.EAST, host.yaw());
        assertEquals(2, host.notifications);
        for (Direction pitch : new Direction[]{Direction.UP, Direction.NORTH, Direction.DOWN}) {
            for (Direction yaw : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                host.setFromPitchAndYaw(pitch, yaw);
                for (Direction direction : Direction.values()) assertEquals(direction, host.toLocal(host.toGlobal(direction)));
            }
        }
        assertNull(host.toGlobal(null));
        assertNull(host.toLocal(null));
    }

    @Test
    public void invalidSavedAxesAreNormalizedAndStatesBelongToIndividualHosts() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("oc:pitch", Direction.EAST.ordinal());
        tag.putInt("oc:yaw", Direction.UP.ordinal());
        Host first = new Host();
        Host second = new Host();
        RotatableBaseBlockJava.loadForClient(first, tag);
        assertEquals(Direction.NORTH, first.pitch());
        assertEquals(Direction.SOUTH, first.yaw());
        first.setFromFacing(Direction.WEST);
        assertEquals(Direction.SOUTH, second.yaw());
    }

    private static final class Host implements RotatableBaseBlock {
        private final RotatableBaseBlockJava state = new RotatableBaseBlockJava();
        private int notifications;
        private Direction notifiedPitch;
        private Direction notifiedYaw;
        @Override public RotatableBaseBlockJava rotationState() { return state; }
        @Override public Level getLevel() { return null; }
        @Override public BlockPos getBlockPos() { return BlockPos.ZERO; }
        @Override public BlockState getBlockState() { return null; }
        @Override public BlockState movingBlockState() { return null; }
        @Override public boolean isMoving() { return false; }
        @Override public boolean isServer() { return true; }
        @Override public void updateTranslation() {
            notifications++;
            notifiedPitch = pitch();
            notifiedYaw = yaw();
        }
    }
}
