package li.cil.oc.common.blockentity.traits;

import li.cil.oc.common.blockentity.EnvironmentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;

/** A working block capability host with no Scala imports or generated state accessors. */
public final class JavaBlockEntityHostTest {
    @Test
    public void nativeNetworkHostRestoresRedstoneInItsEnvironmentPhase() {
        Host original = new Host();
        original._input()[Direction.SOUTH.ordinal()] = 7;
        original._output()[Direction.WEST.ordinal()] = 12;
        CompoundTag tag = new CompoundTag();
        original.saveForServer(tag, null);
        assertTrue(tag.getBoolean("oc:isServerData"));
        Host restored = new Host();
        restored.loadForServer(tag, null);
        assertEquals(7, restored.getInput(Direction.SOUTH));
        assertEquals(12, restored.getOutput(Direction.WEST));
        assertTrue(JavaBlockEntity.class.isInstance(restored));
    }

    @Test
    public void movingContextInitializesAfterCaptureAndTransformsNeighbors() {
        Host host = new Host();
        assertFalse(host.isMoving());
        host.beginMoving(null, new Vec3(12.5, 8, -1.2), value -> new Vec3(-value.z, value.y, value.x), null);
        assertTrue(host.isMoving());
        assertEquals(1, host.initializations);
        assertEquals(new BlockPos(12, 8, -2), host.movingBlockPos());
        assertEquals(Direction.SOUTH, host.movingDirection(Direction.EAST));
        assertEquals(new BlockPos(12, 8, -1), host.movingNeighbor(Direction.EAST));
        host.endMoving();
        assertFalse(host.isMoving());
        assertEquals(BlockPos.ZERO, host.movingBlockPos());
    }

    @Test
    public void coreProductionParentsUseTheNativeJavaRoot() {
        for (Class<?> type : new Class<?>[]{li.cil.oc.common.blockentity.Case.class,
                li.cil.oc.common.blockentity.Robot.class, li.cil.oc.common.blockentity.RobotProxy.class,
                li.cil.oc.common.blockentity.Rack.class, li.cil.oc.common.blockentity.Screen.class,
                li.cil.oc.common.blockentity.Print.class, li.cil.oc.common.blockentity.Relay.class}) {
            assertTrue(type.getName(), JavaBlockEntity.class.isAssignableFrom(type));

        }
    }

    private static final class Host extends EnvironmentBlockEntity implements RedstoneAware {
        private int initializations;
        Host() { super(null, BlockPos.ZERO, null); }
        // The fixture exercises off-world lifecycle without a registered block type.
        @Override public boolean isValidBlockState(net.minecraft.world.level.block.state.BlockState state) { return true; }
        @Override public li.cil.oc.api.network.Node node() { return null; }
        @Override public boolean isServer() { return true; }
        @Override protected void initialize() { super.initialize(); initializations++; }
    }
}



