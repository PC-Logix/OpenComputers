package li.cil.oc.common.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class OCTags {
    private OCTags() {
    }

    public final class Blocks {
        private Blocks() {
        }

        /**
         * Conventional tag for all froglights.
         */
        public static final TagKey<Block> FROGLIGHTS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "froglights"));
    }

    public final class Items {
        private Items() {
        }

        /**
         * Conventional tag for all froglights.
         */
        public static final TagKey<Item> FROGLIGHTS = TagKey.create(Registries.ITEM, Blocks.FROGLIGHTS.location());
    }
}
