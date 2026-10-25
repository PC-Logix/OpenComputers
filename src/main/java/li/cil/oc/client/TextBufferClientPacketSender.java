package li.cil.oc.client;

import li.cil.oc.Settings;
import li.cil.oc.common.CompressedPacketBuilder;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.io.IOException;

public final class TextBufferClientPacketSender {
    private static final int CLIPBOARD_PACKET_SIZE = 16 * 1024;
    private static long clipboardCooldown;

    private TextBufferClientPacketSender() {}

    public static void sendTextBufferInit(String address) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.TextBufferInit);
            packet.writeUTF(address);
            packet.sendToServer();
        });
    }

    public static void sendKeyDown(String address, char character, int code) {
        sendKey(PacketType.KeyDown, address, character, code);
    }

    public static void sendKeyUp(String address, char character, int code) {
        sendKey(PacketType.KeyUp, address, character, code);
    }

    private static void sendKey(PacketType type, String address, char character, int code) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(type);
            packet.writeUTF(address);
            packet.writeChar(character);
            packet.writeInt(code);
            packet.sendToServer();
        });
    }

    public static void sendTextInput(String address, int codePoint) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.TextInput);
            packet.writeUTF(address);
            packet.writeInt(codePoint);
            packet.sendToServer();
        });
    }

    public static void sendClipboard(String address, String value) {
        if (value == null || value.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (value.length() > Settings.get().maxClipboardTextLength() || now < clipboardCooldown) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HARP.value(), 1, 1));
            return;
        }

        clipboardCooldown = now + value.length() / 10;
        for (int start = 0; start < value.length(); start += CLIPBOARD_PACKET_SIZE) {
            String part = value.substring(start, Math.min(value.length(), start + CLIPBOARD_PACKET_SIZE));
            send(() -> {
                CompressedPacketBuilder packet = new CompressedPacketBuilder(PacketType.Clipboard,
                        CompressedPacketBuilder.$lessinit$greater$default$2());
                packet.writeUTF(address);
                packet.writeUTF(part);
                packet.sendToServer();
            });
        }
    }

    public static void sendMouseClick(String address, double x, double y, boolean drag, int button) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.MouseClickOrDrag);
            packet.writeUTF(address);
            packet.writeFloat((float) x);
            packet.writeFloat((float) y);
            packet.writeBoolean(drag);
            packet.writeByte((byte) button);
            packet.sendToServer();
        });
    }

    public static void sendMouseScroll(String address, double x, double y, int scroll) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.MouseScroll);
            packet.writeUTF(address);
            packet.writeFloat((float) x);
            packet.writeFloat((float) y);
            packet.writeByte(scroll);
            packet.sendToServer();
        });
    }

    public static void sendMouseUp(String address, double x, double y, int button) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.MouseUp);
            packet.writeUTF(address);
            packet.writeFloat((float) x);
            packet.writeFloat((float) y);
            packet.writeByte((byte) button);
            packet.sendToServer();
        });
    }

    public static void sendCopyToAnalyzer(String address, int line) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.CopyToAnalyzer);
            packet.writeUTF(address);
            packet.writeInt(line);
            packet.sendToServer();
        });
    }

    private static void send(PacketAction action) {
        try {
            action.send();
        } catch (IOException error) {
            TextBufferClientPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }

    @FunctionalInterface
    private interface PacketAction {
        void send() throws IOException;
    }
}
