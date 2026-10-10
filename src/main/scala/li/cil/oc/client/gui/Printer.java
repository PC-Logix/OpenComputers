package li.cil.oc.client.gui;

import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.widget.ProgressBar;
import li.cil.oc.common.menu.ComponentSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Printer extends DynamicGuiContainer<li.cil.oc.common.menu.Printer> {
    private ProgressBar materialBar;
    private ProgressBar inkBar;
    private ProgressBar progressBar;

    public Printer(li.cil.oc.common.menu.Printer state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    public void init() {
        super.init();
        materialBar = addRenderableWidget(new ProgressBar(leftPos + 40, topPos + 21, 62, 12, Textures.GUISprites$.MODULE$.PrinterMaterial()));
        inkBar = addRenderableWidget(new ProgressBar(leftPos + 40, topPos + 53, 62, 12, Textures.GUISprites$.MODULE$.PrinterInk()));
        progressBar = addRenderableOnly(new ProgressBar(leftPos + 105, topPos + 20, 46, 46, Textures.GUISprites$.MODULE$.PrinterProgress()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        materialBar.level_$eq(inventoryContainer().amountMaterial() / (double) inventoryContainer().maxAmountMaterial());
        materialBar.setTooltip(Tooltip.create(Component.literal(inventoryContainer().amountMaterial() + "/" + inventoryContainer().maxAmountMaterial())));

        inkBar.level_$eq(inventoryContainer().amountInk() / (double) inventoryContainer().maxAmountInk());
        inkBar.setTooltip(Tooltip.create(Component.literal(inventoryContainer().amountInk() + "/" + inventoryContainer().maxAmountInk())));

        progressBar.level_$eq(inventoryContainer().progress());

        super.render(graphics, mouseX, mouseY, dt);
    }

    @Override
    public void renderBg(GuiGraphics graphics, float dt, int mouseX, int mouseY) {
        graphics.blit(Textures.GUI$.MODULE$.Printer(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        drawInventorySlots(graphics);
    }

    @Override
    public void drawDisabledSlot(GuiGraphics graphics, ComponentSlot slot) {
    }
}
