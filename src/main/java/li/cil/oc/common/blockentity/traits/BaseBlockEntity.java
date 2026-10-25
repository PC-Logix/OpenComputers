package li.cil.oc.common.blockentity.traits;

import li.cil.oc.util.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import java.util.function.UnaryOperator;

/** Public lifecycle/context contract shared by native Java hosts and legacy Scala adapters. */
public interface BaseBlockEntity extends MovingBlockEntityHost {
    Level getLevel();
    BlockPos getBlockPos();
    BlockState getBlockState();
    default int x() { return getBlockPos().getX(); }
    default int y() { return getBlockPos().getY(); }
    default int z() { return getBlockPos().getZ(); }
    default BlockPosition position() { return li.cil.oc.util.BlockPositionsJava.apply(x(), y(), z(), getLevel()); }
    default boolean isClient() { return !isServer(); }
    default boolean isServer() { return getLevel() != null ? !getLevel().isClientSide() : li.cil.oc.util.SideTracker.isServer(); }
void updateEntity();
    void dispose();
    void loadForServer(CompoundTag nbt, HolderLookup.Provider provider);
    void saveForServer(CompoundTag nbt, HolderLookup.Provider provider);
    void loadForClient(CompoundTag nbt, HolderLookup.Provider provider);
    void saveForClient(CompoundTag nbt, HolderLookup.Provider provider);
    void loadComponentsCommon(DataComponentHolder holder);
    void saveComponentsCommon(MutableDataComponentHolder holder);
    void loadComponentsForServer(DataComponentHolder holder);
    void saveComponentsForServer(MutableDataComponentHolder holder);
    void loadComponentsForClient(DataComponentHolder holder);
    void saveComponentsForClient(MutableDataComponentHolder holder);
    void loadWithComponents(CompoundTag tag, HolderLookup.Provider provider);
    void saveAdditional(CompoundTag tag, HolderLookup.Provider provider);
    CompoundTag save(CompoundTag tag, HolderLookup.Provider provider);
}

