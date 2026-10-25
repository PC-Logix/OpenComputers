package li.cil.oc.common.blockentity;

import li.cil.oc.Settings;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.item.data.PrintDataSupport;
import li.cil.oc.common.item.data.PrintShape;
import li.cil.oc.server.BlockSyncPacketSender;
import li.cil.oc.util.ResultWrapper;
import li.cil.oc.util.StackOption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import scala.Option;
import scala.Option$;
import java.util.Optional;
import scala.Tuple2;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;

/** Printer component, inventory, and persisted in-progress job state. */
public class Printer extends PrinterBlockEntity {
    private final ComponentConnector node;
    private final int maxAmountMaterial = 256000;
    private int amountMaterial;
    private final int maxAmountInk = 100000;
    private int amountInk;
    private PrintData data = new PrintData();
    private boolean isActive;
    private int limit;
    private StackOption output = StackOption.empty();
    private double totalRequiredEnergy;
    private double requiredEnergy;
    private final int slotMaterial = 0;
    private final int slotInk = 1;
    private final int slotOutput = 2;
    private Map<String, String> deviceInfo;

    private static final String AmountMaterialTag = Settings.namespace() + "amountMaterial";
    private static final String AmountInkTag = Settings.namespace() + "amountInk";
    private static final String DataTag = Settings.namespace() + "data";
    private static final String IsActiveTag = Settings.namespace() + "active";
    private static final String LimitTag = Settings.namespace() + "limit";
    private static final String OutputTag = Settings.namespace() + "output";
    private static final String TotalTag = Settings.namespace() + "total";
    private static final String RemainingTag = Settings.namespace() + "remaining";

    public Printer(BlockPos pos, BlockState state) {
        super(pos, state);
        node = li.cil.oc.api.Network.newNode(this, Visibility.Network)
                .withComponent("printer3d").withConnector(Settings.get().bufferConverter()).create();
    }

