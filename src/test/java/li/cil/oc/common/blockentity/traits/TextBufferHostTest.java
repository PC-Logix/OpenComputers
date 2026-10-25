package li.cil.oc.common.blockentity.traits;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public final class TextBufferHostTest {
    @Test
    public void offWorldServerDataIsDeferredAndCannotBeOverwrittenByDefaultBuffer() {
        List<String> events = new ArrayList<>();
        TextBufferHostStateJava state = new TextBufferHostStateJava();
        TextBuffer host = host(state, false, events);
        CompoundTag input = new CompoundTag();
        input.putInt("screen", 42);
        TextBufferLifecycleJava.loadServer(host, input, null);
        assertTrue(state.persistence.hasPendingServerBufferData());
        CompoundTag output = new CompoundTag();
        output.putInt("screen", 42);
        TextBufferLifecycleJava.saveServer(host, output, null);
        assertEquals(42, output.getInt("screen"));
        assertTrue(events.isEmpty());
    }

    @Test
    public void onlyClientOrConnectedServerBuffersTick() {
        List<String> events = new ArrayList<>();
        TextBufferHostStateJava state = new TextBufferHostStateJava();
        TextBufferLifecycleJava.updateAfterEnvironment(host(state, false, events));
        assertTrue(events.isEmpty());
        TextBufferLifecycleJava.updateAfterEnvironment(host(state, true, events));
        assertEquals(List.of("update"), events);
    }

    private static TextBuffer host(TextBufferHostStateJava state, boolean client, List<String> events) {
        li.cil.oc.api.internal.TextBuffer buffer = (li.cil.oc.api.internal.TextBuffer) Proxy.newProxyInstance(
            TextBuffer.class.getClassLoader(), new Class<?>[]{li.cil.oc.api.internal.TextBuffer.class},
            (proxy, method, args) -> { events.add(method.getName()); return null; });
        return (TextBuffer) Proxy.newProxyInstance(TextBuffer.class.getClassLoader(), new Class<?>[]{TextBuffer.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "textBufferState" -> state;
                case "isClient" -> client;
                case "isServer" -> !client;
                case "getLevel", "node" -> null;
                case "buffer" -> buffer;
                default -> throw new AssertionError("Unexpected callback " + method);
            });
    }
}
