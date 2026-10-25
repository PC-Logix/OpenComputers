package li.cil.oc.common.blockentity;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Settings;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.client.renderer.block.ScreenModel;
import li.cil.oc.common.Tier;
import li.cil.oc.common.component.TextBuffer;
import li.cil.oc.common.blockentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.Color;
import li.cil.oc.util.ExtendedLevelJava;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.renderer.block.model.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Option$;
import scala.Tuple2;
import scala.Tuple4;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/** Mutable wall display panel which merges adjacent screens into a shared display. */
public class Screen extends ScreenBlockEntity {
    private final li.cil.oc.common.blockentity.traits.TextBufferHostStateJava textBufferState =
        new li.cil.oc.common.blockentity.traits.TextBufferHostStateJava();
    @Override public li.cil.oc.common.blockentity.traits.TextBufferHostStateJava textBufferState() { return textBufferState; }
    private final li.cil.oc.common.blockentity.traits.ColoredJava.State colorState =
        new li.cil.oc.common.blockentity.traits.ColoredJava.State();
    @Override public li.cil.oc.common.blockentity.traits.ColoredJava.State colorState() { return colorState; }
    public record ScreenPosition(int x, int y) {}
    public record BufferPoint(double x, double y) {}
    public record ScreenCoordinates(boolean inBounds, Optional<BufferPoint> point) {}

    private int tierValue;
    public boolean shouldCheckForMultiBlock = true;
    public int delayUntilCheckForMultiBlock = 40;
    public int width = 1;
    public int height = 1;
    public Option<Tuple4<Object, Object, Object, AABB>> cachedBounds = Option$.MODULE$.empty();
    public Screen origin = this;
    private final Set<Screen> screensState = new HashSet<>();
    public final scala.collection.mutable.Set<Screen> screens = scala.jdk.javaapi.CollectionConverters.asScala(screensState);
    public boolean hadRedstoneInput;
    public boolean invertTouchMode;
    private final Set<Arrow> arrows = new HashSet<>();
    private final WeakHashMap<Entity, ScreenPosition> lastWalked = new WeakHashMap<>();

    public Screen(BlockPos pos, BlockState state, int tier) {
        super(pos, state, tier);
        this.tierValue = tier;
        screensState.add(this);
        setColor(Color.byTier(this.tierValue));
    }

    public Screen(BlockPos pos, BlockState state) { this(pos, state, 0); }

    public int tier() { return tierValue; }
    public void tier_$eq(int value) { tierValue = value; }
    public boolean shouldCheckForMultiBlock() { return shouldCheckForMultiBlock; }
    public void shouldCheckForMultiBlock_$eq(boolean value) { shouldCheckForMultiBlock = value; }
    public int delayUntilCheckForMultiBlock() { return delayUntilCheckForMultiBlock; }
    public void delayUntilCheckForMultiBlock_$eq(int value) { delayUntilCheckForMultiBlock = value; }
    public int width() { return width; }
    public void width_$eq(int value) { width = value; }
    public int height() { return height; }
    public void height_$eq(int value) { height = value; }
    public Screen origin() { return origin; }
    public void origin_$eq(Screen value) { origin = value; }
    public scala.collection.mutable.Set<Screen> screens() { return screens; }
    public Set<Screen> screensJava() { return screensState; }
    public boolean hadRedstoneInput() { return hadRedstoneInput; }
    public void hadRedstoneInput_$eq(boolean value) { hadRedstoneInput = value; }
    public boolean invertTouchMode() { return invertTouchMode; }
    public void invertTouchMode_$eq(boolean value) { invertTouchMode = value; }
    public Option<Tuple4<Object, Object, Object, AABB>> cachedBounds() { return cachedBounds; }
    public void cachedBounds_$eq(Option<Tuple4<Object, Object, Object, AABB>> value) { cachedBounds = value; }

    @Override public Direction[] validFacings() { return Direction.values(); }

    @OnlyIn(Dist.CLIENT)
    @Override public ModelData getModelData() {
        return ModelData.builder().with(ScreenModel.SCREEN_PROPERTY, this).build();
    }

    @OnlyIn(Dist.CLIENT)
    @Override public boolean canConnect(Direction side) { return side != facing(); }

