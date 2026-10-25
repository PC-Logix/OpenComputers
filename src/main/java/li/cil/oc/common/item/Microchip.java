package li.cil.oc.common.item;

import net.minecraft.world.item.Item;

public final class Microchip extends SimpleItemBase {
    private final int tier;
    private String unlocalizedName;

    public Microchip(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    public int tier() { return tier; }
    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }
}
