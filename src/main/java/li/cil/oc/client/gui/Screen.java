package li.cil.oc.client.gui;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.client.renderer.TextBufferRenderCache;
import li.cil.oc.client.renderer.gui.BufferRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import org.lwjgl.glfw.GLFW;
import scala.Function0;
import scala.Option;

/** Interactive client screen backed by a computer text buffer. */
public class Screen extends ScreenInputBufferBase implements ContainerEventHandler {
    private static final int BUFFER_MARGIN = BufferRenderer.margin + BufferRenderer.innerMargin;

    private final TextBuffer initialBuffer;
    private final boolean hasMouse;
    private final Function0<?> hasKeyboardCallback;
    private final Function0<?> hasPower;

    private boolean didClick;
    private int x;
    private int y;
    private int innerWidth;
    private int innerHeight;
    private int mx = -1;
    private int my = -1;
    private double scale;

    public Screen(TextBuffer initialBuffer, boolean hasMouse, Function0<?> hasKeyboardCallback,
                  Function0<?> hasPower) {
        this.initialBuffer = initialBuffer;
        this.hasMouse = hasMouse;
        this.hasKeyboardCallback = hasKeyboardCallback;
        this.hasPower = hasPower;
    }

    public boolean hasMouse() { return hasMouse; }
    public Function0<?> hasKeyboardCallback() { return hasKeyboardCallback; }
    public Function0<?> hasPower() { return hasPower; }

    @Override public TextBuffer buffer() { return initialBuffer; }
    @Override public boolean hasKeyboard() { return Boolean.TRUE.equals(hasKeyboardCallback.apply()); }
    @Override public int bufferX() { return 8 + x; }
    @Override public int bufferY() { return 8 + y; }
    @Override public double scale() { return scale; }
    @Override public void scale_$eq(double value) { scale = value; }

    protected int topPadding() { return 0; }

    private Option<BufferCoordinates> toBufferCoordinates(double mouseX, double mouseY) {
        double bx = (mouseX - x - BUFFER_MARGIN) / scale / TextBufferRenderCache.renderer.charRenderWidth();
        double by = (mouseY - y - BUFFER_MARGIN) / scale / TextBufferRenderCache.renderer.charRenderHeight();
        int bw = buffer().getViewportWidth();
        int bh = buffer().getViewportHeight();
        if (bx >= 0 && by >= 0 && bx < bw && by < bh) return Option.apply(new BufferCoordinates(bx, by));
        return Option.empty();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hasMouse) {
            Option<BufferCoordinates> coordinates = toBufferCoordinates(mouseX, mouseY);
            if (coordinates.isDefined()) {
                BufferCoordinates point = coordinates.get();
                buffer().mouseScroll(point.x, point.y, (int) Math.signum((int) scrollY), null);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hasMouse && (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            clickOrDrag(mouseX, mouseY, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (hasMouse && (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            clickOrDrag(mouseX, mouseY, button);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (hasMouse) {
            if (didClick) {
                Option<BufferCoordinates> coordinates = toBufferCoordinates(mouseX, mouseY);
                if (coordinates.isDefined()) {
                    BufferCoordinates point = coordinates.get();
                    buffer().mouseUp(point.x, point.y, button, null);
                } else buffer().mouseUp(-1.0, -1.0, button, null);
            }
            boolean hasClicked = didClick;
            didClick = false;
            mx = -1;
            my = -1;
            if (hasClicked) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void clickOrDrag(double mouseX, double mouseY, int button) {
        Option<BufferCoordinates> coordinates = toBufferCoordinates(mouseX, mouseY);
        if (coordinates.isDefined()) {
            BufferCoordinates point = coordinates.get();
            if ((int) point.x != mx || (int) (point.y * 2) != my) {
                if (mx >= 0 && my >= 0) buffer().mouseDrag(point.x, point.y, button, null);
                else buffer().mouseDown(point.x, point.y, button, null);
                didClick = true;
                mx = (int) point.x;
                my = (int) (point.y * 2);
            }
        }
    }

    @Override
    public void init() {
        super.init();
        minecraft.mouseHandler.releaseMouse();
        KeyMapping.releaseAll();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawBufferLayer(graphics);
    }

    @Override
    public void drawBuffer(GuiGraphics graphics) {
        var pose = graphics.pose();
        pose.translate(x, y, 0);
        BufferRenderer.drawBackground(pose, innerWidth, innerHeight);
        if (Boolean.TRUE.equals(hasPower.apply())) {
            pose.translate(BUFFER_MARGIN, BUFFER_MARGIN, 0);
            pose.scale((float) scale, (float) scale, 1);
            BufferRenderer.drawText(pose, buffer());
        }
    }

    @Override
    public double changeSize(double width, double height) {
        int bufferWidth = buffer().renderWidth();
        int bufferHeight = buffer().renderHeight();
        // DisplayBuffer passes terminal columns/rows here; this screen needs pixel bounds.
        double availableWidth = this.width;
        double scaleX = Math.min((availableWidth - BUFFER_MARGIN * 2.0) / bufferWidth, 1);
        double availableHeight = this.height - topPadding();
        double scaleY = Math.min((availableHeight - BUFFER_MARGIN * 2.0) / bufferHeight, 1);
        double newScale = Math.min(scaleX, scaleY);
        innerWidth = (int) (bufferWidth * newScale);
        innerHeight = (int) (bufferHeight * newScale);
        x = (this.width - (innerWidth + BUFFER_MARGIN * 2)) / 2;
        y = (int) (topPadding() + (availableHeight - (innerHeight + BUFFER_MARGIN * 2)) / 2);
        return newScale;
    }

    private record BufferCoordinates(double x, double y) { }
}
