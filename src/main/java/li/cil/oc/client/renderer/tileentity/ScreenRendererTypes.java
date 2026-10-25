package li.cil.oc.client.renderer.tileentity;

import java.util.Optional;

/** Immutable render values shared by the hologram renderer. */
final class ScreenRendererTypes {
    record HologramColor(int red, int green, int blue) {}

    record HologramSurfaceLayout(float left, float top, float depth, int width, int height) {}

    record ProjectionBeamLayout(float anchor, float depth, float projectorEdgeOffset, int width) {}

    record MonitorContentLayout(float border, float liftFromSurface) {}

    record HologramLayout(HologramSurfaceLayout surface, Optional<ProjectionBeamLayout> beam,
                          MonitorContentLayout content) {}

    private ScreenRendererTypes() {}
}
