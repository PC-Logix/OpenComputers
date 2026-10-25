package li.cil.oc.common;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public final class Advancement {
    private Advancement() {}

    public static void onAssemble(ItemStack stack, Player player) {
        award(stack, player, Advancements::getAssemblingAdvancement);
    }

    public static void onCraft(ItemStack stack, Player player) {
        award(stack, player, Advancements::getCraftingAdvancement);
    }

    private static void award(ItemStack stack, Player player, Function<ItemStack, ResourceLocation> getAdvancement) {
        if (player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {
            ResourceLocation location = getAdvancement.apply(stack);
            if (location != null) award(serverPlayer, location);
        }
    }

    private static void award(ServerPlayer player, ResourceLocation location) {
        AdvancementHolder advancement = player.getServer().getAdvancements().get(location);
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : advancement.value().criteria().keySet()) {
            if (!progress.isDone()) player.getAdvancements().award(advancement, criterion);
        }
    }
}
