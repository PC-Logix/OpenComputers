package li.cil.oc.common.component;

import scala.Tuple2;

/** Resolution and viewport state changes for text-buffer displays. */
final class TextBufferVideoModeJava {
    private TextBufferVideoModeJava() { }

    static void setMaximumResolution(TextBuffer screen, int width, int height) {
        validateMaximumResolution(width, height);
        screen.setMaximumResolutionForVideoMode(width, height);
        screen.setFullyLitCostForProxy(screen.computeFullyLitCost());
        screen.proxy().onBufferMaxResolutionChange(width, height);
    }

    static void setAspectRatio(TextBuffer screen, double width, double height) {
        synchronized (screen) {
            screen.setAspectRatioForVideoMode(width, height);
        }
    }

    static double getAspectRatio(TextBuffer screen) {
        return aspectRatio(screen.aspectRatioForVideoMode());
    }

    static void validateMaximumResolution(int width, int height) {
        if (width < 1) throw new IllegalArgumentException("width must be larger or equal to one");
        if (height < 1) throw new IllegalArgumentException("height must be larger or equal to one");
    }

    static double aspectRatio(scala.Tuple2<?, ?> aspectRatio) {
        return ((Number) aspectRatio._1()).doubleValue() / ((Number) aspectRatio._2()).doubleValue();
    }

    static void setMaximumColorDepth(TextBuffer screen, li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {
        screen.setMaximumDepthForVideoMode(depth);
    }

    static boolean afterColorDepthChange(TextBuffer screen,
                                         li.cil.oc.api.internal.TextBuffer.ColorDepth depth,
                                         boolean colorDepthChanged) {
        // Always notify clients; their local state may be dirty even if this value did not change.
        screen.proxy().onBufferDepthChange(depth);
        return colorDepthChanged;
    }

    static boolean setResolution(TextBuffer screen, int width, int height) {
        int maximumWidth = screen.getMaximumWidth();
        int maximumHeight = screen.getMaximumHeight();
        if (width < 1 || height < 1 || width > maximumWidth || height > maximumWidth
            || height * width > maximumWidth * maximumHeight) {
            throw new IllegalArgumentException("unsupported resolution");
        }

        screen.proxy().onBufferResolutionChange(width, height);
        boolean sizeChanged = screen.data().size_$eq(new Tuple2<>(Integer.valueOf(width), Integer.valueOf(height)));
        boolean viewportChanged = screen.setViewport(width, height);
        if (sizeChanged || viewportChanged) {
            if (!viewportChanged && screen.node() != null) {
                screen.node().sendToReachable("computer.signal", "screen_resized", width, height);
            }
            return true;
        }
        return false;
    }

    static boolean setViewport(TextBuffer screen, int width, int height) {
        int maximumWidth = screen.data().width();
        int maximumHeight = screen.data().height();
        if (width < 1 || height < 1 || width > maximumWidth || height > maximumHeight) {
            throw new IllegalArgumentException("unsupported viewport resolution");
        }

        screen.proxy().onBufferViewportResolutionChange(width, height);
        Tuple2<?, ?> currentViewport = screen.viewport();
        if (width != ((Number) currentViewport._1()).intValue()
            || height != ((Number) currentViewport._2()).intValue()) {
            screen.viewport_$eq(new Tuple2<>(Integer.valueOf(width), Integer.valueOf(height)));
            if (screen.node() != null) {
                screen.node().sendToReachable("computer.signal", "screen_resized", width, height);
            }
            return true;
        }
        return false;
    }
}
