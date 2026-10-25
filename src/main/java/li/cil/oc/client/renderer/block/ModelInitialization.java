package li.cil.oc.client.renderer.block;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.common.init.OCBlocks;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class ModelInitialization {
    public static final ModelInitialization INSTANCE = new ModelInitialization();
    public static final ModelResourceLocation NetSplitterBlockLocation = location(Constants.BlockName$.MODULE$.NetSplitter(), "");
    public static final ModelResourceLocation PrintBlockLocation = location(Constants.BlockName$.MODULE$.Print(), "");
    public static final ModelResourceLocation PrintItemLocation = location(Constants.BlockName$.MODULE$.Print(), "inventory");
    public static final ModelResourceLocation RobotItemLocation = location(Constants.BlockName$.MODULE$.Robot(), "inventory");
    private final Map<ModelResourceLocation, ModelResourceLocation> modelRemappings = new HashMap<>();

    private ModelInitialization() {}

    private static ModelResourceLocation location(String name, String variant) {
        var id = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), name);
        return variant.equals("inventory") ? ModelResourceLocation.inventory(id) : new ModelResourceLocation(id, variant);
    }

    private void rebuildModelRemappings() {
        modelRemappings.clear();
        registerBlockRemapping(OCBlocks.NetSplitter().get(), NetSplitterBlockLocation);
        registerBlockRemapping(OCBlocks.Print().get(), PrintBlockLocation);
    }

    private void registerBlockRemapping(Block block, ModelResourceLocation modelLocation) {
        block.getStateDefinition().getPossibleStates().forEach(state ->
            modelRemappings.put(BlockModelShaper.stateToModelLocation(state), modelLocation));
    }

    @SubscribeEvent
    public void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        rebuildModelRemappings();
        var registry = event.getModels();
        registry.put(NetSplitterBlockLocation, NetSplitterModel.INSTANCE);
        registry.put(PrintBlockLocation, PrintModel.INSTANCE);
        registry.put(PrintItemLocation, PrintModel.INSTANCE);
        registry.put(RobotItemLocation, RobotModel.INSTANCE);

        Map<String, UnaryOperator<BakedModel>> modelOverrides = Map.of(
            Constants.BlockName$.MODULE$.ScreenTier1(), ignored -> ScreenModel.INSTANCE,
            Constants.BlockName$.MODULE$.ScreenTier2(), ignored -> ScreenModel.INSTANCE,
            Constants.BlockName$.MODULE$.ScreenTier3(), ignored -> ScreenModel.INSTANCE,
            Constants.BlockName$.MODULE$.ScreenTier4(), ignored -> ScreenModel.INSTANCE,
            Constants.BlockName$.MODULE$.Rack(), ServerRackModel::new
        );

        for (Object key : registry.keySet().toArray()) {
            if (key instanceof ModelResourceLocation location) {
                String value = location.toString();
                for (var override : modelOverrides.entrySet()) {
                    String prefix = Settings.resourceDomain() + ":" + override.getKey() + "#";
                    if (value.startsWith(prefix)) {
                        registry.put(location, override.getValue().apply(registry.get(location)));
                    }
                }
            }
        }
        modelRemappings.forEach((real, virtual) -> registry.put(real, registry.get(virtual)));
    }
}
