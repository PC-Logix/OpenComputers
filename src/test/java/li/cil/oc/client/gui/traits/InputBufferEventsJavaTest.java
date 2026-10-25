package li.cil.oc.client.gui.traits;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class InputBufferEventsJavaTest {
    @Test
    public void unmatchedPasteDoesNotResolveLazyBuffer() {
        AtomicInteger bufferAccesses = new AtomicInteger();
        assertFalse(InputBufferEventsJava.onInput(false, () -> {
            bufferAccesses.incrementAndGet();
            return null;
        }, () -> true, () -> { }, () -> { }));
        assertEquals(0, bufferAccesses.get());
    }

    @Test
    public void focusedSearchFallsThroughWithoutResolvingBuffer() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        boolean handled = InputBufferEventsJava.charTyped(false, () -> {
            throw new AssertionError("buffer should not be forced");
        }, () -> true, () -> { }, () -> { }, () -> {
            fallbackCalls.incrementAndGet();
            return true;
        });
        assertTrue(handled);
        assertEquals(1, fallbackCalls.get());
    }

    @Test
    public void escapeClosesBeforePasteOrBufferChecks() {
        AtomicInteger closeCalls = new AtomicInteger();
        boolean handled = InputBufferEventsJava.keyPressed(true, org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,
            true, closeCalls::incrementAndGet, () -> {
                throw new AssertionError("paste should not be checked");
            }, () -> {
                throw new AssertionError("buffer should not be forced");
            }, () -> true, () -> { }, () -> { }, () -> false);
        assertTrue(handled);
        assertEquals(1, closeCalls.get());
    }

    @Test
    public void middleClickChecksPasteBeforeBuffer() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        boolean handled = InputBufferEventsJava.mouseClicked(() -> true, () -> {
            throw new AssertionError("buffer should not be forced");
        }, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE, () -> true,
            () -> { }, () -> { }, () -> {
                fallbackCalls.incrementAndGet();
                return false;
            });
        assertTrue(handled);
        assertEquals(0, fallbackCalls.get());
    }
}
