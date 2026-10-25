package li.cil.oc.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.DyeColor;

/**
 * A loot disk definition loaded from a datapack.
 * <p>
 * This is used to populate the list of loot disks in the creative tab and recipe mods.
 *
 * @param label         The display name of the loot disk.
 * @param color         The color of the disk.
 * @param weight        The relative weight this loot disk appears in loot tables.
 * @param recipeCycling Whether this disk is included in the loot-disk cycling recipe.
 */
public record LootDisk(String label, DyeColor color, int weight, boolean recipeCycling) {
    public static final String DIRECTORY = "opencomputers/loot_disks";

    public static final MapCodec<LootDisk> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("label").forGetter(LootDisk::label),
        DyeColor.CODEC.optionalFieldOf("color", DyeColor.LIGHT_GRAY).forGetter(LootDisk::color),
        Codec.INT.optionalFieldOf("weight", 1).forGetter(LootDisk::weight),
        Codec.BOOL.optionalFieldOf("recipe_cycling", true).forGetter(LootDisk::recipeCycling)
    ).apply(instance, LootDisk::new));

    public static final Codec<LootDisk> CODEC = MAP_CODEC.codec();
}
