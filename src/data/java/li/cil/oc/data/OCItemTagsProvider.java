package li.cil.oc.data;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.init.OCTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
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
        copy(OCTags.Blocks.FROGLIGHTS, OCTags.Items.FROGLIGHTS);
    }
}
