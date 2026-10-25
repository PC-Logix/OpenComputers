package li.cil.oc.common.blockentity;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.container.InventoryProxy;
import li.cil.oc.server.RobotPacketSender;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import scala.Option;

import java.util.UUID;
import java.util.function.Consumer;

/** Block entity facade for the robot's movable backing environment. */
public class RobotProxy extends RobotProxyBlockEntity {
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

@Override public li.cil.oc.common.blockentity.traits.RotatableBaseBlockJava rotationState() { return robot.rotationState(); }
    private final li.cil.oc.common.blockentity.traits.PowerInformationJava powerInformationState =
        new li.cil.oc.common.blockentity.traits.PowerInformationJava();
    @Override public li.cil.oc.common.blockentity.traits.PowerInformationJava powerInformationState() {
        return powerInformationState;
    }
    private final Robot robot;
    private final Component componentNode;

    public RobotProxy(net.minecraft.core.BlockPos pos, BlockState state, Robot robot) {
        super(pos, state);
        this.robot = robot;
        this.componentNode = li.cil.oc.api.Network.newNode(this, li.cil.oc.api.network.Visibility.Network)
            .withComponent("robot", li.cil.oc.api.network.Visibility.Neighbors).create();
    }

    public RobotProxy(net.minecraft.core.BlockPos pos, BlockState state) {
        this(pos, state, new Robot(pos, state));
    }

    public Robot robot() { return robot; }

    @Override public Component node() { return componentNode; }
    @Override public Machine machine() { return robot.machine(); }
    @Override public int tier() { return robot.tier(); }
    @Override public InventoryProxy equipmentInventory() { return robot.equipmentInventory(); }
    @Override public InventoryProxy mainInventory() { return robot.mainInventory(); }
    @Override public li.cil.oc.api.internal.MultiTank tank() { return robot.tank(); }
    @Override public int selectedSlot() { return robot.selectedSlot(); }
    @Override public void setSelectedSlot(int index) { robot.setSelectedSlot(index); }
    @Override public int selectedTank() { return robot.selectedTank(); }
    @Override public void setSelectedTank(int index) { robot.setSelectedTank(index); }
    @Override public Player player() { return robot.player(); }
    @Override public String name() { return robot.name(); }
    @Override public void setName(String name) { robot.setName(name); }
    @Override public String ownerName() { return robot.ownerName(); }
    @Override public UUID ownerUUID() { return robot.ownerUUID(); }

    @Override public void connectComponents() { }
    @Override public void disconnectComponents() { }
    @Override public boolean isRunning() { return robot.isRunning(); }
    @Override public void setRunning(boolean value) { robot.setRunning(value); }
    @Override public boolean shouldAnimate() { return robot.shouldAnimate(); }
    @Override public int componentCount() { return robot.componentCount(); }
    @Override public ManagedEnvironment getComponentInSlot(int index) { return robot.getComponentInSlot(index); }
    @Override public void synchronizeSlot(int slot) { robot.synchronizeSlot(slot); }

    @Callback(doc = "function():boolean -- Starts the robot. Returns true if the state changed.")
    public Object[] start(Context context, Arguments args) { return ResultWrapper.result(!machine().isPaused() && machine().start()); }

    @Callback(doc = "function():boolean -- Stops the robot. Returns true if the state changed.")
    public Object[] stop(Context context, Arguments args) { return ResultWrapper.result(machine().stop()); }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the robot is running.")
    public Object[] isRunning(Context context, Arguments args) { return ResultWrapper.result(machine().isRunning()); }

    @Callback(doc = "function(name: string):string -- Sets a new name and returns the old name. Robot must not be running")
    public Object[] setName(Context context, Arguments args) {
        String oldName = robot.name();
        String newName = args.checkString(0);
        if (machine().isRunning()) return ResultWrapper.result(ResultWrapper.unit, "is running");
        setName(newName);
        try { RobotPacketSender.sendNameChange(robot); }
        catch (java.io.IOException error) { RobotProxy.<RuntimeException>throwUnchecked(error); }
        return ResultWrapper.result(oldName);
    }

