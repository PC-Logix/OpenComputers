package li.cil.oc.client.gui;

import li.cil.oc.Localization;
import li.cil.oc.client.Textures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.text.DecimalFormat;

public class Relay extends DynamicGuiContainer<li.cil.oc.common.menu.Relay> {
    private final DecimalFormat format = new DecimalFormat("#.##hz");
    public final Rect2i tabPosition;

    public Relay(li.cil.oc.common.menu.Relay state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
        tabPosition = new Rect2i(imageWidth, 10, 23, 26);
    }

    @Override
    public void drawSecondaryBackgroundLayer(GuiGraphics graphics) {
        super.drawSecondaryBackgroundLayer(graphics);
        graphics.blitSprite(Textures.GUISprites$.MODULE$.UpgradeTab(),
            leftPos + tabPosition.getX(), topPos + tabPosition.getY(), tabPosition.getWidth(), tabPosition.getHeight());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int originalWidth = imageWidth;
        try {
            imageWidth += tabPosition.getWidth();
            return super.mouseClicked(mouseX, mouseY, button);
        } finally {
            imageWidth = originalWidth;
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int originalWidth = imageWidth;
        try {
            imageWidth += tabPosition.getWidth();
            return super.mouseReleased(mouseX, mouseY, button);
        } finally {
            imageWidth = originalWidth;
        }
    }

    @Override
    public void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
        graphics.drawString(font, Localization.Switch$.MODULE$.TransferRate(), 14, 20, 0x404040, false);
        graphics.drawString(font, Localization.Switch$.MODULE$.PacketsPerCycle(), 14, 39, 0x404040, false);
        graphics.drawString(font, Localization.Switch$.MODULE$.QueueSize(), 14, 58, 0x404040, false);

        graphics.drawString(font, format.format(20f / inventoryContainer().relayDelay()),
            108, 20, 0x404040, false);
        graphics.drawString(font, inventoryContainer().packetsPerCycleAvg() + " / " + inventoryContainer().relayAmount(),
            108, 39, thresholdBasedColor(inventoryContainer().packetsPerCycleAvg(),
                (int) Math.ceil(inventoryContainer().relayAmount() / 2f), inventoryContainer().relayAmount()), false);
        graphics.drawString(font, inventoryContainer().queueSize() + " / " + inventoryContainer().maxQueueSize(),
            108, 58, thresholdBasedColor(inventoryContainer().queueSize(),
                inventoryContainer().maxQueueSize() / 2, inventoryContainer().maxQueueSize()), false);
    }

    private int thresholdBasedColor(int value, int yellow, int red) {
        if (value < yellow) return 0x009900;
        if (value < red) return 0x999900;
        return 0x990000;
    }
}
