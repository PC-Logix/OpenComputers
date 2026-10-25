package li.cil.oc.client.gui.widget;

import li.cil.oc.client.Textures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

public final class ProgressBar extends AbstractWidget {
    private final ResourceLocation sprite;
    private double level;

    public ProgressBar(int x, int y) {
        this(x, y, 140, 12, Textures.GUISprites.Bar);
    }

    public ProgressBar(int x, int y, int width) {
        this(x, y, width, 12, Textures.GUISprites.Bar);
    }

    public ProgressBar(int x, int y, int width, int height) {
        this(x, y, width, height, Textures.GUISprites.Bar);
    }

    public ProgressBar(int x, int y, int width, int height, ResourceLocation sprite) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.sprite = sprite;
    }

    public ResourceLocation sprite() {
        return sprite;
    }

    public double level() {
        return level;
    }

    public void level_$eq(double value) {
        level = value;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (level > 0) {
            int renderedWidth = (int) (width * level);
            graphics.blitSprite(sprite, width, height, 0, 0, getX(), getY(), renderedWidth, height);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
