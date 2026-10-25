package li.cil.oc.common.item.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.common.datacomponents.ScalaCodec;
import li.cil.oc.common.datacomponents.ScalaStreamCodec;
import li.cil.oc.util.ExtendedAABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.mutable.Set;
import scala.math.Ordering;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Optional;

public final class PrintDataSupport {
    private static final int STEPPING = 4;
    private static final float STEP = STEPPING / 16f;
    private static final float INV_MAX_VOLUME = 1f / (STEPPING * STEPPING * STEPPING);
    private static final LinkedHashSet<Method> INK_PROVIDERS = new LinkedHashSet<>();

    @SuppressWarnings("unchecked")
    private static final Codec<Boolean> BOOL = (Codec<Boolean>) (Codec<?>) ScalaCodec.BOOL;
    @SuppressWarnings("unchecked")
    private static final Codec<Integer> INT = (Codec<Integer>) (Codec<?>) ScalaCodec.INT;
    @SuppressWarnings("unchecked")
    private static final StreamCodec<ByteBuf, Boolean> STREAM_BOOL = (StreamCodec<ByteBuf, Boolean>) (StreamCodec<?, ?>) ScalaStreamCodec.BOOL;
    @SuppressWarnings("unchecked")
    private static final StreamCodec<ByteBuf, Integer> STREAM_INT = (StreamCodec<ByteBuf, Integer>) (StreamCodec<?, ?>) ScalaStreamCodec.INT;
    @SuppressWarnings("unchecked")
    private static final StreamCodec<ByteBuf, Integer> STREAM_VAR_INT = (StreamCodec<ByteBuf, Integer>) (StreamCodec<?, ?>) ScalaStreamCodec.VAR_INT;
    private static final Ordering<PrintShape> SHAPE_ORDERING = PrintShape::compareTo;

