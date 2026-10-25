package li.cil.oc.client.gui;

import li.cil.oc.client.MenuClientPacketSender;
import li.cil.oc.client.Textures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class TapeDrive extends DynamicGuiContainer<li.cil.oc.common.menu.TapeDrive> {
    public TapeDrive(li.cil.oc.common.menu.TapeDrive state, Inventory playerInventory, Component name) {
        super(state, playerInventory, Component.empty());
    }

    @Override public void renderBg(GuiGraphics graphics, float dt, int mouseX, int mouseY) {
        graphics.blit(Textures.GUI.TapePlayer, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        drawInventorySlots(graphics);
        for (int button = 0; button < 4; button++) {
            int state = inventoryContainer().guiState();
            boolean pressed = button == 0 && state == 2 || button == 1 && state == 1 || button == 3 && state == 3;
            int u = pressed || isButtonHovered(button, mouseX, mouseY) ? 20 : 0;
            graphics.blit(Textures.GUI.TapePlayer, leftPos + 48 + button * 20, topPos + 58, u, 170 + button * 15, 20, 15, 256, 256);
        }
    }

    @Override public void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack stack = inventoryContainer().otherInventory().getItem(0);
        String label = "No Tape";
        if (!stack.isEmpty()) {
            var custom = stack.get(DataComponents.CUSTOM_DATA);
            if (custom != null && custom.copyTag().contains("oc:tapeLabel")) label = custom.copyTag().getString("oc:tapeLabel");
            else label = "Unnamed Tape";
        }
        if (label.length() > 24) label = label.substring(0, 22) + "...";
        graphics.drawCenteredString(font, label, 88, 15, stack.isEmpty() ? 0xFF3333 : 0xFFFFFF);
        super.renderLabels(graphics, mouseX, mouseY);
    }

    private boolean isButtonHovered(int button, int mouseX, int mouseY) {
        return mouseX >= leftPos + 48 + button * 20 && mouseX < leftPos + 68 + button * 20 && mouseY >= topPos + 58 && mouseY < topPos + 73;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int control = 0; control < 4; control++) if (isButtonHovered(control, (int) mouseX, (int) mouseY)) {
                MenuClientPacketSender.sendTapeDriveControl(inventoryContainer(), control); return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
