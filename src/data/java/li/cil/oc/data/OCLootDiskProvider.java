package li.cil.oc.data;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.data.LootDisk;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.concurrent.CompletableFuture;

public class OCLootDiskProvider extends JsonCodecProvider<LootDisk> {
    public OCLootDiskProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFiles) {
        super(output, PackOutput.Target.DATA_PACK, LootDisk.DIRECTORY, PackType.SERVER_DATA, LootDisk.CODEC, registries, OpenComputers.ID(), existingFiles);
    }

    @Override
    protected void gather() {
        add("audio", new LootDisk("Audio (Utils for Audio Card)", DyeColor.BLUE, 1, true));
        add("builder", new LootDisk("Builder", DyeColor.YELLOW, 1, true));
        add("data", new LootDisk("Data Card Software", DyeColor.PINK, 0, true));
        add("dig", new LootDisk("Digger", DyeColor.BROWN, 2, true));
        add("generator", new LootDisk("Generator Upgrade Software", DyeColor.PURPLE, 0, true));
        add("irc", new LootDisk("OpenIRC (IRC Client)", DyeColor.LIGHT_BLUE, 1, true));
        add("maze", new LootDisk("Mazer", DyeColor.ORANGE, 1, true));
        add("network", new LootDisk("Network (Network Stack)", DyeColor.LIME, 1, true));
        add("openloader", new LootDisk("OpenLoader (Boot Loader)", DyeColor.MAGENTA, 1, true));
        add("openos", new LootDisk("OpenOS (Operating System)", DyeColor.GREEN, 0, true));
        add("oppm", new LootDisk("OPPM (Package Manager)", DyeColor.CYAN, 0, true));
        add("plan9k", new LootDisk("Plan9k (Operating System)", DyeColor.RED, 1, true));
    }

    private void add(String id, LootDisk disk) {
        unconditional(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), id), disk);
    }
}
