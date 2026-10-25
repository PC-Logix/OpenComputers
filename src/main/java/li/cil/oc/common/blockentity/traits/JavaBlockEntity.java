package li.cil.oc.common.blockentity.traits;

import li.cil.oc.SettingsAccessJava;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.SideTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import java.util.function.UnaryOperator;

/** Native Java root. Protected Minecraft lifecycle hooks never become interface defaults. */
public abstract class JavaBlockEntity extends BlockEntity implements BaseBlockEntity {
    private final MovingBlockEntityStateJava moving = new MovingBlockEntityStateJava();
    private RedstoneStateJava redstone;
    private BundledRedstoneStateJava bundled;
    private ComponentInventoryHostStateJava componentInventory;
    private InventoryRuntimeJava inventory;

    protected JavaBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override public MovingBlockEntityStateJava movingContext() { return moving; }
    @Override public void initializeMovingHost() { initialize(); }
    public synchronized RedstoneStateJava redstoneState() { if (redstone == null) redstone = new RedstoneStateJava(); return redstone; }
    public synchronized BundledRedstoneStateJava bundledRedstoneState() { if (bundled == null) bundled = new BundledRedstoneStateJava(); return bundled; }
    public synchronized ComponentInventoryHostStateJava componentHostState() {
        if (componentInventory == null) componentInventory = new ComponentInventoryHostStateJava();
        return componentInventory;
    }
    public synchronized InventoryRuntimeJava inventoryRuntime() { if (inventory == null) inventory = new InventoryRuntimeJava(); return inventory; }
    public Vec3 inventoryMovingPosition() { return moving.position(); }
    public Level movingLevelForEnvironmentJava() { return moving.level(); }
    public Vec3 movingPositionForEnvironmentJava() { return moving.position(); }
    // Concrete forwarders resolve unrelated capability interfaces' abstract context methods.
    @Override public boolean isMoving() { return BaseBlockEntity.super.isMoving(); }
    @Override public void beginMoving(Level level, Vec3 position, UnaryOperator<Vec3> rotation, BlockState state) { BaseBlockEntity.super.beginMoving(level, position, rotation, state); }
    @Override public void updateMovingPosition(Vec3 position, UnaryOperator<Vec3> rotation, BlockState state) { BaseBlockEntity.super.updateMovingPosition(position, rotation, state); }
    @Override public BlockPos movingBlockPos() { return BaseBlockEntity.super.movingBlockPos(); }
    @Override public BlockState movingBlockState() { return BaseBlockEntity.super.movingBlockState(); }
    @Override public Direction movingDirection(Direction side) { return BaseBlockEntity.super.movingDirection(side); }
    @Override public BlockPos movingNeighbor(Direction side) { return BaseBlockEntity.super.movingNeighbor(side); }
    @Override public void endMoving() { BaseBlockEntity.super.endMoving(); }
    @Override public int x() { return BaseBlockEntity.super.x(); }
    @Override public int y() { return BaseBlockEntity.super.y(); }
    @Override public int z() { return BaseBlockEntity.super.z(); }
    @Override public BlockPosition position() { return BaseBlockEntity.super.position(); }
    @Override public boolean isClient() { return BaseBlockEntity.super.isClient(); }
    @Override public boolean isServer() { return BaseBlockEntity.super.isServer(); }

    @Override public void updateEntity() {
        BaseBlockEntityPhasesJava.update(this);
        if (this instanceof NetworkBlockEntity environment) NetworkBlockEntityLifecycleJava.update(environment);
    }
    protected void initialize() {
        BaseBlockEntityPhasesJava.initialize(this);
        if (this instanceof NetworkBlockEntity environment) NetworkBlockEntityLifecycleJava.initialize(environment);
    }
    @Override public void dispose() {
        BaseBlockEntityPhasesJava.dispose(this);
        if (this instanceof NetworkBlockEntity environment) NetworkBlockEntityLifecycleJava.dispose(environment);
    }
    @Override public void clearRemoved() { super.clearRemoved(); initialize(); RedstoneLifecycleJava.afterClearRemoved(this); }
    @Override public void setRemoved() { super.setRemoved(); dispose(); }
    @Override public void onChunkUnloaded() { super.onChunkUnloaded(); BaseBlockEntityLifecycleJava.onChunkUnloaded(this); }
    @Override public void onLoad() { super.onLoad(); TextBufferLifecycleJava.ensureLoaded(this); }
    @Override public void setChanged() { super.setChanged(); ComputerLifecycleJava.afterSetChanged(this); }

