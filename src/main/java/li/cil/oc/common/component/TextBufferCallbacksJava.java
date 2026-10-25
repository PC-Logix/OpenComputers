package li.cil.oc.common.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.blockentity.Screen;
import li.cil.oc.server.component.Keyboard;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.ResultWrapper;
import scala.Tuple2;

import java.util.LinkedHashSet;
import java.util.Set;

/** Java implementations behind the screen component's annotated callback bridge. */
final class TextBufferCallbacksJava {
    private TextBufferCallbacksJava() { }

    static Object[] isOn(TextBuffer screen) {
        return ResultWrapper.result(screen.getPowerState());
    }

    static Object[] turnOn(TextBuffer screen) {
        boolean oldState = screen.getPowerState();
        screen.setPowerState(true);
        return ResultWrapper.result(screen.getPowerState() != oldState, screen.getPowerState());
    }

    static Object[] turnOff(TextBuffer screen) {
        boolean oldState = screen.getPowerState();
        screen.setPowerState(false);
        return ResultWrapper.result(screen.getPowerState() != oldState, screen.getPowerState());
    }

    static Object[] aspectRatio(TextBuffer screen) {
        synchronized (screen) {
            Tuple2<?, ?> ratio = screen.aspectRatioForCallbacks();
            return ResultWrapper.result(ratio._1(), ratio._2());
        }
    }

    static Object[] keyboards(TextBuffer screen, Context context) {
        context.pause(0.25);
        Set<String> addresses = new LinkedHashSet<>();
        if (screen.host() instanceof Screen screenBlockEntity) {
            for (Screen member : screenBlockEntity.screensJava()) collectKeyboardAddresses(member.node(), addresses);
        } else {
            collectKeyboardAddresses(screen.node(), addresses);
        }
        return new Object[]{addresses.toArray(String[]::new)};
    }

    private static void collectKeyboardAddresses(Node node, Set<String> addresses) {
        if (node == null) return;
        for (Node neighbor : node.neighbors()) {
            if (neighbor.host() instanceof Keyboard) addresses.add(neighbor.address());
        }
    }

    static Object[] isPrecise(TextBuffer screen) {
        return ResultWrapper.result(screen.precisionModeForCallbacks());
    }

    static Object[] setPrecise(TextBuffer screen, Arguments args) {
        if (screen.precisionSupportedForCallbacks()) {
            boolean oldValue = screen.precisionModeForCallbacks();
            screen.setPrecisionModeForCallbacks(args.checkBoolean(0));
            return ResultWrapper.result(oldValue);
        }
        return ResultWrapper.result(ResultWrapper.unit, "unsupported operation");
    }

    static Object[] hardwareResolution(TextBuffer screen) {
        Tuple2<?, ?> resolution = screen.maximumResolutionForCallbacks();
        return ResultWrapper.result(((Number) resolution._1()).intValue(), ((Number) resolution._2()).intValue());
    }

    static Object[] hardwareDepth(TextBuffer screen) {
        return ResultWrapper.result(PackedColor.Depth.bits(screen.maximumDepthForCallbacks()));
    }
}
