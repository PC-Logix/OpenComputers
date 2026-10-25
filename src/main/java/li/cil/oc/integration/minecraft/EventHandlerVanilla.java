package li.cil.oc.integration.minecraft;

import li.cil.oc.Settings;
import li.cil.oc.api.event.GeolyzerEvent;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.bus.api.SubscribeEvent;
import scala.collection.immutable.Map$;

public final class EventHandlerVanilla {
    public static final EventHandlerVanilla INSTANCE = new EventHandlerVanilla();

    private EventHandlerVanilla() {}

    @SubscribeEvent
    public void onGeolyzerScan(GeolyzerEvent.Scan event) {
        var world = event.host.getEnvironmentLevel();
        var origin = BlockPos.containing(event.host.xPosition(), event.host.yPosition(), event.host.zPosition());
        boolean includeReplaceable = !(event.options.get("includeReplaceable") instanceof Boolean value) || value;

        for (int i = 0; i < event.data.length; i++) {
            byte noise = (byte) world.getRandom().nextInt(256);
            event.data[i] = noise / 128f / 33f;
        }

        int width = event.maxX - event.minX + 1;
        int depth = event.maxZ - event.minZ + 1;
        for (int ry = event.minY; ry <= event.maxY; ry++) {
            for (int rz = event.minZ; rz <= event.maxZ; rz++) {
                for (int rx = event.minX; rx <= event.maxX; rx++) {
                    var pos = origin.offset(rx, ry, rz);
                    int index = rx - event.minX + (rz - event.minZ + (ry - event.minY) * depth) * width;
                    if (world.isLoaded(pos) && !world.isEmptyBlock(pos)) {
                        var state = world.getBlockState(pos);
                        boolean fluid = state.getBlock() instanceof LiquidBlock;
                        if (!state.isAir() && (includeReplaceable || fluid || !state.is(BlockTags.REPLACEABLE))) {
                            float distance = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
                            event.data[index] = event.data[index] * distance * Settings.get().geolyzerNoise()
                                    + state.getDestroySpeed(world, pos);
                        } else {
                            event.data[index] = 0;
                        }
                    } else {
                        event.data[index] = 0;
                    }
                }
            }
        }
    }

    private static Float getGrowth(BlockState state) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age")) {
                int max = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
                float growth = state.getValue(age).floatValue() / max;
                return Math.max(0, Math.min(1, growth));
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onGeolyzerAnalyze(GeolyzerEvent.Analyze event) {
        var world = event.host.getEnvironmentLevel();
        var state = world.getBlockState(event.pos);
        var block = state.getBlock();

        event.data.put("name", BuiltInRegistries.BLOCK.getKey(block).toString());
        event.data.put("hardness", state.getDestroySpeed(world, event.pos));
        event.data.put("harvestLevel", ItemUtils.getHarvestLevel(state));
        event.data.put("harvestTool", ItemUtils.getHarvestTool(state));
        event.data.put("color", state.getMapColor(world, event.pos).col);
        event.data.put("metadata", 0);

        scala.collection.immutable.Map<String, Object> properties = Map$.MODULE$.empty();
        for (var property : state.getProperties()) {
            properties = properties.updated(property.getName(), state.getValue(property));
        }
        event.data.put("properties", properties);

        if (Settings.get().insertIdsInConverters()) event.data.put("id", Block.getId(state));

        Float growth = null;
        if (block instanceof CropBlock || block instanceof StemBlock || block == Blocks.COCOA
                || block == Blocks.NETHER_WART || block == Blocks.CHORUS_FLOWER) {
            growth = getGrowth(state);
        } else if (block == Blocks.MELON || block == Blocks.PUMPKIN || block == Blocks.CACTUS
                || block == Blocks.SUGAR_CANE || block == Blocks.CHORUS_PLANT) {
            growth = 1f;
        }
        if (growth != null) event.data.put("growth", growth);
    }
}