    @Callback(doc = "function():string -- Returns the robot name.")
    public Object[] getName(Context context, Arguments args) { return ResultWrapper.result(robot.name()); }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("network.message".equals(message.name()) && message.source() != node()) {
            Object[] data = message.data();
            if (data.length == 1 && data[0] instanceof li.cil.oc.api.network.Packet packet)
                robot.node().sendToReachable(message.name(), packet);
        }
    }

    @Override public void updateEntity() { robot.updateEntity(); }

    @Override
    public void clearRemoved() {
        boolean firstProxy = robot.proxy() == null;
        robot.proxy_$eq(this);
        robot.setLevel(getLevel());
        robot.setWorldPosition(getBlockPos());
        super.clearRemoved();
        if (firstProxy) robot.clearRemoved();
        if (isServer()) {
            if (robot.machine().isRunning()) li.cil.oc.common.EventHandler.onRobotStart(robot);
            robot.connectComponents();
            robot.machine().onHostChanged();
            var addressHolder = new li.cil.oc.api.datacomponents.MutableNbtComponentHolder();
            addressHolder.set(OCComponentsJava.ADDRESS.get(), robot.node().address());
            try { node().loadData(addressHolder); }
            catch (li.cil.oc.api.UnrecoverablePersistanceException error) { RobotProxy.<RuntimeException>throwUnchecked(error); }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (robot.proxy() == this) robot.dispose();
    }

    @Override public void loadComponentsCommon(DataComponentHolder holder) { robot.loadComponentsCommon(holder); super.loadComponentsCommon(holder); }
    @Override public void saveComponentsCommon(MutableDataComponentHolder holder) { super.saveComponentsCommon(holder); robot.saveComponentsCommon(holder); }
    @Override public void loadComponentsForServer(DataComponentHolder holder) { super.loadComponentsForServer(holder); robot.loadData(holder); robot.loadComponentsForServer(holder); }
    @Override public void saveComponentsForServer(MutableDataComponentHolder holder) { super.saveComponentsForServer(holder); robot.saveData(holder); robot.saveComponentsCommon(holder); robot.saveComponentsForServer(holder); }
    @Override public void saveData(MutableDataComponentHolder holder) { robot.saveData(holder); }
    @Override public void loadData(DataComponentHolder holder) { robot.loadData(holder); }
    @Override public void loadComponentsForClient(DataComponentHolder holder) { robot.loadComponentsForClient(holder); }
    @Override public void saveComponentsForClient(MutableDataComponentHolder holder) { robot.saveComponentsForClient(holder); }
    @Override public void setChanged() { robot.setChanged(); }

    @Override public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) { return robot.onAnalyze(player, side, hitX, hitY, hitZ); }
    @Override public int[] _input() { return robot._input(); }
    @Override public int[] _output() { return robot._output(); }
    @Override public int[][] _bundledInput() { return robot._bundledInput(); }
    @Override public int[][] _rednetInput() { return robot._rednetInput(); }
    @Override public int[][] _bundledOutput() { return robot._bundledOutput(); }
    @Override public boolean isOutputEnabled() { return robot.isOutputEnabled(); }
    @Override public li.cil.oc.common.blockentity.traits.RedstoneAware setOutputEnabled(boolean value) { return robot.setOutputEnabled(value); }
    @Override public void checkRedstoneInputChanged() { robot.checkRedstoneInputChanged(); }

    @Override public Direction pitch() { return robot.pitch(); }
    @Override public void pitch_$eq(Direction value) { robot.pitch_$eq(value); }
    @Override public Direction yaw() { return robot.yaw(); }
    @Override public void yaw_$eq(Direction value) { robot.yaw_$eq(value); }
    @Override public boolean setFromEntityPitchAndYaw(Entity entity) { return robot.setFromEntityPitchAndYaw(entity); }
    @Override public boolean setFromFacing(Direction value) { return robot.setFromFacing(value); }
    @Override public boolean invertRotation() { return robot.invertRotation(); }
    @Override public Direction facing() { return robot.facing(); }
    @Override public boolean rotate(Direction axis) { return robot.rotate(axis); }
    @Override public Direction toLocal(Direction value) { return robot.toLocal(value); }
    @Override public Direction toGlobal(Direction value) { return robot.toGlobal(value); }

    @Override public ItemStack getItem(int slot) { return robot.getItem(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return robot.removeItem(slot, amount); }
    @Override public void setItem(int slot, ItemStack stack) { robot.setItem(slot, stack); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return robot.removeItemNoUpdate(slot); }
    @Override public void startOpen(Player player) { robot.startOpen(player); }
    @Override public void stopOpen(Player player) { robot.stopOpen(player); }
    @Override public boolean hasCustomName() { return robot.hasCustomName(); }
    @Override public boolean stillValid(Player player) { return robot.stillValid(player); }
    @Override public void forAllLoot(Consumer<ItemStack> dst) { robot.forAllLoot(dst); }
    @Override public boolean dropSlot(int slot, int count, Option<Direction> direction) { return robot.dropSlot(slot, count, scala.jdk.javaapi.OptionConverters.toJava(direction)); }
    @Override public void dropAllSlots() { robot.dropAllSlots(); }
    @Override public int getMaxStackSize() { return robot.getMaxStackSize(); }
    @Override public int componentSlot(String address) { return robot.componentSlot(address); }
    @Override public net.minecraft.network.chat.Component getName() { return robot.getName(); }
    @Override public int getContainerSize() { return robot.getContainerSize(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return robot.canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return robot.canTakeItemThroughFace(slot, stack, side); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return robot.canPlaceItemThroughFace(slot, stack, side); }
    @Override public int[] getSlotsForFace(Direction side) { return robot.getSlotsForFace(side); }
    @Override public boolean hasRedstoneCard() { return robot.hasRedstoneCard(); }
    @Override public double globalBuffer() { return robot.globalBuffer(); }
    @Override public void globalBuffer_$eq(double value) { robot.globalBuffer_$eq(value); }
    @Override public double globalBufferSize() { return robot.globalBufferSize(); }
    @Override public void globalBufferSize_$eq(double value) { robot.globalBufferSize_$eq(value); }
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E { throw (E) error; }

    @Override public int getTanks() { return robot.getTanks(); }
    @Override public FluidStack getFluidInTank(int tank) { return robot.getFluidInTank(tank); }
    @Override public int getTankCapacity(int tank) { return robot.getTankCapacity(tank); }
    @Override public boolean isFluidValid(int tank, FluidStack resource) { return robot.isFluidValid(tank, resource); }
    @Override public int fill(FluidStack resource, IFluidHandler.FluidAction action) { return robot.fill(resource, action); }
    @Override public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) { return robot.drain(resource, action); }
    @Override public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) { return robot.drain(maxDrain, action); }
}



