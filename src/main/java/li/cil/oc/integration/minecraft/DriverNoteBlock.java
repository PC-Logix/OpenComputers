package li.cil.oc.integration.minecraft;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;

public final class DriverNoteBlock implements DriverBlock {
    public static final DriverNoteBlock INSTANCE = new DriverNoteBlock();
    public static final EnvironmentProvider PROVIDER = stack ->
            !stack.isEmpty() && Block.byItem(stack.getItem()) == Blocks.NOTE_BLOCK ? Environment.class : null;

    private DriverNoteBlock() {}

    @Override
    public boolean worksWith(Level world, BlockPos pos, Direction side) {
        return world.getBlockState(pos).is(Blocks.NOTE_BLOCK);
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment(world, pos);
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock {
        private final Level world;
        private final BlockPos pos;

        public Environment(Level world, BlockPos pos) {
            this.world = world;
            this.pos = pos;
            setNode(Network.newNode(this, Visibility.Network).withComponent(preferredName()).create());
        }

        @Override
        public String preferredName() { return "note_block"; }

        @Override
        public int priority() { return 0; }

        @Callback(direct = true, doc = "function():number -- Get the currently set pitch on this note block.")
        public Object[] getPitch(Context context, Arguments args) {
            var state = world.getBlockState(pos);
            if (!state.is(Blocks.NOTE_BLOCK)) throw new IllegalArgumentException("block removed");
            return ResultWrapper.result(state.getValue(NoteBlock.NOTE).intValue() + 1);
        }

        @Callback(doc = "function(value:number) -- Set the pitch for this note block. Must be in the interval [1, 25].")
        public Object[] setPitch(Context context, Arguments args) {
            setPitch(args.checkInteger(0));
            return ResultWrapper.result(true);
        }

        @Callback(doc = "function([pitch:number]):boolean -- Triggers the note block if possible. Allows setting the pitch for to save a tick.")
        public Object[] trigger(Context context, Arguments args) {
            if (args.count() > 0 && args.checkAny(0) != null) {
                setPitch(args.checkInteger(0));
            } else if (!world.getBlockState(pos).is(Blocks.NOTE_BLOCK)) {
                throw new IllegalArgumentException("block removed");
            }
            boolean canTrigger = world.isEmptyBlock(pos.above());
            if (canTrigger) world.blockEvent(pos, Blocks.NOTE_BLOCK, 0, 0);
            return ResultWrapper.result(canTrigger);
        }

        private void setPitch(int value) {
            int pitch = value - 1;
            if (!NoteBlock.NOTE.getPossibleValues().contains(pitch)) {
                throw new IllegalArgumentException("invalid pitch");
            }
            var state = world.getBlockState(pos);
            if (!state.is(Blocks.NOTE_BLOCK)) throw new IllegalArgumentException("block removed");
            var newState = state.setValue(NoteBlock.NOTE, pitch);
            if (newState != state) world.setBlock(pos, newState, 3);
        }
    }
}
