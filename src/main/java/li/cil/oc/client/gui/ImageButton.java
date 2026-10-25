package li.cil.oc.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ImageButton extends Button {
    private final WidgetSprites image;
    private final boolean canToggle;
    private final int textColor;
    private final int textDisabledColor;
    private final int textHoverColor;
    private final int textIndent;
    private final int textureWidth;
    private final int textureHeight;
    private boolean toggled = true;
    private boolean hoverOverride;

    public ImageButton(int x, int y, int width, int height, OnPress handler) {
        this(x, y, width, height, handler, null, Component.empty(), false, 0xE0E0E0, 0xA0A0A0, 0xFFFFA0, -1, -1, -1);
    }

    public ImageButton(int x, int y, int width, int height, OnPress handler, WidgetSprites image) {
        this(x, y, width, height, handler, image, Component.empty(), false, 0xE0E0E0, 0xA0A0A0, 0xFFFFA0, -1, -1, -1);
    }

    public ImageButton(int x, int y, int width, int height, OnPress handler, WidgetSprites image, Component text) {
        this(x, y, width, height, handler, image, text, false, 0xE0E0E0, 0xA0A0A0, 0xFFFFA0, -1, -1, -1);
    }

    public ImageButton(int x, int y, int width, int height, OnPress handler, WidgetSprites image, Component text,
                       boolean canToggle, int textColor, int textDisabledColor, int textHoverColor,
                       int textIndent, int textureWidth, int textureHeight) {
        super(x, y, width, height, text, handler, supplier -> Component.empty());
        this.image = image;
        this.canToggle = canToggle;
        this.textColor = textColor;
        this.textDisabledColor = textDisabledColor;
        this.textHoverColor = textHoverColor;
        this.textIndent = textIndent;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public boolean toggled() { return toggled; }
    public void toggled_$eq(boolean value) { toggled = value; }
    public boolean hoverOverride() { return hoverOverride; }
    public void hoverOverride_$eq(boolean value) { hoverOverride = value; }
    public boolean canToggle() { return canToggle; }
    public int textColor() { return textColor; }
    public int textDisabledColor() { return textDisabledColor; }
    public int textHoverColor() { return textHoverColor; }
    public int textIndent() { return textIndent; }
    public int textureWidth() { return textureWidth; }
    public int textureHeight() { return textureHeight; }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        boolean hovered = hoverOverride || (isHovered() && active);
        if (image != null) {
            graphics.blitSprite(image.get(active && toggled, hovered), getX(), getY(), width, height);
        } else if (hovered) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height,
                    FastColor.ARGB32.colorFromFloat(0.4f, 1f, 1f, 1f));
        }

        if (!getMessage().getString().isEmpty()) {
            int color = !active ? textDisabledColor : hovered ? textHoverColor : textColor;
            var font = Minecraft.getInstance().font;
            int textX = textIndent >= 0 ? textIndent + getX() : getX() + width / 2 - font.width(getMessage()) / 2;
            graphics.drawString(font, getMessage(), textX, getY() + (height - 8) / 2, color, false);
        }
    }
}
