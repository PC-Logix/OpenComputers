package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;

import static org.junit.Assert.*;

public final class OpenSidesHostTest {
    @Test
    public void missingServerDataRetainsHostDefaultsButMissingClientDataClearsThem() {
        Host host = new Host();
        OpenSidesJava.loadServerHost(host, new CompoundTag());
        assertTrue(host.isSideOpen(Direction.NORTH));
        OpenSidesJava.loadClientHost(host, new CompoundTag());
        assertFalse(host.isSideOpen(Direction.NORTH));
        assertTrue(new Host().isSideOpen(Direction.NORTH));
    }

    @Test
    public void nativeHostMaskRoundTripsExistingTag() {
        Host source = new Host();
        source.setSideOpen(Direction.EAST, false);
        CompoundTag saved = new CompoundTag();
        OpenSidesJava.saveHost(source, saved);
        Host restored = new Host();
        OpenSidesJava.loadClientHost(restored, saved);
        assertTrue(saved.contains("oc:openSides"));
        assertEquals(source.compressSides(), restored.compressSides());
        assertFalse(restored.isSideOpen(Direction.EAST));
        assertTrue(restored.isSideOpen(Direction.WEST));
    }

    private static final class Host implements OpenSides {
        private final OpenSidesJava.State state = new OpenSidesJava.State(SideCount(), this::defaultState);
        @Override public OpenSidesJava.State openSidesState() { return state; }
        @Override public boolean defaultState() { return true; }
    }
}
