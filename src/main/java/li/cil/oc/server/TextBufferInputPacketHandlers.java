package li.cil.oc.server;

import li.cil.oc.Settings;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.blockentity.Screen;
import li.cil.oc.common.datacomponents.CompoundStorage;
import li.cil.oc.common.component.RemoteTerminalHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.util.Optional;


/** Server handlers for text-buffer keyboard, clipboard, and mouse input packets. */
public final class TextBufferInputPacketHandlers {
    private TextBufferInputPacketHandlers() {}

    public static void onKeyDown(PacketParser packet) throws IOException {
        String address = packet.readUTF();
        char key = packet.readChar();
        int code = packet.readInt();
        TextBuffer buffer = interactableBuffer(packet, address);
        if (buffer != null) buffer.keyDown(key, code, packet.player);
    }

    public static void onKeyUp(PacketParser packet) throws IOException {
        String address = packet.readUTF();
        char key = packet.readChar();
        int code = packet.readInt();
        TextBuffer buffer = interactableBuffer(packet, address);
        if (buffer != null) buffer.keyUp(key, code, packet.player);
    }

    public static void onTextInput(PacketParser packet) throws IOException {
        String address = packet.readUTF();
        int codePoint = packet.readInt();
        if (codePoint < 0 || codePoint > Character.MAX_CODE_POINT) return;
        TextBuffer buffer = interactableBuffer(packet, address);
        if (buffer != null) buffer.textInput(codePoint, packet.player);
    }

    public static void onClipboard(PacketParser packet) throws IOException {
        String address = packet.readUTF();
        String copy = packet.readUTF();
        if (copy.length() > Settings.get().maxClipboardTextLength()) return;
        TextBuffer buffer = interactableBuffer(packet, address);
        if (buffer != null) buffer.clipboard(copy, packet.player);
    }

    public static void onMouseClick(PacketParser packet) throws IOException {
        String address = packet.readUTF();
        float x = packet.readFloat();
        float y = packet.readFloat();
        boolean dragging = packet.readBoolean();
        byte button = packet.readByte();
        TextBuffer buffer = interactableBuffer(packet, address);
        if (buffer == null) return;
        if (dragging) buffer.mouseDrag(x, y, button, packet.player);
        else buffer.mouseDown(x, y, button, packet.player);
    }

    public static void onMouseUp(PacketParser packet) throws IOException {
        String address = packet.readUTF();
        float x = packet.readFloat();
        float y = packet.readFloat();
        byte button = packet.readByte();
        TextBuffer buffer = interactableBuffer(packet, address);
        if (buffer != null) buffer.mouseUp(x, y, button, packet.player);
    }

    public static void onMouseScroll(PacketParser packet) throws IOException {
        String address = packet.readUTF();
        float x = packet.readFloat();
        float y = packet.readFloat();
        byte button = packet.readByte();
        TextBuffer buffer = interactableBuffer(packet, address);
        if (buffer != null) buffer.mouseScroll(x, y, button, packet.player);
    }

    public static void onCopyToAnalyzer(PacketParser packet) throws IOException {
        String address = packet.readUTF();
        int line = packet.readInt();
        Optional<ManagedEnvironment> tracked = li.cil.oc.server.ComponentTracker.INSTANCE.getOptional(packet.player.level(), address);
        if (tracked.isPresent() && tracked.get() instanceof li.cil.oc.common.component.TextBuffer buffer) {
            buffer.copyToAnalyzer(line, packet.player);
        }
    }

    public static void onTextBufferInit(PacketParser packet) throws IOException {
        String address = packet.readUTF();
        if (!(packet.player instanceof ServerPlayer player)) return;

        Optional<ManagedEnvironment> tracked = li.cil.oc.server.ComponentTracker.INSTANCE.getOptional(player.level(), address);
        if (tracked.isEmpty() || !(tracked.get() instanceof li.cil.oc.common.component.TextBuffer buffer)) return;

        Object host = buffer.host();
        if (host instanceof Screen screen) screen.ensureServerBufferLoaded();
        if (host instanceof Screen screen && !screen.isOrigin()) return;

        CompoundStorage data = new CompoundStorage();
        buffer.data().saveData(data);
        CompoundTag encoded = (CompoundTag) CompoundStorage.CODEC.encode(data, NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
        TextBufferPacketEncoder.sendInit(address, encoded,
            buffer.getMaximumWidth(), buffer.getMaximumHeight(), buffer.getMaximumColorDepth().ordinal(),
            buffer.getViewportWidth(), buffer.getViewportHeight(), player);
    }

    private static TextBuffer interactableBuffer(PacketParser packet, String address) {
        Optional<ManagedEnvironment> tracked = li.cil.oc.server.ComponentTracker.INSTANCE.getOptional(packet.player.level(), address);
        if (tracked.isEmpty() || !(tracked.get() instanceof TextBuffer buffer)) return null;
        return canInteractWith(buffer, packet.player) ? buffer : null;
    }

    private static boolean canInteractWith(TextBuffer buffer, Player player) {
        if (!(buffer instanceof li.cil.oc.common.component.TextBuffer textBuffer)) return false;
        Object host = textBuffer.host();
        if (host instanceof Screen screen) {
            for (Screen part : screen.screensJava()) {
                if (player.distanceToSqr(part.xPosition(), part.yPosition(), part.zPosition()) <= 64) return true;
            }
            return false;
        }
        if (host instanceof RemoteTerminalHost remote) return remote.isBufferUsable(buffer, player);
        return true;
    }
}
