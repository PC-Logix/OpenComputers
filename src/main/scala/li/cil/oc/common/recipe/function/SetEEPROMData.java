package li.cil.oc.common.recipe.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.common.datacomponents.OCComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Resolve the EEPROM script and data and set it on the resulting item stack.
 *
 * @param script The path to the EEPROM script.
 * @param data   The path to the EEPROM data, if specified.
 */
public record SetEEPROMData(String script, Optional<String> data) implements RecipeFunction {
    private static final MapCodec<SetEEPROMData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.fieldOf("script").forGetter(SetEEPROMData::script),
        Codec.STRING.optionalFieldOf("data").forGetter(SetEEPROMData::data)
    ).apply(i, SetEEPROMData::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, SetEEPROMData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SetEEPROMData::script,
        ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), SetEEPROMData::data,
        SetEEPROMData::new
    );

    public static final RecipeFunction.Type<SetEEPROMData> TYPE = new RecipeFunction.Type<>(CODEC, STREAM_CODEC);

    @Override
    public Type<?> getType() {
        return TYPE;
    }

    @Override
    public void configureResult(ItemStack result) {
        result.set(OCComponents.EEPROM_CODE().get(), readData(script(), Settings.get().eepromSize()));

        if (data().isPresent()) {
            result.set(OCComponents.EEPROM_DATA().get(), readData(data().get(), Settings.get().eepromDataSize()));
        }
    }

    private static @Nullable ByteBuffer readData(String path, int maxSize) {
        try (var stream = OpenComputers.class.getResourceAsStream(Settings.scriptPath() + path)) {
            if (stream == null) throw new NoSuchFileException(path);

            var data = new byte[maxSize];
            var read = stream.read(data);
            if (read >= 0) return ByteBuffer.wrap(Arrays.copyOf(data, read)).asReadOnlyBuffer();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read EEPROM", e);
        }

        return null;
    }
}
