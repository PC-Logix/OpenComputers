package li.cil.oc.util;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.machine.Value;
import li.cil.repack.org.luaj.vm2.LuaString;
import li.cil.repack.org.luaj.vm2.LuaTable;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.Map;
import scala.collection.immutable.List$;
import scala.collection.immutable.Map$;
import scala.jdk.javaapi.CollectionConverters;
import scala.math.ScalaNumber;
import scala.Product;
import scala.runtime.BoxedUnit;

import java.util.ArrayList;
import java.util.Arrays;

/** Converts values between the Scala API and LuaJ. */
public final class ScalaClosureSupport {
    private ScalaClosureSupport() {}

    public static LuaValue toLuaValue(Object value) {
        if (value == null || value == BoxedUnit.UNIT || value instanceof BoxedUnit) return LuaValue.NIL;
        if (value instanceof ScalaNumber number) value = number.underlying();

        if (value instanceof Boolean bool) return LuaValue.valueOf(bool);
        if (value instanceof Byte number) return LuaValue.valueOf(number.intValue());
        if (value instanceof Character character) return LuaValue.valueOf(String.valueOf(character));
        if (value instanceof Short number) return LuaValue.valueOf(number.intValue());
        if (value instanceof Integer number) return LuaValue.valueOf(number);
        if (value instanceof Long number) return LuaValue.valueOf(number.doubleValue());
        if (value instanceof Float number) return LuaValue.valueOf(number);
        if (value instanceof Double number) return LuaValue.valueOf(number);
        if (value instanceof String string) return LuaValue.valueOf(string);
        if (value instanceof byte[] bytes) return LuaValue.valueOf(bytes);
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            Object[] values = new Object[length];
            for (int i = 0; i < length; i++) values[i] = java.lang.reflect.Array.get(value, i);
            return toLuaList(CollectionConverters.asScala(Arrays.asList(values)));
        }
        if (value instanceof Value apiValue && Settings.get().allowUserdata()) return LuaValue.userdataOf(apiValue);
        if (value instanceof Product product) {
            ArrayList<Object> items = new ArrayList<>();
            Iterator<?> iterator = product.productIterator();
            while (iterator.hasNext()) items.add(iterator.next());
            return toLuaList(CollectionConverters.asScala(items));
        }
        if (value instanceof scala.collection.Seq<?> sequence) return toLuaList(sequence);
        if (value instanceof java.util.Map<?, ?> javaMap) {
            java.util.HashMap<Object, Object> entries = new java.util.HashMap<>(javaMap);
            return toLuaTable(CollectionConverters.asScala(entries));
        }
        if (value instanceof Map<?, ?> map) return toLuaTable(map);

        OpenComputers.log().warn("Tried to push an unsupported value of type to Lua: " + value.getClass().getName() + ".");
        return LuaValue.NIL;
    }

    public static LuaValue toLuaList(scala.collection.Iterable<?> values) {
        ArrayList<LuaValue> converted = new ArrayList<>();
        Iterator<?> iterator = values.iterator();
        while (iterator.hasNext()) converted.add(toLuaValue(iterator.next()));
        return LuaValue.listOf(converted.toArray(LuaValue[]::new));
    }

    public static LuaValue toLuaTable(Map<?, ?> values) {
        ArrayList<LuaValue> converted = new ArrayList<>(values.size() * 2);
        Iterator<?> iterator = values.iterator();
        while (iterator.hasNext()) {
            Tuple2<?, ?> entry = (Tuple2<?, ?>) iterator.next();
            converted.add(toLuaValue(entry._1()));
            converted.add(toLuaValue(entry._2()));
        }
        return LuaValue.tableOf(converted.toArray(LuaValue[]::new));
    }

    public static Object toSimpleJavaObject(LuaValue value) {
        return switch (value.type()) {
            case LuaValue.TBOOLEAN -> value.toboolean();
            case LuaValue.TNUMBER -> value.todouble();
            case LuaValue.TSTRING -> value instanceof LuaString string
                ? Arrays.copyOfRange(string.m_bytes, string.m_offset, string.m_offset + string.m_length)
                : value.tojstring();
            case LuaValue.TTABLE -> {
                LuaTable table = value.checktable();
                java.util.HashMap<Object, Object> result = new java.util.HashMap<>();
                for (LuaValue key : table.keys()) result.put(toSimpleJavaObject(key), toSimpleJavaObject(table.get(key)));
                yield Map$.MODULE$.from(CollectionConverters.asScala(result));
            }
            case LuaValue.TUSERDATA -> value.touserdata();
            default -> null;
        };
    }

    public static scala.collection.immutable.Seq<Object> toSimpleJavaObjects(Varargs args, int start) {
        ArrayList<Object> values = new ArrayList<>();
        for (int index = start; index <= args.narg(); index++) values.add(toSimpleJavaObject(args.arg(index)));
        return List$.MODULE$.from(CollectionConverters.asScala(values));
    }
}
