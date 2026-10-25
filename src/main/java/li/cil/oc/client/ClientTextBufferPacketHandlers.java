package li.cil.oc.client;

import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.component.ClientGpuTextBufferHandler;
import li.cil.oc.common.component.TextBuffer;
import li.cil.oc.common.datacomponents.CompoundStorage;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.NbtOps;

import java.io.EOFException;
import java.io.IOException;
import java.util.Optional;

/** Applies clientbound text-buffer snapshots and incremental updates. */
public final class ClientTextBufferPacketHandlers {
    private ClientTextBufferPacketHandlers() {}

    public static void powerChange(PacketParser packet) throws IOException {
        Optional<ManagedEnvironment> environment = ComponentTracker.INSTANCE.getOptional(packet.player.level(), packet.readUTF());
        if (environment.isPresent() && environment.get() instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.setRenderingEnabled(packet.readBoolean());
        }
    }

    public static void initialize(PacketParser packet) throws IOException {
        Optional<ManagedEnvironment> environment = ComponentTracker.INSTANCE.getOptional(packet.player.level(), packet.readUTF());
        if (environment.isPresent() && environment.get() instanceof TextBuffer buffer) {
            CompoundStorage data = CompoundStorage.CODEC.parse(NbtOps.INSTANCE, packet.readNBT()).getOrThrow();
            buffer.setMaximumResolution(packet.readInt(), packet.readInt());
            var depths = li.cil.oc.api.internal.TextBuffer.ColorDepth.values();
            int depth = Math.max(0, Math.min(packet.readInt(), depths.length - 1));
            buffer.setMaximumColorDepth(depths[depth]);
            buffer.data().loadData(data);
            buffer.setViewport(packet.readInt(), packet.readInt());
            buffer.proxy().setChanged();
            buffer.markInitialized();
        }
    }

    public static void multi(PacketParser packet) throws IOException {
        if (packet.player == null) return;
        Optional<ManagedEnvironment> environment = ComponentTracker.INSTANCE.getOptional(packet.player.level(), packet.readUTF());
        if (environment.isEmpty()) return;
        ManagedEnvironment value = environment.get();
        if (value instanceof TextBuffer textBuffer && !textBuffer.isInitialized()) return;
        if (!(value instanceof li.cil.oc.api.internal.TextBuffer buffer)) return;

        try {
            while (true) {
                switch (packet.readPacketType()) {
                    case TextBufferMultiColorChange -> colorChange(packet, buffer);
                    case TextBufferMultiCopy -> copy(packet, buffer);
                    case TextBufferMultiDepthChange -> depthChange(packet, buffer);
                    case TextBufferMultiFill -> fill(packet, buffer);
                    case TextBufferMultiPaletteChange -> paletteChange(packet, buffer);
                    case TextBufferMultiResolutionChange -> resolutionChange(packet, buffer);
                    case TextBufferMultiViewportResolutionChange -> viewportResolutionChange(packet, buffer);
                    case TextBufferMultiMaxResolutionChange -> maxResolutionChange(packet, buffer);
                    case TextBufferMultiSet -> set(packet, buffer);
                    case TextBufferRamInit -> ramInit(packet, buffer);
                    case TextBufferBitBlt -> bitBlt(packet, buffer);
                    case TextBufferRamDestroy -> ramDestroy(packet, buffer);
                    case TextBufferMultiRawSetText -> rawSetText(packet, buffer);
                    case TextBufferMultiRawSetBackground -> rawSetBackground(packet, buffer);
                    case TextBufferMultiRawSetForeground -> rawSetForeground(packet, buffer);
                    default -> { }
                }
            }
        } catch (EOFException ignored) {
            // End of the command stream.
        }
    }

    private static void colorChange(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        int foreground = packet.readInt();
        boolean foregroundIsPalette = packet.readBoolean();
        buffer.setForegroundColor(foreground, foregroundIsPalette);
        int background = packet.readInt();
        boolean backgroundIsPalette = packet.readBoolean();
        buffer.setBackgroundColor(background, backgroundIsPalette);
    }

    private static void copy(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        int column = packet.readInt();
        int row = packet.readInt();
        int width = packet.readInt();
        int height = packet.readInt();
        int tx = packet.readInt();
        int ty = packet.readInt();
        buffer.copy(column, row, width, height, tx, ty);
    }

    private static void depthChange(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        buffer.setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth.values()[packet.readInt()]);
    }

    private static void fill(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        int column = packet.readInt();
        int row = packet.readInt();
        int width = packet.readInt();
        int height = packet.readInt();
        int color = packet.readMedium();
        buffer.fill(column, row, width, height, color);
    }

    private static void paletteChange(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        buffer.setPaletteColor(packet.readInt(), packet.readInt());
    }

    private static void resolutionChange(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        buffer.setResolution(packet.readInt(), packet.readInt());
    }

    private static void viewportResolutionChange(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        buffer.setViewport(packet.readInt(), packet.readInt());
    }

    private static void maxResolutionChange(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        buffer.setMaximumResolution(packet.readInt(), packet.readInt());
    }

    private static void set(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        buffer.set(packet.readInt(), packet.readInt(), packet.readUTF(), packet.readBoolean());
    }

    private static void ramInit(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        String owner = packet.readUTF();
        int id = packet.readInt();
        CompoundStorage data = new CompoundStorage(
            DataComponentMap.CODEC.parse(NbtOps.INSTANCE, packet.readNBT()).getOrThrow());
        ClientGpuTextBufferHandler.loadBuffer(buffer, owner, id, data);
    }

    private static void bitBlt(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        int column = packet.readInt();
        int row = packet.readInt();
        int width = packet.readInt();
        int height = packet.readInt();
        String owner = packet.readUTF();
        int id = packet.readInt();
        int fromColumn = packet.readInt();
        int fromRow = packet.readInt();
        ClientGpuTextBufferHandler.bitblt(buffer, column, row, width, height, owner, id, fromColumn, fromRow);
    }

    private static void ramDestroy(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        ClientGpuTextBufferHandler.removeBuffer(buffer, packet.readUTF(), packet.readInt());
    }

    private static void rawSetText(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        int column = packet.readInt();
        int row = packet.readInt();
        short rows = packet.readShort();
        int[][] text = new int[rows][];
        for (int y = 0; y < rows; y++) {
            short columns = packet.readShort();
            int[] line = new int[columns];
            for (int x = 0; x < columns; x++) line[x] = packet.readMedium();
            text[y] = line;
        }
        buffer.rawSetText(column, row, text);
    }

    private static void rawSetBackground(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        int column = packet.readInt();
        int row = packet.readInt();
        short rows = packet.readShort();
        int[][] colors = new int[rows][];
        for (int y = 0; y < rows; y++) {
            short columns = packet.readShort();
            int[] line = new int[columns];
            for (int x = 0; x < columns; x++) line[x] = packet.readInt();
            colors[y] = line;
        }
        buffer.rawSetBackground(column, row, colors);
    }

    private static void rawSetForeground(PacketParser packet, li.cil.oc.api.internal.TextBuffer buffer) throws IOException {
        int column = packet.readInt();
        int row = packet.readInt();
        short rows = packet.readShort();
        int[][] colors = new int[rows][];
        for (int y = 0; y < rows; y++) {
            short columns = packet.readShort();
            int[] line = new int[columns];
            for (int x = 0; x < columns; x++) line[x] = packet.readInt();
            colors[y] = line;
        }
        buffer.rawSetForeground(column, row, colors);
    }
}
