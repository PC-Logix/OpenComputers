package li.cil.oc.client.gui.traits;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import li.cil.oc.integration.util.ItemSearch;
import li.cil.oc.api.internal.TextBuffer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Input routing policy for screens that display an OpenComputers text buffer. */
public final class InputBufferEventsJava {
    private InputBufferEventsJava() { }

    public static boolean shouldHandleTextInput(Screen screen) {
        return !(screen instanceof AbstractContainerScreen<?> && ItemSearch.isInputFocused());
    }

    public static boolean onInput(boolean pasteMatches, Supplier<TextBuffer> buffer, BooleanSupplier hasKeyboard,
                           Runnable paste, Runnable keyboardMissing) {
        if (!pasteMatches) return false;
        if (buffer.get() != null) {
            if (hasKeyboard.getAsBoolean()) paste.run();
            else keyboardMissing.run();
        }
        return true;
    }

    public static boolean charTyped(boolean handleTextInput, Supplier<TextBuffer> buffer, BooleanSupplier hasKeyboard,
                             Runnable queueChar, Runnable keyboardMissing, BooleanSupplier fallback) {
        if (handleTextInput && buffer.get() != null) {
            if (hasKeyboard.getAsBoolean()) queueChar.run();
            else keyboardMissing.run();
            return true;
        }
        return fallback.getAsBoolean();
    }

    public static boolean keyPressed(boolean handleTextInput, int keyCode,
                              boolean closeOnEscape, Runnable close, BooleanSupplier input,
                              Supplier<TextBuffer> buffer, BooleanSupplier hasKeyboard, Runnable queueKey,
                              Runnable keyboardMissing, BooleanSupplier fallback) {
        if (handleTextInput) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && closeOnEscape) {
                close.run();
                return true;
            }
            if (input.getAsBoolean()) return true;
            if (buffer.get() != null && keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
                if (hasKeyboard.getAsBoolean()) queueKey.run();
                else keyboardMissing.run();
                return true;
            }
        }
        return fallback.getAsBoolean();
    }

    public static boolean keyReleased(boolean handleTextInput, BooleanSupplier release,
                               BooleanSupplier fallback) {
        return handleTextInput && release.getAsBoolean() || fallback.getAsBoolean();
    }

    public static boolean mouseClicked(BooleanSupplier input, Supplier<TextBuffer> buffer, int button,
                                BooleanSupplier hasKeyboard, Runnable paste, Runnable keyboardMissing,
                                BooleanSupplier fallback) {
        if (input.getAsBoolean()) return true;
        if (buffer.get() != null && button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            if (hasKeyboard.getAsBoolean()) paste.run();
            else keyboardMissing.run();
            return true;
        }
        return fallback.getAsBoolean();
    }
}
