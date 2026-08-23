package li.cil.oc.data;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.data.EEPROM;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class OCEEPROMProvider extends JsonCodecProvider<EEPROM> {
    public OCEEPROMProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFiles) {
        super(output, PackOutput.Target.DATA_PACK, EEPROM.DIRECTORY, PackType.SERVER_DATA, EEPROM.CODEC, registries, OpenComputers.ID(), existingFiles);
    }

    @Override
    protected void gather() {
        add("luabios", new EEPROM("EEPROM (Lua BIOS)", Optional.of("bios.lua"), Optional.empty(), false));
    }

    private void add(String id, EEPROM disk) {
        unconditional(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), id), disk);
    }
}
