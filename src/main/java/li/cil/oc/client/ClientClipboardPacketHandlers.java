package li.cil.oc.client;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.Localization;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import net.minecraft.client.Minecraft;

import java.io.IOException;

/** Applies clientbound clipboard updates. */
public final class ClientClipboardPacketHandlers {
    private ClientClipboardPacketHandlers() {}

    public static void analyzerAddress(PacketParser packet) throws IOException {
        String address = packet.readUTF();
        if (KeyBindings.isAnalyzeCopyingAddress()) {
            RenderSystem.recordRenderCall(new RenderCall() {
                @Override
                public void execute() {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.keyboardHandler.setClipboard(address);
                    minecraft.gui.getChat().addMessage(Localization.Analyzer.AddressCopied());
                }
            });
        }
    }

    public static void clipboard(PacketParser packet) throws IOException {
        String contents = packet.readUTF();
        RenderSystem.recordRenderCall(new RenderCall() {
            @Override
            public void execute() {
                Minecraft.getInstance().keyboardHandler.setClipboard(contents);
            }
        });
    }
}
