package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.client.ComponentTracker;
import li.cil.oc.client.MenuClientPacketSender;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.widget.ProgressBar;
import li.cil.oc.client.renderer.TextBufferRenderCache;
import li.cil.oc.client.renderer.gui.BufferRenderer;
import li.cil.oc.common.menu.RobotInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;
import scala.Tuple2;

import java.util.Optional;

/** Robot inventory screen with its text buffer and scrolling component inventory. */
public class Robot extends RobotInputBufferBase {
    private static final int WITH_SCREEN_HEIGHT = 256;
    private static final int NO_SCREEN_HEIGHT = 108;
    private static final int SLOT_SIZE = 18;
    private static final double MAX_BUFFER_WIDTH = 240.0;
    private static final double MAX_BUFFER_HEIGHT = 140.0;
    private static final int INVENTORY_X = 169;
    private static final int SCROLL_WIDTH = 8;
    private static final int SCROLL_HEIGHT = 92;
    private static final int SELECTION_SIZE = 20;

    private final TextBuffer buffer;
    private final boolean hasKeyboard;
    private final int deltaY;
    private final int inventoryY;
    private final int scrollX;
    private final int scrollY;
    private int inventoryOffset;
    private int lastInventorySize;
    private ProgressBar power;
    protected ImageButton powerButton;
    protected ImageButton scrollButton;
    public boolean isScrolling;
    private double scale;

    public Robot(li.cil.oc.common.menu.Robot state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
        this.hasKeyboard = state.info().hasKeyboard();
        this.buffer = findBuffer(state.info());
        this.deltaY = buffer != null ? 0 : WITH_SCREEN_HEIGHT - NO_SCREEN_HEIGHT;
        this.inventoryY = 155 - deltaY;
        this.scrollX = INVENTORY_X + SLOT_SIZE * 4 + 2;
        this.scrollY = inventoryY;
        this.lastInventorySize = inventoryContainer().mainInventorySize();
        imageWidth = 256;
        imageHeight = 256 - deltaY;
    }

    private static TextBuffer findBuffer(RobotInfo info) {
        Optional<String> address = info.screenBufferOptional();
        if (address.isEmpty()) return null;
        Optional<?> tracked = ComponentTracker.INSTANCE.getOptional(Minecraft.getInstance().level, address.get());
        return tracked.isPresent() && tracked.get() instanceof TextBuffer textBuffer ? textBuffer : null;
    }

    @Override public TextBuffer buffer() { return buffer; }
    @Override public boolean hasKeyboard() { return hasKeyboard; }
    @Override public double scale() { return scale; }
    @Override public void scale_$eq(double value) { scale = value; }
    public boolean isScrolling() { return isScrolling; }
    public void isScrolling_$eq(boolean value) { isScrolling = value; }
    @Override public int bufferX() { return (int) (8 + (MAX_BUFFER_WIDTH - bufferRenderWidth()) / 2); }
    @Override public int bufferY() { return (int) (8 + (MAX_BUFFER_HEIGHT - bufferRenderHeight()) / 2); }

    private boolean canScroll() { return inventoryContainer().mainInventorySize() > 16; }
    private int maxOffset() { return Math.max(0, (inventoryContainer().mainInventorySize() + 3) / 4 - 4); }
    private double bufferRenderWidth() {
        Tuple2<Object, Object> resolution = Settings.get().screenResolutionsByTier()[0];
        return Math.min(MAX_BUFFER_WIDTH, TextBufferRenderCache.renderer().charRenderWidth() * ((Number) resolution._1()).doubleValue());
    }
    private double bufferRenderHeight() {
        Tuple2<Object, Object> resolution = Settings.get().screenResolutionsByTier()[0];
        return Math.min(MAX_BUFFER_HEIGHT, TextBufferRenderCache.renderer().charRenderHeight() * ((Number) resolution._2()).doubleValue());
    }

