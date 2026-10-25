package li.cil.oc.integration.multipart;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.multipart.api.MultipartType;
import codechicken.multipart.api.part.CapabilityProviderPart;
import codechicken.multipart.api.part.MultiPart;
import codechicken.multipart.minecraft.McStatePart;
import codechicken.multipart.util.PartRayTraceResult;
import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.Items;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.internal.Colored;
import li.cil.oc.common.Capabilities;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.block.CableHelper;
import li.cil.oc.common.block.property.PropertyCableConnection;
import li.cil.oc.common.blockentity.Cable;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.util.Color;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.BlockCapability;

import java.util.Optional;

/** Cable part used only while CB Multipart owns the containing block entity. */
public final class MultipartCablePart extends McStatePart implements CapabilityProviderPart, Environment, EnvironmentHost, Colored {
    private int color = Color.rgbValues(DyeColor.LIGHT_GRAY);
    private final Node node = Network.newNode(this, Visibility.None).create();
    private final Optional<Cable> original;

    public MultipartCablePart(BlockState initialState, Optional<Cable> original) {
        super(initialState);
        this.original = original;
        original.ifPresent(cable -> color = cable.getColor());
    }
    public MultipartCablePart(BlockState initialState) { this(initialState, Optional.empty()); }

    @Override public Node node() { return node; }
    @Override public MultipartType<?> getType() { return MultipartIntegration.cableMultipartType(); }
    @Override public BlockState defaultBlockState() { return OCBlocks.Cable().get().defaultBlockState(); }
    @Override public BlockState getCurrentState() {
        updateConnections();
        return state;
    }
    @Override public ItemStack getDropStack() { return createItemStack(); }
    @Override public ItemStack getCloneStack(PartRayTraceResult hit, Player player) { return createItemStack(); }
    @Override public ItemStack getCloneStack(PartRayTraceResult hit) { return createItemStack(); }

