package li.cil.oc.integration.util;

import li.cil.oc.server.component.RedstoneWireless;
import scala.collection.mutable.Set;
import scala.collection.mutable.Set$;

public final class WirelessRedstone {
    private static final Set<WirelessRedstoneSystem> systems = Set$.MODULE$.empty();

    private WirelessRedstone() {}

    public static Set<WirelessRedstoneSystem> systems() {
        return systems;
    }

    public static boolean isAvailable() {
        return systems.nonEmpty();
    }

    public static void addReceiver(RedstoneWireless receiver) {
        for (WirelessRedstoneSystem system : scala.jdk.javaapi.CollectionConverters.asJava(systems)) {
            try { system.addReceiver(receiver); } catch (Throwable ignored) {}
        }
    }

    public static void removeReceiver(RedstoneWireless receiver) {
        for (WirelessRedstoneSystem system : scala.jdk.javaapi.CollectionConverters.asJava(systems)) {
            try { system.removeReceiver(receiver); } catch (Throwable ignored) {}
        }
    }

    public static void updateOutput(RedstoneWireless receiver) {
        for (WirelessRedstoneSystem system : scala.jdk.javaapi.CollectionConverters.asJava(systems)) {
            try { system.updateOutput(receiver); } catch (Throwable ignored) {}
        }
    }

    public static void removeTransmitter(RedstoneWireless receiver) {
        for (WirelessRedstoneSystem system : scala.jdk.javaapi.CollectionConverters.asJava(systems)) {
            try { system.removeTransmitter(receiver); } catch (Throwable ignored) {}
        }
    }

    public static boolean getInput(RedstoneWireless receiver) {
        for (WirelessRedstoneSystem system : scala.jdk.javaapi.CollectionConverters.asJava(systems)) {
            if (system.getInput(receiver)) return true;
        }
        return false;
    }

    public interface WirelessRedstoneSystem {
        void addReceiver(RedstoneWireless receiver);

        void removeReceiver(RedstoneWireless receiver);

        void updateOutput(RedstoneWireless receiver);

        void removeTransmitter(RedstoneWireless receiver);

        boolean getInput(RedstoneWireless receiver);
    }
}
