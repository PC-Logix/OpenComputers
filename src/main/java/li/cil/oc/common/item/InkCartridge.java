package li.cil.oc.common.item;

import li.cil.oc.common.init.OCItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class InkCartridge extends SimpleItemBase {
    private String unlocalizedName;

    public InkCartridge(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return OCItems.get("inkcartridgeempty").createItemStack(1);
    }
}
