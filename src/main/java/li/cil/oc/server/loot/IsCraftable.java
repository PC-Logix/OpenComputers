package li.cil.oc.server.loot;

import com.mojang.serialization.MapCodec;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/**
 * Ensure this item is craftable.
 *
 * @param item The item to check.
 */
public record IsCraftable(Item item) implements LootItemCondition {
    public static final MapCodec<IsCraftable> CODEC = BuiltInRegistries.ITEM.byNameCodec()
        .fieldOf("item")
        .xmap(IsCraftable::new, IsCraftable::item);

    public static final LootItemConditionType TYPE = new LootItemConditionType(CODEC);

    @Override
    public LootItemConditionType getType() {
        return TYPE;
    }

    @Override
    public boolean test(LootContext lootContext) {
        return ItemUtils.getIngredients(lootContext.getLevel().getRecipeManager(), new ItemStack(item)).length != 0;
    }

    public static LootItemCondition.Builder builder(Item item) {
        return () -> new IsCraftable(item);
    }
}
