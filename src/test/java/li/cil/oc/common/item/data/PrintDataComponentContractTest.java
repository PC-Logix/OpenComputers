package li.cil.oc.common.item.data;

import net.minecraft.world.phys.AABB;
import org.junit.Test;
import scala.Option$;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PrintDataComponentContractTest {
    @Test
    public void valueEqualityCoversSerializedPrintState() {
        PrintData first = new PrintData();
        PrintData equivalent = new PrintData();

        assertEquals(first, equivalent);
        assertEquals(first.hashCode(), equivalent.hashCode());

        first.stateOn().$plus$eq(new PrintShape(new AABB(0, 0, 0, 1, 1, 1), "stone", Option$.MODULE$.empty()));

        assertNotEquals(first, equivalent);
    }
}
