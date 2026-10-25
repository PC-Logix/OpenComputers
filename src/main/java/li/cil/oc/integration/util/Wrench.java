package li.cil.oc.integration.util;

import li.cil.oc.common.IMC;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import scala.jdk.javaapi.CollectionConverters;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Wrench {
    private static final Set<Method> usages = new LinkedHashSet<>();
    private static final Set<Method> checks = new LinkedHashSet<>();

    private Wrench() {}

    public static void addUsage(Method wrench) {
        usages.add(wrench);
    }

    public static void addCheck(Method checker) {
        checks.add(checker);
    }

    public static boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Method check : checks) {
            if (IMC.tryInvokeStatic(check, args(stack), false)) return true;
        }
        return false;
    }

    public static boolean holdsApplicableWrench(Player player, BlockPos position) {
        if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) return false;
        for (Method usage : usages) {
            if (IMC.tryInvokeStatic(usage, args(player, position, false), false)) return true;
        }
        return false;
    }

    public static void wrenchUsed(Player player, BlockPos position) {
        if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) return;
        for (Method usage : usages) IMC.tryInvokeStaticVoid(usage, args(player, position, true));
    }

    private static scala.collection.immutable.List<Object> args(Object... values) {
        return CollectionConverters.asScala(Arrays.asList(values)).toList();
    }
}
