package li.cil.oc.common.item.data;

import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.common.datacomponents.OCComponents$;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

public class NavigationUpgradeData extends ItemData {
    public ItemStack map = new ItemStack(Items.FILLED_MAP);

    public NavigationUpgradeData() {
        super("navigationupgrade");
    }

    public NavigationUpgradeData(DataComponentHolder holder) {
        this();
        loadData(holder);
    }

    public MapItemSavedData mapData(Level level) throws Exception {
        MapItemSavedData data = MapItem.getSavedData(map, level);
        if (data == null) {
            throw new Exception("invalid map");
        }
        return data;
    }

    public int getSize(Level level) throws Exception {
        return 128 * (1 << mapData(level).scale);
    }

    @Override
    public void loadData(DataComponentHolder holder) {
        Object value = holder.get(OCComponents$.MODULE$.SOURCE_MAP_ITEM().get());
        map = value == null ? null : ((ImmutableItemStack) value).mutableCopy();
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        if (map != null) {
            holder.set(OCComponents$.MODULE$.SOURCE_MAP_ITEM().get(), ImmutableItemStack.copyOf(map));
        }
    }
}
