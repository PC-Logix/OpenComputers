package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.TextBufferRenderCache;
import li.cil.oc.client.renderer.markdown.Document;
import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.Optional;

public class CodeBlockSegment implements InteractiveSegment {
    private final Segment parent;
    private final List<String> lines;
    private final String contents;
    private Segment next;
    private final int padding = 2;

    public CodeBlockSegment(Segment parent, List<String> lines) {
        this.parent = parent;
        this.lines = List.copyOf(lines);
        this.contents = String.join("\n", this.lines);
    }

    @Override
    public Segment parent() {
        return parent;
    }

    @Override
    public Segment next() {
        return next;
    }

    @Override
    public void next_$eq(Segment next) {
        this.next = next;
    }

    @Override
    public int nextX(int indent, int maxWidth, Font renderer) {
        return 0;
    }

    @Override
    public int nextY(int indent, int maxWidth, Font renderer) {
        return Math.max(1, lines.size()) * Document.lineHeight(renderer) + padding * 2;
    }

    @Override
    public Optional<InteractiveSegment> render(GuiGraphics graphics, int x, int y, int indent, int maxWidth,
                                             Font renderer, int mouseX, int mouseY) {
        int height = nextY(indent, maxWidth, renderer);
        graphics.fill(x + indent, y, x + maxWidth, y + height, 0x66000000);
        int currentY = y + padding;
        if (lines.isEmpty()) {
            TextBufferRenderCache.renderer().generateChars(new char[0]);
            TextBufferRenderCache.renderer().drawString(graphics.pose(), "", x + indent + padding, currentY);
        } else {
            var iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                TextBufferRenderCache.renderer().generateChars(line.toCharArray());
                TextBufferRenderCache.renderer().drawString(graphics.pose(), line, x + indent + padding, currentY);
                currentY += Document.lineHeight(renderer);
            }
        }
        return mouseX >= x + indent && mouseY >= y && mouseX <= x + maxWidth && mouseY <= y + height
            ? Optional.of(this) : Optional.empty();
    }

    @Override
    public Optional<String> tooltip() {
        return Optional.of("oc:gui.Analyzer.CopyToClipboard");
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY) {
        Minecraft.getInstance().keyboardHandler.setClipboard(contents);
        return true;
    }

    @Override
    public String toString(MarkupFormat format) {
        return format == MarkupFormat.Markdown ? "```\n" + contents + "\n```" : contents;
    }

    @Override
    public String toString() {
        return toString(MarkupFormat.Markdown);
    }
}
