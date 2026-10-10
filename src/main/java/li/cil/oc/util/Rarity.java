package li.cil.oc.util;

public final class Rarity {
    private static final net.minecraft.world.item.Rarity[] LOOKUP = {
        net.minecraft.world.item.Rarity.COMMON,
        net.minecraft.world.item.Rarity.UNCOMMON,
        net.minecraft.world.item.Rarity.RARE,
        net.minecraft.world.item.Rarity.EPIC
    };

    public static final net.minecraft.world.item.Rarity LEGENDARY = RarityExt.LEGENDARY.getValue();

    private Rarity() {
    }

    public static net.minecraft.world.item.Rarity byTier(int tier) {
        return LOOKUP[Math.max(0, Math.min(tier, LOOKUP.length - 1))];
    }
}
