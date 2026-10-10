package li.cil.oc.client.gui;

import li.cil.oc.client.Textures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Raid extends DynamicGuiContainer<li.cil.oc.common.menu.Raid> {
    public Raid(li.cil.oc.common.menu.Raid state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }

    @Override
    public void renderBg(GuiGraphics graphics, float dt, int mouseX, int mouseY) {
        graphics.blit(Textures.GUI$.MODULE$.Raid(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