    @Override
    public void init() {
        super.init();
        powerButton = addRenderableWidget(new ImageButton(leftPos + 5, topPos + 153 - deltaY, 18, 18,
            ignored -> MenuClientPacketSender.sendRobotPower(inventoryContainer(), !inventoryContainer().isRunning()),
            Textures.GUISprites.ButtonPower));
        scrollButton = addRenderableWidget(new ImageButton(leftPos + scrollX + 1, topPos + scrollY + 1, 6, 13,
            ignored -> { }, Textures.GUISprites.ButtonScroll));
        power = addRenderableWidget(new ProgressBar(leftPos + 26, topPos + 156 - deltaY));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        powerButton.toggled_$eq(inventoryContainer().isRunning());
        powerButton.setTooltip(Tooltip.create(Component.literal(inventoryContainer().isRunning()
            ? Localization.Computer.TurnOff() : Localization.Computer.TurnOn())));

        int bufferSize = inventoryContainer().globalBufferSize();
        if (bufferSize == 0) {
            power.level_$eq(0);
            power.setTooltip(null);
        } else {
            int currentBuffer = inventoryContainer().globalBuffer();
            power.level_$eq((double) currentBuffer / bufferSize);
            String format = Localization.Computer.Power() + ": %d%% (%d/%d)";
            power.setTooltip(Tooltip.create(Component.literal(String.format(format,
                100 * currentBuffer / bufferSize, currentBuffer, bufferSize))));
        }

        int inventorySize = inventoryContainer().mainInventorySize();
        if (inventorySize != lastInventorySize) {
            if (inventoryOffset > maxOffset()) {
                isScrolling = false;
                scrollTo(0);
            } else scrollTo(inventoryOffset);
            lastInventorySize = inventorySize;
        }
        scrollButton.active = canScroll();
        scrollButton.hoverOverride_$eq(isScrolling);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void drawBuffer(GuiGraphics graphics) {
        if (buffer != null) {
            var pose = graphics.pose();
            pose.translate((float) bufferX(), (float) bufferY(), 0);
            pose.pushPose();
            pose.translate(-3, -3, 0);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            BufferRenderer.drawBackground(pose, (int) bufferRenderWidth(), (int) bufferRenderHeight(), true);
            pose.popPose();
            double scaleX = bufferRenderWidth() / buffer.renderWidth();
            double scaleY = bufferRenderHeight() / buffer.renderHeight();
            float bufferScale = (float) Math.min(scaleX, scaleY);
            if (scaleX > bufferScale) pose.translate(buffer.renderWidth() * (scaleX - bufferScale) / 2, 0, 0);
            else if (scaleY > bufferScale) pose.translate(0, buffer.renderHeight() * (scaleY - bufferScale) / 2, 0);
            pose.scale(bufferScale * (float) scale, bufferScale * (float) scale, bufferScale);
            BufferRenderer.drawText(pose, buffer);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
        for (int slot = 0; slot < menu.slots.size(); slot++) drawSlotHighlight(graphics, menu.getSlot(slot));
    }

    @Override protected void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        drawBufferLayer(graphics);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(buffer != null ? Textures.GUI.Robot : Textures.GUI.RobotNoScreen,
            leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (inventoryContainer().mainInventorySize() > 0) drawSelection(graphics);
        drawInventorySlots(graphics);
    }

    @Override protected void drawSlotBackground(GuiGraphics graphics, int x, int y) { }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (canScroll() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isCoordinateOverScrollBar(mx - leftPos, my - topPos)) {
            isScrolling = true;
            scrollMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (canScroll() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isScrolling) {
            isScrolling = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isScrolling) {
            scrollMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void scrollMouse(double mouseY) {
        scrollTo((int) Math.round((mouseY - topPos - scrollY + 1 - 6.5) * maxOffset() / (SCROLL_HEIGHT - 13.0)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int mx = (int) mouseX - leftPos;
        int my = (int) mouseY - topPos;
        if (isCoordinateOverInventory(mx, my) || isCoordinateOverScrollBar(mx, my)) {
            if (scrollY < 0) scrollDown(); else scrollUp();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isCoordinateOverInventory(int x, int y) {
        return x >= INVENTORY_X && x < INVENTORY_X + SLOT_SIZE * 4 && y >= inventoryY && y < inventoryY + SLOT_SIZE * 4;
    }
    private boolean isCoordinateOverScrollBar(int x, int y) {
        return x > scrollX && x < scrollX + SCROLL_WIDTH && y >= scrollY && y < scrollY + SCROLL_HEIGHT;
    }
    private void scrollUp() { scrollTo(inventoryOffset - 1); }
    private void scrollDown() { scrollTo(inventoryOffset + 1); }
    private void scrollTo(int row) {
        inventoryOffset = Math.max(0, Math.min(maxOffset(), row));
        inventoryContainer().generateSlotsFor(inventoryOffset);
        int yMin = topPos + scrollY + 1;
        scrollButton.y = maxOffset() > 0 ? yMin + (SCROLL_HEIGHT - 13) * inventoryOffset / maxOffset() : yMin;
    }

    @Override
    public double changeSize(double width, double height) {
        double bufferWidth = width * TextBufferRenderCache.renderer().charRenderWidth();
        double bufferHeight = height * TextBufferRenderCache.renderer().charRenderHeight();
        return Math.min(Math.min(bufferRenderWidth() / bufferWidth, 1), Math.min(bufferRenderHeight() / bufferHeight, 1));
    }

    private void drawSelection(GuiGraphics graphics) {
        int slot = inventoryContainer().selectedSlot() - inventoryOffset * 4;
        if (slot >= 0 && slot < 16) {
            int x = leftPos + INVENTORY_X - 1 + (slot % 4) * (SELECTION_SIZE - 2);
            int y = topPos + inventoryY - 1 + (slot / 4) * (SELECTION_SIZE - 2);
            RenderSystem.enableBlend();
            graphics.blitSprite(Textures.GUISprites.RobotSelection, x, y, SELECTION_SIZE, SELECTION_SIZE);
            RenderSystem.disableBlend();
        }
    }
}
