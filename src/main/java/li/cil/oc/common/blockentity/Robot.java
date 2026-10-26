package li.cil.oc.common.blockentity;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.item.Container;
import li.cil.oc.api.event.RobotAnalyzeEvent;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.RobotFlags;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.blockentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.common.container.InventoryProxy;
import li.cil.oc.common.datacomponents.Owner;
import li.cil.oc.common.datacomponents.RobotCurrentAnimation;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.integration.opencomputers.DriverKeyboard;
import li.cil.oc.integration.opencomputers.DriverRedstoneCard;
import li.cil.oc.integration.opencomputers.DriverScreen;
import li.cil.oc.integration.opencomputers.Item;
import li.cil.oc.server.RobotPacketSender;
import li.cil.oc.server.component.GraphicsCard;
import li.cil.oc.util.BlockPosHelper;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedLevelJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.StackOption;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import scala.Option;
import scala.Option$;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.immutable.List$;
import scala.collection.mutable.ArrayBuffer;
import scala.jdk.javaapi.CollectionConverters;

import java.util.Optional;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Consumer;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;

/** The live robot implementation, held by the movable proxy block entity. */
public class Robot extends RobotBlockEntity {
    private final li.cil.oc.common.blockentity.traits.ComputerStateJava computerState =
        new li.cil.oc.common.blockentity.traits.ComputerStateJava();
    @Override public li.cil.oc.common.blockentity.traits.ComputerStateJava computerState() { return computerState; }
    @Override public void saveForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForClient(nbt, provider);
        li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava.saveForClient(this, nbt);
    }

    @Override public void loadForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForClient(nbt, provider);
        li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava.loadForClient(this, nbt);
    }

    @Override public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava.saveForServer(this, nbt);
    }

    @Override public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava.loadForServer(this, nbt);
    }

    private final li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava rotationState =
        new li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava();
    @Override public li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava rotationState() { return rotationState; }
    private final li.cil.oc.common.blockentity.traits.PowerInformationJava powerInformationState =
        new li.cil.oc.common.blockentity.traits.PowerInformationJava();
    @Override public li.cil.oc.common.blockentity.traits.PowerInformationJava powerInformationState() {
        return powerInformationState;
    }
    private static final String ROBOT_TAG = Settings.namespace() + "robot";
    private static final String OWNER_TAG = Settings.namespace() + "owner";
    private static final String OWNER_UUID_TAG = Settings.namespace() + "ownerUuid";
    private static final String SELECTED_SLOT_TAG = Settings.namespace() + "selectedSlot";
    private static final String SELECTED_TANK_TAG = Settings.namespace() + "selectedTank";
    private static final String ANIMATION_TOTAL_TAG = Settings.namespace() + "animationTicksTotal";
    private static final String ANIMATION_LEFT_TAG = Settings.namespace() + "animationTicksLeft";
    private static final String MOVE_FROM_X_TAG = Settings.namespace() + "moveFromX";
    private static final String MOVE_FROM_Y_TAG = Settings.namespace() + "moveFromY";
    private static final String MOVE_FROM_Z_TAG = Settings.namespace() + "moveFromZ";
    private static final String SWINGING_TOOL_TAG = Settings.namespace() + "swingingTool";
    private static final String TURN_AXIS_TAG = Settings.namespace() + "turnAxis";

    public RobotProxy proxy;
    public final RobotData info = new RobotData();
    public final li.cil.oc.server.component.Robot bot;
    public final InventoryProxy equipmentInventory;
    public final InventoryProxy mainInventory;
    public final int actualInventorySize = 100;
    public int inventorySize = -1;
    public int selectedSlot;
    public final MultiTank tank;
    public int selectedTank;
    public boolean renderingErrored;
    public double globalBuffer;
    public double globalBufferSize;
    public final int maxComponents = 32;
    public String ownerName = Settings.get().fakePlayerName();
    public UUID ownerUUID = Settings.get().fakePlayerProfile().getId();
    public int animationTicksLeft;
    public int animationTicksTotal;
    public Option<BlockPos> moveFrom = Option$.MODULE$.empty();
    public boolean swingingTool;
    public int turnAxis;
    public boolean appliedToolEnchantments;
    public int getContainerSize = actualInventorySize;
    private boolean updatingInventorySize;
    private li.cil.oc.server.agent.Player player_;

    public Robot(BlockPos pos, BlockState state) {
        super(pos, state);
        bot = isServer() ? new li.cil.oc.server.component.Robot(this) : null;
        if (isServer()) machine().setCostPerTick(Settings.get().robotCost());

        equipmentInventory = new InventoryProxy() {
            @Override public Robot inventory() { return Robot.this; }
            @Override public int getContainerSize() { return 4; }
        };
        mainInventory = new InventoryProxy() {
            @Override public Robot inventory() { return Robot.this; }
            @Override public int getContainerSize() { return Robot.this.inventorySize; }
            @Override public int offset() { return equipmentInventory.getContainerSize(); }
        };
        tank = new MultiTank() {
            @Override public int tankCount() { return Robot.this.tankCount(); }
            @Override public IFluidTank getFluidTank(int index) { return Robot.this.getFluidTank(index); }
        };
    }

    private li.cil.oc.server.agent.Player fakePlayer() {
        if (player_ == null) player_ = new li.cil.oc.server.agent.Player(this);
        return player_;
    }

    public RobotData info() { return info; }
    public li.cil.oc.server.component.Robot bot() { return bot; }
    public RobotProxy proxy() { return proxy; }
    public void proxy_$eq(RobotProxy value) { proxy = value; }
    public InventoryProxy equipmentInventory() { return equipmentInventory; }
    public InventoryProxy mainInventory() { return mainInventory; }
    public MultiTank tank() { return tank; }
    public li.cil.oc.server.agent.Player player() { return fakePlayer(); }
    public void setWorldPosition(BlockPos position) { worldPosition = position; }
    public void setLightColor(int value) { info.lightColor = value; sendPacket(() -> RobotPacketSender.sendLightChange(this)); }
    public void setFlag(Option<ResourceLocation> value) {
        info.flag = value.flatMap(RobotFlags::byId).map(RobotFlags.Flag::id);
        setChanged();
        sendPacket(() -> RobotPacketSender.sendFlagChange(this));
    }
    @Override public void synchronizeSlot(int slot) {
        if (slot < 0 || slot >= getContainerSize) return;
        synchronized (this) {
            ItemStack stack = getItem(slot);
            Optional<ManagedEnvironment> component = ComponentInventorySlotAccessJava.get(this, slot);
            if (component.isPresent()) save(component.get(), Driver.driverFor(stack, getClass()), stack);
            sendPacket(() -> RobotPacketSender.sendInventory(this, slot, stack));
        }
    }
    @Override public ManagedEnvironment getComponentInSlot(int index) {
        Optional<ManagedEnvironment> component = ComponentInventorySlotAccessJava.get(this, index);
        return component.isPresent() ? component.get() : null;
    }
    public boolean isCreative() { return tier() == Tier.Five; }
    public int tier() { return info.tier; }
    public boolean renderingErrored() { return renderingErrored; }
    public void renderingErrored_$eq(boolean value) { renderingErrored = value; }
    public int selectedSlot() { return selectedSlot; }
    public void selectedSlot_$eq(int value) { selectedSlot = value; }
    public int selectedTank() { return selectedTank; }
    public void selectedTank_$eq(int value) { selectedTank = value; }
    public double globalBuffer() { return globalBuffer; }
    public void globalBuffer_$eq(double value) { globalBuffer = value; }
    public double globalBufferSize() { return globalBufferSize; }
    public void globalBufferSize_$eq(double value) { globalBufferSize = value; }
    public int getContainerSize() { return getContainerSize; }
    public void getContainerSize_$eq(int value) { getContainerSize = value; }
    public String ownerName() { return ownerName; }
    public void ownerName_$eq(String value) { ownerName = value; }
    public UUID ownerUUID() { return ownerUUID; }
    public void ownerUUID_$eq(UUID value) { ownerUUID = value; }
    public int animationTicksLeft() { return animationTicksLeft; }
    public void animationTicksLeft_$eq(int value) { animationTicksLeft = value; }
    public int animationTicksTotal() { return animationTicksTotal; }
    public void animationTicksTotal_$eq(int value) { animationTicksTotal = value; }
    public Option<BlockPos> moveFrom() { return moveFrom; }
    public void moveFrom_$eq(Option<BlockPos> value) { moveFrom = value; }
    public boolean swingingTool() { return swingingTool; }
    public void swingingTool_$eq(boolean value) { swingingTool = value; }
    public int turnAxis() { return turnAxis; }
    public void turnAxis_$eq(int value) { turnAxis = value; }
    public boolean appliedToolEnchantments() { return appliedToolEnchantments; }
    public void appliedToolEnchantments_$eq(boolean value) { appliedToolEnchantments = value; }
    public int componentCount() { return info.components.length; }
    // RobotData owns the installed hardware list in COMPONENTS. Keep the robot's
    // ordinary inventory in CONTENTS so loading it cannot duplicate the hardware
    // stacks into the visible inventory.
    @Override public net.minecraft.core.component.DataComponentType<scala.collection.immutable.List<li.cil.oc.api.ImmutableItemStack>> component() {
        return OCComponentsJava.CONTENTS.get();
    }
    public int maxInventorySize() { return actualInventorySize - equipmentInventory.getContainerSize() - componentCount(); }
    public int[] containerSlots() { int[] slots = new int[info.containers.length]; for (int i = 0; i < slots.length; i++) slots[i] = i + 1; return slots; }
    public scala.collection.immutable.Range componentSlotRange() { return scala.collection.immutable.Range$.MODULE$.apply(getContainerSize - componentCount(), getContainerSize()); }
    public scala.collection.immutable.Range inventorySlots() { return scala.collection.immutable.Range$.MODULE$.apply(equipmentInventory.getContainerSize(), equipmentInventory.getContainerSize() + mainInventory.getContainerSize()); }

    @Override public void setSelectedSlot(int index) {
        selectedSlot = Math.max(0, Math.min(index, mainInventory.getContainerSize() - 1));
        if (getLevel() != null) sendPacket(() -> RobotPacketSender.sendSelectedSlotChange(this, Settings.get().maxNetworkClientEffectPacketDistance() / 4.0D));
    }
    @Override public void setSelectedTank(int index) { selectedTank = index; }
    @Override public Node node() { return isServer() ? machine().node() : null; }
    @Override public boolean shouldAnimate() { return isRunning(); }
    @Override public String name() { return info.name.getString(); }
    @Override public void setName(String value) { info.name = Component.literal(value); }
    @Override public int componentSlot(String address) {
        Optional<ManagedEnvironment>[] components = ComponentInventorySlotAccessJava.snapshot(this);
        for (int slot = 0; slot < components.length; slot++) {
            Optional<ManagedEnvironment> env = components[slot];
            if (env.isPresent() && env.get().node() != null && env.get().node().address().equals(address)) return slot;
        }
        return -1;
    }
    @Override public boolean hasRedstoneCard() {
        for (int slot : allComponentSlots()) {
            ItemStack stack = getItem(slot);
            if (stack != null && !stack.isEmpty() && machine().isRunning() && DriverRedstoneCard.INSTANCE.worksWith(stack, getClass())) return true;
        }
        return false;
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        player.sendSystemMessage(Localization.Analyzer.RobotOwner(ownerName));
        player.sendSystemMessage(Localization.Analyzer.RobotName(fakePlayer().getName().getString()));
        NeoForge.EVENT_BUS.post(new RobotAnalyzeEvent(this, player));
        return super.onAnalyze(player, side, hitX, hitY, hitZ);
    }

    public boolean move(Direction direction) {
        BlockPos oldPosition = getBlockPos();
        BlockPos newPosition = BlockPosHelper.relative(oldPosition, direction);
        if (!getLevel().isLoaded(newPosition)) return false;
        if (isServer()) {
            RobotMoveEvent.Pre event = new RobotMoveEvent.Pre(this, direction);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) return false;
        }
        var blockRobotProxy = OCBlocks.Robot().get();
        var blockRobotAfterImage = OCBlocks.RobotAfterimage().get();
        boolean wasAir = getLevel().isEmptyBlock(newPosition);
        BlockState state = getLevel().getBlockState(newPosition);
        Block block = state.getBlock();
        try {
            blockRobotProxy.moving.set(Option$.MODULE$.apply(this));
            getLevel().setBlockAndUpdate(newPosition, Blocks.AIR.defaultBlockState());
            boolean created = getLevel().setBlock(newPosition, getLevel().getBlockState(oldPosition), 1)
                && getLevel().getBlockEntity(newPosition) instanceof RobotProxy newProxy && newProxy.robot() == this;
            if (created) {
                assert getBlockPos().equals(newPosition);
                getLevel().setBlock(oldPosition, Blocks.AIR.defaultBlockState(), 1);
                getLevel().setBlock(oldPosition, blockRobotAfterImage.defaultBlockState(), 1);
                assert getLevel().getBlockState(oldPosition).getBlock() == blockRobotAfterImage;
                setAnimateMove(oldPosition, Math.max((int) (Settings.get().moveDelay() * 20), 1));
                if (isServer()) {
                    sendPacket(() -> RobotPacketSender.sendMove(this, oldPosition, direction));
                    checkRedstoneInputChanged();
                    NeoForge.EVENT_BUS.post(new RobotMoveEvent.Post(this, direction));
                } else {
                    if (!wasAir && block != Blocks.AIR && block != blockRobotAfterImage) {
                        if (!state.getFluidState().isEmpty()) getLevel().playLocalSound(newPosition.getX() + 0.5, newPosition.getY() + 0.5, newPosition.getZ() + 0.5,
                            SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, getLevel().getRandom().nextFloat() * 0.25f + 0.75f, getLevel().getRandom().nextFloat() + 0.5f, false);
                        if (state.getFluidState().isEmpty()) getLevel().levelEvent(2001, newPosition, Block.getId(state));
                    }
                    new ExtendedLevelJava(getLevel()).notifyBlockUpdate(oldPosition);
                    new ExtendedLevelJava(getLevel()).notifyBlockUpdate(newPosition);
                }
                assert !isRemoved();
            } else getLevel().setBlockAndUpdate(newPosition, Blocks.AIR.defaultBlockState());
            return created && getBlockPos().equals(newPosition);
        } finally {
            blockRobotProxy.moving.set(Option$.MODULE$.empty());
        }
    }

    public boolean isAnimatingMove() { return animationTicksLeft > 0 && moveFrom.isDefined(); }
    public boolean isAnimatingSwing() { return animationTicksLeft > 0 && swingingTool; }
    public boolean isAnimatingTurn() { return animationTicksLeft > 0 && turnAxis != 0; }
    public void animateSwing(double duration) { if (!getItem(0).isEmpty()) { setAnimateSwing((int) (duration * 20)); sendPacket(() -> RobotPacketSender.sendAnimateSwing(this, Settings.get().maxNetworkClientEffectPacketDistance())); } }
    public void animateTurn(boolean clockwise, double duration) { setAnimateTurn(clockwise ? 1 : -1, (int) (duration * 20)); sendPacket(() -> RobotPacketSender.sendAnimateTurn(this, Settings.get().maxNetworkClientEffectPacketDistance())); }
    public void setAnimateMove(BlockPos fromPosition, int ticks) { animationTicksTotal = ticks + 2; prepareForAnimation(); moveFrom = Option$.MODULE$.apply(fromPosition); }
    public void setAnimateSwing(int ticks) { animationTicksTotal = Math.max(ticks, 5); prepareForAnimation(); swingingTool = true; }
    public void setAnimateTurn(int axis, int ticks) { animationTicksTotal = ticks; prepareForAnimation(); turnAxis = axis; }
    private void prepareForAnimation() { animationTicksLeft = animationTicksTotal; moveFrom = Option$.MODULE$.empty(); swingingTool = false; turnAxis = 0; }

    @Override
    public void updateEntity() {
        if (animationTicksLeft > 0 && --animationTicksLeft == 0) { moveFrom = Option$.MODULE$.empty(); swingingTool = false; turnAxis = 0; }
        super.updateEntity();
        if (isServer()) {
            if (getLevel().getGameTime() % Settings.get().tickFrequency() == 0) {
                if (isCreative()) bot.node().changeBuffer(Double.POSITIVE_INFINITY);
                globalBuffer = bot.node().globalBuffer();
                globalBufferSize = bot.node().globalBufferSize();
                info.totalEnergy = (int) globalBuffer;
                info.robotEnergy = (int) bot.node().localBuffer();
                updatePowerInformation();
            }
            if (!appliedToolEnchantments) { appliedToolEnchantments = true; applyToolEnchantments(getItem(0)); }
        } else if (isRunning() && isAnimatingMove()) li.cil.oc.client.Sound.updatePosition(this);
        for (int slot = 0; slot < equipmentInventory.getContainerSize() + mainInventory.getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack != null) try { stack.inventoryTick(getLevel(), !getLevel().isClientSide() ? fakePlayer() : null, slot, slot == 0); }
            catch (NullPointerException ignored) { }
        }
    }

    private void applyToolEnchantments(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        modifiers.forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            AttributeInstance instance = fakePlayer().getAttributes().getInstance(attribute);
            if (instance != null) { instance.removeModifier(modifier.id()); instance.addTransientModifier(modifier); }
        });
    }

    @Override public void updateComputer() { }
    @Override public void onRunningChanged() { super.onRunningChanged(); if (isRunning()) EventHandler.onRobotStart(this); else EventHandler.onRobotStopped(this); }
    @Override public void initialize() { if (isServer()) li.cil.oc.api.Network.joinNewNetwork(node()); }

    @Override
    public void dispose() {
        super.dispose();
        if (isClient()) {
            if (Minecraft.getInstance().screen instanceof li.cil.oc.client.gui.Robot robotGui && robotGui.inventoryContainer().otherInventory() == this) robotGui.onClose();
        } else EventHandler.onRobotStopped(this);
    }

    @Override public void loadComponentsForServer(DataComponentHolder holder) {
        machine().onHostChanged();
        try { bot.loadData(holder); }
        catch (UnrecoverablePersistanceException error) { Robot.<RuntimeException>throwUnchecked(error); }
        new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.OWNER.get())
            .ifPresent(owner -> { ownerName = owner.name(); ownerUUID = owner.id(); });
        selectedTank = new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.SELECTED_TANK.get())
            .map(Number::intValue).orElse(0);
        _isOutputEnabled_$eq(hasRedstoneCard());
        if (isRunning()) EventHandler.onRobotStart(this);
    }

    @Override public synchronized void saveComponentsForServer(MutableDataComponentHolder holder) {
        bot.saveData(holder);
        holder.set(OCComponentsJava.OWNER.get(), new Owner(ownerName, ownerUUID));
        holder.set(OCComponentsJava.SELECTED_TANK.get(), selectedTank);
    }

    @Override public void loadComponentsForClient(DataComponentHolder holder) { super.loadComponentsForClient(holder); loadData(holder); connectComponents(); }
    @Override public void saveComponentsForClient(MutableDataComponentHolder holder) { super.saveComponentsForClient(holder); saveData(holder); }

    @Override public void loadComponentsCommon(DataComponentHolder holder) {
        info.loadData(holder);
        updateInventorySize();
        if (inventorySize > 0) {
            int savedSlot = new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.SELECTED_SLOT.get())
                .map(Number::intValue).orElse(0);
            selectedSlot = Math.max(0, Math.min(savedSlot, mainInventory.getContainerSize() - 1));
        }
        new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.ROBOT_TOTAL_ANIMATION_TIME.get())
            .map(Number::intValue).ifPresent(value -> animationTicksTotal = value);
        new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.ROBOT_CURRENT_ANIMATION.get())
            .filter(value -> value.ticksLeft() > 0)
            .ifPresent(value -> {
                animationTicksLeft = value.ticksLeft();
                moveFrom = value.moveFrom();
                swingingTool = value.swingShovel();
                turnAxis = value.turnAxis();
            });
    }

    @Override public synchronized void saveComponentsCommon(MutableDataComponentHolder holder) {
        info.saveData(holder);
        holder.set(OCComponentsJava.SELECTED_SLOT.get(), selectedSlot);
        boolean animating = isAnimatingMove() || isAnimatingSwing() || isAnimatingTurn();
        if (animating) {
            holder.set(OCComponentsJava.ROBOT_TOTAL_ANIMATION_TIME.get(), animationTicksTotal);
            holder.set(OCComponentsJava.ROBOT_CURRENT_ANIMATION.get(), new RobotCurrentAnimation(animationTicksLeft, moveFrom, swingingTool, (byte) turnAxis));
        } else {
            holder.remove(OCComponentsJava.ROBOT_TOTAL_ANIMATION_TIME.get());
            holder.remove(OCComponentsJava.ROBOT_CURRENT_ANIMATION.get());
        }
    }

    @Override public void onMachineConnect(Node node) {
        super.onConnect(node);
        if (node == node()) { node.connect(bot.node()); ((Connector) node).setLocalBufferSize(0); }
    }
    @Override public void onMachineDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == node()) {
            node.remove();
            bot.node().remove();
            for (int slot = getContainerSize - componentCount(); slot < getContainerSize; slot++) {
                ManagedEnvironment component = getComponentInSlot(slot);
                if (component != null && component.node() != null) component.node().remove();
            }
        }
    }

    @Override public void onItemAdded(int slot, ItemStack stack) {
        if (isServer()) {
            if (isToolSlot(slot)) { applyToolEnchantments(stack); sendPacket(() -> RobotPacketSender.sendInventory(this, slot, stack)); }
            if (isUpgradeSlot(slot)) sendPacket(() -> RobotPacketSender.sendInventory(this, slot, stack));
            if (isFloppySlot(slot)) li.cil.oc.common.Sound.playDiskInsert(this);
            if (isComponentSlot(slot, stack)) { super.onItemAdded(slot, stack); new ExtendedLevelJava(getLevel()).notifyBlocksOfNeighborChange(BlockPositionsJava.apply(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), getLevel()), getBlockState().getBlock(), false); }
            if (isInventorySlot(slot)) machine().signal("inventory_changed", Integer.valueOf(slot - equipmentInventory.getContainerSize() + 1));
        } else super.onItemAdded(slot, stack);
    }

    @Override public void onItemRemoved(int slot, ItemStack stack) {
        super.onItemRemoved(slot, stack);
        if (isServer()) {
            if (isToolSlot(slot)) { applyToolEnchantments(stack); sendPacket(() -> RobotPacketSender.sendInventory(this, slot, ItemStack.EMPTY)); }
            if (isUpgradeSlot(slot)) sendPacket(() -> RobotPacketSender.sendInventory(this, slot, ItemStack.EMPTY));
            if (isFloppySlot(slot)) li.cil.oc.common.Sound.playDiskEject(this);
            if (isInventorySlot(slot)) machine().signal("inventory_changed", Integer.valueOf(slot - equipmentInventory.getContainerSize() + 1));
            if (isComponentSlot(slot, stack)) new ExtendedLevelJava(getLevel()).notifyBlocksOfNeighborChange(BlockPositionsJava.apply(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), getLevel()), getBlockState().getBlock(), false);
        }
    }

    @Override public void setChanged() {
        super.setChanged();
        if (inventorySize >= 0) updateInventorySize();
        else if (isClient() && Minecraft.getInstance().screen instanceof li.cil.oc.client.gui.Robot robotGui && robotGui.inventoryContainer().otherInventory() == this) robotGui.onClose();
        renderingErrored = false;
    }

    @Override public void connectItemNode(Node node) {
        super.connectItemNode(node);
        if (node == null) return;
        if (node.host() instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            for (int slot = getContainerSize - componentCount(); slot < getContainerSize; slot++) {
                ManagedEnvironment env = getComponentInSlot(slot);
                if (env instanceof li.cil.oc.api.internal.Keyboard keyboard) buffer.node().connect(keyboard.node());
                else if (env instanceof GraphicsCard gpu) buffer.node().connect(gpu.node());
            }
        } else if (node.host() instanceof li.cil.oc.api.internal.Keyboard keyboard) {
            for (int slot = getContainerSize - componentCount(); slot < getContainerSize; slot++) {
                ManagedEnvironment env = getComponentInSlot(slot);
                if (env instanceof li.cil.oc.api.internal.TextBuffer buffer) keyboard.node().connect(buffer.node());
            }
        }
    }

    @Override public boolean isComponentSlot(int slot, ItemStack stack) { return slot >= 1 && slot <= info.containers.length || slot >= getContainerSize - componentCount() && slot < getContainerSize; }
    public String containerSlotType(int slot) {
        if (!isContainerSlot(slot)) return Slot.None;
        ItemStack stack = info.containers[slot - 1];
        var driver = Driver.driverFor(stack, getClass());
        return driver instanceof Container container ? container.providedSlot(stack) : Slot.None;
    }
    public int containerSlotTier(int slot) {
        if (!isContainerSlot(slot)) return Tier.None;
        ItemStack stack = info.containers[slot - 1];
        var driver = Driver.driverFor(stack, getClass());
        return driver instanceof Container container ? container.providedTier(stack) : Tier.None;
    }
    public boolean isToolSlot(int slot) { return slot == 0; }
    public boolean isContainerSlot(int slot) { return slot >= 1 && slot <= info.containers.length; }
    public boolean isInventorySlot(int slot) { return slot >= equipmentInventory.getContainerSize() && slot < equipmentInventory.getContainerSize() + mainInventory.getContainerSize(); }
    public boolean isFloppySlot(int slot) { ItemStack stack = getItem(slot); var driver = stack == null || stack.isEmpty() || !isComponentSlot(slot, stack) ? null : Driver.driverFor(stack, getClass()); return driver != null && driver.slot(stack) == Slot.Floppy; }
    public boolean isUpgradeSlot(int slot) { return Slot.Upgrade.equals(containerSlotType(slot)); }

    private int[] allComponentSlots() {
        int[] slots = new int[info.containers.length + componentCount()];
        for (int i = 0; i < info.containers.length; i++) slots[i] = i + 1;
        for (int i = 0; i < componentCount(); i++) slots[info.containers.length + i] = getContainerSize - componentCount() + i;
        return slots;
    }
    private int computeInventorySize() {
        int count = 0;
        for (int slot : allComponentSlots()) {
            ItemStack stack = getItem(slot);
            if (stack == null || stack.isEmpty()) continue;
            var driver = Driver.driverFor(stack, getClass());
            if (driver instanceof li.cil.oc.api.driver.item.Inventory inventory) count += inventory.inventoryCapacity(stack);
        }
        return Math.min(maxInventorySize(), count);
    }

    public synchronized void updateInventorySize() {
        if (updatingInventorySize) return;
        updatingInventorySize = true;
        try {
            int newSize = computeInventorySize();
            if (newSize == inventorySize) return;
            inventorySize = newSize;
            int realSize = equipmentInventory.getContainerSize() + mainInventory.getContainerSize();
            int oldSelected = selectedSlot;
            ArrayList<ItemStack> removed = new ArrayList<>();
            for (int slot = realSize; slot < getContainerSize - componentCount(); slot++) {
                ItemStack stack = getItem(slot);
                setItem(slot, ItemStack.EMPTY);
                if (stack != null && !stack.isEmpty()) removed.add(stack);
            }
            int copyCount = Math.min(getContainerSize, componentCount());
            Optional<li.cil.oc.api.network.ManagedEnvironment>[] oldComponents = ComponentInventorySlotAccessJava.snapshot(this);
            for (int index = 0; index < copyCount; index++) {
                ComponentInventorySlotAccessJava.set(this, realSize + index, oldComponents[getContainerSize - copyCount + index].orElse(null));
            }
            for (int slot = Math.max(0, getContainerSize - componentCount()); slot < getContainerSize; slot++) {
                if (slot < realSize || slot >= realSize + componentCount()) ComponentInventorySlotAccessJava.set(this, slot, null);
            }
            getContainerSize = realSize + componentCount();
            if (getLevel() != null && isServer()) {
                for (ItemStack stack : removed) { player().getInventory().add(stack); spawnStackInWorld(stack, Optional.of(facing())); }
                setSelectedSlot(oldSelected);
            }
        } finally { updatingInventorySize = false; }
    }

    @Override public int getMaxStackSize() { return 64; }
    @Override public ItemStack getItem(int slot) {
        if (slot >= getContainerSize) return null;
        if (slot >= getContainerSize - componentCount()) return info.components[slot - (getContainerSize - componentCount())];
        return super.getItem(slot);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < getContainerSize - componentCount() && (canPlaceItem(slot, stack) || stack.isEmpty())) {
            if (!stack.isEmpty() && stack.getCount() > 1 && isComponentSlot(slot, stack)) {
                super.setItem(slot, stack.split(1));
                if (stack.getCount() > 0 && isServer()) { player().getInventory().add(stack); spawnStackInWorld(stack, Optional.of(facing())); }
            } else super.setItem(slot, stack);
        } else if (!stack.isEmpty() && stack.getCount() > 0 && getLevel() != null && !getLevel().isClientSide()) spawnStackInWorld(stack, Optional.of(Direction.UP));
    }
    @Override public boolean stillValid(Player player) { return super.stillValid(player) && (!isCreative() || player.isCreative()); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) return true;
        var driver = Driver.driverFor(stack, getClass());
        if (isContainerSlot(slot)) return driver != DriverScreen.INSTANCE && driver != DriverKeyboard.INSTANCE && driver != null && driver.slot(stack).equals(containerSlotType(slot)) && driver.tier(stack) <= containerSlotTier(slot);
        return isInventorySlot(slot);
    }

    @Override public Component getDisplayName() { return Component.empty(); }
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) { return new li.cil.oc.common.menu.Robot(id, playerInventory, this, new li.cil.oc.common.menu.RobotInfo(this)); }
    @Override public void forAllLoot(Consumer<ItemStack> dst) {
        ItemStack tool = getItem(0); if (tool != null && tool.getCount() > 0) dst.accept(tool);
        for (int slot : containerSlots()) { ItemStack stack = getItem(slot); if (stack != null && stack.getCount() > 0) dst.accept(stack); }
        InventoryUtils.forAllSlots(mainInventory, dst);
    }
    @Override public boolean dropSlot(int slot, int count, Option<Direction> direction) { return InventoryUtils.dropSlot(BlockPositionsJava.apply(getBlockPos(), getLevel()), mainInventory, slot, count, scala.jdk.javaapi.OptionConverters.toJava(direction)); }
    @Override public void dropAllSlots() {
        InventoryUtils.dropSlot(BlockPositionsJava.apply(getBlockPos(), getLevel()), this, 0, Integer.MAX_VALUE);
        for (int slot : containerSlots()) InventoryUtils.dropSlot(BlockPositionsJava.apply(getBlockPos(), getLevel()), this, slot, Integer.MAX_VALUE);
        InventoryUtils.dropAllSlots(BlockPositionsJava.apply(getBlockPos(), getLevel()), mainInventory);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { for (int eligible : getSlotsForFace(side)) if (eligible == slot) return true; return false; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canTakeItemThroughFace(slot, stack, side) && canPlaceItem(slot, stack); }
    @Override public int[] getSlotsForFace(Direction side) {
        Direction local = toLocal(side);
        if (local == Direction.WEST) return new int[]{0};
        if (local == Direction.EAST) return containerSlots();
        int[] slots = new int[mainInventory.getContainerSize()];
        for (int i = 0; i < slots.length; i++) slots[i] = i + equipmentInventory.getContainerSize();
        return slots;
    }

    public Option<IFluidTank> tryGetTank(int tankIndex) {
        ArrayList<IFluidTank> tanks = new ArrayList<>();
        for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this)) if (component.isPresent() && component.get() instanceof IFluidTank tank) tanks.add(tank);
        return tankIndex < 0 || tankIndex >= tanks.size() ? Option$.MODULE$.empty() : Option$.MODULE$.apply(tanks.get(tankIndex));
    }
    public int tankCount() { int count = 0; for (Optional<ManagedEnvironment> component : ComponentInventorySlotAccessJava.snapshot(this)) if (component.isPresent() && component.get() instanceof IFluidTank) count++; return count; }
    public IFluidTank getFluidTank(int tankIndex) { Option<IFluidTank> tank = tryGetTank(tankIndex); return tank.isDefined() ? tank.get() : null; }
    @Override public int getTanks() { return tankCount(); }
    @Override public FluidStack getFluidInTank(int tankIndex) { IFluidTank t = getFluidTank(selectedTank); return t == null ? FluidStack.EMPTY : t.getFluid(); }
    @Override public int getTankCapacity(int tankIndex) { IFluidTank t = getFluidTank(selectedTank); return t == null ? 0 : t.getCapacity(); }
    @Override public boolean isFluidValid(int tankIndex, FluidStack resource) { return true; }
    @Override public int fill(FluidStack resource, IFluidHandler.FluidAction action) { IFluidTank t = getFluidTank(selectedTank); return t == null ? 0 : t.fill(resource, action); }
    @Override public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) { IFluidTank t = getFluidTank(selectedTank); return t == null || t.getFluid() == null || !FluidStack.isSameFluidSameComponents(t.getFluid(), resource) ? FluidStack.EMPTY : t.drain(resource.getAmount(), action); }
    @Override public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) { IFluidTank t = getFluidTank(selectedTank); return t == null ? FluidStack.EMPTY : t.drain(maxDrain, action); }

    @FunctionalInterface private interface PacketAction { void send() throws java.io.IOException; }
    private static void sendPacket(PacketAction action) { try { action.send(); } catch (java.io.IOException error) { Robot.<RuntimeException>throwUnchecked(error); } }
    @SuppressWarnings("unchecked") private static <T extends Throwable> void throwUnchecked(Throwable error) throws T { throw (T) error; }
}


