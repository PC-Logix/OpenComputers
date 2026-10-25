package li.cil.oc.common.component;

import li.cil.oc.Settings;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.client.TextBufferClientPacketSender;
import li.cil.oc.common.PacketBuilder;
import li.cil.oc.common.blockentity.Screen;
import li.cil.oc.common.blockentity.traits.Computer;
import li.cil.oc.common.datacomponents.CompoundStorage;
import li.cil.oc.server.TextBufferPacketEncoder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Server-side packet encoding and keyboard forwarding for a text buffer. */
public final class TextBufferServerProxy extends TextBufferProxyBase {
    @FunctionalInterface
    private interface PacketAction {
        void encode(PacketBuilder packet) throws IOException;
    }

    public TextBufferServerProxy(li.cil.oc.common.component.TextBuffer owner) {
        super(owner);
    }

    private void encode(PacketAction action) {
        synchronized (owner()) {
            try {
                action.encode(owner().pendingCommandsForProxy());
            } catch (IOException error) {
                throw sneaky(error);
            }
        }
    }

    private void changed() {
        owner().host().markChanged();
    }

    @Override
    public void onBufferColorChange() {
        changed();
        encode(packet -> TextBufferPacketEncoder.colorChange(packet, owner().data().foreground(), owner().data().background()));
    }

    @Override
    public void onBufferCopy(int col, int row, int width, int height, int tx, int ty) {
        super.onBufferCopy(col, row, width, height, tx, ty);
        changed();
        encode(packet -> TextBufferPacketEncoder.copy(packet, col, row, width, height, tx, ty));
    }

    @Override
    public void onBufferDepthChange(TextBuffer.ColorDepth depth) {
        changed();
        encode(packet -> TextBufferPacketEncoder.depthChange(packet, depth));
    }

    @Override
    public void onBufferFill(int col, int row, int width, int height, int color) {
        super.onBufferFill(col, row, width, height, color);
        changed();
        encode(packet -> TextBufferPacketEncoder.fill(packet, col, row, width, height, color));
    }

    @Override
    public void onBufferPaletteChange(int index) {
        changed();
        encode(packet -> TextBufferPacketEncoder.paletteChange(packet, index, owner().getPaletteColor(index)));
    }

    @Override
    public void onBufferResolutionChange(int width, int height) {
        super.onBufferResolutionChange(width, height);
        changed();
        encode(packet -> TextBufferPacketEncoder.resolutionChange(packet, width, height));
    }

    @Override
    public void onBufferViewportResolutionChange(int width, int height) {
        super.onBufferViewportResolutionChange(width, height);
        changed();
        encode(packet -> TextBufferPacketEncoder.viewportResolutionChange(packet, width, height));
    }

    @Override
    public void onBufferMaxResolutionChange(int width, int height) {
        if (owner().node().network() != null) {
            super.onBufferMaxResolutionChange(width, height);
            changed();
            encode(packet -> TextBufferPacketEncoder.maxResolutionChange(packet, width, height));
        }
    }

    @Override
    public void onBufferSet(int col, int row, String value, boolean vertical) {
        super.onBufferSet(col, row, value, vertical);
        changed();
        encode(packet -> TextBufferPacketEncoder.set(packet, col, row, value, vertical));
    }

    @Override
    public void onBufferBitBlt(int col, int row, int width, int height, GpuTextBuffer ram, int fromCol, int fromRow) {
        super.onBufferBitBlt(col, row, width, height, ram, fromCol, fromRow);
        changed();
        encode(packet -> TextBufferPacketEncoder.bitBlt(packet, col, row, width, height, ram.owner(), ram.id(), fromCol, fromRow));
    }

