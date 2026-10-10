package li.cil.oc.client.renderer.markdown.segment;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.InteractiveImageRenderer;
import li.cil.oc.client.renderer.markdown.Document;
import li.cil.oc.client.renderer.markdown.MarkupFormat$;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import scala.Enumeration;
import scala.Option;

public class RenderSegment implements InteractiveSegment {
    private final Segment parent;
    private final String title;
    private final ImageRenderer imageRenderer;
    private Segment next;
    private int lastX;
    private int lastY;

    public RenderSegment(Segment parent, String title, ImageRenderer imageRenderer) {
        this.parent = parent;
        this.title = title;
        this.imageRenderer = imageRenderer;
    }

    @Override
    public Segment parent() {
        return parent;
    }

    public String title() {
        return title;
    }

    public ImageRenderer imageRenderer() {
        return imageRenderer;
    }

    @Override
    public Segment next() {
        return next;
    }

    @Override
    public void next_$eq(Segment next) {
        this.next = next;
    }

    public int lastX() {
        return lastX;
    }

    public void lastX_$eq(int value) {
        lastX = value;
    }

    public int lastY() {
        return lastY;
    }

    public void lastY_$eq(int value) {
        lastY = value;
    }

    @Override
    public Option<String> tooltip() {
        return Option.apply(imageRenderer instanceof InteractiveImageRenderer interactive
            ? interactive.getTooltip(title) : title);
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY) {
        return imageRenderer instanceof InteractiveImageRenderer interactive &&
            interactive.onMouseClick(mouseX - lastX, mouseY - lastY);
    }

    private float scale(int maxWidth) {
        return Math.min(1f, maxWidth / (float) imageRenderer.getWidth());
    }

    public int imageWidth(int maxWidth) {
        return Math.min(maxWidth, imageRenderer.getWidth());
    }

    public int imageHeight(int maxWidth) {
        return (int) Math.ceil(imageRenderer.getHeight() * scale(maxWidth)) + 4;
    }

    @Override
    public int nextY(int indent, int maxWidth, Font renderer) {
        return imageHeight(maxWidth) + (indent > 0 ? Document.lineHeight(renderer) : 0);
    }

    @Override
    public int nextX(int indent, int maxWidth, Font renderer) {
        return 0;
    }

    @Override
    public Option<InteractiveSegment> render(GuiGraphics graphics, int x, int y, int indent, int maxWidth,
                                             Font renderer, int mouseX, int mouseY) {
        int width = imageWidth(maxWidth);
        int height = imageHeight(maxWidth);
        int xOffset = (maxWidth - width) / 2;
        int yOffset = 2 + (indent > 0 ? Document.lineHeight(renderer) : 0);
        float factor = scale(maxWidth);

        lastX = x + xOffset;
        lastY = y + yOffset;

        Option<InteractiveSegment> hovered = checkHovered(mouseX, mouseY, x + xOffset, y + yOffset, width, height);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + xOffset, y + yOffset, 0f);
        pose.scale(factor, factor, factor);

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        if (hovered.isDefined()) {
            pose.pushPose();
            graphics.fill(0, 0, imageRenderer.getWidth(), imageRenderer.getHeight(), 0x26FFFFFF);
            pose.popPose();
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
        imageRenderer.render(graphics, mouseX - x, mouseY - y);
        RenderSystem.disableBlend();
        pose.popPose();
        return hovered;
    }

    @Override
    public String toString(Enumeration.Value format) {
        if (format == MarkupFormat$.MODULE$.Markdown()) return "![" + title + "](" + imageRenderer + ")";
        return "(Sorry, images only work in the OpenComputers manual for now.)";
    }

    @Override
    public String toString() {
        return toString(MarkupFormat$.MODULE$.Markdown());
    }
}
