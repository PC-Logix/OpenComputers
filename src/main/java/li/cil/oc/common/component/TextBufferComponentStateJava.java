package li.cil.oc.common.component;

import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.common.blockentity.Projector;
import li.cil.oc.common.blockentity.traits.BaseBlockEntity;
import li.cil.oc.common.blockentity.traits.Computer;
import li.cil.oc.common.datacomponents.CompoundStorage;
import li.cil.oc.common.datacomponents.MaximumVideoMode;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import li.cil.oc.common.datacomponents.VideoMode;
import li.cil.oc.server.ComponentTracker;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedMutableDataComponentHolderJava;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.runtime.BoxedUnit;

import java.util.Objects;

/** Loads and saves component-backed display state for text buffers. */
final class TextBufferComponentStateJava {
    private TextBufferComponentStateJava() { }

    static void load(TextBuffer screen, DataComponentHolder holder) {
        ExtendedDataComponentHolder components = new ExtendedDataComponentHolder(holder);

        components.getComponentOptional(OCComponentsJava.IS_ON.get()).ifPresent(screen::restoreDisplaying);
        components.getComponentOptional(OCComponentsJava.IS_POWERED.get()).ifPresent(screen::restoreHasPower);
        components.getComponentOptional(OCComponentsJava.MAX_VIDEO_MODE.get()).ifPresent(screen::restoreMaximumVideoMode);

        screen.restorePrecision(holder.getOrDefault(OCComponentsJava.IS_PRECISE.get(), false));

        components.getComponentOptional(OCComponentsJava.VIDEO_MODE.get()).ifPresentOrElse(value -> {
            screen.restoreViewport(
                Math.max(1, Math.min(value.width(), screen.data().width())),
                Math.max(1, Math.min(value.height(), screen.data().height())));
        }, () -> {
            screen.restoreViewport(screen.data().width(), screen.data().height());
        });
    }

    static void saveState(TextBuffer screen, MutableDataComponentHolder holder) {
        ExtendedMutableDataComponentHolderJava components = new ExtendedMutableDataComponentHolderJava(holder);
        components.setComponent(OCComponentsJava.IS_ON.get(), screen.getPowerState());
        components.setComponent(OCComponentsJava.IS_POWERED.get(), screen.hasPowerForPersistence());
        components.setComponent(OCComponentsJava.MAX_VIDEO_MODE.get(), new MaximumVideoMode(
            screen.getMaximumWidth(), screen.getMaximumHeight(), screen.getMaximumColorDepth().ordinal()));
        components.setComponent(OCComponentsJava.IS_PRECISE.get(), screen.isPreciseForPersistence());
        components.setComponent(OCComponentsJava.VIDEO_MODE.get(), new VideoMode(
            screen.getViewportWidth(), screen.getViewportHeight()));
    }

    static void loadExternalData(TextBuffer screen, Level level) {
        var host = screen.host();
        var chunk = new ChunkPos(new BlockPos((int) host.xPosition(), (int) host.yPosition(), (int) host.zPosition()));
        CompoundTag saved = SaveHandler.loadNBT(level.dimension().location(), chunk, screen.node().address() + "_buffer");
        if (!saved.isEmpty()) {
            CompoundStorage storage = CompoundStorage.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
            screen.data().loadData(storage);
        }
    }

    static boolean shouldSaveInline(EnvironmentHost host) {
        return host instanceof Tablet
            || host instanceof RemoteTerminalHost
            || host instanceof Projector
            || host instanceof BaseBlockEntity moving && moving.isMoving();
    }

    static void scheduleExternalSave(TextBuffer screen, EnvironmentHost host) {
        SaveHandler.scheduleSave(host, new CompoundTag(), screen.node().address() + "_buffer", tag -> {
            CompoundStorage storage = new CompoundStorage();
            screen.data().saveData(storage);
            CompoundTag encoded = (CompoundTag) CompoundStorage.CODEC
                .encodeStart(NbtOps.INSTANCE, storage)
                .getOrThrow();
            tag.merge(encoded);
            return BoxedUnit.UNIT;
        });
    }

    static void pauseConnectedComputers(TextBuffer screen) {
        Node node = screen.node();
        if (node.network() == null) return;

        for (Node connected : node.network().nodes()) {
            if (connected.host() instanceof Computer computer && !computer.machine().isPaused()) {
                computer.machine().pause(0.1);
            }
        }
    }

    static void onConnect(TextBuffer screen, Node connectedNode) {
        if (!Objects.equals(connectedNode, screen.node())) return;
        ComponentTracker.INSTANCE.add(screen.host().getEnvironmentLevel(), connectedNode.address(), screen);
        if (screen.hasPendingExternalLoadForPersistence()) screen.loadExternalDataForPersistence();
    }

    static void onDisconnect(TextBuffer screen, Node disconnectedNode) {
        if (Objects.equals(disconnectedNode, screen.node())) {
            ComponentTracker.INSTANCE.remove(screen.host().getEnvironmentLevel(), screen);
        }
    }
}
