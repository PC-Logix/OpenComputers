package li.cil.oc.common.item.data;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc.server.component.DebugCardAccessContext;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

import java.util.Optional;

public final class DebugCardDataCodecs {
    private DebugCardDataCodecs() {}

    public static Option<DebugCardAccessContext> load(DataComponentHolder holder) {
        return OptionConverters.toScala(loadOptional(holder));
    }

    public static Optional<DebugCardAccessContext> loadOptional(DataComponentHolder holder) {
        return Optional.ofNullable(holder.get(OCComponentsJava.DEBUG_CARD_ACCESS_CONTEXT.get()));
    }

    public static void save(Option<DebugCardAccessContext> access, MutableDataComponentHolder holder) {
        save(OptionConverters.toJava(access), holder);
    }

    public static void save(Optional<DebugCardAccessContext> access, MutableDataComponentHolder holder) {
        holder.remove(OCComponentsJava.DEBUG_CARD_ACCESS_CONTEXT.get());
        access.ifPresent(value -> value.saveData(holder));
    }

    private static final Codec<DebugCardAccessContext> ACCESS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("player").forGetter(DebugCardAccessContext::player),
        Codec.STRING.fieldOf("nonce").forGetter(DebugCardAccessContext::nonce)
    ).apply(instance, DebugCardAccessContext::new));

    public static final Codec<DebugCardData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ACCESS_CODEC.optionalFieldOf("access").forGetter(DebugCardData::accessOptional)
    ).apply(instance, access -> new DebugCardData(access)));
}
