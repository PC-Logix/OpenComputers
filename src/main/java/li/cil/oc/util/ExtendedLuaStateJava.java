package li.cil.oc.util;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.machine.Value;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaType;
import scala.Function1;
import scala.Product;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.Seq;
import scala.collection.immutable.List;
import scala.collection.immutable.List$;
import scala.math.ScalaNumber;
import scala.runtime.BoxedUnit;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/** Bridges Scala values and Lua values while preserving recursive references. */
public final class ExtendedLuaStateJava {
    private final LuaState lua;

    public ExtendedLuaStateJava(LuaState lua) { this.lua = lua; }

    public void pushScalaFunction(Function1<LuaState, Object> function) {
        lua.pushJavaFunction(state -> (Integer) function.apply(state));
    }

    public void pushValue(Object value) { pushValue(value, new IdentityHashMap<>()); }

    public void pushValue(Object value, IdentityHashMap<Object, Integer> memo) {
        boolean recursive = memo.size() > 0;
        int oldTop = lua.getTop();
        if (memo.containsKey(value)) {
            lua.pushValue(memo.get(value));
            return;
        }

        Object actual = value instanceof ScalaNumber number ? number.underlying() : value;
        if (actual == null || actual instanceof BoxedUnit) lua.pushNil();
        else if (actual instanceof Boolean item) lua.pushBoolean(item);
        else if (actual instanceof Byte item) lua.pushInteger(item.longValue());
        else if (actual instanceof Character item) lua.pushString(String.valueOf(item));
        else if (actual instanceof Short item) lua.pushInteger(item.longValue());
        else if (actual instanceof Integer item) lua.pushInteger(item.longValue());
        else if (actual instanceof Long item) lua.pushInteger(item);
        else if (actual instanceof Float item) lua.pushNumber(item.doubleValue());
        else if (actual instanceof Double item) lua.pushNumber(item);
        else if (actual instanceof String item) lua.pushString(item);
        else if (actual instanceof byte[] item) lua.pushByteArray(item);
        else if (actual.getClass().isArray()) {
            int length = Array.getLength(actual);
            ArrayList<Object> values = new ArrayList<>(length);
            for (int i = 0; i < length; i++) values.add(Array.get(actual, i));
            pushList(actual, values, memo);
        }
        else if (actual instanceof Seq<?> sequence) {
            ArrayList<Object> values = new ArrayList<>();
            Iterator<?> iterator = sequence.iterator();
            while (iterator.hasNext()) values.add(iterator.next());
            pushList(actual, values, memo);
        }
        else if (actual instanceof Product product) {
            ArrayList<Object> values = new ArrayList<>();
            Iterator<?> iterator = product.productIterator();
            while (iterator.hasNext()) values.add(iterator.next());
            pushList(actual, values, memo);
        }
        else if (actual instanceof java.util.Map<?, ?> map) pushTable(actual, map, memo);
        else if (actual instanceof scala.collection.Map<?, ?> map) pushTable(actual, map, memo);
        else if (actual instanceof Value item && Settings.get().allowUserdata()) lua.pushJavaObjectRaw(item);
        else {
            OpenComputers.log().warn("Tried to push an unsupported value of type to Lua: " + actual.getClass().getName() + ".");
            lua.pushNil();
        }

        if (!recursive) lua.setTop(oldTop + 1);
    }

    private void pushList(Object value, java.util.List<Object> values, IdentityHashMap<Object, Integer> memo) {
        lua.newTable();
        int tableIndex = lua.getTop();
        memo.put(value, tableIndex);
        for (int index = 0; index < values.size(); index++) {
            pushValue(values.get(index), memo);
            lua.rawSet(tableIndex, index + 1);
        }
        lua.pushValue(tableIndex);
    }

    private void pushTable(Object value, java.util.Map<?, ?> map, IdentityHashMap<Object, Integer> memo) {
        lua.newTable(0, map.size());
        int tableIndex = lua.getTop();
        memo.put(value, tableIndex);
        for (Map.Entry<?, ?> entry : map.entrySet()) pushEntry(entry.getKey(), entry.getValue(), tableIndex, memo);
        lua.pushValue(tableIndex);
    }

    private void pushTable(Object value, scala.collection.Map<?, ?> map, IdentityHashMap<Object, Integer> memo) {
        lua.newTable(0, map.size());
        int tableIndex = lua.getTop();
        memo.put(value, tableIndex);
        Iterator<?> iterator = map.iterator();
        while (iterator.hasNext()) {
            Tuple2<?, ?> entry = (Tuple2<?, ?>) iterator.next();
            pushEntry(entry._1(), entry._2(), tableIndex, memo);
        }
        lua.pushValue(tableIndex);
    }

    private void pushEntry(Object key, Object value, int tableIndex, IdentityHashMap<Object, Integer> memo) {
        if (key == null || key instanceof BoxedUnit) return;
        pushValue(key, memo);
        int keyIndex = lua.getTop();
        pushValue(value, memo);
        lua.pushValue(keyIndex);
        lua.insert(-2);
        lua.setTable(tableIndex);
    }

    public Object toSimpleJavaObject(int index) {
        LuaType type = lua.type(index);
        if (type == LuaType.BOOLEAN) return Boolean.valueOf(lua.toBoolean(index));
        if (type == LuaType.NUMBER) {
            if (lua.isInteger(index)) return Long.valueOf(lua.toInteger(index));
            return Double.valueOf(lua.toNumber(index));
        }
        if (type == LuaType.STRING) return lua.toByteArray(index);
        if (type == LuaType.TABLE) return lua.toJavaObject(index, java.util.Map.class);
        if (type == LuaType.USERDATA) return lua.toJavaObjectRaw(index);
        return null;
    }

    public List<Object> toSimpleJavaObjects(int start) {
        ArrayList<Object> values = new ArrayList<>();
        for (int index = start; index <= lua.getTop(); index++) values.add(toSimpleJavaObject(index));
        return List$.MODULE$.from(scala.jdk.javaapi.CollectionConverters.asScala(values));
    }
}
