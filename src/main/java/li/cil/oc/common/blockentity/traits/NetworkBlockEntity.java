package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Native network host contract, independent of Scala's compatibility mixin. */
public interface NetworkBlockEntity extends BaseBlockEntity, li.cil.oc.api.network.Environment, li.cil.oc.api.network.EnvironmentHost {
    boolean isChangeScheduledForJava();
    void setChangeScheduledForJava(boolean value);
    Level movingLevelForEnvironmentJava();
    Vec3 movingPositionForEnvironmentJava();
    default void saveMovingState(CompoundTag nbt, HolderLookup.Provider provider) {
        saveAdditional(nbt, provider);
        ComputerLifecycleJava.afterSaveMovingState(this, nbt);
    }
    default void disposeMoving() { EnvironmentJava.disposeMoving(this); }
    @Override default Level getEnvironmentLevel() { return EnvironmentJava.environmentLevel(movingLevelForEnvironmentJava(), getLevel()); }
    @Override default double xPosition() { return EnvironmentJava.xPosition(movingPositionForEnvironmentJava(), x()); }
    @Override default double yPosition() { return EnvironmentJava.yPosition(movingPositionForEnvironmentJava(), y()); }
    @Override default double zPosition() { return EnvironmentJava.zPosition(movingPositionForEnvironmentJava(), z()); }
    @Override default void markChanged() { EnvironmentJava.markChanged(this); }
    @Override default void onMessage(li.cil.oc.api.network.Message message) { }
    @Override default void onConnect(li.cil.oc.api.network.Node node) { ComponentInventoryLifecycleJava.onConnect(this, node); }
    @Override default void onDisconnect(li.cil.oc.api.network.Node node) {
        EnvironmentJava.onDisconnect(node(), node);
        ComponentInventoryLifecycleJava.onDisconnect(this, node);
    }
}
