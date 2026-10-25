package li.cil.oc.common.blockentity.traits;

/** A screen's managed buffer is created lazily after construction supplies its host context. */
public final class TextBufferHostStateJava {
    final TextBufferPersistenceJava persistence = new TextBufferPersistenceJava();
    private li.cil.oc.api.internal.TextBuffer buffer;
    public synchronized li.cil.oc.api.internal.TextBuffer buffer(TextBuffer host) {
        if (buffer == null) buffer = TextBufferComponentFactory.create(host, host.tier());
        return buffer;
    }
}
