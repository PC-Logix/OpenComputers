package li.cil.oc.common.blockentity.traits;

import li.cil.oc.util.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public final class RedstoneHostTest {
    @Test
    public void inventoryPhaseRestoresBothScalarAndBundledStateOnlyAfterInventory() {
        Host source = new Host(true);
        source._input()[Direction.WEST.ordinal()] = 7;
        source._output()[Direction.NORTH.ordinal()] = 12;
        source._bundledOutput()[Direction.EAST.ordinal()][4] = 99;
        source._rednetInput()[Direction.SOUTH.ordinal()][5] = 77;
        CompoundTag saved = new CompoundTag();
        RedstoneLifecycleJava.saveServer(source, RedstoneLifecycleJava.Phase.BASE, saved);
        RedstoneLifecycleJava.saveServer(source, RedstoneLifecycleJava.Phase.ENVIRONMENT, saved);
        assertFalse(saved.contains("oc:rs.output"));
        RedstoneLifecycleJava.saveServer(source, RedstoneLifecycleJava.Phase.INVENTORY, saved);
        Host restored = new Host(true);
        RedstoneLifecycleJava.loadServer(restored, RedstoneLifecycleJava.Phase.ENVIRONMENT, saved);
        assertEquals(0, restored.getOutput(Direction.NORTH));
        RedstoneLifecycleJava.loadServer(restored, RedstoneLifecycleJava.Phase.INVENTORY, saved);
        assertEquals(7, restored.getInput(Direction.WEST));
        assertEquals(12, restored.getOutput(Direction.NORTH));
        assertEquals(99, restored.getBundledOutput(Direction.EAST, 4));
        assertEquals(77, restored.getBundledInput(Direction.SOUTH, 5));
        assertTrue(restored.changes.isEmpty());
    }

    @Test
    public void initialInputsAreQuietAndOutputDisablingClearsBothKindsBeforeCallback() {
        Host host = new Host(false);
        host.setInput(Direction.NORTH, 2);
        host.setBundledInput(Direction.NORTH, 3, 10);
        assertTrue(host.changes.isEmpty());
        host.setInput(Direction.NORTH, 5);
        host.setBundledInput(Direction.NORTH, 3, 12);
        assertEquals(2, host.changes.size());
        host.setOutputEnabled(true);
        host.setOutput(Direction.EAST, 8);
        host.setBundledOutput(Direction.EAST, 3, 150);
        host.setOutputEnabled(false);
        assertEquals(0, host.getOutput(Direction.EAST));
        assertEquals(0, host.getBundledOutput(Direction.EAST, 3));
        assertEquals(2, host.enabledNotifications);
    }

    @Test
    public void clientSnapshotKeepsUnprefixedTagsAndItsOwnPersistencePhase() {
        Host source = new Host(true);
        source.setOutputEnabled(true);
        source.setOutput(Direction.UP, 13);
        CompoundTag saved = new CompoundTag();
        RedstoneLifecycleJava.saveClient(source, false, saved);
        assertFalse(saved.contains("output"));
        RedstoneLifecycleJava.saveClient(source, true, saved);
        assertFalse(saved.contains("oc:rs.output"));
        Host restored = new Host(true);
        RedstoneLifecycleJava.loadClient(restored, false, saved);
        assertEquals(0, restored.getOutput(Direction.UP));
        RedstoneLifecycleJava.loadClient(restored, true, saved);
        assertEquals(13, restored.getOutput(Direction.UP));
        assertTrue(restored.isOutputEnabled());
        assertEquals(0, restored.enabledNotifications);
    }

    private static final class Host implements BundledRedstoneAware {
        private final RedstoneStateJava scalar = new RedstoneStateJava();
        private final BundledRedstoneStateJava bundled = new BundledRedstoneStateJava();
        private final boolean afterInventory;
        private final List<RedstoneChangedEventArgs> changes = new ArrayList<>();
        private int enabledNotifications;
        Host(boolean afterInventory) { this.afterInventory = afterInventory; }
        @Override public RedstoneStateJava redstoneState() { return scalar; }
        @Override public BundledRedstoneStateJava bundledRedstoneState() { return bundled; }
        @Override public boolean redstoneAfterInventory() { return afterInventory; }
        @Override public boolean isServer() { return true; }
        @Override public boolean isMoving() { return false; }
        @Override public BlockPos movingBlockPos() { return BlockPos.ZERO; }
        @Override public BlockState movingBlockState() { return null; }
        @Override public Direction movingDirection(Direction side) { return side; }
        @Override public BlockPos movingNeighbor(Direction side) { return BlockPos.ZERO; }
        @Override public BlockPosition position() { return null; }
        @Override public void onRedstoneInputChanged(RedstoneChangedEventArgs args) { changes.add(args); }
        @Override public void onRedstoneOutputChanged(Direction side) { }
        @Override public void onRedstoneOutputEnabledChanged() {
            enabledNotifications++;
            if (!isOutputEnabled()) {
                assertEquals(0, getOutput(Direction.EAST));
                assertEquals(0, getBundledOutput(Direction.EAST, 3));
            }
        }
    }
}
