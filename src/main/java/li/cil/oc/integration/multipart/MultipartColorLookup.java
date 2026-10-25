package li.cil.oc.integration.multipart;

import codechicken.multipart.api.NormalOcclusionTest;
import codechicken.multipart.api.part.MultiPart;
import codechicken.multipart.block.TileMultipart;
import li.cil.oc.common.block.Cable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Predicate;
import java.util.Optional;

/** Loaded only when the optional CB Multipart mod is installed. */
final class MultipartColorLookup {
    private MultipartColorLookup() {}

    /** True when an OC cable arm can pass through this multipart cell on the given side. */
    static boolean canConnectFromSide(BlockGetter world, BlockPos pos, Direction side) {
        return canConnectFromSide(world, pos, side, part -> part instanceof MultipartCablePart);
    }

    static boolean canAudioConnectFromSide(BlockGetter world, BlockPos pos, Direction side) {
        return canConnectFromSide(world, pos, side, part -> part instanceof MultipartAudioCablePart);
    }

    private static boolean canConnectFromSide(BlockGetter world, BlockPos pos, Direction side, Predicate<MultiPart> isCablePart) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof TileMultipart tile)) return true;

        var cablePath = NormalOcclusionTest.of(Cable.CachedBounds()[Cable.mask(side, 0)]);
        for (MultiPart part : tile.getPartList()) {
            if (!isCablePart.test(part) && !NormalOcclusionTest.test(cablePath, part)) return false;
        }
        return true;
    }

    static Optional<MultipartCablePart> cablePart(BlockGetter world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof TileMultipart tile) {
            for (MultiPart part : tile.getPartList()) {
                if (part instanceof MultipartCablePart cable) return Optional.of(cable);
            }
        }
        return Optional.empty();
    }

    static boolean isAudioCable(BlockGetter world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof TileMultipart tile) {
            for (MultiPart part : tile.getPartList()) {
                if (part instanceof MultipartAudioCablePart) return true;
            }
        }
        return false;
    }

    static Optional<Integer> cableColor(BlockGetter world, BlockPos pos) {
        return cablePart(world, pos).map(MultipartCablePart::getColor);
    }
}
