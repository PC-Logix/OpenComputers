package li.cil.oc.integration.jei;

import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Used to simulate an animated texture. */
public final class DrawableAnimatedIcon implements IDrawableAnimated {
    private final ResourceLocation resourceLocation;
    private final int u, v, width, height, textureWidth, textureHeight;
    private final ITickTimer tickTimer;
    private final int uOffset, vOffset, paddingTop, paddingBottom, paddingLeft, paddingRight;

    public DrawableAnimatedIcon(ResourceLocation resourceLocation, int u, int v, int width, int height,
                                int textureWidth, int textureHeight, ITickTimer tickTimer, int uOffset, int vOffset,
                                int paddingTop, int paddingBottom, int paddingLeft, int paddingRight) {
        this.resourceLocation = resourceLocation;
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.tickTimer = tickTimer;
        this.uOffset = uOffset;
        this.vOffset = vOffset;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
    }

    @Override
    public int getWidth() { return width + paddingLeft + paddingRight; }

    @Override
    public int getHeight() { return height + paddingTop + paddingBottom; }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        int frame = tickTimer.getValue();
        graphics.blit(resourceLocation, xOffset + paddingLeft, yOffset + paddingTop,
                u + uOffset * frame, v + vOffset * frame, width, height, textureWidth, textureHeight);
    }
}
