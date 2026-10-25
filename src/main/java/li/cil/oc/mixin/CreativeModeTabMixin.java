package li.cil.oc.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import li.cil.oc.CreativeTab;
import li.cil.oc.common.init.OCItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin {
    @Shadow private Collection<ItemStack> displayItems = new LinkedList<>();
    @Shadow private Set<ItemStack> displayItemsSearchTab = new LinkedHashSet<>();

    @WrapMethod(method = "buildContents")
    private void openComputers$buildContents(CreativeModeTab.ItemDisplayParameters parameters, Operation<Void> original) {
        CreativeModeTab self = (CreativeModeTab) (Object) this;
        if (self == CreativeTab.MAIN.get()) {
            Collection<ItemStack> sectionDisplayItems = displayItems;
            Set<ItemStack> sectionSearchItems = displayItemsSearchTab;

            original.call(parameters);
            // A memoizing mixin may return without rebuilding these lists. In that case,
            // the fields still contain OC's previously decorated entries.
            boolean rebuilt = displayItems != sectionDisplayItems || displayItemsSearchTab != sectionSearchItems;
            Collection<ItemStack> additionalDisplayItems = rebuilt ? new ArrayList<>(displayItems) : new ArrayList<>();
            Collection<ItemStack> additionalSearchItems = rebuilt ? new ArrayList<>(displayItemsSearchTab) : new ArrayList<>();

            displayItems = sectionDisplayItems;
            displayItemsSearchTab = sectionSearchItems;
            displayItems.clear();
            displayItemsSearchTab.clear();
            OCItems.decorateCreativeTab(displayItems::add, displayItemsSearchTab::add, additionalDisplayItems, additionalSearchItems);
            return;
        }
        original.call(parameters);
    }
}
