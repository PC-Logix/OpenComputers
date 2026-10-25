package li.cil.oc.integration.util;

import li.cil.oc.util.StackOption;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import scala.Function0;
import scala.Function3;
import scala.collection.mutable.Set;
import scala.collection.mutable.Set$;
import scala.jdk.javaapi.CollectionConverters;

public final class ItemSearch {
    private static final Set<Function0<Object>> focusedInput = Set$.MODULE$.empty();
    private static final Set<Function3<AbstractContainerScreen<?>, Object, Object, StackOption>> stackFocusing = Set$.MODULE$.empty();

    private ItemSearch() {}

    public static Set<Function0<Object>> focusedInput() {
        return focusedInput;
    }

    public static Set<Function3<AbstractContainerScreen<?>, Object, Object, StackOption>> stackFocusing() {
        return stackFocusing;
    }

    public static boolean isInputFocused() {
        for (Function0<Object> check : CollectionConverters.asJava(focusedInput)) {
            if ((Boolean) check.apply()) return true;
        }
        return false;
    }

    public static StackOption hoveredStack(AbstractContainerScreen<?> container, int mouseX, int mouseY) {
        for (Function3<AbstractContainerScreen<?>, Object, Object, StackOption> check : CollectionConverters.asJava(stackFocusing)) {
            StackOption stack = check.apply(container, mouseX, mouseY);
            if (stack.isDefined()) return StackOption.apply(stack.get());
        }
        return StackOption.empty();
    }
}
