package li.cil.oc.common.blockentity;

import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.template.DisassemblerTemplates;
import li.cil.oc.server.GeneralPacketSender;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import scala.Option;
import scala.Option$;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.ArrayBuffer$;
import scala.jdk.javaapi.CollectionConverters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.Map;

public class Disassembler extends DisassemblerEnvironmentBlockEntity {
    @Override public boolean powerLifecycleAfterEnvironment() { return true; }
    private static final String QUEUE_TAG = Settings.namespace() + "queue";
    private static final String BUFFER_TAG = Settings.namespace() + "buffer";
    private static final String TOTAL_TAG = Settings.namespace() + "total";
    private static final String ACTIVE_TAG = Settings.namespace() + "isActive";

    private final Connector node = Network.newNode(this, Visibility.None)
            .withConnector(Settings.get().bufferConverter()).create();
    private boolean active;
    private final ArrayBuffer<ItemStack> queue = ArrayBuffer$.MODULE$.empty();
    private double totalRequiredEnergy;
    private double buffer;
    private boolean disassembleNextInstantly;
    private Map<String, String> deviceInfo;

    public Disassembler(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.DISASSEMBLER.get(), pos, state);
    }

    @Override
    public Connector node() {
        return node;
    }

    public boolean isActive() {
        return active;
    }

    public void isActive_$eq(boolean value) {
        active = value;
    }

    public ArrayBuffer<ItemStack> queue() {
        return queue;
    }

    public double totalRequiredEnergy() {
        return totalRequiredEnergy;
    }

    public void totalRequiredEnergy_$eq(double value) {
        totalRequiredEnergy = value;
    }

    public double buffer() {
        return buffer;
    }

    public void buffer_$eq(double value) {
        buffer = value;
    }

    public boolean disassembleNextInstantly() {
        return disassembleNextInstantly;
    }

    public void disassembleNextInstantly_$eq(boolean value) {
        disassembleNextInstantly = value;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public double progress() {
        return queue.isEmpty() ? 0 : (1 - (queue.size() * Settings.get().disassemblerItemCost() - buffer)
                / totalRequiredEnergy) * 100;
    }

    private void setActive(boolean value) {
        if (value == active) return;
        active = value;
        try {
            GeneralPacketSender.sendDisassemblerActive(this, active);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = BlockEntityDeviceInfo.disassembler();
        return deviceInfo;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean hasConnector(Direction side) {
        return side != Direction.UP;
    }

    @Override
    public java.util.Optional<Connector> connectorOptional(Direction side) {
        return java.util.Optional.ofNullable(side != Direction.UP ? node : null);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().disassemblerRate();
    }

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        if (active) return EnumSet.of(StateAware.State.IsWorking);
        if (!queue.isEmpty()) return EnumSet.of(StateAware.State.CanWork);
        return EnumSet.noneOf(StateAware.State.class);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!isServer() || getLevel().getGameTime() % Settings.get().tickFrequency() != 0) return;
        if (queue.isEmpty()) {
            boolean instant = disassembleNextInstantly;
            disassemble(removeItem(0, 1), instant);
            disassembleNextInstantly = instant && !queue.isEmpty();
            setActive(!queue.isEmpty());
        } else {
            if (buffer < Settings.get().disassemblerItemCost()) {
                double want = Settings.get().disassemblerTickAmount();
                boolean success = node.tryChangeBuffer(-want);
                setActive(success);
                if (success) buffer += want;
            }
            while (buffer >= Settings.get().disassemblerItemCost() && !queue.isEmpty()) {
                buffer -= Settings.get().disassemblerItemCost();
                ItemStack stack = queue.remove(0);
                if (disassembleNextInstantly || getLevel().random.nextDouble() >= Settings.get().disassemblerBreakChance()) {
                    drop(stack);
                }
            }
            if (queue.isEmpty()) disassembleNextInstantly = false;
        }
    }

    public void disassemble(ItemStack stack) {
        disassemble(stack, false);
    }

    public void disassemble(ItemStack stack, boolean instant) {
        if (canPlaceItem(0, stack)) {
            ItemStack[] ingredients = ItemUtils.getIngredients(getLevel().getRecipeManager(), stack);
            var selected = DisassemblerTemplates.selectOptional(stack);
            if (selected.isPresent()) {
                var result = selected.get().disassembleOptional(stack, ingredients);
                if (result.items().isPresent()) {
                    for (ItemStack item : result.items().get()) queue.addOne(item);
                    if (result.drops().isPresent()) for (ItemStack item : result.drops().get()) drop(item);
                } else {
                    drop(stack);
                }
            } else {
                for (ItemStack item : ingredients) queue.addOne(item);
            }
            totalRequiredEnergy = queue.size() * Settings.get().disassemblerItemCost();
            if (instant) buffer = totalRequiredEnergy;
        } else {
            drop(stack);
        }
    }

    private void drop(ItemStack stack) {
        if (stack.isEmpty()) return;
        for (Direction side : Direction.values()) {
            if (stack.getCount() <= 0) break;
            InventoryUtils.insertIntoInventoryAt(stack, BlockPositionsJava.apply(this).offset(side),
                    java.util.Optional.of(side.getOpposite()));
        }
        if (stack.getCount() > 0) spawnStackInWorld(stack, java.util.Optional.of(Direction.UP));
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        queue.clear();
        ListTag saved = nbt.getList(QUEUE_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < saved.size(); i++) queue.addOne(ItemStack.parseOptional(provider, saved.getCompound(i)));
        buffer = nbt.getDouble(BUFFER_TAG);
        totalRequiredEnergy = nbt.getDouble(TOTAL_TAG);
        active = !queue.isEmpty();
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        ListTag saved = new ListTag();
        for (ItemStack stack : CollectionConverters.asJava(queue)) saved.add(stack.save(provider));
        nbt.put(QUEUE_TAG, saved);
        nbt.putDouble(BUFFER_TAG, buffer);
        nbt.putDouble(TOTAL_TAG, totalRequiredEnergy);
    }

    @Override
    public void loadForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForClient(nbt, provider);
        active = nbt.getBoolean(ACTIVE_TAG);
    }

    @Override
    public void saveForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForClient(nbt, provider);
        nbt.putBoolean(ACTIVE_TAG, active);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return allowDisassembling(stack) &&
                (((Settings.get().disassembleAllTheThings() || Items.get(stack) != null)
                    && ItemUtils.getIngredients(getLevel().getRecipeManager(), stack).length > 0)
                    || DisassemblerTemplates.selectOptional(stack).isPresent());
    }

    private boolean allowDisassembling(ItemStack stack) {
        return !stack.isEmpty() && (!stack.has(DataComponents.CUSTOM_DATA)
                || !stack.get(DataComponents.CUSTOM_DATA).getUnsafe()
                    .getBoolean(Settings.namespace() + "undisassemblable"));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (!getLevel().isClientSide()) disassembleNextInstantly = false;
    }

    @Override
    public void onSetInventorySlotContents(Player player, int slot, ItemStack stack) {
        if (!getLevel().isClientSide()) disassembleNextInstantly = !stack.isEmpty() && slot == 0 && player.isCreative();
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.menu.Disassembler(id, playerInventory, this);
    }
}
