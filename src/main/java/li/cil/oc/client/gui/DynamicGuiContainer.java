package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.client.Textures;
import li.cil.oc.common.Tier;
import li.cil.oc.common.menu.AbstractMenu;
import li.cil.oc.common.menu.ComponentSlot;
import li.cil.oc.integration.util.ItemSearch;
import li.cil.oc.util.StackOption;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public abstract class DynamicGuiContainer<C extends AbstractContainerMenu> extends CustomGuiContainer<C> {
    protected StackOption hoveredStackNEI = StackOption.empty();

    protected DynamicGuiContainer(C container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = imageHeight - 96 + 2;
    }

    protected void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
        for (int index = 0; index < menu.slots.size(); index++) {
            drawSlotHighlight(graphics, menu.getSlot(index));
        }
    }

    protected void drawSecondaryBackgroundLayer(GuiGraphics graphics) {}

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(Textures.GUI.Background, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        drawSecondaryBackgroundLayer(graphics);
        drawInventorySlots(graphics);
    }

    protected void drawInventorySlots(GuiGraphics graphics) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate((float) leftPos, (float) topPos, 0.0f);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int index = 0; index < menu.slots.size(); index++) {
            drawSlotInventory(graphics, menu.getSlot(index));
        }
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        pose.popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredStackNEI = ItemSearch.hoveredStack(this, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    protected void drawSlotInventory(GuiGraphics graphics, Slot slot) {
        if (slot instanceof ComponentSlot component &&
            (component.slot().equals(li.cil.oc.common.Slot.None) || component.tier() == Tier.None)) {
            if (!slot.hasItem() && slot.x >= 0 && slot.y >= 0 && component.tierIcon() != null) {
                drawDisabledSlot(graphics, component);
            }
            return;
        }

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 1);
        if (!isInPlayerInventory(slot)) drawSlotBackground(graphics, slot.x - 1, slot.y - 1);
        if (slot instanceof ComponentSlot component && !slot.hasItem()) {
            if (component.tierIcon() != null) {
                graphics.blit(component.tierIcon(), slot.x, slot.y, 0, 0, 16, 16, 16, 16);
            }
            if (component.hasBackground()) {
                graphics.blit(component.getBackgroundLocation(), slot.x, slot.y, 0, 0, 16, 16, 16, 16);
            }
        }
        pose.popPose();
    }

    protected void drawSlotHighlight(GuiGraphics graphics, Slot slot) {
        if (!minecraft.player.containerMenu.getCarried().isEmpty()) return;
        if (slot instanceof ComponentSlot component &&
            (component.slot().equals(li.cil.oc.common.Slot.None) || component.tier() == Tier.None)) return;

        boolean currentIsInPlayerInventory = isInPlayerInventory(slot);
        boolean drawHighlight = false;
        if (hoveredSlot != null) {
            boolean hoveredIsInPlayerInventory = isInPlayerInventory(hoveredSlot);
            drawHighlight = currentIsInPlayerInventory != hoveredIsInPlayerInventory &&
                ((currentIsInPlayerInventory && slot.hasItem() && isSelectiveSlot(hoveredSlot) && hoveredSlot.mayPlace(slot.getItem())) ||
                 (hoveredIsInPlayerInventory && hoveredSlot.hasItem() && isSelectiveSlot(slot) && slot.mayPlace(hoveredSlot.getItem())));
        } else if (hoveredStackNEI.isDefined()) {
            drawHighlight = !currentIsInPlayerInventory && isSelectiveSlot(slot) && slot.mayPlace(hoveredStackNEI.get());
        }

        if (drawHighlight) {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(0, 0, 100);
            graphics.fillGradient(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80FFFFFF, 0x80FFFFFF);
            pose.popPose();
        }
    }

    private boolean isSelectiveSlot(Slot slot) {
        if (!(slot instanceof ComponentSlot component)) return false;
        return !component.slot().equals(li.cil.oc.common.Slot.Any) &&
            !component.slot().equals(li.cil.oc.common.Slot.Tool);
    }

    protected void drawDisabledSlot(GuiGraphics graphics, ComponentSlot slot) {
        graphics.blit(slot.tierIcon(), slot.x, slot.y, 0, 0, 16, 16, 16, 16);
    }

    protected void drawSlotBackground(GuiGraphics graphics, int x, int y) {
        graphics.blit(Textures.GUI.Slot, x, y, 0, 0, 18, 18, 18, 18);
    }

    private boolean isInPlayerInventory(Slot slot) {
        return menu instanceof AbstractMenu abstractMenu && slot.container == abstractMenu.playerInventory();
    }
}
