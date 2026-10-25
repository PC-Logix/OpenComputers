package li.cil.oc.common.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.server.ActivityPacketSender;

public class Screen extends TextBuffer {
    private final li.cil.oc.common.blockentity.Screen screen;

    public Screen(li.cil.oc.common.blockentity.Screen screen) {
        super(screen);
        this.screen = screen;
    }

    @Callback(direct = true, doc = "function():boolean -- Whether touch mode is inverted (sneak-activate opens GUI, instead of normal activate).")
    public Object[] isTouchModeInverted(Context computer, Arguments args) {
        return new Object[]{screen.invertTouchMode()};
    }

    @Callback(doc = "function(value:boolean):boolean -- Sets whether to invert touch mode (sneak-activate opens GUI, instead of normal activate).")
    public Object[] setTouchModeInverted(Context computer, Arguments args) {
        boolean newValue = args.checkBoolean(0);
        boolean oldValue = screen.invertTouchMode();
        if (newValue != oldValue) {
            screen.invertTouchMode_$eq(newValue);
            ActivityPacketSender.sendScreenTouchModeUnchecked(screen, newValue);
        }
        return new Object[]{oldValue};
    }
}
