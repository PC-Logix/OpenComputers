package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.server.SoundEffectPacketSender;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.ExtendedLevelJava;
import li.cil.oc.util.ExtendedNBTJava;
import li.cil.oc.util.InventoryAccessJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.Option;
import scala.runtime.BoxedUnit;

import java.io.IOException;
import java.util.Objects;

/** World userdata exposed by the debug card. The binary name is stable for saved values. */
public class DebugCard$WorldValue extends li.cil.oc.api.prefab.AbstractValue {
    private static final String DIMENSION_TAG = "dimension";

    private Level world;
    private Option<DebugCardAccessContext> ctx;

    public DebugCard$WorldValue(Level world, Option<DebugCardAccessContext> ctx) {
        this.world = world;
        this.ctx = ctx;
    }

    public DebugCard$WorldValue() {
        this(null, Option.empty());
    }

    public Level world() { return world; }
    public void world_$eq(Level value) { world = value; }
    public Option<DebugCardAccessContext> ctx() { return ctx; }
    public void ctx_$eq(Option<DebugCardAccessContext> value) { ctx = value; }

    private void checkAccess() {
        DebugCardSupport.checkAccess(ctx);
    }

    @Deprecated
    @Callback(doc = "function():number -- Gets the numeric id of the current dimension.")
    public Object[] getDimensionId(Context context, Arguments args) {
        checkAccess();
        if (Objects.equals(world.dimension(), Level.OVERWORLD)) return ResultWrapper.result(0);
        if (Objects.equals(world.dimension(), Level.NETHER)) return ResultWrapper.result(-1);
        if (Objects.equals(world.dimension(), Level.END)) return ResultWrapper.result(1);
        throw new Error("deprecated");
    }

    @Deprecated
    @Callback(doc = "function():string -- Gets the name of the current dimension.")
    public Object[] getDimensionName(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(world.dimension().location().toString());
    }

    @Callback(doc = "function():string -- Gets the resource location of the current dimension.")
    public Object[] getDimension(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(world.dimension().location().toString());
    }

    @Callback(doc = "function():number -- Gets the seed of the world.")
    public Object[] getSeed(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(((ServerLevel) world).getSeed());
    }

    @Callback(doc = "function():boolean -- Returns whether it is currently raining.")
    public Object[] isRaining(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(world.isRaining());
    }

    @Callback(doc = "function(value:boolean) -- Sets whether it is currently raining.")
    public Object[] setRaining(Context context, Arguments args) {
        checkAccess();
        world.getLevelData().setRaining(args.checkBoolean(0));
        return null;
    }

    @Callback(doc = "function():boolean -- Returns whether it is currently thundering.")
    public Object[] isThundering(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(world.isThundering());
    }

    @Callback(doc = "function(value:boolean) -- Sets whether it is currently thundering.")
    public Object[] setThundering(Context context, Arguments args) {
        checkAccess();
        ((ServerLevelData) world.getLevelData()).setThundering(args.checkBoolean(0));
        return null;
    }

    @Callback(doc = "function():number -- Get the current world time.")
    public Object[] getTime(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(world.getDayTime());
    }

    @Callback(doc = "function(value:number) -- Set the current world time.")
    public Object[] setTime(Context context, Arguments args) {
        checkAccess();
        ((ServerLevel) world).setDayTime((long) args.checkDouble(0));
        return null;
    }

    @Callback(doc = "function():number, number, number -- Get the current spawn point coordinates.")
    public Object[] getSpawnPoint(Context context, Arguments args) {
        checkAccess();
        BlockPos pos = world.getLevelData().getSpawnPos();
        return ResultWrapper.result(pos.getX(), pos.getY(), pos.getZ());
    }

    @Callback(doc = "function(x:number, y:number, z:number) -- Set the spawn point coordinates.")
    public Object[] setSpawnPoint(Context context, Arguments args) {
        checkAccess();
        int x = args.checkInteger(0);
        int y = args.checkInteger(1);
        int z = args.checkInteger(2);
        ((ServerLevelData) world.getLevelData()).setSpawn(new BlockPos(x, y, z), 0);
        return null;
    }

    @Callback(doc = "function(x:number, y:number, z:number, sound:string, range:number) -- Play a sound at the specified coordinates.")
    public Object[] playSoundAt(Context context, Arguments args) {
        checkAccess();
        int x = args.checkInteger(0);
        int y = args.checkInteger(1);
        int z = args.checkInteger(2);
        ResourceLocation sound = ResourceLocation.tryParse(args.checkString(3));
        int range = args.checkInteger(4);
        try {
            SoundEffectPacketSender.sendEffect(world, x, y, z, sound, SoundSource.MASTER, range);
        } catch (IOException error) {
            throw sneaky(error);
        }
        return null;
    }

