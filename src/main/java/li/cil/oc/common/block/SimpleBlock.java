package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.blockentity.traits.Colored;
import li.cil.oc.common.blockentity.traits.BlockInventory;
import li.cil.oc.common.blockentity.traits.Rotatable;
import li.cil.oc.server.loot.LootFunctions;
import li.cil.oc.util.Color;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.function.Consumer;

public abstract class SimpleBlock extends BaseEntityBlock {
    @Deprecated
    private String unlocalizedName;
    protected final Direction[] validRotations_ = {Direction.UP, Direction.DOWN};

    protected SimpleBlock(Properties properties) {
        super(properties);
        unlocalizedName = super.getDescriptionId();
    }

    @Override
    protected MapCodec<? extends SimpleBlock> codec() {
        return MapCodec.unit(this);
    }

    @Deprecated
    public void setUnlocalizedName(String name) {
        unlocalizedName = "blockentity." + name;
    }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return unlocalizedName;
    }

    public ItemStack createItemStack(int amount) {
        return new ItemStack(this, amount);
    }

    public ItemStack createItemStack() {
        return createItemStack(1);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltipHead(stack, context, tooltip, flag);
        tooltipBody(stack, context, tooltip, flag);
        tooltipTail(stack, context, tooltip, flag);
    }

    protected void tooltipHead(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    }

    protected void tooltipBody(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Tooltip.add(tooltip, flag, getClass().getSimpleName().toLowerCase());
    }

    protected void tooltipTail(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    }

    public Direction getFacing(BlockGetter world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof li.cil.oc.common.blockentity.traits.Rotatable rotatable
                ? rotatable.facing() : Direction.SOUTH;
    }

    public boolean setFacing(Level world, BlockPos pos, Direction value) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof li.cil.oc.common.blockentity.traits.Rotatable rotatable) {
            rotatable.setFromFacing(value);
            return true;
        }
        return false;
    }

    public boolean setRotationFromEntityPitchAndYaw(Level world, BlockPos pos, Entity value) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof li.cil.oc.common.blockentity.traits.Rotatable rotatable) {
            rotatable.setFromEntityPitchAndYaw(value);
            return true;
        }
        return false;
    }

    public Direction toLocal(BlockGetter world, BlockPos pos, Direction value) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof li.cil.oc.common.blockentity.traits.Rotatable rotatable
                ? rotatable.toLocal(value) : value;
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }

    /** Vanilla/Create transform block states directly, separately from the in-place wrench rotation path. */
    @SuppressWarnings("deprecation")
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = state;
        if (state.hasProperty(PropertyRotatable.Facing)) {
            result = result.setValue(PropertyRotatable.Facing, rotation.rotate(state.getValue(PropertyRotatable.Facing)));
        }
        if (state.hasProperty(PropertyRotatable.Yaw)) {
            result = result.setValue(PropertyRotatable.Yaw, rotation.rotate(state.getValue(PropertyRotatable.Yaw)));
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state;
        if (state.hasProperty(PropertyRotatable.Facing)) {
            result = result.setValue(PropertyRotatable.Facing, mirror.mirror(state.getValue(PropertyRotatable.Facing)));
        }
        if (state.hasProperty(PropertyRotatable.Yaw)) {
            result = result.setValue(PropertyRotatable.Yaw, mirror.mirror(state.getValue(PropertyRotatable.Yaw)));
        }
        return result;
    }

    public Direction[] getValidRotations(Level world, BlockPos pos) {
        return validRotations_;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        if (context.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BlockInventory) {
            LootParams.Builder updatedContext = context.withDynamicDrop(LootFunctions.DYN_VOLATILE_CONTENTS, consumer -> {
                if (context.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BlockInventory inventory) {
                    inventory.forAllLoot(consumer);
                }
            });
            return super.getDrops(state, updatedContext);
        }
        return super.getDrops(state, context);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && player.isCreative()
                && world.getBlockEntity(pos) instanceof BlockInventory inventory) {
            inventory.dropAllSlots();
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Deprecated
    public boolean rotateBlock(Level world, BlockPos pos, Direction axis) {
        if (world.getBlockEntity(pos) instanceof Rotatable rotatable && rotatable.rotate(axis)) {
            world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            return true;
        }
        return false;
    }

    public void applyColor(Colored colored, Level world, BlockPos pos, Player player, ItemStack dyeItem) {
        colored.setColor(Color.rgbValues(Color.dyeColor(dyeItem)));
        world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        if (!player.isCreative() && colored.consumesDye()) dyeItem.split(1);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof Colored colored && Color.isDye(stack)) {
            applyColor(colored, level, pos, player, stack);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        boolean handled = localOnBlockActivated(level, pos, player, hand, stack, hitResult.getDirection(),
                (float) (hitResult.getLocation().x - pos.getX()),
                (float) (hitResult.getLocation().y - pos.getY()),
                (float) (hitResult.getLocation().z - pos.getZ()));
        return handled ? ItemInteractionResult.sidedSuccess(level.isClientSide())
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
                                            BlockHitResult hitResult) {
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (world.getBlockEntity(pos) instanceof Colored colored && Color.isDye(heldItem)) {
            applyColor(colored, world, pos, player, heldItem);
            return InteractionResult.sidedSuccess(world.isClientSide());
        }
        BlockPos hitPos = hitResult.getBlockPos();
        float x = (float) hitResult.getLocation().x - hitPos.getX();
        float y = (float) hitResult.getLocation().y - hitPos.getY();
        float z = (float) hitResult.getLocation().z - hitPos.getZ();
        boolean handled = localOnBlockActivated(world, hitPos, player, InteractionHand.MAIN_HAND, heldItem,
                hitResult.getDirection(), x, y, z);
        return handled ? InteractionResult.sidedSuccess(world.isClientSide()) : InteractionResult.PASS;
    }

    @Override
    public net.minecraft.world.MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return this instanceof li.cil.oc.common.block.traits.GUI ? null : super.getMenuProvider(state, level, pos);
    }

    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand,
                                         ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        return this instanceof li.cil.oc.common.block.traits.GUI gui &&
            li.cil.oc.common.block.traits.GUIJava.onBlockActivated(world, pos, player,
                serverPlayer -> gui.openGui(serverPlayer, world, pos), () -> false);
    }
}


