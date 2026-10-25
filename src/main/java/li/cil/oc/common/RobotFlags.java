package li.cil.oc.common;

import li.cil.oc.OpenComputers;
import net.minecraft.resources.ResourceLocation;
import scala.Option;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The pride flags that may be displayed on a robot. */
public final class RobotFlags {
    public record Flag(String name, ResourceLocation id, float height) {}

    private RobotFlags() {}

    private static Flag flag(String name, float height) {
        return new Flag(name, ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), name + "_flag"), height);
    }

    public static final Flag Progress = flag("progress", 6f);
    public static final Flag Lesbian = flag("lesbian", 5f);
    public static final Flag Bisexual = flag("bisexual", 6f);
    public static final Flag Pansexual = flag("pansexual", 6f);
    public static final Flag Asexual = flag("asexual", 6f);
    public static final Flag Rainbow = flag("rainbow", 6f);
    public static final Flag Trans = flag("trans", 5f);

    public static final List<Flag> All = List.of(Progress, Lesbian, Bisexual, Pansexual, Asexual, Rainbow, Trans);
    private static final Map<ResourceLocation, Flag> FLAGS_BY_ID = All.stream().collect(Collectors.toUnmodifiableMap(Flag::id, value -> value));
    private static final Map<String, Flag> FLAGS_BY_NAME = All.stream().flatMap(value -> Stream.of(
            Map.entry(value.name(), value),
            Map.entry(value.id().getPath(), value),
            Map.entry(value.id().toString(), value)
    )).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    public static Option<Flag> byId(ResourceLocation id) {
        return Option.apply(id == null ? null : FLAGS_BY_ID.get(id));
    }

    public static Optional<Flag> byIdOptional(ResourceLocation id) {
        return Optional.ofNullable(id == null ? null : FLAGS_BY_ID.get(id));
    }

    public static Option<Flag> byName(String value) {
        return Option.apply(value == null ? null : FLAGS_BY_NAME.get(value.trim().toLowerCase(Locale.ROOT)));
    }
}