    @Override
    public void onBufferRamInit(GpuTextBuffer ram) {
        super.onBufferRamInit(ram);
        changed();
        CompoundStorage storage = new CompoundStorage();
        ram.saveData(storage);
        CompoundTag tag = (CompoundTag) CompoundStorage.CODEC.encode(storage, NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
        encode(packet -> TextBufferPacketEncoder.ramInit(packet, ram.owner(), ram.id(), tag));
    }

    @Override
    public void onBufferRamDestroy(GpuTextBuffer ram) {
        super.onBufferRamDestroy(ram);
        changed();
        encode(packet -> TextBufferPacketEncoder.ramDestroy(packet, ram.owner(), ram.id()));
    }

    @Override
    public void onBufferRawSetText(int col, int row, int[][] text) {
        super.onBufferRawSetText(col, row, text);
        changed();
        encode(packet -> TextBufferPacketEncoder.rawSetText(packet, col, row, text));
    }

    @Override
    public void onBufferRawSetBackground(int col, int row, int[][] color) {
        super.onBufferRawSetBackground(col, row, color);
        changed();
        encode(packet -> TextBufferPacketEncoder.rawSetBackground(packet, col, row, color));
    }

    @Override
    public void onBufferRawSetForeground(int col, int row, int[][] color) {
        super.onBufferRawSetForeground(col, row, color);
        changed();
        encode(packet -> TextBufferPacketEncoder.rawSetForeground(packet, col, row, color));
    }

    @Override
    public void keyDown(char character, int code, Player player) {
        sendToKeyboards("keyboard.keyDown", player, Character.valueOf(character), Integer.valueOf(code));
    }

    @Override
    public void keyUp(char character, int code, Player player) {
        sendToKeyboards("keyboard.keyUp", player, Character.valueOf(character), Integer.valueOf(code));
    }

    @Override
    public void textInput(int codePoint, Player player) {
        sendToKeyboards("keyboard.textInput", player, Integer.valueOf(codePoint));
    }

    @Override
    public void clipboard(String value, Player player) {
        sendToKeyboards("keyboard.clipboard", player, value);
    }

    @Override public void mouseDown(double x, double y, int button, Player player) { sendMouseEvent(player, "touch", x, y, button); }
    @Override public void mouseDrag(double x, double y, int button, Player player) { sendMouseEvent(player, "drag", x, y, button); }
    @Override public void mouseUp(double x, double y, int button, Player player) { sendMouseEvent(player, "drop", x, y, button); }
    @Override public void mouseScroll(double x, double y, int delta, Player player) { sendMouseEvent(player, "scroll", x, y, delta); }

    @Override
    public void copyToAnalyzer(int line, Player player) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!stack.isEmpty()) {
            stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, customData -> {
                CompoundTag tag = customData.copyTag();
                tag.remove(Settings.namespace() + "clipboard");
                return CustomData.of(tag);
            });

            if (line >= 0 && line < owner().getViewportHeight()) {
                String text = owner().data().lineToString(line);
                if (text != null && !text.isEmpty()) {
                    stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .getUnsafe().putString(Settings.namespace() + "clipboard", text);
                }
            }
        }
    }

    private void sendMouseEvent(Player player, String name, double x, double y, int data) {
        List<Object> args = new ArrayList<>();
        args.add(player);
        args.add(name);
        if (owner().precisionModeForProxy()) {
            args.add(Double.valueOf(x));
            args.add(Double.valueOf(y));
        } else {
            args.add(Integer.valueOf((int) x + 1));
            args.add(Integer.valueOf((int) y + 1));
        }
        args.add(Integer.valueOf(data));
        if (Settings.get().inputUsername()) args.add(player.getName().getString());
        owner().node().sendToReachable("computer.checked_signal", args.toArray());
    }

    private void sendToKeyboards(String name, Object... values) {
        if (owner().host() instanceof Screen screen) {
            for (Screen screenPart : screen.screensJava()) screenPart.node().sendToNeighbors(name, values);
        } else {
            owner().node().sendToNeighbors(name, values);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T {
        throw (T) error;
    }
}
