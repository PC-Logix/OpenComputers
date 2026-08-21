
package li.cil.oc.data;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.common.init.OCTags;
import li.cil.oc.common.openprinter.OpenPrinter;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

class OCBlockTagsProvider extends BlockTagsProvider {
    public OCBlockTagsProvider(
        PackOutput output,
        CompletableFuture<HolderLookup.Provider> lookupProvider,
        ExistingFileHelper existingFiles
    ) {
        super(output, lookupProvider, OpenComputers.ID(), existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_AXE).add(
            OpenPrinter.BRIEFCASE.get()
        );

        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
            OpenPrinter.PRINTER.get(),
            OpenPrinter.SHREDDER.get(),
            OpenPrinter.FILE_CABINET.get()
        );

        tag(Tags.Blocks.END_STONES).add(OCBlocks.Endstone().get());

        tag(OCTags.Blocks.FROGLIGHTS).add(Blocks.OCHRE_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.VERDANT_FROGLIGHT);
    }
}
