package li.cil.oc.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.client.KeyBindings;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.traits.DisplayBuffer;
import li.cil.oc.client.gui.traits.InputBuffer;
import li.cil.oc.client.gui.traits.InputBufferEventsJava;
import li.cil.oc.client.gui.traits.InputBufferStateJava;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Java superclass for text-buffer screens that directly extend Minecraft Screen. */
public abstract class ScreenInputBufferBase extends Screen implements InputBuffer, DisplayBuffer, ContainerEventHandler {
    private final InputBufferStateJava inputState = new InputBufferStateJava();

    protected ScreenInputBufferBase() {
        super(Component.empty());
    }

    public abstract TextBuffer buffer();
    public abstract boolean hasKeyboard();

    @Override public int bufferColumns() { return buffer() == null ? 0 : buffer().getViewportWidth(); }
    @Override public int bufferRows() { return buffer() == null ? 0 : buffer().getViewportHeight(); }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void drawBufferLayer(GuiGraphics graphics) {
        DisplayBuffer.super.drawBufferLayer(graphics);
        if (System.currentTimeMillis() - inputState.keyboardMissingAt() < 1000) {
            int x = bufferX() + buffer().renderWidth() - 16;
            int y = bufferY() + buffer().renderHeight() - 16;
            graphics.blitSprite(Textures.GUISprites.KeyboardMissing, x, y, 16, 16);
        }
    }

    @Override
    public void removed() {
        super.removed();
        inputState.released(buffer());
    }

    public boolean onInput(InputConstants.Key input) {
        return InputBufferEventsJava.onInput(KeyBindings.clipboardPaste.isActiveAndMatches(input), this::buffer,
            this::hasKeyboard, () -> buffer().clipboard(Minecraft.getInstance().keyboardHandler.getClipboard(), null),
            inputState::showKeyboardMissing);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return InputBufferEventsJava.charTyped(InputBufferEventsJava.shouldHandleTextInput(this), this::buffer,
            this::hasKeyboard, () -> inputState.pushQueuedChar(buffer(), codePoint), inputState::showKeyboardMissing,
            () -> super.charTyped(codePoint, modifiers));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return InputBufferEventsJava.keyPressed(InputBufferEventsJava.shouldHandleTextInput(this), keyCode,
            shouldCloseOnEsc(), this::onClose, () -> onInput(InputConstants.getKey(keyCode, scanCode)), this::buffer,
            this::hasKeyboard, () -> inputState.pushQueuedKey(buffer(), keyCode, scanCode, modifiers),
            inputState::showKeyboardMissing, () -> super.keyPressed(keyCode, scanCode, modifiers));
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return InputBufferEventsJava.keyReleased(InputBufferEventsJava.shouldHandleTextInput(this),
            () -> inputState.keyReleased(buffer(), keyCode), () -> super.keyReleased(keyCode, scanCode, modifiers));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return InputBufferEventsJava.mouseClicked(() -> onInput(InputConstants.Type.MOUSE.getOrCreate(button)),
            this::buffer, button, this::hasKeyboard,
            () -> buffer().clipboard(Minecraft.getInstance().keyboardHandler.getClipboard(), null),
            inputState::showKeyboardMissing, () -> super.mouseClicked(mouseX, mouseY, button));
    }
}
