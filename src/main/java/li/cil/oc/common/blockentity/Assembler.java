package li.cil.oc.common.blockentity;

import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.template.AssemblerTemplates;
import li.cil.oc.server.RobotPacketSender;
import li.cil.oc.util.ResultWrapper;
import li.cil.oc.util.StackOption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import scala.Option;
import scala.Option$;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.Map;

public class Assembler extends AssemblerEnvironmentBlockEntity {
    @Override public boolean powerLifecycleAfterEnvironment() { return true; }
    private static final String OUTPUT_TAG = Settings.namespace() + "output";
    private static final String OUTPUT_TAG_COMPAT = Settings.namespace() + "robot";
    private static final String TOTAL_TAG = Settings.namespace() + "total";
    private static final String REMAINING_TAG = Settings.namespace() + "remaining";

    private final ComponentConnector node = Network.newNode(this, Visibility.Network)
            .withComponent("assembler").withConnector(Settings.get().bufferConverter()).create();
    private StackOption output = StackOption.empty();
    private double totalRequiredEnergy;
    private double requiredEnergy;
    private Map<String, String> deviceInfo;

    public Assembler(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.ASSEMBLER.get(), pos, state);
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    public StackOption output() {
        return output;
    }

    public void output_$eq(StackOption value) {
        output = value;
    }

    public double totalRequiredEnergy() {
        return totalRequiredEnergy;
    }

    public void totalRequiredEnergy_$eq(double value) {
        totalRequiredEnergy = value;
    }

    public double requiredEnergy() {
        return requiredEnergy;
    }

    public void requiredEnergy_$eq(double value) {
        requiredEnergy = value;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = BlockEntityDeviceInfo.assembler();
        return deviceInfo;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean canConnect(Direction side) {
        return side != Direction.UP;
    }

    @Override
    public ComponentConnector sidedNode(Direction side) {
        return side != Direction.UP ? node : null;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean hasConnector(Direction side) {
        return canConnect(side);
    }

    @Override
    public java.util.Optional<Connector> connectorOptional(Direction side) {
        return java.util.Optional.ofNullable(side != Direction.UP ? node : null);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().assemblerRate();
    }

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        if (isAssembling()) return EnumSet.of(StateAware.State.IsWorking);
        if (canAssemble()) return EnumSet.of(StateAware.State.CanWork);
        return EnumSet.noneOf(StateAware.State.class);
    }

    public boolean canAssemble() {
        var selected = AssemblerTemplates.selectOptional(getItem(0));
        return selected.isPresent() && !isAssembling() && output.isEmpty()
                && selected.get().validate(this)._1();
    }

    public boolean isAssembling() {
        return requiredEnergy > 0;
    }

    public double progress() {
        return (1 - requiredEnergy / totalRequiredEnergy) * 100;
    }

    public int timeRemaining() {
        return (int) (requiredEnergy / Settings.get().assemblerTickAmount() / 20);
    }

    public synchronized boolean start(boolean finishImmediately) {
        var selected = AssemblerTemplates.selectOptional(getItem(0));
        if (selected.isEmpty() || isAssembling() || !output.isEmpty() || !selected.get().validate(this)._1()) return false;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty() && !canPlaceItem(slot, stack)) return false;
        }
        var assembled = selected.get().assemble(this);
        output = StackOption.apply(assembled._1());
        totalRequiredEnergy = finishImmediately ? 0 : Math.max(1, assembled._2());
        requiredEnergy = totalRequiredEnergy;
        sendAssembling(true);
        for (int slot = 0; slot < getContainerSize(); slot++) updateItems(slot, ItemStack.EMPTY);
        setChanged();
        return true;
    }

    public boolean start() {
        return start(false);
    }

    @Callback(doc = "function(): string, number or boolean -- The current state of the assembler, `busy' or `idle', followed by the progress or template validity, respectively.")
    public Object[] status(Context context, Arguments args) {
        if (isAssembling()) return ResultWrapper.result("busy", progress());
        var selected = AssemblerTemplates.selectOptional(getItem(0));
        return ResultWrapper.result("idle", selected.isPresent() && selected.get().validate(this)._1());
    }

    @Callback(doc = "function():boolean -- Start assembling, if possible. Returns whether assembly was started or not.")
    public Object[] start(Context context, Arguments args) {
        return ResultWrapper.result(start());
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!output.isEmpty() && getLevel().getGameTime() % Settings.get().tickFrequency() == 0) {
            double want = Math.max(1, Math.min(requiredEnergy,
                    Settings.get().assemblerTickAmount() * Settings.get().tickFrequency()));
            double have = want + (Settings.get().ignorePower() ? 0 : node.changeBuffer(-want));
            requiredEnergy -= have;
            if (requiredEnergy <= 0) {
                setItem(0, output.get());
                output = StackOption.empty();
                requiredEnergy = 0;
            }
            sendAssembling(have > 0.5 && !output.isEmpty());
        }
    }

    private void sendAssembling(boolean assembling) {
        try {
            RobotPacketSender.sendAssembling(this, assembling);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        if (nbt.contains(OUTPUT_TAG)) output = StackOption.apply(ItemStack.parseOptional(provider, nbt.getCompound(OUTPUT_TAG)));
        else if (nbt.contains(OUTPUT_TAG_COMPAT)) output = StackOption.apply(ItemStack.parseOptional(provider, nbt.getCompound(OUTPUT_TAG_COMPAT)));
        totalRequiredEnergy = nbt.getDouble(TOTAL_TAG);
        requiredEnergy = nbt.getDouble(REMAINING_TAG);
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        if (!output.isEmpty()) nbt.put(OUTPUT_TAG, output.get().save(provider));
        else nbt.remove(OUTPUT_TAG);
        nbt.putDouble(TOTAL_TAG, totalRequiredEnergy);
        nbt.putDouble(REMAINING_TAG, requiredEnergy);
    }

    @Override
    public void loadForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForClient(nbt, provider);
        requiredEnergy = nbt.getDouble(REMAINING_TAG);
    }

    @Override
    public void saveForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForClient(nbt, provider);
        nbt.putDouble(REMAINING_TAG, requiredEnergy);
    }

    @Override
    public int getContainerSize() {
        return 22;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) return !isAssembling() && AssemblerTemplates.selectOptional(stack).isPresent();
        var selected = AssemblerTemplates.selectOptional(getItem(0));
        if (selected.isEmpty()) return false;
        var template = selected.get();
        AssemblerTemplates.AssemblerSlot templateSlot;
        if (slot >= 1 && slot < 4) templateSlot = template.containerSlots()[slot - 1];
        else if (slot >= 4 && slot < 13) templateSlot = template.upgradeSlots()[slot - 4];
        else if (slot >= 13 && slot < 21) templateSlot = template.componentSlots()[slot - 13];
        else templateSlot = AssemblerTemplates.NoSlot;
        return templateSlot.validate(this, slot, stack);
    }

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.menu.Assembler(id, playerInventory, this);
    }
}
