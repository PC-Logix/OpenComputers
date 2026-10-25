package li.cil.oc.integration.multipart;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.multipart.api.MultipartType;
import codechicken.multipart.api.part.MultiPart;
import codechicken.multipart.api.part.redstone.RedstonePart;
import codechicken.multipart.minecraft.McStatePart;
import codechicken.multipart.util.PartRayTraceResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import li.cil.oc.Settings;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.common.blockentity.Print;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.item.data.PrintDataSupport;
import li.cil.oc.common.item.data.PrintShape;
import li.cil.oc.util.ExtendedAABB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;
import java.util.Set;
import scala.jdk.javaapi.CollectionConverters;

/** A print keeps its own design and state when sharing a CB Multipart block. */
public final class MultipartPrintPart extends McStatePart implements RedstonePart {
    private PrintData data = new PrintData();
    private Direction facing = Direction.SOUTH;
    private boolean active;
    private boolean updatingInput;

    public MultipartPrintPart(BlockState initialState, Optional<Print> original) {
        super(initialState);
        original.ifPresent(print -> {
            data = print.data();
            facing = print.facing();
            active = print.state();
        });
    }

    public MultipartPrintPart(BlockState initialState) {
        this(initialState, Optional.empty());
    }

    public PrintData data() { return data; }
    public void data_$eq(PrintData value) { data = value; }
    public Direction facing() { return facing; }
    public void facing_$eq(Direction value) { facing = value; }
    public boolean active() { return active; }
    public void active_$eq(boolean value) { active = value; }

    @Override public MultipartType<?> getType() { return MultipartIntegration.printMultipartType(); }
    @Override public BlockState defaultBlockState() { return OCBlocks.Print().get().defaultBlockState(); }
    @Override public ItemStack getDropStack() { return data.createItemStack(); }
    @Override public ItemStack getCloneStack(PartRayTraceResult hit, Player player) { return getDropStack(); }
    @Override public ItemStack getCloneStack(PartRayTraceResult hit) { return getDropStack(); }

    public Set<PrintShape> shapes() {
        return CollectionConverters.asJava(active ? data.stateOn() : data.stateOff());
    }

    private VoxelShape printedShape() {
        VoxelShape result = Shapes.empty();
        for (PrintShape shape : shapes()) {
            result = Shapes.or(result, Shapes.create(ExtendedAABB.rotateTowards(shape.bounds(), facing)));
        }
        return result;
    }

    @Override public VoxelShape getShape(CollisionContext context) {
        VoxelShape shape = printedShape();
        return shape.isEmpty() ? Shapes.box(0.4375, 0.4375, 0.4375, 0.5625, 0.5625, 0.5625) : shape;
    }
    @Override public VoxelShape getCollisionShape(CollisionContext context) {
        return (active ? data.noclipOn() : data.noclipOff()) ? Shapes.empty() : printedShape();
    }
    @Override public VoxelShape getOcclusionShape() { return printedShape(); }
    @Override public VoxelShape getRenderOcclusionShape() { return printedShape(); }
    @Override public int getLightEmission() { return data.lightLevel(); }

    @Override public boolean canConnectRedstone(int side) { return true; }
    @Override public int strongPowerLevel(int side) { return weakPowerLevel(side); }
    @Override public int weakPowerLevel(int side) {
        return data.emitRedstone(active) ? data.redstoneLevel() : 0;
    }

    @Override public InteractionResult useWithoutItem(Player player, PartRayTraceResult hit) {
        if (!data.hasActiveState() || (active && data.isButtonMode())) return super.useWithoutItem(player, hit);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        return toggleState() ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override public void scheduledTick() {
        if (active && data.isButtonMode() && hasLevel() && !level().isClientSide) toggleState();
    }

    private boolean toggleState() {
        if (!hasTile() || !hasLevel() || level().isClientSide) return false;
        boolean previous = active;
        active = !active;
        if (!tile().canReplacePart(this, this)) {
            active = previous;
            return false;
        }
        level().playSound(null, pos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3f, active ? 0.6f : 0.5f);
        tile().setChanged();
        tile().notifyShapeChange();
        tile().notifyPartChange(this);
        sendUpdate(this::writeDesc);
        if (active && data.isButtonMode()) scheduleTick(20);
        return true;
    }

    private void updateFromRedstone() {
        if (updatingInput || !hasLevel() || !hasTile() || level().isClientSide || data.emitRedstone() || !data.hasActiveState()) return;
        updatingInput = true;
        try {
            int otherPower = 0;
            for (MultiPart part : tile().getPartList()) {
                if (part instanceof RedstonePart redstone && part != this) {
                    for (Direction side : Direction.values()) otherPower = Math.max(otherPower, redstone.weakPowerLevel(side.ordinal()));
                }
            }
            int input = Math.max(level().getBestNeighborSignal(pos()), otherPower);
            if (active != (input > 1)) toggleState();
        } finally {
            updatingInput = false;
        }
    }

    @Override public void onPartChanged(MultiPart other) {
        super.onPartChanged(other);
        updateFromRedstone();
    }
    @Override public void onNeighborBlockChanged(BlockPos changedPos) {
        super.onNeighborBlockChanged(changedPos);
        updateFromRedstone();
    }
    @Override public void onWorldJoin() {
        super.onWorldJoin();
        updateFromRedstone();
    }

    private final String dataTag = Settings.namespace() + "data";
    private final String facingTag = Settings.namespace() + "facing";
    private final String activeTag = Settings.namespace() + "state";

    @Override public void save(CompoundTag tag, HolderLookup.Provider registry) {
        super.save(tag, registry);
        CompoundTag printTag = new CompoundTag();
        data.saveData(printTag, registry);
        tag.put(dataTag, printTag);
        tag.putInt(facingTag, facing.ordinal());
        tag.putBoolean(activeTag, active);
    }
    @Override public void load(CompoundTag tag, HolderLookup.Provider registry) {
        super.load(tag, registry);
        try {
            data.loadData(tag.getCompound(dataTag), registry);
        } catch (UnrecoverablePersistanceException exception) {
            throw new IllegalStateException("Failed to load multipart print data", exception);
        }
        facing = Direction.from3DDataValue(tag.getInt(facingTag));
        active = tag.getBoolean(activeTag);
    }
    @Override public void writeDesc(MCDataOutput out) {
        super.writeDesc(out);
        ByteBuf buffer = Unpooled.buffer();
        try {
            PrintDataSupport.STREAM_CODEC.encode(buffer, data);
            out.writeByteBuf(buffer);
        } finally {
            buffer.release();
        }
        out.writeDirection(facing);
        out.writeBoolean(active);
    }
    @Override public void readDesc(MCDataInput in) {
        super.readDesc(in);
        ByteBuf buffer = in.readByteBuf();
        try {
            data = PrintDataSupport.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
        facing = in.readDirection();
        active = in.readBoolean();
        if (hasTile()) tile().markRender();
    }
}