    @Override
    public Node sidedNode(Direction side) {
        BlockPos adjacent = getBlockPos().relative(side);
        return side != facing() || (getLevel().isLoaded(adjacent) && getLevel().getBlockEntity(adjacent) instanceof Keyboard)
            ? node() : null;
    }

    public boolean isOrigin() { return origin == this; }

    public AABB getRenderBoundingBox() {
        if (cachedBounds.isDefined()) {
            Tuple4<Object, Object, Object, AABB> cached = cachedBounds.get();
            if (Boolean.valueOf(isOrigin()).equals(cached._1()) && Integer.valueOf(width).equals(cached._2()) && Integer.valueOf(height).equals(cached._3())) return cached._4();
        }
        AABB bounds;
        if ((width == 1 && height == 1) || !isOrigin()) bounds = new AABB(getBlockPos());
        else {
            BlockPosition size = unproject(width - 1, height - 1, 0);
            bounds = new AABB(getBlockPos()).expandTowards(size.x(), size.y(), size.z());
        }
        cachedBounds = Option$.MODULE$.apply(new Tuple4<>(isOrigin(), width, height, bounds));
        return bounds;
    }

    public Tuple2<Integer, Integer> localPosition() {
        ScreenPosition position = localPositionJava();
        return new Tuple2<>(position.x(), position.y());
    }

    public ScreenPosition localPositionJava() {
        BlockPosition local = project(this);
        BlockPosition parent = project(origin);
        return new ScreenPosition(local.x() - parent.x(), local.y() - parent.y());
    }

    public boolean hasKeyboard() {
        Iterator<Screen> screensIterator = screensState.iterator();
        while (screensIterator.hasNext()) {
            Screen screen = screensIterator.next();
            for (Direction side : Direction.values()) {
                BlockPosition pos = BlockPositionsJava.apply(screen).offset(side);
                if (!new ExtendedLevelJava(getLevel()).blockExists(pos)) continue;
                if (getLevel().getBlockEntity(pos.toBlockPos()) instanceof Keyboard keyboard && keyboard.hasNodeOnSide(side.getOpposite())) return true;
            }
        }
        return false;
    }

    public void checkMultiBlock() {
        shouldCheckForMultiBlock = true;
        width = 1;
        height = 1;
        origin = this;
        screensState.clear();
        screensState.add(this);
        invertTouchMode = false;
    }

    public Tuple2<Boolean, Option<Tuple2<Double, Double>>> toScreenCoordinates(double hitX, double hitY, double hitZ) {
        ScreenCoordinates mapped = toScreenCoordinatesJava(hitX, hitY, hitZ);
        return new Tuple2<>(mapped.inBounds(), mapped.point().<Option<Tuple2<Double, Double>>>map(point ->
            Option$.MODULE$.apply(new Tuple2<>(point.x(), point.y()))).orElseGet(Option$.MODULE$::empty));
    }

    public ScreenCoordinates toScreenCoordinatesJava(double hitX, double hitY, double hitZ) {
        double hx = dot(toGlobal(Direction.EAST), hitX, hitY, hitZ);
        double hy = dot(toGlobal(Direction.UP), hitX, hitY, hitZ);
        double tx = hx < 0 ? 1 + hx : hx;
        double ty = 1 - (hy < 0 ? 1 + hy : hy);
        ScreenPosition local = localPositionJava();
        double ax = local.x() + tx;
        double ay = height - 1 - local.y() + ty;
        double border = 2.25 / 16.0;
        if (ax <= border || ay <= border || ax >= width - border || ay >= height - border)
            return new ScreenCoordinates(false, Optional.empty());
        if (!getLevel().isClientSide()) return new ScreenCoordinates(true, Optional.empty());
        double iw = width - border * 2;
        double ih = height - border * 2;
        double rx = (ax - border) / iw;
        double ry = (ay - border) / ih;
        var buffer = origin.buffer();
        int bw = buffer.getViewportWidth();
        int bh = buffer.getViewportHeight();
        double bpw = buffer.renderWidth() / iw;
        double bph = buffer.renderHeight() / ih;
        double brx;
        double bry;
        if (bpw > bph) {
            double rh = bph / bpw;
            bry = (ry - (1 - rh) * 0.5) / rh;
            brx = rx;
        } else if (bph > bpw) {
            double rw = bpw / bph;
            brx = (rx - (1 - rw) * 0.5) / rw;
            bry = ry;
        } else { brx = rx; bry = ry; }
        boolean inBounds = bry >= 0 && bry <= 1 && brx >= 0 || brx <= 1;
        return new ScreenCoordinates(inBounds, Optional.of(new BufferPoint(brx * bw, bry * bh)));
    }

