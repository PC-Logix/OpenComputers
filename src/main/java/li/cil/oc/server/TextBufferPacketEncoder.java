package li.cil.oc.server;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.CompressedPacketBuilder;
import li.cil.oc.common.PacketBuilder;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.util.PackedColorColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

public final class TextBufferPacketEncoder {
    private TextBufferPacketEncoder() {}

    public static void colorChange(PacketBuilder packet, PackedColorColor foreground, PackedColorColor background) throws IOException {
        packet.writePacketType(PacketType.TextBufferMultiColorChange);
        packet.writeInt(foreground.value());
        packet.writeBoolean(foreground.isPalette());
        packet.writeInt(background.value());
        packet.writeBoolean(background.isPalette());
    }

    public static void copy(PacketBuilder packet, int col, int row, int width, int height, int tx, int ty) throws IOException {
        packet.writePacketType(PacketType.TextBufferMultiCopy);
        packet.writeInt(col); packet.writeInt(row); packet.writeInt(width); packet.writeInt(height); packet.writeInt(tx); packet.writeInt(ty);
    }

    public static void depthChange(PacketBuilder packet, TextBuffer.ColorDepth depth) throws IOException {
        packet.writePacketType(PacketType.TextBufferMultiDepthChange);
        packet.writeInt(depth.ordinal());
    }

    public static void fill(PacketBuilder packet, int col, int row, int width, int height, int color) throws IOException {
        packet.writePacketType(PacketType.TextBufferMultiFill);
        packet.writeInt(col); packet.writeInt(row); packet.writeInt(width); packet.writeInt(height); packet.writeMedium(color);
    }

    public static void paletteChange(PacketBuilder packet, int index, int color) throws IOException {
        packet.writePacketType(PacketType.TextBufferMultiPaletteChange);
        packet.writeInt(index); packet.writeInt(color);
    }

    public static void resolutionChange(PacketBuilder packet, int width, int height) throws IOException {
        packet.writePacketType(PacketType.TextBufferMultiResolutionChange);
        packet.writeInt(width); packet.writeInt(height);
    }

    public static void viewportResolutionChange(PacketBuilder packet, int width, int height) throws IOException {
        packet.writePacketType(PacketType.TextBufferMultiViewportResolutionChange);
        packet.writeInt(width); packet.writeInt(height);
    }

    public static void maxResolutionChange(PacketBuilder packet, int width, int height) throws IOException {
        packet.writePacketType(PacketType.TextBufferMultiMaxResolutionChange);
        packet.writeInt(width); packet.writeInt(height);
    }

    public static void set(PacketBuilder packet, int col, int row, String value, boolean vertical) throws IOException {
        packet.writePacketType(PacketType.TextBufferMultiSet);
        packet.writeInt(col); packet.writeInt(row); packet.writeUTF(value); packet.writeBoolean(vertical);
    }

    public static void bitBlt(PacketBuilder packet, int col, int row, int width, int height, String owner, int id, int fromCol, int fromRow) throws IOException {
        packet.writePacketType(PacketType.TextBufferBitBlt);
        packet.writeInt(col); packet.writeInt(row); packet.writeInt(width); packet.writeInt(height);
        packet.writeUTF(owner); packet.writeInt(id); packet.writeInt(fromCol); packet.writeInt(fromRow);
    }

    public static void ramInit(PacketBuilder packet, String address, int id, CompoundTag nbt) throws IOException {
        packet.writePacketType(PacketType.TextBufferRamInit);
        packet.writeUTF(address); packet.writeInt(id); packet.writeNBT(nbt);
    }

    public static void ramDestroy(PacketBuilder packet, String owner, int id) throws IOException {
        packet.writePacketType(PacketType.TextBufferRamDestroy);
        packet.writeUTF(owner); packet.writeInt(id);
    }

    public static void rawSetText(PacketBuilder packet, int col, int row, int[][] text) throws IOException {
        packet.writePacketType(PacketType.TextBufferMultiRawSetText);
        packet.writeInt(col); packet.writeInt(row);
        short height = (short) text.length;
        packet.writeShort(height);
        for (int y = 0; y < height; y++) {
            int[] line = text[y];
            short width = (short) line.length;
            packet.writeShort(width);
            for (int x = 0; x < width; x++) packet.writeMedium(line[x]);
        }
    }

    public static void rawSetBackground(PacketBuilder packet, int col, int row, int[][] colors) throws IOException {
        rawSetColor(packet, PacketType.TextBufferMultiRawSetBackground, col, row, colors);
    }

    public static void rawSetForeground(PacketBuilder packet, int col, int row, int[][] colors) throws IOException {
        rawSetColor(packet, PacketType.TextBufferMultiRawSetForeground, col, row, colors);
    }

    private static void rawSetColor(PacketBuilder packet, PacketType type, int col, int row, int[][] colors) throws IOException {
        packet.writePacketType(type);
        packet.writeInt(col); packet.writeInt(row);
        short height = (short) colors.length;
        packet.writeShort(height);
        for (int y = 0; y < height; y++) {
            int[] line = colors[y];
            short width = (short) line.length;
            packet.writeShort(width);
            for (int x = 0; x < width; x++) packet.writeInt(line[x]);
        }
    }

    public static void sendInit(String address, CompoundTag value, int maxWidth, int maxHeight, int maxDepth,
                                int viewportWidth, int viewportHeight, ServerPlayer player) throws IOException {
        CompressedPacketBuilder packet = new CompressedPacketBuilder(
                PacketType.TextBufferInit, CompressedPacketBuilder.$lessinit$greater$default$2());
        packet.writeUTF(address);
        packet.writeNBT(value);
        packet.writeInt(maxWidth); packet.writeInt(maxHeight); packet.writeInt(maxDepth);
        packet.writeInt(viewportWidth); packet.writeInt(viewportHeight);
        packet.sendToPlayer(player);
    }

    public static void sendPowerChange(String address, boolean hasPower, EnvironmentHost host) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.TextBufferPowerChange);
        packet.writeUTF(address);
        packet.writeBoolean(hasPower);
        packet.sendToPlayersNearHost(host, packet.sendToPlayersNearHost$default$2());
    }
}
