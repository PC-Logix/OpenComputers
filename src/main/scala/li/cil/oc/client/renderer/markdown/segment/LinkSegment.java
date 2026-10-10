package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.client.Manual;
import li.cil.oc.client.renderer.markdown.MarkupFormat$;
import net.minecraft.Util;
import scala.Enumeration;
import scala.Option;
import scala.Some;

import java.net.URI;

public class LinkSegment extends TextSegment implements InteractiveSegment {
    private static final int NORMAL_COLOR = 0x66FF66;
    private static final int NORMAL_HOVER_COLOR = 0xAAFFAA;
    private static final int ERROR_COLOR = 0xFF6666;
    private static final int ERROR_HOVER_COLOR = 0xFFAAAA;
    private static final int FADE_TIME = 500;

    private final String url;
    private Boolean linkValid;
    private long lastHovered = System.currentTimeMillis() - FADE_TIME;

    public LinkSegment(Segment parent, String text, String url) {
        super(parent, text);
        this.url = url;
    }

    public String url() {
        return url;
    }

    private boolean isLinkValid() {
        if (linkValid == null) {
            linkValid = isExternalUrl() || API.manual.contentFor(Manual.makeRelative(url, Manual.history().top().path())) != null;
        }
        return linkValid;
    }

    private boolean isExternalUrl() {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    @Override
    public Option<Object> color() {
        int color = isLinkValid() ? NORMAL_COLOR : ERROR_COLOR;
        int hoverColor = isLinkValid() ? NORMAL_HOVER_COLOR : ERROR_HOVER_COLOR;
        int timeSinceHover = (int) (System.currentTimeMillis() - lastHovered);
        return new Some<>(timeSinceHover > FADE_TIME
            ? color : fadeColor(hoverColor, color, timeSinceHover / (float) FADE_TIME));
    }

    @Override
    public Option<String> tooltip() {
        return Option.apply(url);
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY) {
        if (isExternalUrl()) {
            Util.getPlatform().openUri(URI.create(url));
        } else {
            Manual.navigate(Manual.makeRelative(url, Manual.history().top().path()));
        }
        return true;
    }

    @Override
    public void notifyHover() {
        lastHovered = System.currentTimeMillis();
    }

    private int fadeColor(int first, int second, float fraction) {
        int red = (int) (((first >>> 16) & 0xFF) + (((second >>> 16) & 0xFF) - ((first >>> 16) & 0xFF)) * fraction);
        int green = (int) (((first >>> 8) & 0xFF) + (((second >>> 8) & 0xFF) - ((first >>> 8) & 0xFF)) * fraction);
        int blue = (int) ((first & 0xFF) + ((second & 0xFF) - (first & 0xFF)) * fraction);
        return (red << 16) | (green << 8) | blue;
    }

    @Override
    public String toString(Enumeration.Value format) {
        if (format == MarkupFormat$.MODULE$.Markdown()) return "[" + text() + "](" + url + ")";
        return isExternalUrl() ? text() : "[link{" + OpenComputers.ID() + ":" + url + "}]" + text() + " [link{}]";
    }
}
