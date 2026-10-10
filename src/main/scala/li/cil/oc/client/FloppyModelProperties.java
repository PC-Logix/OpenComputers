package li.cil.oc.client;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.datacomponents.OCComponents;
import li.cil.oc.common.init.OCItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "opencomputers", value = Dist.CLIENT)
public final class FloppyModelProperties {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                OCItems.Floppy().get(),
                ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "floppy_color"),
                (stack, level, entity, seed) -> stack.getOrDefault(OCComponents.DISK_COLOR().get(), DyeColor.GRAY).getId()
        ));
    }

    private FloppyModelProperties() {
    }
}
