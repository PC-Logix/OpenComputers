package li.cil.oc.common.blockentity;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Items;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.datacomponents.VideoMode;
import li.cil.oc.util.ExtendedDataComponentHolderSupportJava;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Option$;

public class HoloScreen extends HoloScreenBlockEntity {
    private static final String KEYBOARD_TAG = Settings.namespace() + "keyboard";
    private static final String HAS_INTERNAL_KEYBOARD_TAG = Settings.namespace() + "hasInternalKeyboard";
    private static final int KEYBOARD_SLOT = 0;

    private Option<Keyboard> internalKeyboard = Option$.MODULE$.empty();
    private boolean clientHasInternalKeyboard;

    public HoloScreen(BlockPos pos, BlockState state, int tier) {
        super(pos, state, tier);
        shouldCheckForMultiBlock_$eq(false);
        delayUntilCheckForMultiBlock_$eq(0);
    }

    @Override
    public Direction facing() {
        return mount();
    }

    public Direction mount() {
        return getLevel() != null && getLevel().isLoaded(getBlockPos())
                ? getBlockState().getValue(PropertyRotatable.Mount) : Direction.UP;
    }

    @Override
    public boolean hasKeyboard() {
        return hasInternalKeyboard() || super.hasKeyboard();
    }

    public boolean hasInternalKeyboard() {
        return isClient() ? clientHasInternalKeyboard : !getItem(KEYBOARD_SLOT).isEmpty();
    }

    @Override public String inventoryName() { return "holoscreen"; }
    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public int getInventoryStackRequired() { return 1; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == KEYBOARD_SLOT && Items.get(stack) == Items.get(Constants.BlockName$.MODULE$.Keyboard());
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.menu.HoloScreen(id, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(Settings.namespace() + "container.holoscreen");
    }

    @Override
    public void checkMultiBlock() {
        shouldCheckForMultiBlock_$eq(false);
        origin_$eq(this);
        screensJava().clear();
        screensJava().add(this);
    }

    public boolean resize(Direction operation) {
        int oldWidth = width();
        int oldHeight = height();
        switch (operation) {
            case UP -> height_$eq(Math.min(oldHeight + 1, Settings.get().maxScreenHeight()));
            case DOWN -> height_$eq(Math.max(oldHeight - 1, 1));
            case EAST -> width_$eq(Math.min(oldWidth + 1, Settings.get().maxScreenWidth()));
            case WEST -> width_$eq(Math.max(oldWidth - 1, 1));
            default -> { }
        }
        if (oldWidth != width() || oldHeight != height()) {
            buffer().setAspectRatio(width(), height());
            setChanged();
            if (getLevel() != null && !getLevel().isClientSide()) {
                getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            return true;
        }
        return false;
    }

    public Direction resizeOperationForWorldSide(Direction side) {
        if (side == screenTopSide()) return Direction.UP;
        if (side == screenTopSide().getOpposite()) return Direction.DOWN;
        if (side == screenRight()) return Direction.EAST;
        if (side == screenRight().getOpposite()) return Direction.WEST;
        return Direction.UP;
    }

    public float projectionBeamAnchor(int screenHeight) { return isCeilingMounted() ? screenHeight : screenHeight - 1f; }
    public float projectionBeamProjectorEdgeOffset() { return isCeilingMounted() ? -0.6f : 0.6f; }
    public float projectionPlaneLeft(int screenWidth) { return -((screenWidth - 1) / 2f); }
    public float projectionPlaneTop(int screenHeight) { return isCeilingMounted() ? screenHeight : -1f; }
    public float projectionDepth() { return -0.46f; }

    private void updateInternalKeyboard() {
        boolean shouldHaveKeyboard = hasInternalKeyboard();
        if (shouldHaveKeyboard && internalKeyboard.isEmpty() && isServer()) {
            ItemStack keyboardItem = Items.get(Constants.BlockName$.MODULE$.Keyboard()).createItemStack(1);
            internalKeyboard = Option$.MODULE$.apply((Keyboard) Driver.driverFor(keyboardItem, HoloScreen.class)
                    .createEnvironment(keyboardItem, this));
            connectInternalKeyboard();
        } else if (!shouldHaveKeyboard && !internalKeyboard.isEmpty()) {
            internalKeyboard.get().node().remove();
            internalKeyboard = Option$.MODULE$.empty();
        }
    }

    private void connectInternalKeyboard() {
        if (isServer() && isConnected() && !internalKeyboard.isEmpty()) {
            buffer().node().connect(internalKeyboard.get().node());
        }
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == node()) connectInternalKeyboard();
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        super.onItemAdded(slot, stack);
        updateInternalKeyboard();
        cachedBounds_$eq(Option$.MODULE$.empty());
        syncToClient();
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        super.onItemRemoved(slot, stack);
        updateInternalKeyboard();
        cachedBounds_$eq(Option$.MODULE$.empty());
        syncToClient();
    }

    @Override
    public void dispose() {
        if (!internalKeyboard.isEmpty()) internalKeyboard.get().node().remove();
        internalKeyboard = Option$.MODULE$.empty();
        super.dispose();
    }

    @Override
    public void loadForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForServer(nbt, provider);
        updateInternalKeyboard();
        if (!internalKeyboard.isEmpty()) {
            try {
                internalKeyboard.get().loadData(nbt.getCompound(KEYBOARD_TAG), provider);
            } catch (UnrecoverablePersistanceException e) {
                throw new IllegalStateException("Unable to restore holographic screen keyboard", e);
            }
        }
    }

