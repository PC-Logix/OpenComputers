package li.cil.oc.data;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.block.property.PropertyCableConnection;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.block.property.PropertyRunning;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.common.openprinter.OpenPrinter;
import li.cil.oc.common.openprinter.block.DeviceBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class OCBlockStateProvider extends BlockStateProvider {
    public static final ResourceLocation GENERIC_TOP = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "block/generic_top");
    public static final ResourceLocation BLOCK_SIDE = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "block/block_side");

    public OCBlockStateProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, OpenComputers.ID(), existingFiles);
    }

    @Override
    protected void registerStatesAndModels() {
        // Block Entity Renderers
        simpleBlock(OCBlocks.NetSplitter().get(), existingModel(Blocks.AIR));
        simpleBlock(OCBlocks.Print().get(), existingModel(Blocks.AIR));
        simpleBlock(OCBlocks.Robot().get(), existingModel(Blocks.AIR));
        simpleBlock(OCBlocks.RobotAfterimage().get(), existingModel(Blocks.AIR));

        simpleBlockWithItem(OCBlocks.Adapter().get(), models().cubeTop(modelName(OCBlocks.Adapter().get()),
            textureName(OCBlocks.Adapter().get(), "side"),
            textureName(OCBlocks.Adapter().get(), "top")
        ));

        simpleBlockWithItem(OCBlocks.Assembler().get(), existingModel(OCBlocks.Assembler().get()));

        simpleBlockWithItem(OCBlocks.Capacitor().get(), cubeGenericBottomTop(OCBlocks.Capacitor().get()));

        simpleBlockWithItem(OCBlocks.CarpetedCapacitor().get(), models().cubeBottomTop(modelName(OCBlocks.CarpetedCapacitor().get()),
            textureName(OCBlocks.Capacitor().get(), "side"),
            GENERIC_TOP,
            ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "block/carpeted_capacitor_top")
        ));

        caseBlock(OCBlocks.CaseTier1().get());
        caseBlock(OCBlocks.CaseTier2().get());
        caseBlock(OCBlocks.CaseTier3().get());
        caseBlock(OCBlocks.CaseTier4().get());
        caseBlock(OCBlocks.CaseCreative().get());

        simpleBlockWithItem(OCBlocks.ChameliumBlock().get(), existingModel(OCBlocks.ChameliumBlock().get()));

        horizontalBlockGenericTop(OCBlocks.Charger().get());
        itemModels().simpleBlockItem(OCBlocks.Charger().get());

        simpleBlockWithItem(OCBlocks.Disassembler().get(), cubeGenericBottomTop(OCBlocks.Disassembler().get()));

        horizontalBlockGenericTop(OCBlocks.DiskDrive().get());
        itemModels().simpleBlockItem(OCBlocks.DiskDrive().get());

        simpleBlockWithItem(OCBlocks.Geolyzer().get(), cubeGenericBottomTop(OCBlocks.Geolyzer().get()));

        simpleBlockWithItem(OCBlocks.HologramTier1().get(), existingModel(OCBlocks.HologramTier1().get()));
        simpleBlockWithItem(OCBlocks.HologramTier2().get(), existingModel(OCBlocks.HologramTier2().get()));
        simpleBlockWithItem(OCBlocks.HologramTier3().get(), existingModel(OCBlocks.HologramTier3().get()));

        holoScreenBlock(OCBlocks.HoloScreenTier1().get());
        holoScreenBlock(OCBlocks.HoloScreenTier2().get());
        holoScreenBlock(OCBlocks.HoloScreenTier3().get());
        holoScreenBlock(OCBlocks.HoloScreenTier4().get());

        keyboardBlock();

        simpleBlockWithItem(OCBlocks.MotionSensor().get(), models().cubeColumn(modelName(OCBlocks.MotionSensor().get()),
            textureName(OCBlocks.MotionSensor().get(), "side"),
            textureName(OCBlocks.MotionSensor().get(), "top")
        ));

        simpleBlockWithItem(OCBlocks.PowerConverter().get(), models().cubeColumn(modelName(OCBlocks.PowerConverter().get()),
            textureName(OCBlocks.PowerConverter().get(), "side"),
            GENERIC_TOP
        ));

        simpleBlockWithItem(OCBlocks.PowerDistributor().get(), cubeGenericBottomTop(OCBlocks.PowerDistributor().get()));

        simpleBlockWithItem(OCBlocks.Printer().get(), existingModel(OCBlocks.Printer().get()));

        horizontalBlockGenericTop(OCBlocks.Raid().get());
        itemModels().simpleBlockItem(OCBlocks.Raid().get());

        simpleBlockWithItem(OCBlocks.Redstone().get(), models().cube(modelName(OCBlocks.Redstone().get()),
            textureName(OCBlocks.Redstone().get(), "bottom"),
            textureName(OCBlocks.Redstone().get(), "top"),
            textureName(OCBlocks.Redstone().get(), "north"),
            textureName(OCBlocks.Redstone().get(), "south"),
            textureName(OCBlocks.Redstone().get(), "east"),
            textureName(OCBlocks.Redstone().get(), "west")
        ).texture("particle", textureName(OCBlocks.Redstone().get(), "top")));

        simpleBlockWithItem(OCBlocks.Relay().get(), models().cubeBottomTop(modelName(OCBlocks.Relay().get()),
            ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "block/switch_side"),
            GENERIC_TOP,
            ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "block/switch_top"))
        );

        screenBlock(OCBlocks.ScreenTier1().get());
        screenBlock(OCBlocks.ScreenTier2().get());
        screenBlock(OCBlocks.ScreenTier3().get());
        screenBlock(OCBlocks.ScreenTier4().get());

        flatScreenBlock(OCBlocks.FlatScreenBackTier1().get());
        flatScreenBlock(OCBlocks.FlatScreenBackTier2().get());
        flatScreenBlock(OCBlocks.FlatScreenBackTier3().get());
        flatScreenBlock(OCBlocks.FlatScreenBackTier4().get());

        flatScreenBlock(OCBlocks.FlatScreenFrontTier1().get());
        flatScreenBlock(OCBlocks.FlatScreenFrontTier2().get());
        flatScreenBlock(OCBlocks.FlatScreenFrontTier3().get());
        flatScreenBlock(OCBlocks.FlatScreenFrontTier4().get());

        horizontalBlockGenericTop(OCBlocks.Rack().get());
        itemModels().simpleBlockItem(OCBlocks.Rack().get());

        waypointBlock();

        horizontalBlock(OCBlocks.Microcontroller().get());
        itemModels().simpleBlockItem(OCBlocks.Microcontroller().get());

        simpleBlock(OCBlocks.Endstone().get(), existingModel(Blocks.END_STONE));

        simpleBlockWithItem(OCBlocks.Transposer().get(), existingModel(OCBlocks.Transposer().get()));

        // Open Printers
        horizontalBlock(OpenPrinter.FILE_CABINET.get(), BLOCK_SIDE, ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "block/filingcabinet_front"), BLOCK_SIDE);
        itemModels().simpleBlockItem(OpenPrinter.FILE_CABINET.get());

        horizontalBlock(OpenPrinter.SHREDDER.get(), BLOCK_SIDE, textureName(OpenPrinter.SHREDDER.get(), "front"), BLOCK_SIDE);
        itemModels().simpleBlockItem(OpenPrinter.SHREDDER.get());

        horizontalBlock(OpenPrinter.BRIEFCASE.get(), existingModel(OpenPrinter.BRIEFCASE.get()));
        itemModels().simpleBlockItem(OpenPrinter.BRIEFCASE.get());

        printerBlock();
        itemModels().simpleBlockItem(OpenPrinter.PRINTER.get());

        cableBlock();
    }

    private void caseBlock(Block block) {
        var model = models().getExistingFile(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "case"));
        var runningModel = models().getExistingFile(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "case_running"));
        horizontalBlock(block, s -> s.getValue(PropertyRunning.Running()) ? runningModel : model);
        simpleBlockItem(block, model);
    }

    private void screenBlock(Block block) {
        var model = models().getExistingFile(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "screen"));
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
            .modelFile(model)
            .rotationY(getInvYRotation(state.getValue(PropertyRotatable.Yaw())))
            .rotationX(getXRotation(state.getValue(PropertyRotatable.Pitch())))
            .build()
        );
        simpleBlockItem(block, model);
    }

    private void flatScreenBlock(Block block) {
        var model = existingModel(block);
        getVariantBuilder(block).forAllStates(state -> {
                var pitch = state.getValue(PropertyRotatable.Pitch());
                var yaw = state.getValue(PropertyRotatable.Yaw());
                return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY(pitch == Direction.DOWN ? getInvYRotation(yaw) : getYRotation(yaw))
                    .rotationX(-getXRotation(pitch))
                    .build();
            }
        );
        simpleBlockItem(block, model);
    }

    private void holoScreenBlock(Block block) {
        var model = existingModel(block);
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
            .modelFile(model)
            .rotationY(getYRotation(state.getValue(PropertyRotatable.Facing())))
            .rotationX(state.getValue(PropertyRotatable.Mount()) == Direction.DOWN ? 180 : 0)
            .build()
        );
        simpleBlockItem(block, model);
    }

    private void keyboardBlock() {
        var block = OCBlocks.Keyboard().get();
        var model = existingModel(block);
        getVariantBuilder(block).forAllStates(state -> {
            var yaw = state.getValue(PropertyRotatable.Yaw());
            var pitch = state.getValue(PropertyRotatable.Pitch());
            return ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(getYRotation(yaw, pitch == Direction.NORTH ? 180 : 0))
                .rotationX(-getXRotation(pitch))
                .build();
        });
        simpleBlockItem(block, model);
    }

    private void waypointBlock() {
        var block = OCBlocks.Waypoint().get();
        var model = models().orientableWithBottom(
            modelName(block), textureName(block, "side"), textureName(block, "front"), GENERIC_TOP, textureName(block, "top")
        );
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
            .modelFile(model)
            .rotationY(getYRotation(state.getValue(PropertyRotatable.Yaw())))
            .rotationX(-getXRotation(state.getValue(PropertyRotatable.Pitch())))
            .build());

        simpleBlockItem(block, model);
    }

    private void cableBlock() {
        var core = models().getExistingFile(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "block/cable_core"));
        var cableArm = models().getExistingFile(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "block/cable_arm_cable"));
        var deviceArm = models().getExistingFile(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "block/cable_arm_device"));
        var disconnected = models().getExistingFile(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "block/cable_disconnected"));

        var builder = getMultipartBuilder(OCBlocks.Cable().get());

        // The core model is always present
        builder.part().modelFile(core).addModel();

        // Add the arms by default
        for (var direction : Direction.values()) {
            var property = PropertyCableConnection.BY_DIRECTION.get(direction);
            var rotationX = -getXRotation(direction);
            var rotationY = getYRotation(direction);
            builder.part()
                .modelFile(cableArm).rotationX(rotationX).rotationY(rotationY).addModel()
                .condition(property, PropertyCableConnection.Shape.CABLE);

            builder.part()
                .modelFile(deviceArm).rotationX(rotationX).rotationY(rotationY).addModel()
                .condition(property, PropertyCableConnection.Shape.DEVICE);

            // Show the "disconnected" model if either *just* the opposite arm is connected, or none are.
            var disconnectedBuilder = builder.part().modelFile(disconnected).rotationX(rotationX).rotationY(rotationY).addModel().useOr();
            var noArms = disconnectedBuilder.nestedGroup();
            var oneArm = disconnectedBuilder.nestedGroup();
            for (var otherDir : Direction.values()) {
                var otherProp = PropertyCableConnection.BY_DIRECTION.get(otherDir);
                noArms.condition(otherProp, PropertyCableConnection.Shape.NONE);
                if (otherDir == direction.getOpposite()) {
                    oneArm.condition(otherProp, PropertyCableConnection.Shape.CABLE, PropertyCableConnection.Shape.DEVICE);
                } else {
                    oneArm.condition(otherProp, PropertyCableConnection.Shape.NONE);
                }
            }
        }
    }

    private String modelName(Block block) {
        return "block/" + BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    private ResourceLocation textureName(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).withPrefix("block/");
    }

    private ResourceLocation textureName(Block block, String variant) {
        return textureName(block).withSuffix("_" + variant);
    }

    private ModelFile existingModel(Block block) {
        return models().getExistingFile(BuiltInRegistries.BLOCK.getKey(block).withPrefix("block/"));
    }

    private ModelFile cubeGenericBottomTop(Block block) {
        return models().cubeBottomTop(modelName(block), textureName(block, "side"), GENERIC_TOP, textureName(block, "top"));
    }

    private void horizontalBlock(Block block) {
        horizontalBlock(block, models().orientable(
            modelName(block), textureName(block, "side"), textureName(block, "front"), textureName(block, "top"))
        );
    }

    private void horizontalBlockGenericTop(Block block) {
        horizontalBlock(block, models().orientable(
            modelName(block), textureName(block, "side"), textureName(block, "front"), GENERIC_TOP)
        );
    }

    private void printerBlock() {
        var block = OpenPrinter.PRINTER.get();
        var model = existingModel(block);
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
            .modelFile(model)
            .rotationY(getYRotation(state.getValue(DeviceBlock.FACING), 0))
            .build()
        );
    }

    /**
     * Get the Y rotation of a direction.
     *
     * @param direction The direction.
     * @return The Y rotation.
     */
    private static int getYRotation(Direction direction) {
        return getYRotation(direction, 180);
    }

    private static int getYRotation(Direction direction, int offset) {
        return ((int) direction.toYRot() + offset) % 360;
    }

    /**
     * Get the inverse Y rotation of a direction. This is similar to {@link #getYRotation(Direction)}, but with
     * {@link Direction#EAST} and {@link Direction#WEST} swapped.
     *
     * @param direction The direction.
     * @return The Y rotation.
     */
    private static int getInvYRotation(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case SOUTH -> 180;
            case EAST -> 270;
            case WEST -> 90;
            default -> throw new UnsupportedOperationException("Expected a horizontal direction");
        };
    }

    private static int getXRotation(Direction direction) {
        return switch (direction) {
            case UP -> 90;
            case DOWN -> -90;
            default -> 0;
        };
    }

}
