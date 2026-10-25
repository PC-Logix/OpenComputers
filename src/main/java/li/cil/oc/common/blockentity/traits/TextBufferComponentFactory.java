package li.cil.oc.common.blockentity.traits;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.internal.TextBuffer;
import net.minecraft.world.item.ItemStack;
import scala.Tuple2;

/** Creates and configures the managed screen component for a block entity host. */
public final class TextBufferComponentFactory {
    private TextBufferComponentFactory() { }

    public static TextBuffer create(li.cil.oc.common.blockentity.traits.TextBuffer host, int tier) {
        ItemStack screenItem = li.cil.oc.api.Items.get(
                li.cil.oc.Constants.BlockName$.MODULE$.ScreenTier1()).createItemStack(1);
        TextBuffer buffer = (TextBuffer) li.cil.oc.api.Driver.driverFor(screenItem, host.getClass())
                .createEnvironment(screenItem, host);
        Tuple2<Object, Object> resolution = Settings.get().screenResolutionsByTier()[tier];
        buffer.setMaximumResolution(((Number) resolution._1()).intValue(),
                ((Number) resolution._2()).intValue());
        buffer.setMaximumColorDepth(Settings.get().screenDepthsByTier()[tier]);
        return buffer;
    }
}
