package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.block.property.PropertyCableConnection;
import li.cil.oc.common.blockentity.RobotProxy;
import li.cil.oc.common.capabilities.Capabilities;
import li.cil.oc.integration.multipart.ModCBMultipart;
import li.cil.oc.util.Color;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.BlockCapability;

public class Cable extends SimpleBlock {
    private static final double MIN = 0.375;
    private static final double MAX = 1 - MIN;
    private static final VoxelShape DEFAULT_SHAPE = Shapes.box(MIN, MIN, MIN, MAX, MAX, MAX);
    private static final VoxelShape[] CACHED_PARTS = {
        Shapes.box(MIN, 0, MIN, MAX, MIN, MAX),
        Shapes.box(MIN, MAX, MIN, MAX, 1, MAX),
        Shapes.box(MIN, MIN, 0, MAX, MAX, MIN),
        Shapes.box(MIN, MIN, MAX, MAX, MAX, 1),
        Shapes.box(0, MIN, MIN, MIN, MAX, MAX),
        Shapes.box(MAX, MIN, MIN, 1, MAX, MAX)
    };
    private static final VoxelShape[] CACHED_BOUNDS = createCachedBounds();
    public static final MapCodec<Cable> CODEC = BlockBehaviour.simpleCodec(Cable::new);

    private scala.Option<Object> colorMultiplierOverride = scala.Option.empty();

    public Cable(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(CableHelper.helperRegisterDefaultState(getStateDefinition()));
    }

    @Override
    public MapCodec<Cable> codec() {
        return CODEC;
    }

    public scala.Option<Object> colorMultiplierOverride() {
        return colorMultiplierOverride;
    }

    public void colorMultiplierOverride_$eq(final scala.Option<Object> value) {
        colorMultiplierOverride = value;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final int color = getConnectionColor(context.getItemInHand());
        final BlockPos.MutableBlockPos fromPos = new BlockPos.MutableBlockPos();
        BlockState state = defaultBlockState();
        for (final Direction side : Direction.values()) {
            fromPos.setWithOffset(context.getClickedPos(), side);
            state = updateState(state, null, color, side, context.getLevel().getBlockState(fromPos),
                context.getLevel(), fromPos);
        }
        return state;
    }

