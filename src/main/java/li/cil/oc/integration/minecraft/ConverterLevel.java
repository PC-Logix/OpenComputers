package li.cil.oc.integration.minecraft;

import com.google.common.hash.Hashing;
import li.cil.oc.api.driver.Converter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public final class ConverterLevel implements Converter {
    public static final ConverterLevel INSTANCE = new ConverterLevel();

    private ConverterLevel() {}

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ServerLevel world) {
            var hash = Hashing.md5().newHasher()
                    .putLong(world.getSeed())
                    .putString(world.dimension().location().toString(), StandardCharsets.UTF_8)
                    .hash().asBytes();
            output.put("id", UUID.nameUUIDFromBytes(hash).toString());
        }
        if (value instanceof Level world) {
            output.put("name", world.dimension().location().toString());
        }
    }
}
