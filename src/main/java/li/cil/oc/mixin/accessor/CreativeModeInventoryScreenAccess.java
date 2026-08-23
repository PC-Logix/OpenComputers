package li.cil.oc.mixin.accessor;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccess {
    @Shadow
    static CreativeModeTab selectedTab = null;
}