    @Override
    public void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyCableConnection.DOWN, PropertyCableConnection.UP, PropertyCableConnection.NORTH,
            PropertyCableConnection.SOUTH, PropertyCableConnection.WEST, PropertyCableConnection.EAST);
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        final var blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof li.cil.oc.common.blockentity.Cable cable
            ? cable.createItemStack() : createItemStack(1);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                               final CollisionContext context) {
        return shape(state);
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block other,
                                final BlockPos otherPos, final boolean moved) {
        if (level.isClientSide()) return;
        final var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof li.cil.oc.common.blockentity.Cable cable) {
            final BlockPos.MutableBlockPos fromPos = new BlockPos.MutableBlockPos();
            BlockState newState = state;
            for (final Direction side : Direction.values()) {
                fromPos.setWithOffset(pos, side);
                newState = updateState(newState, cable, -1, side, level.getBlockState(fromPos), level, fromPos);
            }
            if (!newState.equals(state)) level.setBlock(pos, newState, 0x13);
        }
    }

    @Override
    public BlockState updateShape(final BlockState state, final Direction fromSide, final BlockState fromState,
                                 final LevelAccessor level, final BlockPos pos, final BlockPos fromPos) {
        return updateState(state, level.getBlockEntity(pos), -1, fromSide, fromState, level, fromPos);
    }

    @Override
    public li.cil.oc.common.blockentity.Cable newBlockEntity(final BlockPos pos, final BlockState state) {
        return new li.cil.oc.common.blockentity.Cable(pos, state);
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
                            final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Cable cable) {
            cable.fromItemStack(stack);
            state.updateNeighbourShapes(level, pos, 2);
        }
    }

    public static double MIN() { return MIN; }
    public static double MAX() { return MAX; }
    public static VoxelShape DefaultShape() { return DEFAULT_SHAPE; }
    public static VoxelShape[] CachedParts() { return CACHED_PARTS; }
    public static VoxelShape[] CachedBounds() { return CACHED_BOUNDS; }
    public static MapCodec<Cable> CODEC() { return CODEC; }

    public static int mask(final Direction side, final int value) {
        return value | (1 << side.get3DDataValue());
    }

    public static int mask$default$2() {
        return 0;
    }

    public static VoxelShape shape(final BlockState state) {
        int result = 0;
        for (final Direction side : Direction.values()) {
            if (CableHelper.getCableShape(state, side) != PropertyCableConnection.Shape.NONE) {
                result = mask(side, result);
            }
        }
        return CACHED_BOUNDS[result];
    }

    public static BlockState updateState(final BlockState state, final net.minecraft.world.level.block.entity.BlockEntity tileEntity,
                                         final int defaultColor, final Direction fromSide, final BlockState fromState,
                                         final BlockGetter world, final BlockPos fromPos) {
        final EnumProperty<PropertyCableConnection.Shape> property = PropertyCableConnection.BY_DIRECTION.get(fromSide);
        final var neighbor = world.getBlockEntity(fromPos);
        if (neighbor != null && neighbor.getLevel() != null) {
            final boolean hasNode = hasNetworkNode(neighbor, fromSide.getOpposite());
            final boolean sameColor = canConnectBasedOnColor(tileEntity, neighbor, defaultColor);
            final boolean multipartConnects = ModCBMultipart.canConnectFromSide(world, fromPos, fromSide.getOpposite());
            if (hasNode && sameColor && multipartConnects) {
                final PropertyCableConnection.Shape shape = fromState.is(state.getBlock())
                    || ModCBMultipart.isCable(world, fromPos)
                    ? PropertyCableConnection.Shape.CABLE : PropertyCableConnection.Shape.DEVICE;
                return CableHelper.helperSetCableShapeState(state, fromSide, shape);
            }
        }
        return CableHelper.helperSetCableShapeState(state, fromSide, PropertyCableConnection.Shape.NONE);
    }

    private static VoxelShape[] createCachedBounds() {
        final VoxelShape[] bounds = new VoxelShape[(0xFF >> 2) + 1];
        for (int mask = 0; mask < bounds.length; mask++) {
            VoxelShape shape = DEFAULT_SHAPE;
            for (final Direction side : Direction.values()) {
                if (((1 << side.get3DDataValue()) & mask) != 0) {
                    shape = Shapes.or(shape, CACHED_PARTS[side.ordinal()]);
                }
            }
            bounds[mask] = shape;
        }
        return bounds;
    }

    private static boolean hasNetworkNode(final net.minecraft.world.level.block.entity.BlockEntity tileEntity,
                                          final Direction side) {
        if (tileEntity == null || tileEntity instanceof RobotProxy) return false;
        final Level level = tileEntity.getLevel();
        final BlockPos pos = tileEntity.getBlockPos();
        if (level == null) return false;

        final var sidedHost = level.getCapability(Capabilities.SidedEnvironmentCapability(), pos, side);
        if (sidedHost != null) return level.isClientSide() ? sidedHost.canConnect(side) : sidedHost.sidedNode(side) != null;
        return level.getCapability(Capabilities.EnvironmentCapability(), pos, side) != null;
    }

    private static int getConnectionColor(final ItemStack stack) {
        final int color = ItemColorizer.getColor(stack);
        return color == -1 ? Color.rgbValues(DyeColor.LIGHT_GRAY) : color;
    }

    private static int getConnectionColor(final net.minecraft.world.level.block.entity.BlockEntity tileEntity) {
        if (tileEntity != null) {
            final Level level = tileEntity.getLevel();
            final BlockPos pos = tileEntity.getBlockPos();
            if (level != null) {
                final var colored = level.getCapability(Capabilities.ColoredCapability(), pos, null);
                if (colored != null && colored.controlsConnectivity()) return colored.getColor();
            }
        }
        return Color.rgbValues(DyeColor.LIGHT_GRAY);
    }

    private static boolean canConnectBasedOnColor(final net.minecraft.world.level.block.entity.BlockEntity first,
                                                  final net.minecraft.world.level.block.entity.BlockEntity second,
                                                  final int firstDefault) {
        final int firstColor = first == null ? firstDefault : getConnectionColor(first);
        final int secondColor = getConnectionColor(second);
        final int defaultColor = Color.rgbValues(DyeColor.LIGHT_GRAY);
        return firstColor == secondColor || firstColor == defaultColor || secondColor == defaultColor;
    }
}