    @Override
    public void saveForServer(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForServer(nbt, provider);
        if (!internalKeyboard.isEmpty()) {
            CompoundTag keyboardTag = new CompoundTag();
            internalKeyboard.get().saveData(keyboardTag, provider);
            nbt.put(KEYBOARD_TAG, keyboardTag);
        }
    }

    @Override
    public void loadForClient(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadForClient(nbt, provider);
        clientHasInternalKeyboard = nbt.getBoolean(HAS_INTERNAL_KEYBOARD_TAG);
    }

    @Override
    public void saveForClientDirect(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveForClientDirect(nbt, provider);
        nbt.putBoolean(HAS_INTERNAL_KEYBOARD_TAG, hasInternalKeyboard());
    }

    @Override
    public void loadComponentsCommon(DataComponentHolder holder) {
        super.loadComponentsCommon(holder);
        ExtendedDataComponentHolderSupportJava.convert(holder)
            .getComponentOptional(OCComponentsJava.VIDEO_MODE.get()).ifPresent(mode -> {
            width_$eq(Math.min(Math.max(mode.width(), 1), Settings.get().maxScreenWidth()));
            height_$eq(Math.min(Math.max(mode.height(), 1), Settings.get().maxScreenHeight()));
            checkMultiBlock();
        });
    }

    @Override
    public void loadComponentsForServer(DataComponentHolder holder) {
        super.loadComponentsForServer(holder);
        updateInternalKeyboard();
    }

    @Override
    public void saveComponentsCommon(MutableDataComponentHolder holder) {
        super.saveComponentsCommon(holder);
        ExtendedDataComponentHolderSupportJava.convert(holder)
                .setComponent(OCComponentsJava.VIDEO_MODE.get(), new VideoMode(width(), height()));
    }

    @Override
    public AABB getRenderBoundingBox() {
        double horizontalRange = Math.max(width(), 1) + 1.0;
        double verticalRange = Math.max(height(), 1) + 1.0;
        double minY = isCeilingMounted() ? yPosition() - verticalRange : yPosition();
        double maxY = isCeilingMounted() ? yPosition() + 1.0 : yPosition() + 1.0 + verticalRange;
        return new AABB(xPosition() - horizontalRange, minY, zPosition() - horizontalRange,
                xPosition() + 1.0 + horizontalRange, maxY, zPosition() + 1.0 + horizontalRange).inflate(0.25);
    }

    private void syncToClient() {
        if (getLevel() != null && !getLevel().isClientSide()) {
            setChanged();
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private boolean isCeilingMounted() { return mount() == Direction.DOWN; }
    private Direction screenTopSide() { return mount(); }
    private Direction screenRight() { return counterClockwise(yaw()); }

    private Direction counterClockwise(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            default -> facing;
        };
    }
}
