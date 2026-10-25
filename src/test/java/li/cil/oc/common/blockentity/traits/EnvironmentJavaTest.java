package li.cil.oc.common.blockentity.traits;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import li.cil.oc.api.network.Node;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class EnvironmentJavaTest {
    @Test
    public void tickableDefersChangedNotificationUntilUpdate() {
        List<String> events = new ArrayList<>();
        AtomicBoolean pending = new AtomicBoolean();
        NetworkBlockEntity environment = environment(true, events, pending);

        EnvironmentJava.markChanged(environment, () -> events.add("changed"));
        assertEquals(List.of("scheduled:true"), events);
        assertTrue(pending.get());

        EnvironmentJava.updateEntity(environment, () -> events.add("changed"));
        assertEquals(List.of("scheduled:true", "changed", "scheduled:false"), events);
        assertFalse(pending.get());
    }

    @Test
    public void nonTickableMarksChangedImmediately() {
        List<String> events = new ArrayList<>();
        EnvironmentJava.markChanged(environment(false, events, new AtomicBoolean()), () -> events.add("changed"));
        assertEquals(List.of("changed"), events);
    }

    @Test
    public void movingDisposalRemovesNodeBeforeClearingMovingState() {
        List<String> events = new ArrayList<>();
        Node node = (Node) Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[]{Node.class},
            (proxy, method, args) -> {
                if (method.getName().equals("remove")) events.add("remove-node");
                return null;
            });
        NetworkBlockEntity environment = (NetworkBlockEntity) Proxy.newProxyInstance(NetworkBlockEntity.class.getClassLoader(),
            new Class<?>[]{NetworkBlockEntity.class}, (proxy, method, args) -> switch (method.getName()) {
                case "node" -> node;
                case "endMoving" -> { events.add("end-moving"); yield null; }
                default -> null;
            });

        EnvironmentJava.disposeMoving(environment);

        assertEquals(List.of("remove-node", "end-moving"), events);
    }

    @Test
    public void stationaryEnvironmentUsesBlockCenterAndMovingEnvironmentUsesExactPosition() {
        assertEquals(2.5, EnvironmentJava.xPosition(null, 2), 0.0);
        assertEquals(4.5, EnvironmentJava.yPosition(null, 4), 0.0);
        assertEquals(-1.5, EnvironmentJava.zPosition(null, -2), 0.0);

        Vec3 movingPosition = new Vec3(8.25, 9.5, -3.75);
        assertEquals(8.25, EnvironmentJava.xPosition(movingPosition, 0), 0.0);
        assertEquals(9.5, EnvironmentJava.yPosition(movingPosition, 0), 0.0);
        assertEquals(-3.75, EnvironmentJava.zPosition(movingPosition, 0), 0.0);
    }

    private static NetworkBlockEntity environment(boolean tickable, List<String> events, AtomicBoolean pending) {
        Class<?>[] interfaces = tickable
            ? new Class<?>[]{NetworkBlockEntity.class, Tickable.class}
            : new Class<?>[]{NetworkBlockEntity.class};
        return (NetworkBlockEntity) Proxy.newProxyInstance(NetworkBlockEntity.class.getClassLoader(), interfaces,
            (proxy, method, args) -> switch (method.getName()) {
                case "setChangeScheduledForJava" -> {
                    pending.set((boolean) args[0]);
                    events.add("scheduled:" + args[0]);
                    yield null;
                }
                case "isChangeScheduledForJava" -> pending.get();
                case "setChanged" -> { events.add("changed"); yield null; }
                default -> null;
            });
    }
}

