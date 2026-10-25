package li.cil.oc.client.gui.traits;

import java.util.Arrays;
import org.lwjgl.glfw.GLFW;

public final class GLFWTranslator {
    private static final int[] toLWJGL = new int[GLFW.GLFW_KEY_LAST + 1];

    static {
        Arrays.fill(toLWJGL, -1);
        toLWJGL[GLFW.GLFW_KEY_SPACE] = 0x39;
        toLWJGL[GLFW.GLFW_KEY_APOSTROPHE] = 0x28;
        toLWJGL[GLFW.GLFW_KEY_COMMA] = 0x33;
        toLWJGL[GLFW.GLFW_KEY_MINUS] = 0x0C;
        toLWJGL[GLFW.GLFW_KEY_PERIOD] = 0x34;
        toLWJGL[GLFW.GLFW_KEY_SLASH] = 0x35;
        toLWJGL[GLFW.GLFW_KEY_0] = 0x0B;
        toLWJGL[GLFW.GLFW_KEY_1] = 0x02;
        toLWJGL[GLFW.GLFW_KEY_2] = 0x03;
        toLWJGL[GLFW.GLFW_KEY_3] = 0x04;
        toLWJGL[GLFW.GLFW_KEY_4] = 0x05;
        toLWJGL[GLFW.GLFW_KEY_5] = 0x06;
        toLWJGL[GLFW.GLFW_KEY_6] = 0x07;
        toLWJGL[GLFW.GLFW_KEY_7] = 0x08;
        toLWJGL[GLFW.GLFW_KEY_8] = 0x09;
        toLWJGL[GLFW.GLFW_KEY_9] = 0x0A;
        toLWJGL[GLFW.GLFW_KEY_SEMICOLON] = 0x27;
        toLWJGL[GLFW.GLFW_KEY_EQUAL] = 0x0D;
        toLWJGL[GLFW.GLFW_KEY_A] = 0x1E;
        toLWJGL[GLFW.GLFW_KEY_B] = 0x30;
        toLWJGL[GLFW.GLFW_KEY_C] = 0x2E;
        toLWJGL[GLFW.GLFW_KEY_D] = 0x20;
        toLWJGL[GLFW.GLFW_KEY_E] = 0x12;
        toLWJGL[GLFW.GLFW_KEY_F] = 0x21;
        toLWJGL[GLFW.GLFW_KEY_G] = 0x22;
        toLWJGL[GLFW.GLFW_KEY_H] = 0x23;
        toLWJGL[GLFW.GLFW_KEY_I] = 0x17;
        toLWJGL[GLFW.GLFW_KEY_J] = 0x24;
        toLWJGL[GLFW.GLFW_KEY_K] = 0x25;
        toLWJGL[GLFW.GLFW_KEY_L] = 0x26;
        toLWJGL[GLFW.GLFW_KEY_M] = 0x32;
        toLWJGL[GLFW.GLFW_KEY_N] = 0x31;
        toLWJGL[GLFW.GLFW_KEY_O] = 0x18;
        toLWJGL[GLFW.GLFW_KEY_P] = 0x19;
        toLWJGL[GLFW.GLFW_KEY_Q] = 0x10;
        toLWJGL[GLFW.GLFW_KEY_R] = 0x13;
        toLWJGL[GLFW.GLFW_KEY_S] = 0x1F;
        toLWJGL[GLFW.GLFW_KEY_T] = 0x14;
        toLWJGL[GLFW.GLFW_KEY_U] = 0x16;
        toLWJGL[GLFW.GLFW_KEY_V] = 0x2F;
        toLWJGL[GLFW.GLFW_KEY_W] = 0x11;
        toLWJGL[GLFW.GLFW_KEY_X] = 0x2D;
        toLWJGL[GLFW.GLFW_KEY_Y] = 0x15;
        toLWJGL[GLFW.GLFW_KEY_Z] = 0x2C;
        toLWJGL[GLFW.GLFW_KEY_LEFT_BRACKET] = 0x1A;
        toLWJGL[GLFW.GLFW_KEY_BACKSLASH] = 0x2B;
        toLWJGL[GLFW.GLFW_KEY_RIGHT_BRACKET] = 0x1B;
        toLWJGL[GLFW.GLFW_KEY_GRAVE_ACCENT] = 0x29;
        toLWJGL[GLFW.GLFW_KEY_WORLD_1] = 0x00;
        toLWJGL[GLFW.GLFW_KEY_WORLD_2] = 0x00;
        toLWJGL[GLFW.GLFW_KEY_ESCAPE] = 0x01;
        toLWJGL[GLFW.GLFW_KEY_ENTER] = 0x1C;
        toLWJGL[GLFW.GLFW_KEY_TAB] = 0x0F;
        toLWJGL[GLFW.GLFW_KEY_BACKSPACE] = 0x0E;
        toLWJGL[GLFW.GLFW_KEY_INSERT] = 0xD2;
        toLWJGL[GLFW.GLFW_KEY_DELETE] = 0xD3;
        toLWJGL[GLFW.GLFW_KEY_RIGHT] = 0xCD;
        toLWJGL[GLFW.GLFW_KEY_LEFT] = 0xCB;
        toLWJGL[GLFW.GLFW_KEY_DOWN] = 0xD0;
        toLWJGL[GLFW.GLFW_KEY_UP] = 0xC8;
        toLWJGL[GLFW.GLFW_KEY_PAGE_UP] = 0xC9;
        toLWJGL[GLFW.GLFW_KEY_PAGE_DOWN] = 0xD1;
        toLWJGL[GLFW.GLFW_KEY_HOME] = 0xC7;
        toLWJGL[GLFW.GLFW_KEY_END] = 0xCF;
        toLWJGL[GLFW.GLFW_KEY_CAPS_LOCK] = 0x3A;
        toLWJGL[GLFW.GLFW_KEY_SCROLL_LOCK] = 0x46;
        toLWJGL[GLFW.GLFW_KEY_NUM_LOCK] = 0x45;
        toLWJGL[GLFW.GLFW_KEY_PRINT_SCREEN] = 0xB7;
        toLWJGL[GLFW.GLFW_KEY_PAUSE] = 0xC5;
        toLWJGL[GLFW.GLFW_KEY_F1] = 0x3B;
        toLWJGL[GLFW.GLFW_KEY_F2] = 0x3C;
        toLWJGL[GLFW.GLFW_KEY_F3] = 0x3D;
        toLWJGL[GLFW.GLFW_KEY_F4] = 0x3E;
        toLWJGL[GLFW.GLFW_KEY_F5] = 0x3F;
        toLWJGL[GLFW.GLFW_KEY_F6] = 0x40;
        toLWJGL[GLFW.GLFW_KEY_F7] = 0x41;
        toLWJGL[GLFW.GLFW_KEY_F8] = 0x42;
        toLWJGL[GLFW.GLFW_KEY_F9] = 0x43;
        toLWJGL[GLFW.GLFW_KEY_F10] = 0x44;
        toLWJGL[GLFW.GLFW_KEY_F11] = 0x57;
        toLWJGL[GLFW.GLFW_KEY_F12] = 0x58;
        toLWJGL[GLFW.GLFW_KEY_F13] = 0x64;
        toLWJGL[GLFW.GLFW_KEY_F14] = 0x65;
        toLWJGL[GLFW.GLFW_KEY_F15] = 0x66;
        toLWJGL[GLFW.GLFW_KEY_F16] = 0x67;
        toLWJGL[GLFW.GLFW_KEY_F17] = 0x68;
        toLWJGL[GLFW.GLFW_KEY_F18] = 0x69;
        toLWJGL[GLFW.GLFW_KEY_F19] = 0x71;
        toLWJGL[GLFW.GLFW_KEY_F20] = -1;
        toLWJGL[GLFW.GLFW_KEY_F21] = -1;
        toLWJGL[GLFW.GLFW_KEY_F22] = -1;
        toLWJGL[GLFW.GLFW_KEY_F23] = -1;
        toLWJGL[GLFW.GLFW_KEY_F24] = -1;
        toLWJGL[GLFW.GLFW_KEY_F25] = -1;
        toLWJGL[GLFW.GLFW_KEY_KP_0] = 0x52;
        toLWJGL[GLFW.GLFW_KEY_KP_1] = 0x4F;
        toLWJGL[GLFW.GLFW_KEY_KP_2] = 0x50;
        toLWJGL[GLFW.GLFW_KEY_KP_3] = 0x51;
        toLWJGL[GLFW.GLFW_KEY_KP_4] = 0x4B;
        toLWJGL[GLFW.GLFW_KEY_KP_5] = 0x4C;
        toLWJGL[GLFW.GLFW_KEY_KP_6] = 0x4D;
        toLWJGL[GLFW.GLFW_KEY_KP_7] = 0x47;
        toLWJGL[GLFW.GLFW_KEY_KP_8] = 0x48;
        toLWJGL[GLFW.GLFW_KEY_KP_9] = 0x49;
        toLWJGL[GLFW.GLFW_KEY_KP_DECIMAL] = 0x53;
        toLWJGL[GLFW.GLFW_KEY_KP_DIVIDE] = 0xB5;
        toLWJGL[GLFW.GLFW_KEY_KP_MULTIPLY] = 0x37;
        toLWJGL[GLFW.GLFW_KEY_KP_SUBTRACT] = 0x4A;
        toLWJGL[GLFW.GLFW_KEY_KP_ADD] = 0x4E;
        toLWJGL[GLFW.GLFW_KEY_KP_ENTER] = 0x9C;
        toLWJGL[GLFW.GLFW_KEY_KP_EQUAL] = 0x8D;
        toLWJGL[GLFW.GLFW_KEY_LEFT_SHIFT] = 0x2A;
        toLWJGL[GLFW.GLFW_KEY_LEFT_CONTROL] = 0x1D;
        toLWJGL[GLFW.GLFW_KEY_LEFT_ALT] = 0x38;
        toLWJGL[GLFW.GLFW_KEY_LEFT_SUPER] = 0xDB;
        toLWJGL[GLFW.GLFW_KEY_RIGHT_SHIFT] = 0x36;
        toLWJGL[GLFW.GLFW_KEY_RIGHT_CONTROL] = 0x9D;
        toLWJGL[GLFW.GLFW_KEY_RIGHT_ALT] = 0xB8;
        toLWJGL[GLFW.GLFW_KEY_RIGHT_SUPER] = 0xDC;
        toLWJGL[GLFW.GLFW_KEY_MENU] = 0xDD;
    }

