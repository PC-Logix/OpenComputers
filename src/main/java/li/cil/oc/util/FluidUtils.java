package li.cil.oc.util;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import scala.jdk.javaapi.OptionConverters;

import java.util.Optional;

public final class FluidUtils {
    private FluidUtils() {}

    /** Scala compatibility accessor; Java callers should use {@link #fluidHandlerAtOptional(BlockPosition, Direction)}. */
    @Deprecated
    public static scala.Option<IFluidHandler> fluidHandlerAt(BlockPosition position, Direction side) {
        return OptionConverters.toScala(fluidHandlerAtOptional(position, side));
    }

    /** Retrieves a fluid handler at a world coordinate, including fluid blocks. */
    public static Optional<IFluidHandler> fluidHandlerAtOptional(BlockPosition position, Direction side) {
        Level world = position.worldOptional().isPresent() ? position.worldOptional().orElseThrow() : null;
        if (world == null || !new ExtendedLevelJava(world).blockExists(position)) return Optional.empty();

        BlockEntity blockEntity = new ExtendedLevelJava(world).getBlockEntity(position);
        if (blockEntity instanceof IFluidHandler handler) return Optional.of(handler);
        if (blockEntity instanceof BlockEntity) {
            IFluidHandler handler = world.getCapability(Capabilities.FluidHandler.BLOCK, position.toBlockPos(), side);
            if (handler != null) return Optional.of(handler);
        }
        return Optional.of(new GenericBlockWrapper(position));
    }

    public static IFluidHandlerItem fluidHandlerOf(ItemStack stack) {
        return stack == null ? null : stack.getCapability(Capabilities.FluidHandler.ITEM);
    }

    public static int transferBetweenFluidHandlers(IFluidHandler source, IFluidHandler sink, int limit, int sourceTank) {
        FluidStack stackToDrain = null;
        if (sourceTank >= 0 && sourceTank < source.getTanks()) {
            stackToDrain = source.getFluidInTank(sourceTank);
            if (stackToDrain != null && !stackToDrain.isEmpty()) {
                stackToDrain = stackToDrain.copy();
                stackToDrain.setAmount(Math.min(stackToDrain.getAmount(), limit));
            }
        }

        FluidStack drained = stackToDrain != null
            ? source.drain(stackToDrain, FluidAction.SIMULATE)
            : source.drain(limit, FluidAction.SIMULATE);
        if (drained == null || drained.isEmpty()) return 0;

        int filledAmount = sink.fill(drained, FluidAction.SIMULATE);
        if (stackToDrain != null) {
            FluidStack filledStack = drained.copy();
            filledStack.setAmount(filledAmount);
            return sink.fill(source.drain(filledStack, FluidAction.EXECUTE), FluidAction.EXECUTE);
        }
        return sink.fill(source.drain(filledAmount, FluidAction.EXECUTE), FluidAction.EXECUTE);
    }

    public static int transferBetweenFluidHandlers(IFluidHandler source, IFluidHandler sink) {
        return transferBetweenFluidHandlers(source, sink, FluidType.BUCKET_VOLUME, -1);
    }

    public static int transferBetweenFluidHandlers(IFluidHandler source, IFluidHandler sink, int limit) {
        return transferBetweenFluidHandlers(source, sink, limit, -1);
    }

    public static int transferBetweenFluidHandlersAt(BlockPosition sourcePos, Direction sourceSide,
                                                       BlockPosition sinkPos, Direction sinkSide,
                                                       int limit, int sourceTank) {
        Optional<IFluidHandler> source = fluidHandlerAtOptional(sourcePos, sourceSide);
        if (source.isEmpty()) return 0;
        Optional<IFluidHandler> sink = fluidHandlerAtOptional(sinkPos, sinkSide);
        return sink.isEmpty() ? 0 : transferBetweenFluidHandlers(source.get(), sink.get(), limit, sourceTank);
    }

    public static int transferBetweenFluidHandlersAt(BlockPosition sourcePos, Direction sourceSide,
                                                       BlockPosition sinkPos, Direction sinkSide) {
        return transferBetweenFluidHandlersAt(sourcePos, sourceSide, sinkPos, sinkSide, FluidType.BUCKET_VOLUME, -1);
    }

    public static int transferBetweenFluidHandlersAt(BlockPosition sourcePos, Direction sourceSide,
                                                       BlockPosition sinkPos, Direction sinkSide, int limit) {
        return transferBetweenFluidHandlersAt(sourcePos, sourceSide, sinkPos, sinkSide, limit, -1);
    }

    /** Returns null for non-fluid blocks for legacy compatibility. */
    @Deprecated
    public static Fluid lookupFluidForBlock(Block block) {
        return block instanceof LiquidBlock liquid ? liquid.fluid : null;
    }

    private static final class GenericBlockWrapper implements IFluidHandler {
        private final BlockPosition position;

        private GenericBlockWrapper(BlockPosition position) {
            this.position = position;
        }

