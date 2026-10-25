package li.cil.oc.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * An EEPROM definition loaded from a datapack.
 * <p>
 * This is used to populate the list of EEPROMS in the creative tab and recipe mods.
 *
 * @param label    The display name for the EEPROM.
 * @param code     The relative path to the EEPROM's code.
 * @param data     The relative path to the EEPROM's data.
 * @param readOnly Whether the EEPROM is read-only.
 * @see li.cil.oc.common.Loot
 */
public record EEPROM(String label, Optional<String> code, Optional<String> data, boolean readOnly) {
    public static final String DIRECTORY = "opencomputers/eeproms";

    public static final MapCodec<EEPROM> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("label").forGetter(EEPROM::label),
        Codec.STRING.optionalFieldOf("code").forGetter(EEPROM::code),
        Codec.STRING.optionalFieldOf("data").forGetter(EEPROM::data),
        Codec.BOOL.optionalFieldOf("readonly", false).forGetter(EEPROM::readOnly)
    ).apply(instance, EEPROM::new));

    public static final Codec<EEPROM> CODEC = MAP_CODEC.codec();
}
