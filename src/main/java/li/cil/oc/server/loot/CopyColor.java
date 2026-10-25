package li.cil.oc.server.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc.api.internal.Colored;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CopyColor extends LootItemConditionalFunction {
    public static final MapCodec<CopyColor> CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance).apply(instance, CopyColor::new)
    );

    private CopyColor(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<CopyColor> getType() {
        return LootFunctions.COPY_COLOR.get();
    }

    public static LootItemConditionalFunction.Builder<?> copyColor() {
        return simpleBuilder(CopyColor::new);
    }

    @Override
    public @NotNull ItemStack run(ItemStack stack, @NotNull LootContext ctx) {
        if (stack.isEmpty()) return stack;

        BlockEntity be = ctx.getParamOrNull(LootContextParams.BLOCK_ENTITY);

        if (be instanceof Colored colored) {
            int color = colored.getColor();
            if (color == 0xABABAB) {
                ItemColorizer.removeColor(stack);
            } else {
                ItemColorizer.setColor(stack, color);
            }
        } else {
            ItemColorizer.removeColor(stack);
        }
        return stack;
    }
}