    @Deprecated
    @Callback(doc = "function(x:number, y:number, z:number):number -- Get the ID of the block at the specified coordinates.")
    public Object[] getBlockId(Context context, Arguments args) {
        checkAccess();
        Block block = world.getBlockState(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))).getBlock();
        return ResultWrapper.result(BuiltInRegistries.BLOCK.getId(block));
    }

    @Deprecated
    @Callback(doc = "function(x:number, y:number, z:number):number -- Get the metadata of the block at the specified coordinates.")
    public Object[] getMetadata(Context context, Arguments args) {
        checkAccess();
        args.checkInteger(0);
        args.checkInteger(1);
        args.checkInteger(2);
        return ResultWrapper.result(0);
    }

    @Deprecated
    @Callback(doc = "function(x:number, y:number, z:number[, actualState:boolean=false]) - gets the block state for the block at the specified position, optionally getting additional display related data")
    public Object[] getBlockState(Context context, Arguments args) {
        checkAccess();
        BlockPos pos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
        BlockState state = world.getBlockState(pos);
        args.optBoolean(3, false);
        return ResultWrapper.result(state);
    }

    @Callback(doc = "function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates is loaded.")
    public Object[] isLoaded(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(world.isLoaded(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))));
    }

    @Callback(doc = "function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates has a block entity.")
    public Object[] hasBlockEntity(Context context, Arguments args) {
        checkAccess();
        BlockPos pos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
        return ResultWrapper.result(world.getBlockState(pos).hasBlockEntity());
    }

    /** OC1 compatibility alias for the pre-1.17 Minecraft terminology. */
    @Callback(doc = "function(x:number, y:number, z:number):number -- Alias for hasBlockEntity.")
    public Object[] hasTileEntity(Context context, Arguments args) {
        return hasBlockEntity(context, args);
    }

    @Callback(doc = "function(x:number, y:number, z:number):table -- Get the NBT of the block at the specified coordinates.")
    public Object[] getTileNBT(Context context, Arguments args) {
        checkAccess();
        BlockPos pos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
        var blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) return null;
        CompoundTag tag = ExtendedNBTJava.toNbt(nbt -> blockEntity.saveWithFullMetadata(world.registryAccess()));
        return ResultWrapper.result(ExtendedNBTJava.toTypedMap(tag));
    }

    @Callback(doc = "function(x:number, y:number, z:number, nbt:table):boolean -- Set the NBT of the block at the specified coordinates.")
    public Object[] setTileNBT(Context context, Arguments args) {
        checkAccess();
        BlockPos pos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
        var blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) return ResultWrapper.result(BoxedUnit.UNIT, "no tile entity");
        Tag tag = ExtendedNBTJava.typedMapToNbt(args.checkTable(3));
        if (tag instanceof CompoundTag compound) {
            blockEntity.loadWithComponents(compound, world.registryAccess());
            blockEntity.setChanged();
            new ExtendedLevelJava(world).notifyBlockUpdate(pos);
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(BoxedUnit.UNIT, "nbt tag COMPOUND expected, got 'nbt.getType.getName'");
    }

    @Callback(doc = "function(x:number, y:number, z:number):number -- Get the light opacity of the block at the specified coordinates.")
    public Object[] getLightOpacity(Context context, Arguments args) {
        checkAccess();
        BlockPos pos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
        BlockState state = world.getBlockState(pos);
        return ResultWrapper.result(state.getLightBlock(world, pos));
    }

    @Callback(doc = "function(x:number, y:number, z:number):number -- Get the light value (emission) of the block at the specified coordinates.")
    public Object[] getLightValue(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(world.getLightEmission(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))));
    }

    @Callback(doc = "function(x:number, y:number, z:number):number -- Get whether the block at the specified coordinates is directly under the sky.")
    public Object[] canSeeSky(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(world.canSeeSkyFromBelowWater(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))));
    }

    @Deprecated
    private BlockState getStateFromMeta(Block block, int meta) {
        var states = block.getStateDefinition().getPossibleStates();
        return meta >= 0 && meta < states.size() ? states.get(meta) : block.defaultBlockState();
    }

    @Deprecated
    @Callback(doc = "function(x:number, y:number, z:number, id:string, meta:number):number -- Set the block at the specified coordinates.")
    public Object[] setBlock(Context context, Arguments args) {
        checkAccess();
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(args.checkString(3)));
        int metadata = args.checkInteger(4);
        BlockPos pos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
        return ResultWrapper.result(world.setBlockAndUpdate(pos, getStateFromMeta(block, metadata)));
    }

    @Deprecated
    @Callback(doc = "function(x1:number, y1:number, z1:number, x2:number, y2:number, z2:number, id:string, meta:number):number -- Set all blocks in the area defined by the two corner points (x1, y1, z1) and (x2, y2, z2).")
    public Object[] setBlocks(Context context, Arguments args) {
        checkAccess();
        int xMin = args.checkInteger(0), yMin = args.checkInteger(1), zMin = args.checkInteger(2);
        int xMax = args.checkInteger(3), yMax = args.checkInteger(4), zMax = args.checkInteger(5);
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(args.checkString(3)));
        int metadata = args.checkInteger(7);
        int lowX = Math.min(xMin, xMax), highX = Math.max(xMin, xMax);
        int lowY = Math.min(yMin, yMax), highY = Math.max(yMin, yMax);
        int lowZ = Math.min(zMin, zMax), highZ = Math.max(zMin, zMax);
        for (long x = lowX; x <= highX; x++) {
            for (long y = lowY; y <= highY; y++) {
                for (long z = lowZ; z <= highZ; z++) {
                    world.setBlockAndUpdate(new BlockPos((int) x, (int) y, (int) z), getStateFromMeta(block, metadata));
                }
            }
        }
        return null;
    }

    @Deprecated
    @Callback(doc = "function(id:string, count:number, damage:number, nbt:string, x:number, y:number, z:number, side:number):boolean - Insert an item stack into the inventory at the specified location. NBT tag is expected in JSON format.")
    public Object[] insertItem(Context context, Arguments args) {
        checkAccess();
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(args.checkString(0)));
        if (item == null) throw new IllegalArgumentException("invalid item id");
        int count = args.checkInteger(1);
        int damage = args.checkInteger(2);
        String tagJson = args.optString(3, "");
        CompoundTag tag = com.google.common.base.Strings.isNullOrEmpty(tagJson) ? new CompoundTag() : parseTag(tagJson);
        var position = BlockPositionsJava.apply(args.checkDouble(4), args.checkDouble(5), args.checkDouble(6), world);
        var side = new ExtendedArgumentsJava(args).checkSideAny(7);
        var inventory = InventoryAccessJava.inventoryAt(position, side);
        if (inventory.isPresent()) {
            ItemStack stack = new ItemStack(item, count);
            CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
            stack.setDamageValue(damage);
            return ResultWrapper.result(InventoryUtils.insertIntoInventory(stack, inventory.get()));
        }
        return ResultWrapper.result(BoxedUnit.UNIT, "no inventory");
    }

    @Callback(doc = "function(x:number, y:number, z:number, slot:number[, count:number]):number - Reduce the size of an item stack in the inventory at the specified location.")
    public Object[] removeItem(Context context, Arguments args) {
        checkAccess();
        var position = BlockPositionsJava.apply(args.checkDouble(0), args.checkDouble(1), args.checkDouble(2), world);
        var inventory = InventoryAccessJava.anyInventoryAt(position);
        if (inventory.isPresent()) {
            var handler = inventory.get();
            int slot = new ExtendedArgumentsJava(args).checkSlot(handler, 3);
            int count = args.optInteger(4, 64);
            ItemStack removed = handler.extractItem(slot, count, false);
            return ResultWrapper.result(removed.isEmpty() ? 0 : removed.getCount());
        }
        return ResultWrapper.result(BoxedUnit.UNIT, "no inventory");
    }

    @Callback(doc = "function(id:string, amount:number, x:number, y:number, z:number, side:number):boolean - Insert some fluid into the tank at the specified location.")
    public Object[] insertFluid(Context context, Arguments args) {
        checkAccess();
        var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(args.checkString(0)));
        if (fluid == null) throw new IllegalArgumentException("invalid fluid id");
        int amount = args.checkInteger(1);
        var position = BlockPositionsJava.apply(args.checkDouble(2), args.checkDouble(3), args.checkDouble(4), world);
        new ExtendedArgumentsJava(args).checkSideAny(5);
        var blockEntity = new ExtendedLevelJava(world).getBlockEntity(position);
        if (blockEntity instanceof IFluidHandler handler)
            return ResultWrapper.result(handler.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE));
        return ResultWrapper.result(BoxedUnit.UNIT, "no tank");
    }

    @Callback(doc = "function(amount:number, x:number, y:number, z:number, side:number):boolean - Remove some fluid from a tank at the specified location.")
    public Object[] removeFluid(Context context, Arguments args) {
        checkAccess();
        int amount = args.checkInteger(0);
        var position = BlockPositionsJava.apply(args.checkDouble(1), args.checkDouble(2), args.checkDouble(3), world);
        new ExtendedArgumentsJava(args).checkSideAny(4);
        var blockEntity = new ExtendedLevelJava(world).getBlockEntity(position);
        if (blockEntity instanceof IFluidHandler handler)
            return ResultWrapper.result(handler.drain(amount, IFluidHandler.FluidAction.EXECUTE));
        return ResultWrapper.result(BoxedUnit.UNIT, "no tank");
    }

    @Override
    public void loadData(DataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadData(holder, nbt, provider);
        ctx = Option.apply(holder.get(OCComponentsJava.DEBUG_CARD_ACCESS_CONTEXT.get()));
        ResourceLocation id = ResourceLocation.tryParse(nbt.getString(DIMENSION_TAG));
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
        world = ServerLifecycleHooks.getCurrentServer().getLevel(key);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveData(holder, nbt, provider);
        nbt.putString(DIMENSION_TAG, world.dimension().location().toString());
    }

    private static CompoundTag parseTag(String text) {
        try {
            return TagParser.parseTag(text);
        } catch (CommandSyntaxException error) {
            throw sneaky(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T {
        throw (T) error;
    }
}
