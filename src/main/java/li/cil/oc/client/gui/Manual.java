package li.cil.oc.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import li.cil.oc.Localization;
import li.cil.oc.client.ManualTab;
import li.cil.oc.client.renderer.markdown.Document;
import li.cil.oc.client.renderer.markdown.segment.InteractiveSegment;
import li.cil.oc.client.renderer.markdown.segment.Segment;
import li.cil.oc.client.gui.traits.Window;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** In-game documentation browser with tab navigation and a scrollable markdown page. */
public final class Manual extends Window {
    private static final int DOCUMENT_MAX_WIDTH = 230;
    private static final int DOCUMENT_MAX_HEIGHT = 176;
    private static final int SCROLL_POS_X = 244;
    private static final int SCROLL_POS_Y = 6;
    private static final int SCROLL_WIDTH = 6;
    private static final int SCROLL_HEIGHT = 180;
    private static final int TAB_POS_X = -23;
    private static final int TAB_POS_Y = 7;
    private static final int TAB_WIDTH = 23;
    private static final int TAB_HEIGHT = 26;
    private static final int MAX_TABS_PER_SIDE = 7;

    public boolean isScrolling;
    public Segment document;
    public int documentHeight;
    private Optional<InteractiveSegment> currentSegment = Optional.empty();
    private ImageButton scrollButton;
    private final List<ImageButton> tabButtons = new ArrayList<>();

    @Override protected int windowWidth() { return 256; }
    @Override protected int windowHeight() { return 192; }
    @Override public ResourceLocation backgroundImage() { return li.cil.oc.client.Textures.GUI.Manual; }
    @Override protected void renderBlurredBackground(float partialTick) { }

    private boolean canScroll() { return maxOffset() > 0; }
    private int offset() { return li.cil.oc.client.Manual.history.top().offset(); }
    private int maxOffset() { return documentHeight - DOCUMENT_MAX_HEIGHT; }

    public String resolveLink(String path, String current) {
        if (path.startsWith("/")) return path;
        int splitAt = current.lastIndexOf('/');
        return splitAt >= 0 ? current.substring(0, splitAt) + "/" + path : path;
    }

    public void refreshPage() {
        String path = li.cil.oc.client.Manual.history.top().path();
        Iterable<String> content = li.cil.oc.client.Manual.contentFor(path);
        if (content == null) content = List.of("Document not found: " + path);
        document = Document.parse(content);
        documentHeight = Document.height(document, DOCUMENT_MAX_WIDTH, font);
        scrollTo(offset());
    }

    public void pushPage(String path) {
        if (!path.equals(li.cil.oc.client.Manual.history.top().path())) {
            li.cil.oc.client.Manual.history.push(new li.cil.oc.client.ManualHistory(path));
            refreshPage();
        }
    }

    public void popPage() {
        if (li.cil.oc.client.Manual.history.size() > 1) {
            li.cil.oc.client.Manual.history.pop();
            refreshPage();
        } else onClose();
    }

