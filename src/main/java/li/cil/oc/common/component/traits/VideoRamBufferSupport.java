package li.cil.oc.common.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.TextBuffer;

final class VideoRamBufferSupport {
    private VideoRamBufferSupport() {}

    static TextBuffer newBuffer() {
        return new TextBuffer(1, 1, new PackedColor.SingleBitFormat(Settings.get().monochromeColor()));
    }
}
