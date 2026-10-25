package li.cil.oc.common.blockentity.traits.power;

import net.minecraft.core.Direction;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import scala.Option;

/** Energy budget calculations shared by power-accepting block entities. */
public final class PowerCommonJava {
    private PowerCommonJava() { }


    public static double tryChangeBuffer(boolean isClient, BooleanSupplier ignorePower,
                                         Supplier<Option<li.cil.oc.api.network.Connector>> connector,
                                         double throughput, double amount, DoubleSupplier demand,
                                         boolean doReceive) {
        if (isClient || ignorePower.getAsBoolean()) return 0;
        Option<li.cil.oc.api.network.Connector> selected = connector.get();
        if (selected == null || selected.isEmpty()) return 0;
        return tryChangeBuffer(selected.get(), throughput, amount, demand.getAsDouble(), doReceive);
    }

    public static double globalBuffer(boolean isClient,
                                      Supplier<Option<li.cil.oc.api.network.Connector>> connector) {
        if (isClient) return 0;
        Option<li.cil.oc.api.network.Connector> selected = connector.get();
        return selected != null && selected.isDefined() ? selected.get().globalBuffer() : 0;
    }

    public static double globalBufferSize(boolean isClient,
                                          Supplier<Option<li.cil.oc.api.network.Connector>> connector) {
        if (isClient) return 0;
        Option<li.cil.oc.api.network.Connector> selected = connector.get();
        return selected != null && selected.isDefined() ? selected.get().globalBufferSize() : 0;
    }
    public static boolean canConnectPower(boolean ignorePower, boolean isClient,
                                          BooleanSupplier clientHasConnector,
                                          BooleanSupplier serverHasConnector) {
        if (ignorePower) return false;
        return isClient ? clientHasConnector.getAsBoolean() : serverHasConnector.getAsBoolean();
    }
    public static double cappedAmount(double throughput, double amount, double demand) {
        return Math.max(0, Math.min(Math.min(throughput, amount), demand));
    }
    public static double globalDemand(double throughput, double bufferSize, double buffer) {
        return Math.max(0, Math.min(throughput, bufferSize - buffer));
    }

    public static double tryChangeBuffer(li.cil.oc.api.network.Connector connector, double throughput,
                                         double amount, double demand, boolean doReceive) {
        double cappedAmount = cappedAmount(throughput, amount, demand);
        return doReceive ? cappedAmount - connector.changeBuffer(cappedAmount) : cappedAmount;
    }

    public static double globalBuffer(boolean isClient, li.cil.oc.api.network.Connector connector) {
        return !isClient && connector != null ? connector.globalBuffer() : 0;
    }

    public static double globalBufferSize(boolean isClient, li.cil.oc.api.network.Connector connector) {
        return !isClient && connector != null ? connector.globalBufferSize() : 0;
    }
    public static void tryAllSides(double throughput, double tickFrequency,
                                   ToDoubleFunction<Direction> globalDemand,
                                   BiFunction<Double, Direction, Double> provider,
                                   Function<Double, Double> fromOther,
                                   Function<Double, Double> toOther,
                                   BiFunction<Direction, Double, Double> tryChangeBuffer) {
        double budget = throughput * tickFrequency;
        for (Direction side : Direction.values()) {
            double demand = toOther.apply(Math.min(budget, globalDemand.applyAsDouble(side)));
            if (demand > 1) {
                double energy = fromOther.apply(provider.apply(demand, side));
                if (energy > 0) budget -= tryChangeBuffer.apply(side, energy);
            }
        }
    }
}
