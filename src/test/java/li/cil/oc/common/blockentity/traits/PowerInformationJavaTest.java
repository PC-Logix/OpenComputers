package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PowerInformationJavaTest {
    @Test
    public void sendsFirstSignificantAndPeriodicPowerChanges() {
        PowerInformationJava state = new PowerInformationJava();
        double[] values = {0.5, 1.0};

        assertTrue(PowerInformationJava.shouldSend(power(values), state, 20.0));
        assertFalse(PowerInformationJava.shouldSend(power(values), state, 20.0));
        values[0] = 0.56;
        assertTrue(PowerInformationJava.shouldSend(power(values), state, 20.0));
    }

    @Test
    public void emptyBufferSizeUsesZeroRatio() {
        PowerInformationJava state = new PowerInformationJava();
        assertTrue(PowerInformationJava.shouldSend(power(new double[]{4.0, 0.0}), state, 20.0));
    }

    private static PowerInformation power(double[] values) {
        return (PowerInformation) Proxy.newProxyInstance(PowerInformation.class.getClassLoader(),
            new Class<?>[]{PowerInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                case "globalBuffer" -> values[0];
                case "globalBufferSize" -> values[1];
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                default -> null;
            });
    }
}
