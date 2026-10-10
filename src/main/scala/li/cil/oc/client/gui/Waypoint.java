package li.cil.oc.client.gui;

import li.cil.oc.client.PacketSender;
import li.cil.oc.client.Textures;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class Waypoint extends Screen {
    private final li.cil.oc.common.blockentity.Waypoint waypoint;
    private final int imageWidth = 176;
    private final int imageHeight = 24;
    private int leftPos;
    private int topPos;
    private EditBox textField;

    public Waypoint(li.cil.oc.common.blockentity.Waypoint waypoint) {
        super(Component.empty());
        this.waypoint = waypoint;
    }

    public li.cil.oc.common.blockentity.Waypoint waypoint() {
        return waypoint;
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft.player.distanceToSqr(waypoint.x() + 0.5, waypoint.y() + 0.5, waypoint.z() + 0.5) > 64) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        minecraft.mouseHandler.releaseMouse();
        KeyMapping.releaseAll();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        textField = new EditBox(font, leftPos + 7, topPos + 8, 164 - 12, 12, Component.empty()) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int mods) {
                if (keyCode == GLFW.GLFW_KEY_ENTER) {
                    String label = textField.getValue().substring(0, Math.min(32, textField.getValue().length()));
                    if (!label.equals(waypoint.label())) {
                        waypoint.label_$eq(label);
                        PacketSender.sendWaypointLabel(waypoint);
                        onClose();
                    }
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, mods);
            }
        };
        textField.setMaxLength(32);
        textField.setBordered(false);
        textField.setCanLoseFocus(false);
        textField.setTextColor(0xFFFFFF);
        textField.setValue(waypoint.label());
        addWidget(textField);
        setFocused(textField);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        super.render(graphics, mouseX, mouseY, dt);
        graphics.blit(Textures.GUI$.MODULE$.Waypoint(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        textField.render(graphics, mouseX, mouseY, dt);
    }
}
