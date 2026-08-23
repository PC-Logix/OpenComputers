package li.cil.oc.data;

import li.cil.oc.OpenComputers;
import net.minecraft.Util;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.registries.RegistryPatchGenerator;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;

@EventBusSubscriber
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator().getVanillaPack(true);
        var existingFiles = event.getExistingFileHelper();

        var registriesPatch = RegistryPatchGenerator.createLookup(
            event.getLookupProvider(),
            Util.make(new RegistrySetBuilder(), builder -> {
                builder.add(Registries.DAMAGE_TYPE, OCDamageTypeBootstrap::bootstrap);
            })
        );
        var registries = registriesPatch.thenApply(RegistrySetBuilder.PatchedRegistries::full);

        generator.addProvider(o -> new DatapackBuiltinEntriesProvider(o, registriesPatch, Set.of(OpenComputers.ID())));

        var blockTags = generator.addProvider(o -> new OCBlockTagsProvider(o, registries, existingFiles));
        generator.addProvider(o -> new OCItemTagsProvider(o, registries, blockTags.contentsGetter(), existingFiles));
        generator.addProvider(o -> new OCDamageTypeTagsProvider(o, registries, existingFiles));

        generator.addProvider(o -> new OCEEPROMProvider(o, registries, existingFiles));
        generator.addProvider(o -> new OCLootDiskProvider(o, registries, existingFiles));

        generator.addProvider(o -> new OCRecipeProvider(o, registries));
        generator.addProvider(o -> new AdvancementProvider(o, registries, existingFiles, List.of(new OCAdvancementProvider())));

        generator.addProvider(o -> new LootTableProvider(o, Set.of(), List.of(
            new LootTableProvider.SubProviderEntry(OCBlockLoot::new, LootContextParamSets.BLOCK)
        ), registries));

        generator.addProvider(o -> new OCItemModelProvider(o, existingFiles));
        generator.addProvider(o -> new OCBlockStateProvider(o, existingFiles));
    }
}
