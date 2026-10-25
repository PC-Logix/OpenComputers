package li.cil.oc.common.blockentity.traits.power;

import net.minecraft.core.Direction;
import li.cil.oc.api.network.Connector;
import java.lang.reflect.Proxy;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PowerCommonJavaTest {
    @Test
    public void clampsDemandToAvailableCapacityAndThroughput() {
        assertEquals(4.0, PowerCommonJava.globalDemand(4.0, 20.0, 12.0), 0.0);
        assertEquals(0.0, PowerCommonJava.globalDemand(4.0, 12.0, 12.0), 0.0);
        assertEquals(0.0, PowerCommonJava.globalDemand(4.0, 10.0, 12.0), 0.0);
        assertEquals(3.0, PowerCommonJava.cappedAmount(4.0, 5.0, 3.0), 0.0);
        assertEquals(0.0, PowerCommonJava.cappedAmount(4.0, -1.0, 3.0), 0.0);
    }

    @Test
    public void tryAllSidesSpendsOnlyThePerCycleBudget() {
        List<Double> supplied = new ArrayList<>();
        List<Direction> sides = new ArrayList<>();
        PowerCommonJava.tryAllSides(5.0, 2.0, side -> 8.0,
            (demand, side) -> demand, energy -> energy, value -> value,
            (side, energy) -> {
                sides.add(side);
                supplied.add(energy);
                return energy;
            });
        assertEquals(List.of(8.0, 2.0), supplied);
        assertEquals(List.of(Direction.DOWN, Direction.UP), sides);
    }

    @Test
    public void connectorAndDemandLookupsRemainLazy() {
        int[] calls = {0, 0};
        java.util.function.Supplier<scala.Option<Connector>> connector = () -> {
            calls[0]++;
            return scala.Option.empty();
        };
        java.util.function.DoubleSupplier demand = () -> {
            calls[1]++;
            return 2.0;
        };

        assertEquals(0.0, PowerCommonJava.tryChangeBuffer(true, () -> false, connector,
            4.0, 3.0, demand, true), 0.0);
        assertEquals(List.of(0, 0), List.of(calls[0], calls[1]));
        assertEquals(0.0, PowerCommonJava.tryChangeBuffer(false, () -> true, connector,
            4.0, 3.0, demand, true), 0.0);
        assertEquals(List.of(0, 0), List.of(calls[0], calls[1]));
        assertEquals(0.0, PowerCommonJava.tryChangeBuffer(false, () -> false, connector,
            4.0, 3.0, demand, true), 0.0);
        assertEquals(List.of(1, 0), List.of(calls[0], calls[1]));
    }

    @Test
    public void clientBufferAccessSkipsConnectorLookup() {
        int[] calls = {0};
        java.util.function.Supplier<scala.Option<Connector>> connector = () -> {
            calls[0]++;
            return scala.Option.empty();
        };
        assertEquals(0.0, PowerCommonJava.globalBuffer(true, connector), 0.0);
        assertEquals(0.0, PowerCommonJava.globalBufferSize(true, connector), 0.0);
        assertEquals(0, calls[0]);
    }
}