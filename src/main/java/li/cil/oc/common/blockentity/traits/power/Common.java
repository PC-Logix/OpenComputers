package li.cil.oc.common.blockentity.traits.power;

import li.cil.oc.SettingsAccessJava;
import li.cil.oc.api.network.Connector;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.DoubleUnaryOperator;

/** Java power-port behavior independent of the block entity's lifecycle hierarchy. */
public interface Common {
    boolean isClient();
    double energyThroughput();

    @OnlyIn(Dist.CLIENT)
    default boolean hasConnector(Direction side) { return false; }
    default Optional<Connector> connectorOptional(Direction side) { return Optional.empty(); }

    /** Scala compatibility accessor. */
    @Deprecated
    default Option<Connector> connector(Direction side) { return OptionConverters.toScala(connectorOptional(side)); }

    default void tryAllSidesJava(BiFunction<Double, Direction, Double> provider,
                                 DoubleUnaryOperator fromOther, DoubleUnaryOperator toOther) {
        PowerCommonJava.tryAllSides(energyThroughput(), SettingsAccessJava.get().tickFrequency(),
            this::globalDemand, provider, value -> fromOther.applyAsDouble(value),
            value -> toOther.applyAsDouble(value), (side, energy) -> tryChangeBuffer(side, energy));
    }

    /** Scala callback compatibility; Java integrations can use tryAllSidesJava. */
    default void tryAllSides(scala.Function2<Object, Direction, Object> provider,
                            scala.Function1<Object, Object> fromOther,
                            scala.Function1<Object, Object> toOther) {
        tryAllSidesJava((value, side) -> ((Number) provider.apply(value, side)).doubleValue(),
            value -> ((Number) fromOther.apply(value)).doubleValue(),
            value -> ((Number) toOther.apply(value)).doubleValue());
    }

    default boolean canConnectPower(Direction side) {
        return PowerCommonJava.canConnectPower(SettingsAccessJava.get().ignorePower(), isClient(),
            () -> hasConnector(side), () -> connectorOptional(side).isPresent());
    }
    default double tryChangeBuffer(Direction side, double amount) { return tryChangeBuffer(side, amount, true); }
    default boolean tryChangeBuffer$default$3() { return true; }
    default double tryChangeBuffer(Direction side, double amount, boolean receive) {
        if (isClient() || SettingsAccessJava.get().ignorePower()) return 0;
        Optional<Connector> selected = connectorOptional(side);
        return selected.isEmpty() ? 0 :
            PowerCommonJava.tryChangeBuffer(selected.get(), energyThroughput(), amount, globalDemand(side), receive);
    }
    default double globalBuffer(Direction side) {
        return isClient() ? 0 : connectorOptional(side).map(Connector::globalBuffer).orElse(0.0);
    }
    default double globalBufferSize(Direction side) {
        return isClient() ? 0 : connectorOptional(side).map(Connector::globalBufferSize).orElse(0.0);
    }
    default double globalDemand(Direction side) {
        return PowerCommonJava.globalDemand(energyThroughput(), globalBufferSize(side), globalBuffer(side));
    }
}