    public static final Codec<PrintData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.optionalField("label", Codec.STRING, false).forGetter(PrintData::labelOptional),
        Codec.optionalField("tooltip", Codec.STRING, false).forGetter(PrintData::tooltipOptional),
        BOOL.fieldOf("button").forGetter(PrintData::isButtonMode),
        INT.fieldOf("redstone_level").forGetter(PrintData::redstoneLevel),
        BOOL.fieldOf("pressure_plate").forGetter(PrintData::pressurePlate),
        ScalaCodec.mutableSet(Shape.CODEC, SHAPE_ORDERING).fieldOf("state_off").forGetter(PrintData::stateOff),
        ScalaCodec.mutableSet(Shape.CODEC, SHAPE_ORDERING).fieldOf("state_on").forGetter(PrintData::stateOn),
        BOOL.fieldOf("beacon_base").forGetter(PrintData::isBeaconBase),
        INT.fieldOf("light_level").forGetter(PrintData::lightLevel),
        BOOL.fieldOf("noclip_off").forGetter(PrintData::noclipOff),
        BOOL.fieldOf("noclip_on").forGetter(PrintData::noclipOn)
    ).apply(instance, (label, tooltip, button, redstone, pressure, off, on, beacon, light, noclipOff, noclipOn) ->
        new PrintData(label, tooltip, button, redstone, pressure, off, on, beacon, light, noclipOff, noclipOn)));

    public static final StreamCodec<ByteBuf, PrintData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PrintData decode(ByteBuf buffer) {
            return new PrintData(
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buffer),
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buffer),
                STREAM_BOOL.decode(buffer),
                STREAM_VAR_INT.decode(buffer),
                STREAM_BOOL.decode(buffer),
                ScalaStreamCodec.mutableSet(Shape.STREAM_CODEC).decode(buffer),
                ScalaStreamCodec.mutableSet(Shape.STREAM_CODEC).decode(buffer),
                STREAM_BOOL.decode(buffer),
                STREAM_INT.decode(buffer),
                STREAM_BOOL.decode(buffer),
                STREAM_BOOL.decode(buffer));
        }

        @Override
        public void encode(ByteBuf buffer, PrintData value) {
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buffer, value.labelOptional());
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buffer, value.tooltipOptional());
            STREAM_BOOL.encode(buffer, value.isButtonMode());
            STREAM_VAR_INT.encode(buffer, value.redstoneLevel());
            STREAM_BOOL.encode(buffer, value.pressurePlate());
            ScalaStreamCodec.mutableSet(Shape.STREAM_CODEC).encode(buffer, value.stateOff());
            ScalaStreamCodec.mutableSet(Shape.STREAM_CODEC).encode(buffer, value.stateOn());
            STREAM_BOOL.encode(buffer, value.isBeaconBase());
            STREAM_INT.encode(buffer, value.lightLevel());
            STREAM_BOOL.encode(buffer, value.noclipOff());
            STREAM_BOOL.encode(buffer, value.noclipOn());
        }
    };

    private PrintDataSupport() {}

    public static void addInkProvider(Method provider) {
        INK_PROVIDERS.add(provider);
    }

    public static float computeApproximateOpacity(scala.collection.Iterable<PrintShape> shapes) {
        float volume = 1f;
        if (shapes.nonEmpty()) {
            for (int x = 0; x < 16 / STEPPING; x++) {
                for (int y = 0; y < 16 / STEPPING; y++) {
                    for (int z = 0; z < 16 / STEPPING; z++) {
                        AABB bounds = new AABB(x * STEP, y * STEP, z * STEP,
                            (x + 1) * STEP, (y + 1) * STEP, (z + 1) * STEP);
                        boolean intersects = false;
                        Iterator<PrintShape> iterator = shapes.iterator();
                        while (iterator.hasNext()) {
                            if (iterator.next().bounds().intersects(bounds)) {
                                intersects = true;
                                break;
                            }
                        }
                        if (!intersects) volume -= INV_MAX_VOLUME;
                    }
                }
            }
        }
        return volume;
    }

    public static scala.Option<Tuple2<Integer, Integer>> computeCosts(PrintData data) {
        return scala.jdk.javaapi.OptionConverters.toScala(computeCostsOptional(data));
    }

    public static Optional<Tuple2<Integer, Integer>> computeCostsOptional(PrintData data) {
        int totalVolume = total(data.stateOn(), true) + total(data.stateOff(), true);
        int totalSurface = total(data.stateOn(), false) + total(data.stateOff(), false);
        double multiplier = data.noclipOff() || data.noclipOn() ? Settings.get().noclipMultiplier() : 1;
        if (totalVolume <= 0) return Optional.empty();

        int baseMaterialRequired = Math.max(totalVolume / 2, 1);
        int materialRequired = data.redstoneLevel() > 0 && data.redstoneLevel() < 15
            ? baseMaterialRequired + Settings.get().printCustomRedstone()
            : baseMaterialRequired;
        int inkRequired = Math.max(totalSurface / 6, 1);
        return Optional.of(new Tuple2<>((int) (materialRequired * multiplier), inkRequired));
    }

    private static int total(Set<PrintShape> shapes, boolean volume) {
        int result = 0;
        Iterator<PrintShape> iterator = shapes.iterator();
        while (iterator.hasNext()) {
            AABB bounds = iterator.next().bounds();
            result += volume ? ExtendedAABB.volume(bounds) : ExtendedAABB.surface(bounds);
        }
        return result;
    }

    private static final int MATERIAL_PER_ITEM = Settings.get().printMaterialValue();

    public static int materialValue(ItemStack stack) {
        if (Items.get(stack) == Items.get(Constants.ItemName$.MODULE$.Chamelium())) return MATERIAL_PER_ITEM;
        if (Items.get(stack) == Items.get(Constants.BlockName$.MODULE$.Print())) {
            Optional<Tuple2<Integer, Integer>> costs = computeCostsOptional(new PrintData(stack));
            if (costs.isPresent()) return (int) (costs.get()._1() * Settings.get().printRecycleRate());
        }
        return 0;
    }

    public static int inkValue(ItemStack stack) {
        for (Method provider : INK_PROVIDERS) {
            try {
                int value = (Integer) provider.invoke(null, stack);
                if (value > 0) return value;
            } catch (Throwable error) {
                OpenComputers.log().warn("Error invoking callback " +
                    provider.getDeclaringClass().getCanonicalName() + "." + provider.getName() + ".", error);
            }
        }
        return 0;
    }

    public static PrintShape nbtToShape(CompoundTag nbt) {
        byte[] bounds;
        if (nbt.contains("minX")) {
            bounds = new byte[]{nbt.getByte("minX"), nbt.getByte("minY"), nbt.getByte("minZ"),
                nbt.getByte("maxX"), nbt.getByte("maxY"), nbt.getByte("maxZ")};
        } else {
            bounds = nbt.getByteArray("bounds");
        }
        AABB aabb = new AABB(bound(bounds, 0) / 16f, bound(bounds, 1) / 16f, bound(bounds, 2) / 16f,
            bound(bounds, 3) / 16f, bound(bounds, 4) / 16f, bound(bounds, 5) / 16f);
        Optional<Integer> tint = nbt.contains("tint") ? Optional.of(nbt.getInt("tint")) : Optional.empty();
        return new PrintShape(aabb, nbt.getString("texture"), tint);
    }

    private static byte bound(byte[] bounds, int index) {
        return index < bounds.length ? bounds[index] : 0;
    }

    public static CompoundTag shapeToNBT(PrintShape shape) {
        CompoundTag nbt = new CompoundTag();
        AABB bounds = shape.bounds();
        nbt.putByteArray("bounds", new byte[]{
            (byte) Math.round(bounds.minX * 16), (byte) Math.round(bounds.minY * 16), (byte) Math.round(bounds.minZ * 16),
            (byte) Math.round(bounds.maxX * 16), (byte) Math.round(bounds.maxY * 16), (byte) Math.round(bounds.maxZ * 16)
        });
        nbt.putString("texture", shape.texture());
        shape.tintJava().ifPresent(tint -> nbt.putInt("tint", tint));
        return nbt;
    }

    public static final class Shape {
        private Shape() {}

        @SuppressWarnings("unchecked")
        public static final Codec<PrintShape> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ScalaCodec.AABB.fieldOf("bounds").forGetter(PrintShape::bounds),
            Codec.STRING.fieldOf("texture").forGetter(PrintShape::texture),
            Codec.optionalField("tint", INT, false).forGetter(PrintShape::tintJava)
        ).apply(instance, PrintShape::new));

        public static final StreamCodec<ByteBuf, PrintShape> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public PrintShape decode(ByteBuf buffer) {
                return new PrintShape(
                    ScalaStreamCodec.AABB.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.optional(STREAM_INT).decode(buffer));
            }

            @Override
            public void encode(ByteBuf buffer, PrintShape value) {
                ScalaStreamCodec.AABB.encode(buffer, value.bounds());
                ByteBufCodecs.STRING_UTF8.encode(buffer, value.texture());
                ByteBufCodecs.optional(STREAM_INT).encode(buffer, value.tintJava());
            }
        };
    }
}
