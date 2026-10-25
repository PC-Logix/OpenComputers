package li.cil.oc.common;

import li.cil.oc.OpenComputers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import scala.Option;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ToolDurabilityProviders {
    private static final List<Method> providers = new ArrayList<>();

    private ToolDurabilityProviders() {}

    public static void add(Method provider) {
        providers.add(provider);
    }

    public static Option<Object> getDurability(ItemStack stack) {
        for (Method provider : providers) {
            double durability;
            try {
                durability = ((Number) provider.invoke(null, stack)).doubleValue();
            } catch (Throwable error) {
                OpenComputers.log().warn("Error invoking callback " +
                        provider.getDeclaringClass().getCanonicalName() + "." + provider.getName() + ".", error);
                durability = Double.NaN;
            }
            if (!Double.isNaN(durability)) return Option.apply(durability);
        }

        if (stack.getItem().components().has(DataComponents.MAX_DAMAGE)) {
            return Option.apply(1.0 - (double) stack.getDamageValue() / stack.getMaxDamage());
        }
        return Option.empty();
    }
}
