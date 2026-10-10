package li.cil.oc.client.gui;

import li.cil.oc.Localization;
import li.cil.oc.client.PacketSender$;
import li.cil.oc.client.Textures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Case extends DynamicGuiContainer<li.cil.oc.common.menu.Case> {
    protected ImageButton powerButton;

    public Case(li.cil.oc.common.menu.Case state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        powerButton.toggled_$eq(inventoryContainer().isRunning());
        powerButton.setTooltip(Tooltip.create(Component.literal(inventoryContainer().isRunning()
            ? Localization.Computer$.MODULE$.TurnOff() : Localization.Computer$.MODULE$.TurnOn())));
        super.render(graphics, mouseX, mouseY, dt);
    }

    @Override
    public void init() {
        super.init();
        powerButton = addRenderableWidget(new ImageButton(leftPos + 70, topPos + 33, 18, 18,
            button -> PacketSender$.MODULE$.sendComputerPower(inventoryContainer(), !inventoryContainer().isRunning()),
            Textures.GUISprites$.MODULE$.ButtonPower(), Component.empty(), false,
            0xE0E0E0, 0xA0A0A0, 0xFFFFA0, -1, -1, -1));
    }

    @Override
    public void drawSecondaryBackgroundLayer(GuiGraphics graphics) {
        graphics.blit(Textures.GUI$.MODULE$.Computer(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
