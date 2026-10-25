package li.cil.oc.data;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.init.OCItemsHardware;
import li.cil.oc.common.init.OCItems;
import li.cil.oc.common.recipe.ExtendedRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

class OCItemTagsProvider extends ItemTagsProvider {
    public OCItemTagsProvider(
        PackOutput output,
        CompletableFuture<HolderLookup.Provider> lookupProvider,
        CompletableFuture<TagLookup<Block>> blockTags,
        ExistingFileHelper existingFiles
    ) {
        super(output, lookupProvider, blockTags, OpenComputers.ID(), existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        copy(Tags.Blocks.END_STONES, Tags.Items.END_STONES);

        tag(ExtendedRecipe.beaconBlocks).add(
            Items.NETHERITE_BLOCK,
            Items.EMERALD_BLOCK,
            Items.DIAMOND_BLOCK,
            Items.GOLD_BLOCK,
            Items.IRON_BLOCK
        );

        tag(ItemTags.create(ResourceLocation.withDefaultNamespace("bookshelf_books")))
            .add(OCItemsHardware.Manual.get());
    }
}
