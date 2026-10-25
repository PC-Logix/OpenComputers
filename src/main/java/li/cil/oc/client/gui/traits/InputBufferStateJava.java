package li.cil.oc.client.gui.traits;

import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.internal.TextBuffer;
import org.lwjgl.glfw.GLFW;

/** Java-owned input state shared by the text-buffer screen bases. */
public final class InputBufferStateJava {
    private static final class PressedKey {
        private final char character;
        private final int lwjglCode;

        private PressedKey(char character, int lwjglCode) {
            this.character = character;
            this.lwjglCode = lwjglCode;
        }
    }

    private final Map<Integer, PressedKey> pressedKeys = new HashMap<>();

    private long showKeyboardMissing;
    private boolean hasQueuedKey;
    private int queuedKey;
    private int queuedScanCode;
    private char queuedChar = '\u0000';
    private char highSurrogate = '\u0000';

    public long keyboardMissingAt() {
        return showKeyboardMissing;
    }

    public void showKeyboardMissing() {
        showKeyboardMissing = System.currentTimeMillis();
    }

    public void pushQueuedKey(TextBuffer buffer, int keyCode, int scanCode, int mods) {
        flushQueuedKey(buffer);
        hasQueuedKey = true;
        queuedKey = keyCode;
        queuedScanCode = scanCode;
        queuedChar = GLFWTranslator.keyToChar(keyCode, scanCode, mods);
    }

    public void pushQueuedChar(TextBuffer buffer, char character) {
        if (hasQueuedKey) {
            if (!Character.isSurrogate(character)) queuedChar = character;
            // Flush either way as the next code point will be unrelated.
            flushQueuedKey(buffer);
        }

        // text_input happens independent of key events.
        if (Character.isSurrogate(character)) {
            // Convert two successive surrogates back into a code point.
            if (Character.isHighSurrogate(character)) highSurrogate = character;
            else buffer.textInput(Character.toCodePoint(highSurrogate, character), null);
        } else {
            buffer.textInput(character, null);
        }
    }

    public void flushQueuedKey(TextBuffer buffer) {
        if (hasQueuedKey) {
            hasQueuedKey = false;
            if (!pressedKeys.containsKey(queuedKey) || !ignoreRepeat(queuedKey)) {
                int lwjglCode = GLFWTranslator.glfwToLWJGL(queuedKey, queuedScanCode);
                if (lwjglCode > 0) {
                    pressedKeys.put(queuedKey, new PressedKey(queuedChar, lwjglCode));
                    if (buffer != null) buffer.keyDown(queuedChar, lwjglCode, null);
                } else if (queuedChar > 0) {
                    pressedKeys.put(queuedKey, new PressedKey(queuedChar, 0));
                    if (buffer != null) buffer.keyDown(queuedChar, 0, null);
                }
            }
        }
    }

    public void released(TextBuffer buffer) {
        if (buffer != null) {
            flushQueuedKey(buffer);
            for (PressedKey key : pressedKeys.values()) {
                buffer.keyUp(key.character, key.lwjglCode, null);
            }
        }
    }

    public boolean keyReleased(TextBuffer buffer, int keyCode) {
        flushQueuedKey(buffer);
        PressedKey key = pressedKeys.remove(keyCode);
        if (key == null) return false;

        if (key.lwjglCode > 0) {
            buffer.keyUp(key.character, key.lwjglCode, null);
            return true;
        } else if (key.character > 0) {
            buffer.keyUp(key.character, 0, null);
            return true;
        }
        return false;
    }

    private static boolean ignoreRepeat(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL ||
            keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL ||
            keyCode == GLFW.GLFW_KEY_MENU ||
            keyCode == GLFW.GLFW_KEY_LEFT_ALT ||
            keyCode == GLFW.GLFW_KEY_RIGHT_ALT ||
            keyCode == GLFW.GLFW_KEY_LEFT_SHIFT ||
            keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT ||
            keyCode == GLFW.GLFW_KEY_LEFT_SUPER ||
            keyCode == GLFW.GLFW_KEY_RIGHT_SUPER;
    }
}
