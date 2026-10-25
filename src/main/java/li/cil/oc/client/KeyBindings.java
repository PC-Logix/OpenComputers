package li.cil.oc.client;

import com.mojang.blaze3d.platform.InputConstants;
import li.cil.oc.OpenComputers;
import li.cil.oc.client.gui.traits.InputBuffer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    private KeyBindings() {}

    public static final IKeyConflictContext textInputConflict = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return Minecraft.getInstance().screen instanceof InputBuffer;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return this == other;
        }
    };

    public static final KeyMapping extendedTooltip = new KeyMapping("key.opencomputers.extendedTooltip",
        KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, OpenComputers.Name());
    public static final KeyMapping analyzeCopyAddr = new KeyMapping("key.opencomputers.analyzeCopyAddress",
        KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, OpenComputers.Name());
    public static final KeyMapping clipboardPaste = new KeyMapping("key.opencomputers.clipboardPaste",
        textInputConflict, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_INSERT, OpenComputers.Name());

    public static boolean showExtendedTooltips() {
        if (extendedTooltip.isDown()) return true;
        if (!extendedTooltip.getKeyConflictContext().isActive()) return false;
        if (extendedTooltip.getKey().equals(InputConstants.UNKNOWN)) return false;
        KeyModifier modifier = extendedTooltip.getKeyModifier();
        if (modifier == KeyModifier.NONE) {
            return KeyModifier.isKeyCodeModifier(extendedTooltip.getKey()) && isActive(extendedTooltip.getKey());
        }
        return modifier.isActive(extendedTooltip.getKeyConflictContext()) && isActive(extendedTooltip.getKey());
    }

    public static boolean isAnalyzeCopyingAddress() {
        return analyzeCopyAddr.isDown();
    }

    private static boolean isActive(InputConstants.Key input) {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (input.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, input.getValue()) == GLFW.GLFW_PRESS;
        }
        if (input.getType() == InputConstants.Type.SCANCODE) return false;
        if (input.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, input.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
