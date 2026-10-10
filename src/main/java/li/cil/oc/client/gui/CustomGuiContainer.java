package li.cil.oc.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class CustomGuiContainer<C extends AbstractContainerMenu> extends AbstractContainerScreen<C> {
    private final C inventoryContainer;

    public CustomGuiContainer(C inventoryContainer, Inventory inventory, Component title) {
        super(inventoryContainer, inventory, title);
        this.inventoryContainer = inventoryContainer;
    }

    public C inventoryContainer() {
        return inventoryContainer;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