    @Override public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        BaseBlockEntityPhasesJava.loadServer(this, nbt, provider);
        if (this instanceof NetworkBlockEntity environment) NetworkBlockEntityLifecycleJava.loadServer(environment, nbt, provider);
        if (this instanceof BlockInventory blockInventory) BlockInventoryLifecycleJava.loadServer(blockInventory, nbt, provider);
    }
    @Override public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        if (this instanceof NetworkBlockEntity environment)
            NetworkBlockEntityLifecycleJava.saveServer(environment, nbt, provider, () -> BaseBlockEntityPhasesJava.saveServer(this, nbt, provider));
        else BaseBlockEntityPhasesJava.saveServer(this, nbt, provider);
        if (this instanceof BlockInventory blockInventory) BlockInventoryLifecycleJava.saveServer(blockInventory, nbt, provider);
    }
    @Override public void loadForClient(CompoundTag nbt, HolderLookup.Provider provider) { BaseBlockEntityPhasesJava.loadClient(this, nbt, provider); }
    @Override public void saveForClient(CompoundTag nbt, HolderLookup.Provider provider) { BaseBlockEntityPhasesJava.saveClient(this, nbt, provider); }
    @Override public void loadComponentsCommon(DataComponentHolder holder) { ColoredJava.loadHostComponents(this, holder); }
    @Override public void saveComponentsCommon(MutableDataComponentHolder holder) { ColoredJava.saveHostComponents(this, holder); }
    @Override public void loadComponentsForServer(DataComponentHolder holder) {
        TextBufferLifecycleJava.reapplyTier(this);
        if (this instanceof BlockInventory blockInventory) BlockInventoryLifecycleJava.loadComponents(blockInventory, holder);
    }
    @Override public void saveComponentsForServer(MutableDataComponentHolder holder) {
        if (this instanceof BlockInventory blockInventory) BlockInventoryLifecycleJava.saveComponents(blockInventory, holder);
    }
    @Override public void loadComponentsForClient(DataComponentHolder holder) {
        ComputerLifecycleJava.loadComponentsForClient(this, holder);
        TextBufferLifecycleJava.reapplyTier(this);
    }
    @Override public void saveComponentsForClient(MutableDataComponentHolder holder) { ComputerLifecycleJava.saveComponentsForClient(this, holder); }
    @Override public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        ComputerLifecycleJava.beforeLoadAdditional(this, nbt);
        super.loadAdditional(nbt, provider);
        BaseBlockEntityPersistenceJava.loadAdditional(this, nbt, provider, isServer());
    }
    @Override public void loadWithComponents(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadWithComponents(tag, provider);
        BaseBlockEntityPersistenceJava.loadWithComponents(this, this, tag, provider, isServer());
    }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        save(tag, provider);
        BaseBlockEntityPersistenceJava.saveComponents(this, this, tag, provider, isServer());
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) { if (isServer()) saveForServer(tag, provider); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return BaseBlockEntityUpdateTagJava.getUpdateTag(this, this, super.getUpdateTag(provider), provider);
    }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider provider) {
        BaseBlockEntityLifecycleJava.onDataPacket(this, packet, provider);
    }
    public boolean hasProperty(ModelProperty<?> property) { return false; }
    public <T> T getData(ModelProperty<T> property) { return null; }
    public <T> T setData(ModelProperty<T> property, T value) { return null; }
}