    private static double dot(Direction direction, double x, double y, double z) {
        return direction.getStepX() * x + direction.getStepY() * y + direction.getStepZ() * z;
    }

    public boolean copyToAnalyzer(double hitX, double hitY, double hitZ) {
        ScreenCoordinates mapped = toScreenCoordinatesJava(hitX, hitY, hitZ);
        if (mapped.point().isPresent()) {
            BufferPoint point = mapped.point().get();
            Object buffer = origin.buffer();
            if (buffer instanceof TextBuffer textBuffer) { textBuffer.copyToAnalyzer((int) point.y(), null); return true; }
            return false;
        }
        return mapped.inBounds();
    }

    public boolean click(double hitX, double hitY, double hitZ) {
        ScreenCoordinates mapped = toScreenCoordinatesJava(hitX, hitY, hitZ);
        if (mapped.point().isPresent()) {
            BufferPoint point = mapped.point().get();
            origin.buffer().mouseDown(point.x(), point.y(), 0, null);
            return true;
        }
        return mapped.inBounds();
    }

    public void walk(Entity entity) {
        ScreenPosition position = localPositionJava();
        ScreenPosition previous = lastWalked.put(entity, position);
        int x = position.x(), y = position.y();
        if (position.equals(previous)) return;
        if (entity instanceof Player player && Settings.get().inputUsername())
            origin.node().sendToReachable("computer.signal", "walk", x + 1, height - y, player.getName().getString());
        else origin.node().sendToReachable("computer.signal", "walk", x + 1, height - y);
    }