    private GLFWTranslator() {}

    private static int layoutKeyCode(int keyCode, int scanCode) {
        // GLFW key tokens describe positions on a US layout. The key name describes
        // the printable key at that position in the active keyboard layout.
        if (keyCode < GLFW.GLFW_KEY_SPACE || keyCode > GLFW.GLFW_KEY_WORLD_2) return keyCode;

        String name = GLFW.glfwGetKeyName(keyCode, scanCode);
        if (name == null || name.length() != 1) return keyCode;

        char c = name.charAt(0);
        if (c >= 'a' && c <= 'z') return GLFW.GLFW_KEY_A + c - 'a';
        if (c >= 'A' && c <= 'Z') return GLFW.GLFW_KEY_A + c - 'A';
        if (c >= '0' && c <= '9') return GLFW.GLFW_KEY_0 + c - '0';
        return switch (c) {
            case ' ' -> GLFW.GLFW_KEY_SPACE;
            case '\'' -> GLFW.GLFW_KEY_APOSTROPHE;
            case ',' -> GLFW.GLFW_KEY_COMMA;
            case '-' -> GLFW.GLFW_KEY_MINUS;
            case '.' -> GLFW.GLFW_KEY_PERIOD;
            case '/' -> GLFW.GLFW_KEY_SLASH;
            case ';' -> GLFW.GLFW_KEY_SEMICOLON;
            case '=' -> GLFW.GLFW_KEY_EQUAL;
            case '[' -> GLFW.GLFW_KEY_LEFT_BRACKET;
            case '\\' -> GLFW.GLFW_KEY_BACKSLASH;
            case ']' -> GLFW.GLFW_KEY_RIGHT_BRACKET;
            case '`' -> GLFW.GLFW_KEY_GRAVE_ACCENT;
            default -> keyCode;
        };
    }

    public static int glfwToLWJGL(int keyCode, int scanCode) {
        int translatedKeyCode = layoutKeyCode(keyCode, scanCode);
        return translatedKeyCode >= 0 && translatedKeyCode < toLWJGL.length ? toLWJGL[translatedKeyCode] : -1;
    }

    public static char keyToChar(int keyCode, int scanCode, int mods) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return '\u001B';
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) return '\r';
        if (keyCode == GLFW.GLFW_KEY_TAB) return '\t';
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) return '\b';
        if ((mods & GLFW.GLFW_MOD_CONTROL) != 0) {
            char c = (char) Math.max(0, layoutKeyCode(keyCode, scanCode) - 64);
            return c > 32 ? '\u0000' : c;
        }
        return '\u0000';
    }
}
