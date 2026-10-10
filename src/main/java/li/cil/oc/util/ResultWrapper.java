package li.cil.oc.util;

import net.minecraft.world.item.ItemStack;
import scala.math.ScalaNumber;
import scala.runtime.BoxedUnit;

public final class ResultWrapper {
    public static final Object unit = BoxedUnit.UNIT;

    private ResultWrapper() {
    }

    public static Object[] result(Object... args) {
        Object[] values = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof ScalaNumber number) {
                values[i] = number.underlying();
            } else if (arg instanceof ItemStack stack && stack.isEmpty()) {
                values[i] = null;
            } else {
                values[i] = arg;
            }
        }
        return values;
    }
}