    public ComponentConnector node() { return node; }
    public int maxAmountMaterial() { return maxAmountMaterial; }
    public int amountMaterial() { return amountMaterial; }
    public void amountMaterial_$eq(int value) { amountMaterial = value; }
    public int maxAmountInk() { return maxAmountInk; }
    public int amountInk() { return amountInk; }
    public void amountInk_$eq(int value) { amountInk = value; }
    public PrintData data() { return data; }
    public void data_$eq(PrintData value) { data = value; }
    public boolean isActive() { return isActive; }
    public void isActive_$eq(boolean value) { isActive = value; }
    public int limit() { return limit; }
    public void limit_$eq(int value) { limit = value; }
    public StackOption output() { return output; }
    public void output_$eq(StackOption value) { output = value; }
    public double totalRequiredEnergy() { return totalRequiredEnergy; }
    public void totalRequiredEnergy_$eq(double value) { totalRequiredEnergy = value; }
    public double requiredEnergy() { return requiredEnergy; }
    public void requiredEnergy_$eq(double value) { requiredEnergy = value; }
    public int slotMaterial() { return slotMaterial; }
    public int slotInk() { return slotInk; }
    public int slotOutput() { return slotOutput; }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = BlockEntityDeviceInfo.printer();
        return deviceInfo;
    }

    @OnlyIn(Dist.CLIENT)
    @Override public boolean canConnect(Direction side) { return side != Direction.UP; }

    @Override public ComponentConnector sidedNode(Direction side) { return side != Direction.UP ? node : null; }

    @Override
    public java.util.EnumSet<StateAware.State> getCurrentState() {
        if (isPrinting()) return EnumSet.of(StateAware.State.IsWorking);
        if (canPrint()) return EnumSet.of(StateAware.State.CanWork);
        return EnumSet.noneOf(StateAware.State.class);
    }

    public boolean canPrint() {
        return data.stateOff().nonEmpty() && data.stateOff().size() <= Settings.get().maxPrintComplexity()
                && data.stateOn().size() <= Settings.get().maxPrintComplexity();
    }
    public boolean isPrinting() { return output.isDefined(); }
    public double progress() { return (1 - requiredEnergy / totalRequiredEnergy) * 100; }
    public int timeRemaining() { return (int) (requiredEnergy / Settings.get().assemblerTickAmount() / 20); }

    @Callback(doc = "function() -- Resets the configuration of the printer and stop printing (current job will finish).")
    public Object[] reset(Context context, Arguments args) {
        data = new PrintData();
        isActive = false;
        return null;
    }

    @Callback(doc = "function(value:string) -- Set a label for the block being printed.")
    public Object[] setLabel(Context context, Arguments args) {
        String value = args.optString(0, null);
        data.labelOptional_$eq(Optional.ofNullable(value).map(text -> text.substring(0, Math.min(24, text.length()))));
        if (data.labelOptional().filter(String::isEmpty).isPresent()) data.labelOptional_$eq(Optional.empty());
        isActive = false;
        return null;
    }
    @Callback(doc = "function():string -- Get the current label for the block being printed.")
    public Object[] getLabel(Context context, Arguments args) { return ResultWrapper.result(data.labelOptional().orElse(null)); }

    @Callback(doc = "function(value:string) -- Set a tooltip for the block being printed.")
    public Object[] setTooltip(Context context, Arguments args) {
        String value = args.optString(0, null);
        data.tooltipOptional_$eq(Optional.ofNullable(value).map(text -> text.substring(0, Math.min(128, text.length()))));
        if (data.tooltipOptional().filter(String::isEmpty).isPresent()) data.tooltipOptional_$eq(Optional.empty());
        isActive = false;
        return null;
    }
    @Callback(doc = "function():string -- Get the current tooltip for the block being printed.")
    public Object[] getTooltip(Context context, Arguments args) { return ResultWrapper.result(data.tooltipOptional().orElse(null)); }

    @Callback(doc = "function(value:number) -- Set what light level the printed block should have.")
    public Object[] setLightLevel(Context context, Arguments args) {
        data.lightLevel_$eq(Math.max(0, Math.min(args.checkInteger(0), Settings.get().maxPrintLightLevel())));
        isActive = false;
        return null;
    }
    @Callback(doc = "function():number -- Get which light level the printed block should have.")
    public Object[] getLightLevel(Context context, Arguments args) { return ResultWrapper.result(data.lightLevel()); }

    @Callback(doc = "function(value:boolean or number) -- Set whether the printed block should emit redstone when in its active state.")
    public Object[] setRedstoneEmitter(Context context, Arguments args) {
        data.redstoneLevel_$eq(args.isBoolean(0) ? (args.checkBoolean(0) ? 15 : 0) : Math.max(0, Math.min(args.checkInteger(0), 15)));
        isActive = false;
        return null;
    }
    @Callback(doc = "function():boolean, number -- Get whether the printed block should emit redstone when in its active state.")
    public Object[] isRedstoneEmitter(Context context, Arguments args) { return ResultWrapper.result(data.emitRedstone(), data.redstoneLevel()); }

    @Callback(doc = "function(value:boolean) -- Set whether the printed block should automatically return to its off state.")
    public Object[] setButtonMode(Context context, Arguments args) { data.isButtonMode_$eq(args.checkBoolean(0)); isActive = false; return null; }
    @Callback(doc = "function():boolean -- Get whether the printed block should automatically return to its off state.")
    public Object[] isButtonMode(Context context, Arguments args) { return ResultWrapper.result(data.isButtonMode()); }

    @Callback(doc = "function(collideOff:boolean, collideOn:boolean) -- Set whether the printed block should be collidable or not.")
    public Object[] setCollidable(Context context, Arguments args) {
        data.noclipOff_$eq(!args.checkBoolean(0)); data.noclipOn_$eq(!args.checkBoolean(1)); return null;
    }
    @Callback(doc = "function():boolean, boolean -- Get whether the printed block should be collidable or not.")
    public Object[] isCollidable(Context context, Arguments args) { return ResultWrapper.result(!data.noclipOff(), !data.noclipOn()); }

    @Callback(doc = "function(minX:number, minY:number, minZ:number, maxX:number, maxY:number, maxZ:number, texture:string[, state:boolean=false][,tint:number]) -- Adds a shape to the printers configuration, optionally specifying whether it is for the off or on state.")
    public Object[] addShape(Context context, Arguments args) {
        if (data.stateOff().size() > Settings.get().maxPrintComplexity() || data.stateOn().size() > Settings.get().maxPrintComplexity())
            return ResultWrapper.result(ResultWrapper.unit, "model too complex");
        float minX = Math.max(0, Math.min(args.checkInteger(0), 16)) / 16f;
        float minY = Math.max(0, Math.min(args.checkInteger(1), 16)) / 16f;
        float minZ = (16 - Math.max(0, Math.min(args.checkInteger(2), 16))) / 16f;
        float maxX = Math.max(0, Math.min(args.checkInteger(3), 16)) / 16f;
        float maxY = Math.max(0, Math.min(args.checkInteger(4), 16)) / 16f;
        float maxZ = (16 - Math.max(0, Math.min(args.checkInteger(5), 16))) / 16f;
        String rawTexture = args.checkString(6);
        String texture = rawTexture.substring(0, Math.min(64, rawTexture.length()));
        boolean state = args.isBoolean(7) && args.checkBoolean(7);
        Option<Integer> tint = args.isInteger(7) ? Option$.MODULE$.apply(args.checkInteger(7))
                : args.isInteger(8) ? Option$.MODULE$.apply(args.checkInteger(8)) : Option$.MODULE$.empty();
        if (minX == maxX || minY == maxY || minZ == maxZ) throw new IllegalArgumentException("empty block");
        dataSet(state).add(new PrintShape(new AABB(Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ),
                Math.max(maxX, minX), Math.max(maxY, minY), Math.max(maxZ, minZ)), texture, tint));
        isActive = false;
        getLevel().sendBlockUpdated(getBlockPos(), getLevel().getBlockState(getBlockPos()), getLevel().getBlockState(getBlockPos()), 3);
        return ResultWrapper.result(true);
    }
    private scala.collection.mutable.Set<PrintShape> dataSet(boolean state) { return state ? data.stateOn() : data.stateOff(); }

    @Callback(doc = "function():number -- Get the number of shapes in the current configuration.")
    public Object[] getShapeCount(Context context, Arguments args) { return ResultWrapper.result(data.stateOff().size(), data.stateOn().size()); }
    @Callback(doc = "function():number -- Get the maximum allowed number of shapes.")
    public Object[] getMaxShapeCount(Context context, Arguments args) { return ResultWrapper.result(Settings.get().maxPrintComplexity()); }
    @Callback(doc = "function([count:number]):boolean -- Commit and begin printing the current configuration.")
    public Object[] commit(Context context, Arguments args) {
        if (!canPrint()) return ResultWrapper.result(ResultWrapper.unit, "model invalid");
        limit = (int) Math.max(0, Math.min(args.optDouble(0, 1), Integer.MAX_VALUE));
        isActive = limit > 0;
        return ResultWrapper.result(true);
    }
    @Callback(doc = "function(): string, number or boolean -- The current state of the printer, `busy' or `idle', followed by the progress or model validity, respectively.")
    public Object[] status(Context context, Arguments args) {
        if (isPrinting()) return ResultWrapper.result("busy", progress());
        return ResultWrapper.result("idle", canPrint());
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isClient()) return;
        if (isActive && output.isEmpty() && canMergeOutput()) {
            Optional<Tuple2<Integer, Integer>> costs = PrintDataSupport.computeCostsOptional(data);
            if (costs.isPresent()) {
                int materialRequired = costs.get()._1(), inkRequired = costs.get()._2();
                totalRequiredEnergy = Settings.get().printCost();
                requiredEnergy = totalRequiredEnergy;
                if (amountMaterial >= materialRequired && amountInk >= inkRequired) {
                    amountMaterial -= materialRequired; amountInk -= inkRequired; limit--;
                    output = StackOption.apply(data.createItemStack());
                    if (limit < 1) isActive = false;
                    sendPrinting(true);
                }
            } else {
                isActive = false;
                data = new PrintData();
            }
        }
        if (output.isDefined()) {
            double want = Math.max(1, Math.min(requiredEnergy, Settings.get().printerTickAmount()));
            double have = want + (Settings.get().ignorePower() ? 0 : node.changeBuffer(-want));
            requiredEnergy -= have;
            if (requiredEnergy <= 0) {
                ItemStack result = getItem(slotOutput);
                if (result.isEmpty()) setItem(slotOutput, output.get());
                else if (result.getCount() < result.getMaxStackSize() && canMergeOutput()) { result.grow(1); setChanged(); }
                else return;
                requiredEnergy = 0;
                output = StackOption.empty();
            }
            sendPrinting(have > 0.5 && output.isDefined());
        }
        int inputValue = PrintDataSupport.materialValue(getItem(slotMaterial));
        if (inputValue > 0 && maxAmountMaterial - amountMaterial >= inputValue) {
            ItemStack material = removeItem(slotMaterial, 1);
            if (material != null) amountMaterial += inputValue;
        }
        int inkValue = PrintDataSupport.inkValue(getItem(slotInk));
        if (inkValue > 0 && maxAmountInk - amountInk >= inkValue) {
            ItemStack material = removeItem(slotInk, 1);
            if (material != null) {
                amountInk += inkValue;
                if (material.hasCraftingRemainingItem()) setItem(slotInk, material.getCraftingRemainingItem());
            }
        }
    }

    private boolean canMergeOutput() {
        ItemStack presentStack = getItem(slotOutput), outputStack = data.createItemStack();
        return presentStack.isEmpty() || ItemStack.matches(presentStack, outputStack);
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        amountMaterial = nbt.getInt(AmountMaterialTag); amountInk = nbt.getInt(AmountInkTag);
        loadPrintData(nbt, provider);
        isActive = nbt.getBoolean(IsActiveTag); limit = nbt.getInt(LimitTag);
        output = nbt.contains(OutputTag) ? StackOption.apply(ItemStack.parseOptional(provider, nbt.getCompound(OutputTag))) : StackOption.empty();
        totalRequiredEnergy = nbt.getDouble(TotalTag); requiredEnergy = nbt.getDouble(RemainingTag);
    }
    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        nbt.putInt(AmountMaterialTag, amountMaterial); nbt.putInt(AmountInkTag, amountInk);
        CompoundTag tag = new CompoundTag(); data.saveData(tag, provider); nbt.put(DataTag, tag);
        nbt.putBoolean(IsActiveTag, isActive); nbt.putInt(LimitTag, limit);
        if (output.isDefined()) nbt.put(OutputTag, output.get().save(provider));
        nbt.putDouble(TotalTag, totalRequiredEnergy); nbt.putDouble(RemainingTag, requiredEnergy);
    }
    @Override
    public void loadForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForClient(nbt, provider);
        loadPrintData(nbt, provider); requiredEnergy = nbt.getDouble(RemainingTag);
    }
    @Override
    public void saveForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForClient(nbt, provider);
        CompoundTag tag = new CompoundTag(); data.saveData(tag, provider); nbt.put(DataTag, tag);
        nbt.putDouble(RemainingTag, requiredEnergy);
    }

    @Override public int getContainerSize() { return 3; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == slotMaterial ? PrintDataSupport.materialValue(stack) > 0
                : slot == slotInk && PrintDataSupport.inkValue(stack) > 0;
    }
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new li.cil.oc.common.menu.Printer(id, inventory, this);
    }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{slotMaterial, slotInk, slotOutput}; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return !canPlaceItem(slot, stack); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot != slotOutput; }

    private void sendPrinting(boolean printing) {
        try { BlockSyncPacketSender.sendPrinting(this, printing); }
        catch (IOException exception) { Printer.<RuntimeException>throwUnchecked(exception); }
    }
    private void loadPrintData(CompoundTag nbt, HolderLookup.Provider provider) {
        try { data.loadData(nbt.getCompound(DataTag), provider); }
        catch (UnrecoverablePersistanceException exception) {
            throw new IllegalStateException("Unable to restore printer data", exception);
        }
    }
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable exception) throws T { throw (T) exception; }
}
