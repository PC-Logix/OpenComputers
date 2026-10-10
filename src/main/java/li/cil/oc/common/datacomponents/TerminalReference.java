package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TerminalReference(String key, String server) {
    public static final Codec<TerminalReference> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("key").forGetter(TerminalReference::key),
        Codec.STRING.fieldOf("server").forGetter(TerminalReference::server)
    ).apply(instance, TerminalReference::new));
}
