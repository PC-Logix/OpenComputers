package li.cil.oc.common.blockentity.traits;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedMutableDataComponentHolder;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.util.ColorRGBA;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

/** Data component access for colored block entities. */
public final class ColoredJava {
    private ColoredJava() { }


    public static final class State {
        private int color;

        public int color() {
            return color;
        }

        public boolean setColor(int value) {
            if (value == color) return false;
            color = value;
            return true;
        }

        public void load(DataComponentHolder holder) {
            color = loadColor(holder, color);
        }

        public void save(MutableDataComponentHolder holder) {
            saveColor(holder, color);
        }
    }
    public static int loadColor(DataComponentHolder holder, int currentColor) {
        return new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.RENDER_COLOR)
            .map(ColorRGBA::rgba).orElse(currentColor);
    }

    public static void saveColor(MutableDataComponentHolder holder, int color) {
        new ExtendedMutableDataComponentHolder(holder).setComponent(OCComponentsJava.RENDER_COLOR, new ColorRGBA(color));
    }
    public static void loadHostComponents(Object host, DataComponentHolder holder) {
        if (host instanceof Colored colored) colored.colorState().load(holder);
    }

    public static void saveHostComponents(Object host, MutableDataComponentHolder holder) {
        if (host instanceof Colored colored) colored.colorState().save(holder);
    }

    public static void onColorChanged(Colored host) {
        net.minecraft.world.level.block.entity.BlockEntity blockEntity =
            (net.minecraft.world.level.block.entity.BlockEntity) host;
        if (blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide()) return;
        try {
            li.cil.oc.server.GeneralPacketSender.sendColorChange(host);
        } catch (java.io.IOException error) {
            throw unchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T { throw (T) error; }
}
