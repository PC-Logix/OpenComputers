package li.cil.oc.common.container;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ComponentInventoryPersistenceJavaTest {
    @Test
    public void clearsTemporaryIdentityComponentsFromRamStack() {
        DataComponentType<String> address = DataComponentType.<String>builder().persistent(Codec.STRING).build();
        DataComponentType<Integer> visibility = DataComponentType.<Integer>builder().persistent(Codec.INT).build();
        List<Object> removed = new ArrayList<>();
        MutableDataComponentHolder holder = (MutableDataComponentHolder) Proxy.newProxyInstance(
                MutableDataComponentHolder.class.getClassLoader(),
                new Class<?>[]{MutableDataComponentHolder.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("remove")) removed.add(arguments[0]);
                    return null;
                });

        ComponentInventoryPersistenceJava.clearMemoryIdentity(holder, address, visibility);

        assertEquals(2, removed.size());
        assertSame(address, removed.get(0));
        assertSame(visibility, removed.get(1));
    }
}
