package li.cil.oc.data;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.nanomachines.ControllerImplConstants;
import li.cil.oc.common.nanomachines.provider.HungryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

class OCDamageTypeTagsProvider extends TagsProvider<DamageType> {
    public OCDamageTypeTagsProvider(
        PackOutput output,
        CompletableFuture<HolderLookup.Provider> lookupProvider,
        ExistingFileHelper existingFiles
    ) {
        super(output, Registries.DAMAGE_TYPE, lookupProvider, OpenComputers.ID(), existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(DamageTypeTags.BYPASSES_ARMOR).add(HungryProvider.HUNGRY_DAMAGE_KEY, ControllerImplConstants.OverloadDamageKey);
        tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(ControllerImplConstants.OverloadDamageKey);
    }
}
