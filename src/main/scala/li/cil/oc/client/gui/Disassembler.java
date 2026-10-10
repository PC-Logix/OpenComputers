package li.cil.oc.client.gui;

import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.widget.ProgressBar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Disassembler extends DynamicGuiContainer<li.cil.oc.common.menu.Disassembler> {
    private ProgressBar progress;

    public Disassembler(li.cil.oc.common.menu.Disassembler state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }

    @Override
    public void init() {
        super.init();
        progress = addRenderableWidget(new ProgressBar(leftPos + 18, topPos + 65, 140, 12, Textures.GUISprites$.MODULE$.Bar()));
    }

    @Override
    public void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040);
        drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
        for (int slot = 0; slot < menu.slots.size(); slot++) {
            drawSlotHighlight(graphics, menu.getSlot(slot));
        }
    }

    @Override
    public void renderBg(GuiGraphics graphics, float dt, int mouseX, int mouseY) {
        graphics.blit(Textures.GUI$.MODULE$.Disassembler(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        progress.level_$eq(inventoryContainer().disassemblyProgress() / 100.0);
    }
}
