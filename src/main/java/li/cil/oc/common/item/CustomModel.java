package li.cil.oc.common.item;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ModelEvent;

public interface CustomModel {
    @OnlyIn(Dist.CLIENT)
    ModelResourceLocation getModelLocation(ItemStack stack);

    @OnlyIn(Dist.CLIENT)
    default void registerModelLocations() {
    }

    @OnlyIn(Dist.CLIENT)
    default void bakeModels(ModelEvent.RegisterAdditional event) {
    }
}
