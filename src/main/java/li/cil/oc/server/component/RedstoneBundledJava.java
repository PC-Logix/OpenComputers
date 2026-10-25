package li.cil.oc.server.component;

import net.minecraft.core.Direction;
import li.cil.oc.api.machine.Arguments;
import scala.collection.immutable.Map$;

import java.util.LinkedHashMap;

/** Array and table conversions for bundled redstone callbacks. */
final class RedstoneBundledJava {
    private RedstoneBundledJava() { }

    static int checkColor(Arguments args, int index) {
        int color = args.checkInteger(index);
        if (color < 0 || color >= 16) throw new IllegalArgumentException("invalid color");
        return color;
    }

    static scala.collection.immutable.Map<Integer, Integer> colorsToMap(int[] values) {
        var result = new LinkedHashMap<Integer, Integer>();
        for (int color = 0; color < Math.min(16, values.length); color++) result.put(color, values[color]);
        return Map$.MODULE$.from(scala.jdk.javaapi.CollectionConverters.asScala(result));
    }

    static scala.collection.immutable.Map<Integer, scala.collection.immutable.Map<Integer, Integer>> sidesToMap(int[][] values) {
        var result = new LinkedHashMap<Integer, scala.collection.immutable.Map<Integer, Integer>>();
        Direction[] sides = Direction.values();
        for (Direction side : sides) {
            int index = side.ordinal();
            if (index < values.length && values[index].length > 0) result.put(index, colorsToMap(values[index]));
        }
        return Map$.MODULE$.from(scala.jdk.javaapi.CollectionConverters.asScala(result));
    }
}
