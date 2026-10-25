package li.cil.oc;

import li.cil.oc.api.Items;
import li.cil.oc.common.init.OCItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreativeTab {
    public static final CreativeTab INSTANCE = new CreativeTab();
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenComputers.ID());
    public static int CURRENT_ROW;
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + OpenComputers.Name()))
                    .icon(() -> Items.get(Constants.BlockName$.MODULE$.CaseTier1()).createItemStack(1))
                    .build());

    private CreativeTab() {}

    @SubscribeEvent
    public void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(OCItems.createChargedHoverBoots());
        }
    }
}
