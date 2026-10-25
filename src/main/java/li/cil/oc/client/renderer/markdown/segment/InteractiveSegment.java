package li.cil.oc.client.renderer.markdown.segment;

import java.util.Optional;

/** Segments that can react to mouse presence and input. */
public interface InteractiveSegment extends Segment {
    default Optional<String> tooltip() {
        return Optional.empty();
    }

    default boolean onMouseClick(int mouseX, int mouseY) {
        return false;
    }

    default void notifyHover() {
    }

    default Optional<InteractiveSegment> checkHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height
                ? Optional.of(this) : Optional.empty();
    }
}
