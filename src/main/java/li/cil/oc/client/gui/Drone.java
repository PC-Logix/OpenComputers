package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.Localization;
import li.cil.oc.client.MenuClientPacketSender;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.traits.DisplayBuffer;
import li.cil.oc.client.gui.widget.ProgressBar;
import li.cil.oc.client.renderer.TextBufferRenderCache;
import li.cil.oc.client.renderer.font.TextBufferRenderData;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.RenderState;
import li.cil.oc.util.TextBuffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import scala.Tuple2;

/** Screen for the drone inventory and its text display. */
@OnlyIn(Dist.CLIENT)
public final class Drone extends DynamicGuiContainer<li.cil.oc.common.menu.Drone> implements DisplayBuffer {
    private double scale;
    private ImageButton powerButton;
    private final TextBuffer buffer = new TextBuffer(20, 2, new PackedColor.SingleBitFormat(0x33FF33));
    private final TextBufferRenderData bufferRenderer = new TextBufferRenderData() {
        private boolean dirty = true;
        @Override public boolean dirty() { return dirty; }
        @Override public void dirty_$eq(boolean value) { dirty = value; }
        @Override public TextBuffer data() { return buffer; }
        @Override public Tuple2<Object, Object> viewport() { return new Tuple2<>(buffer.width(), buffer.height()); }
    };
    private final int inventoryX = 97;
    private final int inventoryY = 7;
    private ProgressBar power;
    private final int selectionSize = 20;

    public Drone(li.cil.oc.common.menu.Drone state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
        imageWidth = 176;
        imageHeight = 148;
    }

    @Override public int bufferX() { return 9; }
    @Override public int bufferY() { return 9; }
    @Override public int bufferColumns() { return 80; }
    @Override public int bufferRows() { return 16; }
    @Override public double scale() { return scale; }
    @Override public void scale_$eq(double value) { scale = value; }

    @Override
    protected void init() {
        super.init();
        powerButton = addRenderableWidget(new ImageButton(leftPos + 7, topPos + 45, 18, 18,
            (Button ignored) -> MenuClientPacketSender.sendDronePower(inventoryContainer(), !inventoryContainer().isRunning()),
            Textures.GUISprites.ButtonPower));
        power = addRenderableWidget(new ProgressBar(leftPos + 28, topPos + 48));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        String format = Localization.Computer.Power() + ": %d%% (%d/%d)";
        power.level_$eq((float) inventoryContainer().globalBuffer() / Math.max((float) inventoryContainer().globalBufferSize(), 1.0f));
        power.setTooltip(Tooltip.create(Component.literal(String.format(format,
            inventoryContainer().globalBuffer() * 100 / Math.max(inventoryContainer().globalBufferSize(), 1),
            inventoryContainer().globalBuffer(), inventoryContainer().globalBufferSize()))));

        powerButton.toggled_$eq(inventoryContainer().isRunning());
        powerButton.setTooltip(Tooltip.create(Component.literal(
            inventoryContainer().isRunning() ? Localization.Computer.TurnOff() : Localization.Computer.TurnOn())));

        var lines = inventoryContainer().statusText().getString().lines().iterator();
        int line = 0;
        boolean dirty = false;
        while (lines.hasNext() && !dirty) dirty = buffer.set(0, line++, lines.next(), false);
        bufferRenderer.dirty_$eq(dirty);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void drawBuffer(GuiGraphics graphics) {
        var pose = graphics.pose();
        pose.translate(bufferX(), bufferY(), 0);
        RenderState.makeItBlend();
        pose.scale((float) scale, (float) scale, 1);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(0.5f, 0.5f, 1f, 1f);
        TextBufferRenderCache.render(pose, bufferRenderer);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    @Override public double changeSize(double width, double height) { return 2.0; }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        drawBufferLayer(graphics);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(Textures.GUI.Drone, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (inventoryContainer().otherInventory().getContainerSize() > 0) drawSelection(graphics);
        drawInventorySlots(graphics);
    }

    @Override protected void drawSlotBackground(GuiGraphics graphics, int x, int y) { }

    private void drawSelection(GuiGraphics graphics) {
        int slot = inventoryContainer().selectedSlot();
        if (slot >= 0 && slot < 16) {
            int x = leftPos + inventoryX - 1 + (slot % 4) * (selectionSize - 2);
            int y = topPos + inventoryY - 1 + (slot / 4) * (selectionSize - 2);
            RenderSystem.enableBlend();
            graphics.blitSprite(Textures.GUISprites.RobotSelection, x, y, selectionSize, selectionSize);
            RenderSystem.disableBlend();
        }
    }
}