        private Optional<IFluidHandler> currentWrapper() {
            Level world = position.worldOptional().orElseThrow();
            if (!new ExtendedLevelJava(world).blockExists(position)) return Optional.empty();
            Block block = new ExtendedLevelJava(world).getBlock(position);
            if (block instanceof LiquidBlock liquid && lookupFluidForBlock(liquid) != null && isFullLiquidBlock()) {
                return Optional.of(new LiquidBlockWrapper(position, liquid));
            }
            ExtendedBlockJava extendedBlock = new ExtendedBlockJava(block);
            if (extendedBlock.isAir(position) || extendedBlock.isReplaceable(position)) {
                return Optional.of(new AirBlockWrapper(position, block));
            }
            return Optional.empty();
        }

        private boolean isFullLiquidBlock() {
            Level world = position.worldOptional().orElseThrow();
            return world.getBlockState(position.toBlockPos()).getValue(LiquidBlock.LEVEL) == 0;
        }

        @Override public int getTanks() {
            Optional<IFluidHandler> wrapper = currentWrapper();
            return wrapper.isEmpty() ? 0 : wrapper.get().getTanks();
        }
        @Override public FluidStack getFluidInTank(int tank) {
            Optional<IFluidHandler> wrapper = currentWrapper();
            return wrapper.isEmpty() ? FluidStack.EMPTY : wrapper.get().getFluidInTank(tank);
        }
        @Override public int getTankCapacity(int tank) {
            Optional<IFluidHandler> wrapper = currentWrapper();
            return wrapper.isEmpty() ? 0 : wrapper.get().getTankCapacity(tank);
        }
        @Override public boolean isFluidValid(int tank, FluidStack fluid) {
            Optional<IFluidHandler> wrapper = currentWrapper();
            return wrapper.isPresent() && wrapper.get().isFluidValid(tank, fluid);
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            Optional<IFluidHandler> wrapper = currentWrapper();
            return wrapper.isEmpty() ? null : wrapper.get().drain(resource, action);
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            Optional<IFluidHandler> wrapper = currentWrapper();
            return wrapper.isEmpty() ? null : wrapper.get().drain(maxDrain, action);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            Optional<IFluidHandler> wrapper = currentWrapper();
            return wrapper.isEmpty() ? 0 : wrapper.get().fill(resource, action);
        }
    }

    private abstract static class BlockWrapperBase implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public int getTankCapacity(int tank) { return FluidType.BUCKET_VOLUME; }

        protected abstract FluidStack uncheckedDrain(FluidAction action);

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack drained = uncheckedDrain(FluidAction.SIMULATE);
            if (drained != null && (resource == null ||
                (drained.getFluid() == resource.getFluid() && drained.getAmount() <= resource.getAmount()))) {
                return uncheckedDrain(action);
            }
            return null;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = uncheckedDrain(FluidAction.SIMULATE);
            return drained != null && drained.getAmount() <= maxDrain ? uncheckedDrain(action) : null;
        }

        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
    }

    private static final class LiquidBlockWrapper extends BlockWrapperBase {
        private final BlockPosition position;
        private final LiquidBlock block;
        private final Fluid fluid;

        private LiquidBlockWrapper(BlockPosition position, LiquidBlock block) {
            this.position = position;
            this.block = block;
            this.fluid = lookupFluidForBlock(block);
        }

        @Override public FluidStack getFluidInTank(int tank) {
            return isFullLiquidBlock() ? new FluidStack(fluid, FluidType.BUCKET_VOLUME) : FluidStack.EMPTY;
        }
        @Override public boolean isFluidValid(int tank, FluidStack fluid) { return block.fluid.isSame(fluid.getFluid()); }

        @Override
        protected FluidStack uncheckedDrain(FluidAction action) {
            if (action.execute()) new ExtendedLevelJava(position.worldOptional().orElseThrow()).setBlockToAir(position);
            return isFullLiquidBlock() ? new FluidStack(fluid, FluidType.BUCKET_VOLUME) : FluidStack.EMPTY;
        }

        private boolean isFullLiquidBlock() {
            return position.worldOptional().orElseThrow().getBlockState(position.toBlockPos()).getValue(LiquidBlock.LEVEL) == 0;
        }
    }

    private static final class AirBlockWrapper implements IFluidHandler {
        private final BlockPosition position;
        private final Block block;

        private AirBlockWrapper(BlockPosition position, Block block) {
            this.position = position;
            this.block = block;
        }

        @Override public int getTanks() { return 1; }
        @Override public int getTankCapacity(int tank) { return FluidType.BUCKET_VOLUME; }
        @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
        @Override public boolean isFluidValid(int tank, FluidStack fluid) {
            return fluid.getFluid().defaultFluidState().createLegacyBlock() != null;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource != null && resource.getFluid().defaultFluidState().createLegacyBlock() != null &&
                resource.getAmount() >= FluidType.BUCKET_VOLUME) {
                if (action.execute()) {
                    Level world = position.worldOptional().orElseThrow();
                    if (!new ExtendedBlockAccessJava(world).isAirBlock(position) && !world.containsAnyLiquid(position.bounds())) {
                        new ExtendedLevelJava(world).breakBlock(position);
                    }
                    world.setBlockAndUpdate(position.toBlockPos(), resource.getFluid().defaultFluidState().createLegacyBlock());
                    new ExtendedLevelJava(world).notifyBlockOfNeighborChange(position, new ExtendedLevelJava(world).getBlock(position));
                }
                return FluidType.BUCKET_VOLUME;
            }
            return 0;
        }
    }
}
