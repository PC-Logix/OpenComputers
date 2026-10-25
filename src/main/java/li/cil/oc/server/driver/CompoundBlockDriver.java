package li.cil.oc.server.driver;

import com.google.common.base.Strings;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import scala.Tuple2;
import scala.collection.immutable.List$;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class CompoundBlockDriver implements DriverBlock {
    public final DriverBlock[] sidedBlocks;

    public CompoundBlockDriver(DriverBlock[] sidedBlocks) {
        this.sidedBlocks = sidedBlocks;
    }

    public DriverBlock[] sidedBlocks() {
        return sidedBlocks;
    }

    @Override
    public CompoundBlockEnvironment createEnvironment(Level level, BlockPos pos, Direction side) {
        List<Tuple2<String, ManagedEnvironment>> entries = new ArrayList<>();
        List<ManagedEnvironment> environments = new ArrayList<>();
        for (DriverBlock driver : sidedBlocks) {
            ManagedEnvironment environment = driver.createEnvironment(level, pos, side);
            if (environment != null) {
                entries.add(new Tuple2<>(driver.getClass().getName(), environment));
                environments.add(environment);
            }
        }
        if (entries.isEmpty()) return null;
        return new CompoundBlockEnvironment(cleanName(tryGetName(level, pos, environments)),
            List$.MODULE$.from(CollectionConverters.asScala(entries)));
    }

    @Override
    public boolean worksWith(Level level, BlockPos pos, Direction side) {
        for (DriverBlock driver : sidedBlocks) {
            if (!driver.worksWith(level, pos, side)) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CompoundBlockDriver driver) || driver.sidedBlocks.length != sidedBlocks.length) return false;
        List<DriverBlock> unmatched = new ArrayList<>(Arrays.asList(driver.sidedBlocks));
        for (DriverBlock sidedBlock : sidedBlocks) {
            if (!unmatched.remove(sidedBlock)) return false;
        }
        return true;
    }

    private String tryGetName(Level level, BlockPos pos, List<ManagedEnvironment> environments) {
        NamedBlock named = null;
        for (ManagedEnvironment environment : environments) {
            if (environment instanceof NamedBlock candidate &&
                (named == null || candidate.priority() >= named.priority())) named = candidate;
        }
        if (named != null) return named.preferredName();

        try {
            Block block = level.getBlockState(pos).getBlock();
            if (block.asItem() != null) {
                String description = new ItemStack(block, 1).getDescriptionId();
                return description.startsWith("tile.") ? description.substring("tile.".length()) : description;
            }
        } catch (Throwable ignored) {
        }
        try {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString();
        } catch (Throwable ignored) {
        }
        return "component";
    }

    private String cleanName(String name) {
        String safeStart = name.matches("^[^a-zA-Z_]") ? "_" + name : name;
        String identifier = safeStart.replaceAll("[^\\w_]", "_").trim();
        return Strings.isNullOrEmpty(identifier) ? "component" : identifier.toLowerCase(Locale.getDefault());
    }
}