    public void shot(Arrow arrow) { arrows.add(arrow); }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (shouldCheckForMultiBlock && ((isClient() && isClientReadyForMultiBlockCheck()) || (isServer() && isConnected()))) {
            java.util.TreeSet<Screen> pending = new java.util.TreeSet<>();
            pending.add(this);
            java.util.ArrayDeque<Screen> queue = new java.util.ArrayDeque<>();
            queue.add(this);
            while (!queue.isEmpty()) {
                Screen current = queue.removeFirst();
                BlockPosition pos = project(current);
                tryQueue(pos, -1, 0, pending, queue);
                tryQueue(pos, 1, 0, pending, queue);
                tryQueue(pos, 0, -1, pending, queue);
                tryQueue(pos, 0, 1, pending, queue);
            }
            while (!pending.isEmpty()) {
                Screen current = pending.first();
                while (current.tryMerge()) { }
                Iterator<Screen> currentScreens = current.screensState.iterator();
                while (currentScreens.hasNext()) {
                    Screen screen = currentScreens.next();
                    screen.shouldCheckForMultiBlock = false;
                    pending.remove(screen);
                    queue.add(screen);
                }
            }
            if (isClient()) updateMergedModels();
            for (Screen screen : queue) {
                var buffer = screen.buffer();
                if (screen.isOrigin()) {
                    if (isServer()) {
                        ((Component) buffer.node()).setVisibility(Visibility.Network);
                        buffer.setEnergyCostPerTick(Settings.get().screenCost() * screen.width * screen.height);
                        buffer.setAspectRatio(screen.width, screen.height);
                    }
                } else {
                    if (isServer()) {
                        ((Component) buffer.node()).setVisibility(Visibility.None);
                        buffer.setEnergyCostPerTick(Settings.get().screenCost());
                    }
                    buffer.setAspectRatio(1, 1);
                    int w = buffer.getWidth(); int h = buffer.getHeight();
                    buffer.setForegroundColor(0xFFFFFF, false);
                    buffer.setBackgroundColor(0x000000, false);
                    buffer.fill(0, 0, w, h, 0x20);
                }
            }
        }
        if (!arrows.isEmpty()) {
            Iterator<Arrow> iterator = arrows.iterator();
            while (iterator.hasNext()) {
                Arrow arrow = iterator.next();
                if (arrow.getOwner() == Minecraft.getInstance().player)
                    click(arrow.getX() - getBlockPos().getX(), arrow.getY() - getBlockPos().getY(), arrow.getZ() - getBlockPos().getZ());
            }
            arrows.clear();
        }
    }

    private void tryQueue(BlockPosition position, int dx, int dy, java.util.Set<Screen> pending, java.util.ArrayDeque<Screen> queue) {
        BlockPosition next = unproject(position.x() + dx, position.y() + dy, position.z());
        if (!new ExtendedLevelJava(getLevel()).blockExists(next)) return;
        if (getLevel().getBlockEntity(next.toBlockPos()) instanceof Screen screen && screen.pitch().equals(pitch()) && screen.yaw().equals(yaw()) && pending.add(screen)) queue.addLast(screen);
    }

    private void updateMergedModels() {
        if (getLevel() == Minecraft.getInstance().level) {
            var renderer = Minecraft.getInstance().levelRenderer;
            Iterator<Screen> iterator = screensState.iterator();
            while (iterator.hasNext()) {
                BlockPos pos = iterator.next().getBlockPos();
                renderer.setSectionDirty(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            }
        }
    }

    private boolean isClientReadyForMultiBlockCheck() {
        if (delayUntilCheckForMultiBlock > 0) { delayUntilCheckForMultiBlock--; return false; }
        return true;
    }

    private java.util.List<Screen> snapshotScreens() {
        java.util.ArrayList<Screen> copy = new java.util.ArrayList<>();
        Iterator<Screen> iterator = screensState.iterator();
        while (iterator.hasNext()) copy.add(iterator.next());
        return copy;
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Screen screen : snapshotScreens()) screen.checkMultiBlock();
        if (isClient() && Minecraft.getInstance().screen instanceof li.cil.oc.client.gui.Screen screenGui && screenGui.buffer() == buffer()) screenGui.onClose();
    }

    @Override
    public void onColorChanged() {
        super.onColorChanged();
        for (Screen screen : snapshotScreens()) screen.checkMultiBlock();
    }

    @Override public void loadComponentsCommon(DataComponentHolder holder) {
        new li.cil.oc.util.ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.TIER.get())
            .map(Number::intValue).ifPresent(value -> this.tierValue = value);
        setColor(Color.byTier(this.tierValue));
        super.loadComponentsCommon(holder);
        invertTouchMode = holder.has(OCComponentsJava.INVERT_TOUCH.get());
    }

    @Override public void saveComponentsCommon(MutableDataComponentHolder holder) {
        holder.set(OCComponentsJava.TIER.get(), (byte) tierValue);
        super.saveComponentsCommon(holder);
        new li.cil.oc.util.ExtendedMutableDataComponentHolderJava(holder).setUnitComponent(OCComponentsJava.INVERT_TOUCH, invertTouchMode);
    }

    @Override public void loadComponentsForServer(DataComponentHolder holder) {
        super.loadComponentsForServer(holder);
        hadRedstoneInput = new li.cil.oc.util.ExtendedDataComponentHolder(holder)
            .getComponentOptional(OCComponentsJava.HAS_REDSTONE_INPUT.get()).orElse(false);
    }

    @Override public void saveComponentsForServer(MutableDataComponentHolder holder) {
        super.saveComponentsForServer(holder);
        holder.set(OCComponentsJava.HAS_REDSTONE_INPUT.get(), hadRedstoneInput);
    }

    @Override public void saveForClient(CompoundTag nbt, HolderLookup.Provider provider) { saveForClientDirect(nbt, provider); }
    public void saveForClientDirect(CompoundTag nbt, HolderLookup.Provider provider) { super.saveForClient(nbt, provider); }
    @Override public void loadComponentsForClient(DataComponentHolder holder) { super.loadComponentsForClient(holder); requestModelDataUpdate(); }
    @Override public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) { return new Node[]{origin.node()}; }

    @Override
    public void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        super.onRedstoneInputChanged(args);
        int maximum = 0;
        Iterator<Screen> iterator = screensState.iterator();
        while (iterator.hasNext()) maximum = Math.max(maximum, iterator.next().maxInput());
        boolean hasInput = maximum > 0;
        if (hasInput != hadRedstoneInput) {
            hadRedstoneInput = hasInput;
            if (hasInput) origin.buffer().setPowerState(!origin.buffer().getPowerState());
        }
    }

    @Override
    public void onRotationChanged() {
        super.onRotationChanged();
        for (Screen screen : snapshotScreens()) screen.checkMultiBlock();
    }

    @Override
    public int compare(Screen that) {
        if (getBlockPos().getX() != that.getBlockPos().getX()) return getBlockPos().getX() - that.getBlockPos().getX();
        if (getBlockPos().getY() != that.getBlockPos().getY()) return getBlockPos().getY() - that.getBlockPos().getY();
        return getBlockPos().getZ() - that.getBlockPos().getZ();
    }

    private boolean tryMerge() {
        BlockPosition originPosition = project(origin);
        if (tryMergeTowards(originPosition, 0, height) || tryMergeTowards(originPosition, 0, -1) ||
            tryMergeTowards(originPosition, width, 0) || tryMergeTowards(originPosition, -1, 0)) return true;
        return false;
    }

    private boolean tryMergeTowards(BlockPosition originPosition, int dx, int dy) {
        BlockPosition next = unproject(originPosition.x() + dx, originPosition.y() + dy, originPosition.z());
        if (!new ExtendedLevelJava(getEnvironmentLevel()).blockExists(next)) return false;
        if (!(new ExtendedLevelJava(getLevel()).getBlockEntity(next) instanceof Screen screen)) return false;
        if (screen.tier() != tierValue || !screen.pitch().equals(pitch()) || screen.getColor() != getColor() || !screen.yaw().equals(yaw()) || screensState.contains(screen)) return false;
        BlockPosition otherOrigin = project(screen.origin);
        boolean mergeX = otherOrigin.y() == originPosition.y() && screen.height == height && screen.width + width <= Settings.get().maxScreenWidth();
        boolean mergeY = otherOrigin.x() == originPosition.x() && screen.width == width && screen.height + height <= Settings.get().maxScreenHeight();
        if (!mergeX && !mergeY) return false;
        Screen newOrigin = mergeX ? (otherOrigin.x() < originPosition.x() ? screen.origin : origin) : (otherOrigin.y() < originPosition.y() ? screen.origin : origin);
        int newWidth = mergeX ? width + screen.width : width;
        int newHeight = mergeX ? height : height + screen.height;
        java.util.HashSet<Screen> merged = new java.util.HashSet<>();
        Iterator<Screen> ownScreens = screensState.iterator();
        while (ownScreens.hasNext()) merged.add(ownScreens.next());
        Iterator<Screen> otherScreens = screen.screensState.iterator();
        while (otherScreens.hasNext()) merged.add(otherScreens.next());
        for (Screen part : merged) {
            part.width = newWidth;
            part.height = newHeight;
            part.origin = newOrigin;
            for (Screen member : merged) part.screensState.add(member);
        }
        return true;
    }

    private BlockPosition project(Screen screen) {
        Direction east = toGlobal(Direction.EAST), up = toGlobal(Direction.UP), south = toGlobal(Direction.SOUTH);
        int x = east.getStepX() * screen.getBlockPos().getX() + east.getStepY() * screen.getBlockPos().getY() + east.getStepZ() * screen.getBlockPos().getZ();
        int y = up.getStepX() * screen.getBlockPos().getX() + up.getStepY() * screen.getBlockPos().getY() + up.getStepZ() * screen.getBlockPos().getZ();
        int z = south.getStepX() * screen.getBlockPos().getX() + south.getStepY() * screen.getBlockPos().getY() + south.getStepZ() * screen.getBlockPos().getZ();
        return BlockPositionsJava.apply(x, y, z);
    }

    private BlockPosition unproject(int x, int y, int z) {
        Direction east = toLocal(Direction.EAST), up = toLocal(Direction.UP), south = toLocal(Direction.SOUTH);
        int px = east.getStepX() * x + east.getStepY() * y + east.getStepZ() * z;
        int py = up.getStepX() * x + up.getStepY() * y + up.getStepZ() * z;
        int pz = south.getStepX() * x + south.getStepY() * y + south.getStepZ() * z;
        return BlockPositionsJava.apply(px, py, pz);
    }
}

