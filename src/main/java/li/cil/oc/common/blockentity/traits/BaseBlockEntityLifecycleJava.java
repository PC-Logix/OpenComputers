package li.cil.oc.common.blockentity.traits;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Non-sound lifecycle behavior shared by Scala-mixed block entities. */
final class BaseBlockEntityLifecycleJava {
    private BaseBlockEntityLifecycleJava() { }

    static void updateEntity(BaseBlockEntity self) {
        BlockEntity entity = (BlockEntity) self;
        Level level = entity.getLevel();
        if (Settings.get().periodicallyForceLightUpdate()
            && level.getGameTime() % 40 == 0
            && entity.getBlockState().getLightEmission(level, entity.getBlockPos()) > 0) {
            BlockPos position = entity.getBlockPos();
            BlockState state = level.getBlockState(position);
            level.sendBlockUpdated(position, state, state, 3);
        }
    }

    static void onChunkUnloaded(BaseBlockEntity self) {
        try {
            self.dispose();
        } catch (Throwable error) {
            OpenComputers.log().error("Failed properly disposing a block entity, things may leak and or break.", error);
        }
    }

    static void onDataPacket(BaseBlockEntity self, ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider provider) {
        try {
            self.loadWithComponents(packet.getTag(), provider);
        } catch (Throwable error) {
            OpenComputers.log().warn("There was a problem reading a BlockEntity description packet. Please report this if you see it!", error);
        }
    }
}
