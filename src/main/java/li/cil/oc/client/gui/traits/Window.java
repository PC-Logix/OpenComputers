package li.cil.oc.client.gui.traits;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class Window extends Screen {
    public int leftPos;
    public int topPos;
    public int imageWidth;
    public int imageHeight;

    protected int windowWidth() { return 176; }
    protected int windowHeight() { return 166; }
    public abstract ResourceLocation backgroundImage();

    protected Window() {
        super(Component.empty());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        imageWidth = windowWidth();
        imageHeight = windowHeight();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(backgroundImage(), leftPos, topPos, 0, 0, imageWidth, imageHeight, windowWidth(), windowHeight());
    }
}
