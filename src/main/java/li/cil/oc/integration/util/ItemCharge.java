package li.cil.oc.integration.util;

import li.cil.oc.common.IMC;
import net.minecraft.world.item.ItemStack;
import scala.jdk.javaapi.CollectionConverters;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ItemCharge {
    private static final Set<Charger> chargers = new LinkedHashSet<>();

    private ItemCharge() {}

    public static void add(Method canCharge, Method charge) {
        chargers.add(new Charger(canCharge, charge));
    }

    public static boolean canCharge(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Charger charger : chargers) {
            if (IMC.tryInvokeStatic(charger.canCharge(), args(stack), false)) return true;
        }
        return false;
    }

    // Returns the amount of the delta that could not be applied.
    public static double charge(ItemStack stack, double amount) {
        if (stack.isEmpty()) return amount;
        for (Charger charger : chargers) {
            if (IMC.tryInvokeStatic(charger.canCharge(), args(stack), false)) {
                return IMC.tryInvokeStatic(charger.charge(), args(stack, amount, false), amount);
            }
        }
        return amount;
    }

    private static scala.collection.immutable.List<Object> args(Object... values) {
        return CollectionConverters.asScala(Arrays.asList(values)).toList();
    }

    private record Charger(Method canCharge, Method charge) {}
}
