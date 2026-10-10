package li.cil.oc.client.gui;

import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.traits.LockedHotbar;
import li.cil.oc.common.Tier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class Database extends DynamicGuiContainer<li.cil.oc.common.menu.Database>
    implements LockedHotbar<li.cil.oc.common.menu.Database> {

    public Database(li.cil.oc.common.menu.Database state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
        imageHeight = 256;
    }

    @Override
    public ItemStack lockedStack() {
        return inventoryContainer().container();
    }

    @Override
    public void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (slot == null || !ItemStack.isSameItem(slot.getItem(), lockedStack())) {
            super.slotClicked(slot, slotId, mouseButton, clickType);
        }
    }

    @Override
    public void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
    }

    @Override
    public void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void renderBg(GuiGraphics graphics, float dt, int mouseX, int mouseY) {
        graphics.blit(Textures.GUI$.MODULE$.Database(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (inventoryContainer().tier() > Tier.One) {
            graphics.blit(Textures.GUI$.MODULE$.Database1(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }
        if (inventoryContainer().tier() > Tier.Two) {
            graphics.blit(Textures.GUI$.MODULE$.Database2(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }
    }
}
