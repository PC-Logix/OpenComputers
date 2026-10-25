package li.cil.oc.common.blockentity.traits;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class TextBufferPersistenceJavaTest {
    @Test
    public void defersServerRestoreAndDoesNotReplaceSavedDataUntilLevelIsAvailable() throws Exception {
        TextBufferPersistenceJava persistence = new TextBufferPersistenceJava();
        CompoundTag original = new CompoundTag();
        original.putInt("screenContents", 42);

        persistence.deferServerBufferData(original);
        original.putInt("screenContents", 0);

        assertTrue(persistence.hasPendingServerBufferData());
        persistence.ensureServerBufferLoaded(false, null, null, null);
        assertTrue(persistence.hasPendingServerBufferData());
        persistence.ensureServerBufferLoaded(true, null, null, null);
        assertTrue(persistence.hasPendingServerBufferData());

        CompoundTag saved = new CompoundTag();
        saved.putInt("existingScreenContents", 17);
        persistence.saveForServer(saved, null, null);
        assertEquals(17, saved.getInt("existingScreenContents"));
        assertTrue(persistence.hasPendingServerBufferData());
    }
}
