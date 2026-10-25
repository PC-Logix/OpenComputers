package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ComputerComponentAccessJavaTest {
    @Test
    public void appliesClientOwnershipAllowListOnlyWhenEnabled() {
        ComputerComponentAccessJava access = new ComputerComponentAccessJava();
        access.setUsers(List.of("owner"));

        assertTrue(access.canInteractClient(true, "owner"));
        assertFalse(access.canInteractClient(true, "visitor"));
        assertTrue(access.canInteractClient(false, "visitor"));

        access.setUsers(List.of());
        assertTrue(access.canInteractClient(true, "visitor"));
    }
}
