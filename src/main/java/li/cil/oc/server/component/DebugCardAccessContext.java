package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

public record DebugCardAccessContext(String player, String nonce) {
    public static final Codec<DebugCardAccessContext> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("player").forGetter(DebugCardAccessContext::player),
            Codec.STRING.fieldOf("nonce").forGetter(DebugCardAccessContext::nonce)
    ).apply(instance, DebugCardAccessContext::new));
    public void saveData(MutableDataComponentHolder holder) {
        holder.set(OCComponentsJava.DEBUG_CARD_ACCESS_CONTEXT, this);
    }
}
