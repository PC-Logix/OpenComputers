package li.cil.oc.common.component;

import li.cil.oc.client.TextBufferClientPacketSender;
import li.cil.oc.common.PacketBuilder;
import scala.Option;
import scala.Option$;

/** Deferred packet and client initialization work for text buffers. */
final class TextBufferUpdateJava {
    private TextBufferUpdateJava() { }

    static void markInitialized(TextBuffer screen) {
        screen.syncCooldownForUpdate_$eq(-1);
        screen.invalidateLitAreaForProxy();
    }

    static void requestSynchronization(TextBuffer screen, boolean isClient) {
        // Do not turn an initialized buffer back into an uninitialized one.
        if (shouldRequestSynchronization(isClient, isInitialized(screen))) {
            TextBufferClientRegistry.registerClientBuffer(screen);
        }
    }

    static boolean isInitialized(TextBuffer screen) {
        return screen.syncCooldownForUpdate() < 0;
    }

    static boolean shouldRequestSynchronization(boolean isClient, boolean initialized) {
        return isClient && !initialized;
    }

    static void flushPendingCommands(TextBuffer screen, double wirelessRangeSquared) {
        synchronized (screen) {
            Option<PacketBuilder> pending = screen.pendingCommandBufferForUpdate();
            if (pending.isDefined()) {
                pending.get().sendToPlayersNearHost(screen.host(), Option$.MODULE$.apply(wirelessRangeSquared));
            }
            screen.clearPendingCommandBufferForUpdate();
        }
    }

    static void tickClientSync(TextBuffer screen, boolean isClient, int syncInterval) {
        if (!isClient) return;
        int cooldown = screen.syncCooldownForUpdate();
        if (cooldown > 0) {
            cooldown--;
            if (cooldown == 0) {
                screen.syncCooldownForUpdate_$eq(syncInterval);
                TextBufferClientPacketSender.sendTextBufferInit(screen.proxy().nodeAddress());
            } else {
                screen.syncCooldownForUpdate_$eq(cooldown);
            }
        }
    }
}
