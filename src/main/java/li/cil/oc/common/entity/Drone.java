package li.cil.oc.common.entity;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Machine;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.item.Inventory;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.datacomponents.DroneState;
import li.cil.oc.common.datacomponents.Owner;
import li.cil.oc.common.item.data.DroneData;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.server.agent.PlayerSupport;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedLevelJava;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.fluids.IFluidTank;
import org.joml.Vector3d;
import scala.Option;
import scala.Option$;

import java.util.Optional;
import java.util.ArrayList;
import java.util.UUID;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;

/** A flying computer entity with an installed component set and cargo inventory. */
public class Drone extends Entity implements MachineHost, li.cil.oc.api.internal.Drone,
        li.cil.oc.api.internal.Rotatable, Analyzable, Context, Persistable {
    public static final EntityDataAccessor<Boolean> DataRunning = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DataTargetX = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DataTargetY = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DataTargetZ = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DataMaxAcceleration = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> DataSelectedSlot = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DataCurrentEnergy = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DataMaxEnergy = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Component> DataStatusText = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.COMPONENT);
    public static final EntityDataAccessor<Integer> DataInventorySize = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DataLightColor = SynchedEntityData.defineId(Drone.class, EntityDataSerializers.INT);

    public final float gravity = 0.05f;
    public final float drag = 0.8f;
    public final float maxAcceleration = 0.1f;
    public final float maxVelocity = 0.4f;
    public final int maxInventorySize = 8;
    public final float[][] targetFlapAngles = new float[4][2];
    public final float[][] flapAngles = new float[4][2];
    public int nextFlapChange;
    public float bodyAngle = (float) (Math.random() * 90);
    public float angularVelocity;
    public int nextAngularVelocityChange;
    public int lastEnergyUpdate;
    private boolean droppedAsItem;

    public final DroneData info = new DroneData();
    public final li.cil.oc.api.machine.Machine machine;
    public final li.cil.oc.server.component.Drone control;
    public final li.cil.oc.common.container.ComponentInventory components;
    public final li.cil.oc.common.container.Inventory equipmentInventory;
    public final DroneInventory mainInventory;
    public final MultiTank tank;
    public int selectedTank;
    public String ownerName = Settings.get().fakePlayerName();
    public UUID ownerUUID = Settings.get().fakePlayerProfile().getId();
    private li.cil.oc.server.agent.Player player_;
    public final net.minecraft.world.MenuProvider containerProvider;

    public Drone(EntityType<? extends Drone> type, Level level) {
        super(type, level);
        if (!level.isClientSide()) {
            machine = Machine.create(this);
            ((Connector) machine.node()).setLocalBufferSize(0);
            control = new li.cil.oc.server.component.Drone(this);
        } else { machine = null; control = null; }

        components = new DroneComponentInventory(this);
        equipmentInventory = new li.cil.oc.common.container.Inventory() {
            @Override public ItemStack[] items() { return new ItemStack[0]; }
            @Override public int getContainerSize() { return 0; }
            @Override public int getMaxStackSize() { return 0; }
            @Override public void setChanged() { }
            @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
            @Override public boolean stillValid(Player player) { return false; }
        };
        mainInventory = new DroneInventory(this) {
            private final ItemStack[] items = new ItemStack[8];
            { java.util.Arrays.fill(items, ItemStack.EMPTY); }
            @Override public ItemStack[] items() { return items; }
            @Override public int getContainerSize() { return inventorySize(); }
            @Override public int getMaxStackSize() { return 64; }
            @Override public void setChanged() { }
            @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot >= 0 && slot < getContainerSize(); }
            @Override public boolean stillValid(Player player) { return player.distanceToSqr(Drone.this) < 64; }
        };
        tank = new MultiTank() {
            @Override public int tankCount() {
                int count = 0;
                for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(components)) if (component.isPresent() && component.get() instanceof IFluidTank) count++;
                return count;
            }
            @Override public IFluidTank getFluidTank(int index) {
                ArrayList<IFluidTank> tanks = new ArrayList<>();
                for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(components)) if (component.isPresent() && component.get() instanceof IFluidTank fluidTank) tanks.add(fluidTank);
                return tanks.get(index);
            }
        };
        containerProvider = new net.minecraft.world.MenuProvider() {
            @Override public Component getDisplayName() { return Component.empty(); }
            @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
                return new li.cil.oc.common.menu.Drone(id, playerInventory, mainInventory, mainInventory.getContainerSize());
            }
        };
    }

    private li.cil.oc.server.agent.Player fakePlayer() { if (player_ == null) player_ = new li.cil.oc.server.agent.Player(this); return player_; }
    @Override public Level getEnvironmentLevel() { return level(); }
    public DroneData info() { return info; }
    public li.cil.oc.api.machine.Machine machine() { return machine; }
    public li.cil.oc.server.component.Drone control() { return control; }
    public li.cil.oc.common.container.ComponentInventory components() { return components; }
    public li.cil.oc.common.container.Inventory equipmentInventory() { return equipmentInventory; }
    public DroneInventory mainInventory() { return mainInventory; }
    public MultiTank tank() { return tank; }
    public int selectedTank() { return selectedTank; }
    public void selectedTank_$eq(int value) { selectedTank = value; }
    public String ownerName() { return ownerName; }
    public void ownerName_$eq(String value) { ownerName = value; }
    public UUID ownerUUID() { return ownerUUID; }
    public void ownerUUID_$eq(UUID value) { ownerUUID = value; }
    public int tier() { return info.tier; }
    public int maxInventorySize() { return maxInventorySize; }
    public float gravity() { return gravity; }
    public float drag() { return drag; }
    public float maxAcceleration() { return maxAcceleration; }
    public float maxVelocity() { return maxVelocity; }
    public float[][] targetFlapAngles() { return targetFlapAngles; }
    public float[][] flapAngles() { return flapAngles; }
    public int nextFlapChange() { return nextFlapChange; }
    public void nextFlapChange_$eq(int value) { nextFlapChange = value; }
    public float bodyAngle() { return bodyAngle; }
    public void bodyAngle_$eq(float value) { bodyAngle = value; }
    public float angularVelocity() { return angularVelocity; }
    public void angularVelocity_$eq(float value) { angularVelocity = value; }
    public int nextAngularVelocityChange() { return nextAngularVelocityChange; }
    public void nextAngularVelocityChange_$eq(int value) { nextAngularVelocityChange = value; }
    public int lastEnergyUpdate() { return lastEnergyUpdate; }
    public void lastEnergyUpdate_$eq(int value) { lastEnergyUpdate = value; }
    public net.minecraft.world.MenuProvider containerProvider() { return containerProvider; }
    @Override public Player player() { PlayerSupport.updatePositionAndRotation(fakePlayer(), facing(), facing()); PlayerSupport.setPlayerInventoryItems(fakePlayer()); return fakePlayer(); }
    @Override public String name() { return info.name; }
    @Override public void setName(String name) { info.name = name; }
    @Override public Node node() { return machine.node(); }
    @Override public boolean canInteract(String player) { return machine.canInteract(player); }
    @Override public boolean isPaused() { return machine.isPaused(); }
    @Override public boolean start() { if (level().isClientSide() || machine.isRunning()) return false; preparePowerUp(); return machine.start(); }
    @Override public boolean pause(double seconds) { return machine.pause(seconds); }
    @Override public boolean stop() { return machine.stop(); }
    @Override public void consumeCallBudget(double callCost) { machine.consumeCallBudget(callCost); }
    @Override public boolean signal(String name, Object... args) { return machine.signal(name, args); }
    @Override public Vector3d getTarget() { return new Vector3d(targetX(), targetY(), targetZ()); }
    @Override public void setTarget(Vector3d value) { setTargetX((float) value.x); setTargetY((float) value.y); setTargetZ((float) value.z); }
    @Override public Vector3d getVelocity() { Vec3 velocity = getDeltaMovement(); return new Vector3d(velocity.x, velocity.y, velocity.z); }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return true; }
    @Override public double xPosition() { return getX(); }
    @Override public double yPosition() { return getY(); }
    @Override public double zPosition() { return getZ(); }
    @Override public void markChanged() { }
    @Override public Vec3 getRopeHoldPosition(float dt) { return getPosition(dt).add(0, -0.056, 0); }
    @Override public Direction facing() { return Direction.SOUTH; }
    @Override public Direction toLocal(Direction direction) { return direction; }
    @Override public Direction toGlobal(Direction direction) { return direction; }
    @Override public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) { return new Node[]{machine.node()}; }
    @Override public java.lang.Iterable<ItemStack> internalComponents() { return java.util.Arrays.asList(info.components); }
    @Override public int componentSlot(String address) {
        Optional<ManagedEnvironment>[] slots = ComponentInventorySlotAccessJava.snapshot(components);
        for (int index = 0; index < slots.length; index++) if (slots[index].isPresent() && slots[index].get().node() != null && address.equals(slots[index].get().node().address())) return index;
        return -1;
    }
    @Override public void onMachineConnect(Node node) { }
    @Override public void onMachineDisconnect(Node node) { }
    public int computeInventorySize() {
        int size = 0;
        for (ItemStack stack : info.components) {
            if (stack == null) continue;
            var driver = Driver.driverFor(stack, getClass());
            if (driver instanceof Inventory inventory) size += Math.max(1, inventory.inventoryCapacity(stack) / 4);
        }
        return Math.min(maxInventorySize, size);
    }

    @Override public void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DataRunning, false); builder.define(DataTargetX, 0f); builder.define(DataTargetY, 0f); builder.define(DataTargetZ, 0f);
        builder.define(DataMaxAcceleration, 0f); builder.define(DataSelectedSlot, 0); builder.define(DataCurrentEnergy, 0); builder.define(DataMaxEnergy, 100);
        builder.define(DataStatusText, Component.empty()); builder.define(DataInventorySize, 0); builder.define(DataLightColor, 0x66DD55);
    }
    public void initializeAfterPlacement(ItemStack stack, Vec3 position) { info.loadData(stack); control.node().changeBuffer(info.storedEnergy - control.node().localBuffer()); wireThingsTogether(); setInventorySize(computeInventorySize()); setPos(position.x, position.y, position.z); }
    public void preparePowerUp() { setTargetX((float) Math.floor(getX()) + 0.5f); setTargetY((float) Math.round(getY()) + 0.5f); setTargetZ((float) Math.floor(getZ()) + 0.5f); setTargetAcceleration(maxAcceleration); wireThingsTogether(); }
    private void wireThingsTogether() { li.cil.oc.api.Network.joinNewNetwork(machine.node()); machine.node().connect(control.node()); machine.setCostPerTick(Settings.get().droneCost()); components.connectComponents(); }

    public boolean isRunning() { return entityData.get(DataRunning); }
    public float targetX() { return entityData.get(DataTargetX); }
    public float targetY() { return entityData.get(DataTargetY); }
    public float targetZ() { return entityData.get(DataTargetZ); }
    public float targetAcceleration() { return entityData.get(DataMaxAcceleration); }
    public int selectedSlot() { return entityData.get(DataSelectedSlot) & 0xFF; }
    public Integer globalBuffer() { return entityData.get(DataCurrentEnergy); }
    public Integer globalBufferSize() { return entityData.get(DataMaxEnergy); }
    public Component statusText() { return entityData.get(DataStatusText); }
    public int inventorySize() { return entityData.get(DataInventorySize) & 0xFF; }
    public Integer lightColor() { return entityData.get(DataLightColor); }
    public void setRunning(boolean value) { entityData.set(DataRunning, value); }
    public void setTargetX(float value) { entityData.set(DataTargetX, Math.round(value * 4) / 4f); }
    public void setTargetY(float value) { entityData.set(DataTargetY, Math.round(value * 4) / 4f); }
    public void setTargetZ(float value) { entityData.set(DataTargetZ, Math.round(value * 4) / 4f); }
    public void targetX_$eq(float value) { setTargetX(value); }
    public void targetY_$eq(float value) { setTargetY(value); }
    public void targetZ_$eq(float value) { setTargetZ(value); }
    public void setTargetAcceleration(float value) { entityData.set(DataMaxAcceleration, Math.max(0, Math.min(maxAcceleration, value))); }
    public void targetAcceleration_$eq(float value) { setTargetAcceleration(value); }
    public void setSelectedSlot(int value) { entityData.set(DataSelectedSlot, (int) (byte) value); }
    public void globalBuffer_$eq(int value) { entityData.set(DataCurrentEnergy, value); }
    public void globalBufferSize_$eq(int value) { entityData.set(DataMaxEnergy, value); }
    public void statusText_$eq(Component value) { entityData.set(DataStatusText, value); }
    public void setInventorySize(int value) { entityData.set(DataInventorySize, (int) (byte) value); }
    public void inventorySize_$eq(int value) { setInventorySize(value); }
    public void lightColor_$eq(int value) { entityData.set(DataLightColor, value); }
    @Override public void setSelectedTank(int index) { selectedTank = index; }

    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        if (!isRunning() || distanceToSqr(x, y, z) > 1) super.absMoveTo(x, y, z, yaw, pitch);
        else { setTargetX((float) x); setTargetY((float) y); setTargetZ((float) z); }
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (isInWater() || isInLava()) machine.stop();
            machine.update(); components.updateComponents(); setRunning(machine.isRunning());
            int buffer = (int) Math.round(((Connector) machine.node()).globalBuffer());
            if (Math.abs(lastEnergyUpdate - buffer) > 1 || level().getGameTime() % 200 == 0) { lastEnergyUpdate = buffer; globalBuffer_$eq(buffer); globalBufferSize_$eq((int) ((Connector) machine.node()).globalBufferSize()); }
        } else if (isRunning()) updateClientAnimation();
        xo = getX(); yo = getY(); zo = getZ(); noPhysics = !level().noCollision(this);
        if (noPhysics) moveTowardsClosestSpace(getX(), (getBoundingBox().minY + getBoundingBox().maxY) / 2, getZ());
        updateMovement();
    }

    private void updateClientAnimation() {
        var rng = level().getRandom();
        nextFlapChange--; nextAngularVelocityChange--;
        if (nextFlapChange < 0) {
            nextFlapChange = 5 + rng.nextInt(10);
            for (int i = 0; i < 2; i++) { int flap = rng.nextInt(targetFlapAngles.length); targetFlapAngles[flap][0] = (float) Math.toRadians(rng.nextFloat() * 4 - 2); targetFlapAngles[flap][1] = (float) Math.toRadians(rng.nextFloat() * 4 - 2); }
        }
        if (nextAngularVelocityChange < 0) {
            if (angularVelocity != 0) { angularVelocity = 0; nextAngularVelocityChange = 20; }
            else { angularVelocity = rng.nextBoolean() ? 0.1f : -0.1f; nextAngularVelocityChange = 100; }
        }
        for (int flap = 0; flap < flapAngles.length; flap++) for (int axis = 0; axis < 2; axis++) flapAngles[flap][axis] = flapAngles[flap][axis] * 0.7f + targetFlapAngles[flap][axis] * 0.3f;
        bodyAngle += angularVelocity;
    }
    private void updateMovement() {
        if (isRunning()) {
            Vec3 toTarget = new Vec3(targetX() - getX(), targetY() - getY(), targetZ() - getZ());
            double distance = toTarget.length(); Vec3 velocity = getDeltaMovement();
            if (distance > 0 && (distance > 0.005f || velocity.dot(velocity) > 0.005f)) {
                double acceleration = Math.min(targetAcceleration(), distance) / distance;
                setDeltaMovement(new Vec3(clamp(velocity.x + toTarget.x * acceleration, maxVelocity), clamp(velocity.y + toTarget.y * acceleration, maxVelocity), clamp(velocity.z + toTarget.z * acceleration, maxVelocity)));
            } else { setDeltaMovement(Vec3.ZERO); setPos(targetX(), targetY(), targetZ()); }
        } else setDeltaMovement(getDeltaMovement().subtract(0, gravity, 0));
        move(MoverType.SELF, getDeltaMovement());
        if (isRunning()) setDeltaMovement(getDeltaMovement().scale(drag));
        else {
            float groundDrag = (float) (new ExtendedLevelJava(level()).getBlock(BlockPositionsJava.apply((Entity) this).offset(Direction.DOWN)).getFriction() * drag);
            setDeltaMovement(getDeltaMovement().multiply(groundDrag, drag * (onGround() ? -0.5 : 1), groundDrag));
        }
    }
    private static double clamp(double value, double bound) { return Math.max(-bound, Math.min(bound, value)); }

    @Override public boolean skipAttackInteraction(Entity entity) {
        if (isRunning()) {
            Vec3 direction = new Vec3(entity.getX() - getX(), entity.getY() + entity.getEyeHeight() - getY(), entity.getZ() - getZ()).normalize();
            if (!level().isClientSide()) {
                if (Settings.get().inputUsername()) machine.signal("hit", direction.x, direction.z, direction.y, entity.getName().getString());
                else machine.signal("hit", direction.x, direction.z, direction.y);
            }
            setDeltaMovement(getDeltaMovement().subtract(direction).scale(0.5));
        }
        return super.skipAttackInteraction(entity);
    }

    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (!isAlive()) return InteractionResult.PASS;
        if (player.isCrouching()) {
            if (Wrench.isWrench(player.getItemInHand(InteractionHand.MAIN_HAND))) { if (!level().isClientSide()) dropAsItemAndDiscard(); }
            else if (!level().isClientSide() && !machine.isRunning()) start();
        } else if (player instanceof ServerPlayer serverPlayer && !level().isClientSide()) MenuTypes.openDroneGui(serverPlayer, this);
        return InteractionResult.sidedSuccess(level().isClientSide());
    }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { if (EventHandler.isItTime()) super.playStepSound(pos, state); }

    private boolean isChangingDimension;
    @Override public Entity changeDimension(DimensionTransition dimension) {
        setTargetX(targetX() - (float) getX()); setTargetY(targetY() - (float) getY()); setTargetZ(targetZ() - (float) getZ());
        try { isChangingDimension = true; return super.changeDimension(dimension); }
        finally { isChangingDimension = false; remove(RemovalReason.CHANGED_DIMENSION); }
    }
    @Override public void restoreFrom(Entity entity) {
        super.restoreFrom(entity);
        if (entity instanceof Drone drone) { setTargetX((float) getX() + drone.targetX()); setTargetY((float) getY() + drone.targetY()); setTargetZ((float) getZ() + drone.targetZ()); }
        else { setTargetX((float) getX()); setTargetY((float) getY()); setTargetZ((float) getZ()); }
    }
    @Override public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide() && !isChangingDimension) { machine.stop(); machine.node().remove(); components.disconnectComponents(); components.saveComponents(); }
    }
    private void dropAsItemAndDiscard() {
        if (!level().isClientSide() && !droppedAsItem) {
            droppedAsItem = true;
            ItemStack stack = li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.Drone()).createItemStack(1);
            info.storedEnergy = (int) control.node().localBuffer(); info.saveData(stack);
            ItemEntity entity = new ItemEntity(level(), getX(), getY(), getZ(), stack); entity.setPickUpDelay(15); level().addFreshEntity(entity);
            InventoryUtils.dropAllSlots(BlockPositionsJava.apply((Entity) this), mainInventory);
            remove(RemovalReason.DISCARDED);
        }
    }
    @Override public void checkBelowWorld() { if (getY() < level().getMinBuildHeight() - 64) dropAsItemAndDiscard(); else super.checkBelowWorld(); }
    @Override public Component getName() { return Localization.localizeLater("entity.oc.Drone.name"); }
    @Override public ClientboundAddEntityPacket getAddEntityPacket(ServerEntity entityTrackerEntry) { return new ClientboundAddEntityPacket(this, entityTrackerEntry); }

    @Override public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        info.loadData(holder);
        if (!level().isClientSide()) { machine.loadData(holder); control.loadData(holder); components.loadData(holder); mainInventory.loadData(holder); }
        var data = new li.cil.oc.util.ExtendedDataComponentHolder(holder);
        data.getComponentOptional(OCComponentsJava.OWNER.get())
            .ifPresent(owner -> { ownerName = owner.name(); ownerUUID = owner.id(); });
        data.getComponentOptional(OCComponentsJava.DRONE_STATE.get()).ifPresent(state -> {
            setTargetX(state.targetX());
            setTargetY(state.targetY());
            setTargetZ(state.targetZ());
            setTargetAcceleration(state.targetAcceleration());
            setSelectedSlot(state.selectedSlot() & 0xFF);
            setSelectedTank(state.selectedTank() & 0xFF);
        });
        data.getComponentOptional(OCComponentsJava.STATUS_TEXT.get()).ifPresent(this::statusText_$eq);
        data.getComponentOptional(OCComponentsJava.LIGHT_COLOR.get()).ifPresent(color -> lightColor_$eq(color.rgba()));
    }
    @Override public void saveData(MutableDataComponentHolder holder) {
        info.saveData(holder);
        if (!level().isClientSide()) { machine.saveData(holder); control.saveData(holder); components.saveData(holder); mainInventory.saveData(holder); }
        holder.set(OCComponentsJava.OWNER.get(), new Owner(ownerName, ownerUUID));
        holder.set(OCComponentsJava.DRONE_STATE.get(), new DroneState(targetX(), targetY(), targetZ(), targetAcceleration(), (byte) selectedSlot(), (byte) selectedTank));
        holder.set(OCComponentsJava.STATUS_TEXT.get(), statusText());
        holder.set(OCComponentsJava.LIGHT_COLOR.get(), new ColorRGBA(lightColor()));
    }
    @Override protected void readAdditionalSaveData(CompoundTag nbt) {
        try { loadData(nbt, level().registryAccess()); }
        catch (UnrecoverablePersistanceException error) { Drone.<RuntimeException>throwUnchecked(error); }
        setInventorySize(computeInventorySize()); if (!level().isClientSide()) wireThingsTogether();
    }
    @Override protected void addAdditionalSaveData(CompoundTag nbt) {
        if (level().isClientSide()) return;
        components.saveComponents(); info.storedEnergy = globalBuffer(); saveData(nbt, level().registryAccess());
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E { throw (E) error; }
}
