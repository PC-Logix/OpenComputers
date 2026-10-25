package li.cil.oc.util;

import net.minecraft.resources.ResourceLocation;
import scala.Option;
import scala.Option$;

public final class ParseLocation {
    private ParseLocation() {}

    public static Option<ResourceLocation> unapply(String value) {
        return Option$.MODULE$.apply(ResourceLocation.tryParse(value));
    }
}
