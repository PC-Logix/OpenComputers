package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.function.BiFunction;

public final class DriverRecordPlayer extends DriverSidedBlockEntity {
    public static final DriverRecordPlayer INSTANCE = new DriverRecordPlayer();
    public static final EnvironmentProvider PROVIDER = stack ->
            stack.getItem() == Blocks.JUKEBOX.asItem() ? Environment.class : null;

    private DriverRecordPlayer() {}

    @Override
    public Class<?> getBlockEntityClass() { return JukeboxBlockEntity.class; }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((JukeboxBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<JukeboxBlockEntity> implements NamedBlock {
        public Environment(JukeboxBlockEntity blockEntity) { super(blockEntity, "jukebox"); }

        @Override
        public String preferredName() { return "jukebox"; }

        @Override
        public int priority() { return 0; }

        private Object[] withRecord(BiFunction<Holder<JukeboxSong>, JukeboxPlayable, Object[]> action) {
            var playable = blockEntity.getTheItem().get(DataComponents.JUKEBOX_PLAYABLE);
            if (playable == null) return null;
            var song = playable.song().unwrap(ServerLifecycleHooks.getCurrentServer().registryAccess());
            return song.map(holder -> action.apply(holder, playable)).orElse(null);
        }

        @Callback(doc = "function():string -- Get the title of the record currently in the jukebox.")
        public Object[] getRecord(Context context, Arguments args) {
            return withRecord((song, playable) -> ResultWrapper.result(song.value().description().getString()));
        }

        @Callback(doc = "function() -- Start playing the record currently in the jukebox.")
        public Object[] play(Context context, Arguments args) {
            return withRecord((song, playable) -> {
                blockEntity.getSongPlayer().play(blockEntity.getLevel(), song);
                return ResultWrapper.result(true);
            });
        }

        @Callback(doc = "function() -- Stop playing the record currently in the jukebox.")
        public Object[] stop(Context context, Arguments args) {
            blockEntity.getSongPlayer().stop(blockEntity.getLevel(), blockEntity.getBlockState());
            return null;
        }
    }
}
