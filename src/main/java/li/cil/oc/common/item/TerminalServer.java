package li.cil.oc.common.item;

import li.cil.oc.Settings;
import net.minecraft.world.item.Item;
import scala.collection.immutable.Seq;
import scala.jdk.javaapi.CollectionConverters;

import java.util.List;

public final class TerminalServer extends SimpleItemBase {
    private String unlocalizedName;

    public TerminalServer(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public Seq<Object> tooltipData() {
        return CollectionConverters.asScala(List.<Object>of(Settings.get().terminalsPerServer())).toSeq();
    }
}
