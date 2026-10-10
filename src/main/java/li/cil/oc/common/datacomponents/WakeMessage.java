package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record WakeMessage(String message, boolean fuzzy) {
    public static final Codec<WakeMessage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("message").forGetter(WakeMessage::message),
        Codec.BOOL.fieldOf("fuzzy").forGetter(WakeMessage::fuzzy)
    ).apply(instance, WakeMessage::new));
}
