package li.cil.oc.integration.multipart;

import codechicken.multipart.api.MultipartType;
import codechicken.multipart.api.PartConverter;
import codechicken.multipart.api.SimpleMultipartType;
import codechicken.multipart.api.part.MultiPart;
import codechicken.multipart.block.TileMultipart;
import codechicken.multipart.util.MultipartPlaceContext;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.common.blockentity.Cable;
import li.cil.oc.common.blockentity.Print;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.util.ItemColorizer;
import li.cil.oc.util.SableCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class MultipartIntegration {
    private static final DeferredRegister<MultipartType<?>> MULTIPART_TYPES =
            DeferredRegister.create(MultipartType.MULTIPART_TYPES, OpenComputers.ID());
    private static final DeferredRegister<PartConverter> PART_CONVERTERS =
            DeferredRegister.create(PartConverter.PART_CONVERTERS, OpenComputers.ID());

    private static final DeferredHolder<MultipartType<?>, MultipartType<MultipartCablePart>> CABLE_TYPE =
            MULTIPART_TYPES.register("cable", () -> new SimpleMultipartType<>(ignored ->
                    new MultipartCablePart(OCBlocks.Cable().get().defaultBlockState(), Optional.empty())));
    private static final DeferredHolder<MultipartType<?>, MultipartType<MultipartAudioCablePart>> AUDIO_CABLE_TYPE =
            MULTIPART_TYPES.register("audio_cable", () -> new SimpleMultipartType<>(ignored ->
                    new MultipartAudioCablePart(OCBlocks.AudioCable().get().defaultBlockState())));
    private static final DeferredHolder<MultipartType<?>, MultipartType<MultipartPrintPart>> PRINT_TYPE =
            MULTIPART_TYPES.register("print", () -> new SimpleMultipartType<>(ignored ->
                    new MultipartPrintPart(OCBlocks.Print().get().defaultBlockState(), Optional.empty())));

    static {
        PART_CONVERTERS.register("cable", () -> new PartConverter() {
            @Override
            public ConversionResult<Collection<MultiPart>> convert(LevelAccessor level, BlockPos pos, BlockState state) {
                if (state.getBlock() != OCBlocks.Cable().get()) return PartConverter.emptyResultList();
                if (level.getBlockEntity(pos) instanceof Cable cable) {
                    return ConversionResult.success(Collections.singletonList(
                            new MultipartCablePart(state, Optional.of(cable))));
                }
                return ConversionResult.success(Collections.singletonList(
                        new MultipartCablePart(state, Optional.empty())));
            }

            @Override
            public ConversionResult<MultiPart> convert(MultipartPlaceContext context) {
                final ItemStack stack = context.getItemInHand();
                if (stack.getItem() != OCBlocks.Cable().get().asItem()) return PartConverter.emptyResult();
                final BlockState state = OCBlocks.Cable().get().getStateForPlacement(context);
                if (state == null) return PartConverter.emptyResult();

                final MultipartCablePart part = new MultipartCablePart(state, Optional.empty());
                if (ItemColorizer.hasColor(stack)) part.copyColor(ItemColorizer.getColor(stack));
                return ConversionResult.success(part);
            }
        });

        PART_CONVERTERS.register("audio_cable", () -> new PartConverter() {
            @Override
            public ConversionResult<Collection<MultiPart>> convert(LevelAccessor level, BlockPos pos, BlockState state) {
                if (state.getBlock() != OCBlocks.AudioCable().get()) return PartConverter.emptyResultList();
                return ConversionResult.success(Collections.singletonList(
                        new MultipartAudioCablePart(state)));
            }

            @Override
            public ConversionResult<MultiPart> convert(MultipartPlaceContext context) {
                final ItemStack stack = context.getItemInHand();
                if (stack.getItem() != OCBlocks.AudioCable().get().asItem()) return PartConverter.emptyResult();
                final BlockState state = OCBlocks.AudioCable().get().getStateForPlacement(context);
                if (state == null) return PartConverter.emptyResult();
                return ConversionResult.success(new MultipartAudioCablePart(state));
            }
        });

        PART_CONVERTERS.register("print", () -> new PartConverter() {
            @Override
            public ConversionResult<Collection<MultiPart>> convert(LevelAccessor level, BlockPos pos, BlockState state) {
                if (state.getBlock() != OCBlocks.Print().get()) return PartConverter.emptyResultList();
                if (level.getBlockEntity(pos) instanceof Print print) {
                    return ConversionResult.success(Collections.singletonList(
                            new MultipartPrintPart(state, Optional.of(print))));
                }
                return PartConverter.emptyResultList();
            }

            @Override
            public ConversionResult<MultiPart> convert(MultipartPlaceContext context) {
                final ItemStack stack = context.getItemInHand();
                if (stack.getItem() != OCBlocks.Print().get().asItem()) return PartConverter.emptyResult();
                final BlockState state = OCBlocks.Print().get().getStateForPlacement(context);
                final PrintData data = new PrintData(stack);
                if (state == null || !withinPrintLimit(context.getLevel(), context.getClickedPos(), data)) {
                    return PartConverter.emptyResult();
                }

                final MultipartPrintPart part = new MultipartPrintPart(state, Optional.empty());
                part.data_$eq(data);
                final var player = context.getPlayer();
                if (player != null) {
                    final Vec3 clickPos = Vec3.atCenterOf(context.getClickedPos());
                    final Vec3 forward = Vec3.directionFromRotation(player.getXRot(), player.getYRot()).reverse();
                    final Vec3 side = Vec3.directionFromRotation(0, player.getYRot() + 90).reverse();
                    final double yaw = SableCompat.localHeading(context.getLevel(), clickPos, forward, side);
                    final double pitch = SableCompat.localPitch(context.getLevel(), clickPos, forward);
                    final Print orientation = new Print(context.getClickedPos(), state);
                    orientation.setFromPitchAndYaw((float) pitch, (float) yaw);
                    orientation.invertRotation();
                    part.facing_$eq(orientation.facing());
                }
                return ConversionResult.success(part);
            }
        });
    }

    public static void register(IEventBus bus) {
        MULTIPART_TYPES.register(bus);
        PART_CONVERTERS.register(bus);
    }

    public static MultipartType<MultipartCablePart> cableMultipartType() {
        return CABLE_TYPE.get();
    }

    public static MultipartType<MultipartAudioCablePart> audioCableMultipartType() {
        return AUDIO_CABLE_TYPE.get();
    }

    public static MultipartType<MultipartPrintPart> printMultipartType() {
        return PRINT_TYPE.get();
    }

    private static boolean withinPrintLimit(LevelAccessor level, BlockPos pos, PrintData incoming) {
        final List<PrintData> others = new ArrayList<>();
        if (level.getBlockEntity(pos) instanceof TileMultipart tile) {
            for (MultiPart part : tile.getPartList()) {
                if (part instanceof MultipartPrintPart print) others.add(print.data());
            }
        } else if (level.getBlockEntity(pos) instanceof Print print) {
            others.add(print.data());
        }

        int offCount = incoming.stateOff().size();
        int onCount = incoming.stateOn().size();
        for (PrintData other : others) {
            offCount += other.stateOff().size();
            onCount += other.stateOn().size();
        }
        return offCount <= Settings.get().maxPrintComplexity() && onCount <= Settings.get().maxPrintComplexity();
    }

    private MultipartIntegration() {
    }
}