    @Override protected void init() {
        super.init();
        tabButtons.clear();
        minecraft.mouseHandler.releaseMouse();
        KeyMapping.releaseAll();

        java.util.Iterator<ManualTab> tabs = li.cil.oc.client.Manual.tabs.iterator();
        int index = 0;
        while (tabs.hasNext() && index < MAX_TABS_PER_SIDE) {
            ManualTab tab = tabs.next();
            int x = leftPos + TAB_POS_X;
            int y = topPos + TAB_POS_Y + index * (TAB_HEIGHT - 1);
            ImageButton button = new ImageButton(x, y, TAB_WIDTH, TAB_HEIGHT,
                    ignored -> li.cil.oc.api.Manual.navigate(tab.path()), li.cil.oc.client.Textures.GUISprites.ManualTab);
            tabButtons.add(addRenderableWidget(button));
            index++;
        }

        scrollButton = addRenderableWidget(new ImageButton(leftPos + SCROLL_POS_X, topPos + SCROLL_POS_Y,
                SCROLL_WIDTH, 13, ignored -> { }, li.cil.oc.client.Textures.GUISprites.ButtonScroll));
        refreshPage();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        scrollButton.active = canScroll();
        scrollButton.hoverOverride_$eq(isScrolling);

        var pose = graphics.pose();
        for (int index = 0; index < tabButtons.size(); index++) {
            ManualTab tab = li.cil.oc.client.Manual.tabs.get(index);
            ImageButton button = tabButtons.get(index);
            pose.pushPose();
            pose.translate(button.getX() + 5.0f, button.getY() + 5.0f, 0.0f);
            tab.renderer().render(graphics);
            pose.popPose();
        }

        currentSegment = Document.render(graphics, document, leftPos + 8, topPos + 8,
                DOCUMENT_MAX_WIDTH, DOCUMENT_MAX_HEIGHT, offset(), font, mouseX, mouseY);

        if (!isScrolling && currentSegment.isPresent()) {
            Optional<String> tooltip = currentSegment.get().tooltip();
            if (tooltip.isPresent() && !tooltip.get().isEmpty())
                graphics.renderComponentTooltip(font, localizedLines(tooltip.get()), mouseX, mouseY);
        }

        if (!isScrolling) {
            for (int index = 0; index < tabButtons.size(); index++) {
                ImageButton button = tabButtons.get(index);
                if (mouseX > button.getX() && mouseX < button.getX() + TAB_WIDTH
                        && mouseY > button.getY() && mouseY < button.getY() + TAB_HEIGHT) {
                    Optional<String> tooltip = li.cil.oc.client.Manual.tabs.get(index).tooltip();
                    if (tooltip.isPresent())
                        graphics.renderComponentTooltip(font, localizedLines(tooltip.get()), mouseX, mouseY);
                }
            }
        }

        if (canScroll() && (isCoordinateOverScrollBar(mouseX - leftPos, mouseY - topPos) || isScrolling)) {
            graphics.renderComponentTooltip(font, List.of(Component.literal((100 * offset() / maxOffset()) + "%")),
                    leftPos + SCROLL_POS_X + SCROLL_WIDTH, scrollButton.getY() + scrollButton.getHeight() + 1);
        }
    }

    private List<Component> localizedLines(String text) {
        return Localization.localizeImmediately(text).lines().map(line -> (Component) Component.literal(line)).toList();
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int mods) {
        InputConstants.Key input = InputConstants.getKey(keyCode, scanCode);
        if (minecraft.options.keyJump.isActiveAndMatches(input)) { popPage(); return true; }
        if (minecraft.options.keyInventory.isActiveAndMatches(input)) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, mods);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0) { scrollDown(); return true; }
        if (scrollY > 0) { scrollUp(); return true; }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (canScroll() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isCoordinateOverScrollBar(localX, localY)) {
            isScrolling = true;
            scrollMouse(mouseY);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isCoordinateOverContent(localX, localY)
                && currentSegment.isPresent()
                && currentSegment.get().onMouseClick((int) mouseX, (int) mouseY)) return true;
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { popPage(); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public void mouseMoved(double mouseX, double mouseY) {
        if (isScrolling) scrollMouse(mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isScrolling) { scrollMouse(mouseY); return true; }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isScrolling) { isScrolling = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void scrollMouse(double mouseY) {
        scrollTo((int) Math.round((mouseY - topPos - SCROLL_POS_Y - 6.5) * maxOffset() / (SCROLL_HEIGHT - 13.0)));
    }
    private void scrollUp() { scrollTo(offset() - Document.lineHeight(font) * 3); }
    private void scrollDown() { scrollTo(offset() + Document.lineHeight(font) * 3); }

    private void scrollTo(int row) {
        li.cil.oc.client.Manual.history.top().offset_$eq(Math.max(0, Math.min(maxOffset(), row)));
        int yMin = topPos + SCROLL_POS_Y;
        scrollButton.setY(maxOffset() > 0 ? yMin + (SCROLL_HEIGHT - 13) * offset() / maxOffset() : yMin);
    }

    private boolean isCoordinateOverContent(int x, int y) {
        return x >= 8 && x < 8 + DOCUMENT_MAX_WIDTH && y >= 8 && y < 8 + DOCUMENT_MAX_HEIGHT;
    }
    private boolean isCoordinateOverScrollBar(int x, int y) {
        return x >= SCROLL_POS_X && x < SCROLL_POS_X + SCROLL_WIDTH
                && y >= SCROLL_POS_Y && y < SCROLL_POS_Y + SCROLL_HEIGHT;
    }
}