    @Override public ItemInteractionResult useItemOn(ItemStack stack, Player player, PartRayTraceResult hit, InteractionHand hand) {
        if (!Color.isDye(stack)) return super.useItemOn(stack, player, hit, hand);
        setColor(Color.rgbValues(Color.dyeColor(stack)));
        if (!player.isCreative()) stack.shrink(1);
        return ItemInteractionResult.sidedSuccess(level().isClientSide());
    }
    @Override public InteractionResult useWithoutItem(Player player, PartRayTraceResult hit) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!Color.isDye(stack)) return super.useWithoutItem(player, hit);
        setColor(Color.rgbValues(Color.dyeColor(stack)));
        if (!player.isCreative()) stack.shrink(1);
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    public void copyColor(int value) { color = value; }
    @Override public int getColor() { return color; }
    @Override public void setColor(int value) {
        if (value == color) return;
        color = value;
        updateConnections();
        markChanged();
        if (hasLevel() && !level().isClientSide()) {
            sendUpdate(this::writeDesc);
            Network.joinOrCreateNetwork(level(), pos());
            refreshAdjacentBlocks();
        }
    }
    @Override public boolean controlsConnectivity() { return true; }

    private ItemStack createItemStack() {
        ItemStack stack = Items.get(Constants.BlockName$.MODULE$.Cable()).createItemStack(1);
        if (color != Color.rgbValues(DyeColor.LIGHT_GRAY)) ItemColorizer.setColor(stack, color);
        return stack;
    }
    @Override public VoxelShape getShape(CollisionContext context) { return state.getShape(level(), pos(), context); }
    @Override public VoxelShape getCollisionShape(CollisionContext context) { return state.getCollisionShape(level(), pos(), context); }
    @Override public VoxelShape getRenderOcclusionShape() { return state.getShape(level(), pos(), CollisionContext.empty()); }

    @Override public <T, C> T getCapability(BlockCapability<T, C> capability, C context) {
        if (capability == Capabilities.EnvironmentCapability()) return (T) this;
        if (capability == Capabilities.ColoredCapability()) return (T) this;
        return null;
    }
    @Override public Level getEnvironmentLevel() { return hasLevel() ? level() : null; }
    @Override public double xPosition() { return pos().getX() + 0.5; }
    @Override public double yPosition() { return pos().getY() + 0.5; }
    @Override public double zPosition() { return pos().getZ() + 0.5; }
    @Override public void markChanged() { if (hasTile()) tile().setChanged(); }
    @Override public void onMessage(Message message) { }
    @Override public void onConnect(Node other) { }
    @Override public void onDisconnect(Node other) { }

    private void scheduleNetworkJoin() {
        if (hasLevel() && !level().isClientSide()) scheduleServerTask(() -> {
            if (hasLevel()) Network.joinOrCreateNetwork(level(), pos());
        });
    }
    private static void scheduleServerTask(Runnable task) {
        EventHandler.scheduleServer(new scala.runtime.AbstractFunction0<scala.runtime.BoxedUnit>() {
            @Override public scala.runtime.BoxedUnit apply() {
                task.run();
                return scala.runtime.BoxedUnit.UNIT;
            }
        });
    }
    @Override public void onAdded() {
        super.onAdded();
        updateConnections();
        scheduleNetworkJoin();
        refreshAdjacentBlocks();
    }
    @Override public void onPartChanged(MultiPart other) {
        super.onPartChanged(other);
        updateConnections();
        scheduleNetworkJoin();
        refreshAdjacentBlocks();
    }
    @Override public void onNeighborBlockChanged(BlockPos changedPos) {
        super.onNeighborBlockChanged(changedPos);
        updateConnections();
    }
    @Override public void onRemoved() {
        super.onRemoved();
        if (node != null) node.remove();
    }
    @Override public void onChunkUnload() {
        super.onChunkUnload();
        if (node != null) node.remove();
    }
    @Override public void onChunkLoad(net.minecraft.world.level.chunk.LevelChunk chunk) {
        super.onChunkLoad(chunk);
        scheduleNetworkJoin();
        refreshAdjacentBlocks();
    }
    @Override public void onWorldJoin() {
        super.onWorldJoin();
        updateConnections();
        scheduleNetworkJoin();
        refreshAdjacentBlocks();
    }
    @Override public void onWorldSeparate() {
        super.onWorldSeparate();
        if (node != null) node.remove();
    }
    @Override public void invalidateConvertedTile() {
        super.invalidateConvertedTile();
        if (hasLevel() && !level().isClientSide() && original.isPresent()) {
            original.get().node().neighbors().forEach(neighbor -> neighbor.connect(node));
        }
    }

    @Override public void save(CompoundTag tag, HolderLookup.Provider provider) {
        super.save(tag, provider);
        tag.putInt(Settings.namespace() + "renderColor", color);
        if (node != null) {
            CompoundTag nodeTag = new CompoundTag();
            node.saveData(nodeTag, provider);
            tag.put(Settings.namespace() + "node", nodeTag);
        }
    }
    @Override public void load(CompoundTag tag, HolderLookup.Provider provider) {
        super.load(tag, provider);
        color = tag.getInt(Settings.namespace() + "renderColor");
        if (color == 0) color = Color.rgbValues(DyeColor.LIGHT_GRAY);
        if (node != null && tag.contains(Settings.namespace() + "node")) {
            try {
                node.loadData(tag.getCompound(Settings.namespace() + "node"), provider);
            } catch (UnrecoverablePersistanceException exception) {
                throw new IllegalStateException("Failed to load multipart cable node", exception);
            }
        }
    }
    @Override public void writeDesc(MCDataOutput out) { super.writeDesc(out); out.writeInt(color); }
    @Override public void readDesc(MCDataInput in) { super.readDesc(in); color = in.readInt(); }

    private void refreshAdjacentBlocks() {
        if (hasLevel() && !level().isClientSide()) scheduleServerTask(() -> {
        if (hasLevel() && MultipartColorLookup.cablePart(level(), pos()).orElse(null) == this) {
                level().updateNeighborsAt(pos(), level().getBlockState(pos()).getBlock());
            }
        });
    }

    private void updateConnections() {
        if (!hasLevel()) return;
        int gray = Color.rgbValues(DyeColor.LIGHT_GRAY);
        BlockState baseState = OCBlocks.Cable().get().defaultBlockState();
        BlockState nextState = baseState;
        for (Direction side : Direction.values()) {
            BlockPos neighborPos = pos().relative(side);
            Direction neighborSide = side.getOpposite();
            Optional<MultipartCablePart> multipartCable = MultipartColorLookup.cablePart(level(), neighborPos);
            boolean canPassMultipart = MultipartColorLookup.canConnectFromSide(level(), pos(), side) &&
                    MultipartColorLookup.canConnectFromSide(level(), neighborPos, neighborSide);
            var sided = level().getCapability(Capabilities.SidedEnvironmentCapability(), neighborPos, neighborSide);
            boolean hasNode = canPassMultipart && (multipartCable.isPresent() || (sided != null
                    ? level().isClientSide() ? sided.canConnect(neighborSide) : sided.sidedNode(neighborSide) != null
                    : level().getCapability(Capabilities.EnvironmentCapability(), neighborPos, neighborSide) != null));

            Colored neighborColor = level().getCapability(Capabilities.ColoredCapability(), neighborPos, null);
            int otherColor = multipartCable.map(MultipartCablePart::getColor)
                    .orElseGet(() -> neighborColor == null ? gray : neighborColor.getColor());
            boolean compatibleColor = color == otherColor || color == gray || otherColor == gray;
            boolean isCable = multipartCable.isPresent() || level().getBlockState(neighborPos).getBlock() == OCBlocks.Cable().get();
            PropertyCableConnection.Shape shape = !hasNode || !compatibleColor ? PropertyCableConnection.Shape.NONE
                    : isCable || (neighborColor != null && neighborColor.controlsConnectivity())
                    ? PropertyCableConnection.Shape.CABLE : PropertyCableConnection.Shape.DEVICE;
            nextState = CableHelper.helperSetCableShapeState(nextState, side, shape);
        }

        if (state != nextState) {
            state = nextState;
            if (hasTile()) tile().notifyShapeChange();
            if (hasLevel() && !level().isClientSide()) sendUpdate(this::writeDesc);
        }
    }
}
