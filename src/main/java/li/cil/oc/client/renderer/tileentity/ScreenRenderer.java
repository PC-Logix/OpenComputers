package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.Settings;
import li.cil.oc.client.PacketSender;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.block.FlatScreen;
import li.cil.oc.common.blockentity.HoloScreen;
import li.cil.oc.common.component.TextBuffer;
import li.cil.oc.common.blockentity.Screen;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.util.RenderState;
import li.cil.oc.util.SableCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static li.cil.oc.client.renderer.tileentity.ScreenRendererTypes.*;

public final class ScreenRenderer implements BlockEntityRendererProvider<Screen>, BlockEntityRenderer<Screen> {
    public static final ScreenRenderer INSTANCE = new ScreenRenderer();
    private static final Map<Long, Screen> QUEUED_HOLO_SCREENS = new LinkedHashMap<>();
    private static final Map<Long, Screen> RENDERED_HOLO_SCREENS = new LinkedHashMap<>();
    private static final ScreenRenderer LATE_RENDERER = new ScreenRenderer();

    private final double maxRenderDistanceSq;
    private final double fadeDistanceSq;
    private final double fadeRatio;
    private Screen screen;

    private ScreenRenderer() {
        maxRenderDistanceSq = Settings.get().maxScreenTextRenderDistance() * Settings.get().maxScreenTextRenderDistance();
        fadeDistanceSq = Settings.get().screenTextFadeStartDistance() * Settings.get().screenTextFadeStartDistance();
        fadeRatio = 1.0 / (maxRenderDistanceSq - fadeDistanceSq);
    }

    @Override
    public ScreenRenderer create(BlockEntityRendererProvider.Context context) {
        return new ScreenRenderer();
    }

    public static void queueHolo(Screen screen) {
        QUEUED_HOLO_SCREENS.put(screen.getBlockPos().asLong(), screen);
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            QUEUED_HOLO_SCREENS.clear();
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            QUEUED_HOLO_SCREENS.clear();
            RENDERED_HOLO_SCREENS.clear();
            return;
        }

        PoseStack stack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        var screens = QUEUED_HOLO_SCREENS.values().stream()
            .filter(screen -> screen.getLevel() == minecraft.level && !screen.isRemoved())
            .toList();

        RENDERED_HOLO_SCREENS.clear();
        for (Screen screen : screens) RENDERED_HOLO_SCREENS.put(screen.getBlockPos().asLong(), screen);
        if (screens.isEmpty()) {
            QUEUED_HOLO_SCREENS.clear();
            return;
        }

        stack.pushPose();
        stack.translate(-camera.x, -camera.y, -camera.z);
        for (Screen screen : screens) {
            var pos = screen.getBlockPos();
            stack.pushPose();
            stack.translate(pos.getX(), pos.getY(), pos.getZ());
            LATE_RENDERER.renderLateHolo(screen, stack, buffer);
            stack.popPose();
        }
        buffer.endBatch();
        stack.popPose();
        QUEUED_HOLO_SCREENS.clear();
    }

    @SubscribeEvent
    public void onInteractionKeyMapping(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || RENDERED_HOLO_SCREENS.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.level == null || player == null || minecraft.screen != null || minecraft.gameMode == null) return;

        float partialTicks = minecraft.getTimer().getGameTimeDeltaTicks();
        Vec3 start = player.getEyePosition(partialTicks);
        double reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue();
        Vec3 end = start.add(player.getViewVector(partialTicks).scale(reach));

        Screen nearestScreen = null;
        HoloHit nearestHit = null;
        for (Screen candidate : RENDERED_HOLO_SCREENS.values()) {
            if (candidate.getLevel() != minecraft.level || candidate.isRemoved() || !(candidate instanceof HoloScreen)) continue;
            HoloHit hit = LATE_RENDERER.projectedHoloHit(candidate, start, end);
            if (hit != null && (nearestHit == null || hit.t() < nearestHit.t())) {
                nearestScreen = candidate;
                nearestHit = hit;
            }
        }

        if (nearestScreen == null) return;
        if (isShiftHeld(minecraft) && nearestScreen instanceof HoloScreen holoScreen) {
            PacketSender.sendHoloScreenResize(holoScreen, LATE_RENDERER.resizeSideForHit(holoScreen, nearestHit.x(), nearestHit.y()));
            event.setSwingHand(false);
            event.setCanceled(true);
        } else if (nearestScreen.hasKeyboard()) {
            Block block = nearestScreen.getBlockState().getBlock();
            if (block instanceof li.cil.oc.common.block.Screen screenBlock) {
                var heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (screenBlock.rightClick(minecraft.level, nearestScreen.getBlockPos(), player,
                    InteractionHand.MAIN_HAND, heldItem, nearestScreen.facing(), 0.5f, 0.5f, 0.5f, true)) {
                    event.setSwingHand(false);
                    event.setCanceled(true);
                }
            }
        }
    }

    private static boolean isShiftHeld(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public AABB getRenderBoundingBox(Screen screen) {
        return screen.getRenderBoundingBox();
    }

    @Override
    public void render(Screen screen, float partialTick, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");
        this.screen = screen;
        if (!screen.isOrigin()) return;

        double distance = playerDistanceSq() / Math.min(screen.width(), screen.height());
        if (distance > maxRenderDistanceSq) return;

        if (screen instanceof HoloScreen) {
            queueHolo(screen);
            RenderState.checkError(getClass().getName() + ".render: leaving");
            return;
        }

        if (!isFlatScreen()) {
            Vec3 eyePosition = Minecraft.getInstance().player.getEyePosition(partialTick);
            Vec3 screenPosition = SableCompat.physicalPosition(screen.getLevel(), screen.getBlockPos());
            Direction screenFacing = SableCompat.physicalFacing(screen.getLevel(),
                Vec3.atCenterOf(screen.getBlockPos()), screen.facing()).getOpposite();
            double x = screenPosition.x - eyePosition.x;
            double y = screenPosition.y - eyePosition.y;
            double z = screenPosition.z - eyePosition.z;
            if (screenFacing.getStepX() * x + screenFacing.getStepY() * y + screenFacing.getStepZ() * z < 0) return;
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        stack.pushPose();
        stack.translate(0.5, 0.5, 0.5);
        RenderState.checkError(getClass().getName() + ".render: setup");
        drawOverlay(stack, buffer.getBuffer(RenderTypes.BLOCK_OVERLAY));
        RenderState.checkError(getClass().getName() + ".render: overlay");

        float alpha = distance > fadeDistanceSq
            ? Math.max(0f, 1f - (float) ((distance - fadeDistanceSq) * fadeRatio))
            : 1f;
        RenderState.checkError(getClass().getName() + ".render: fade");
        if (screen.buffer().isRenderingEnabled()) {
            var profiler = Minecraft.getInstance().getProfiler();
            profiler.push("opencomputers:screen_text");
            draw(stack, alpha, buffer);
            profiler.pop();
        }
        stack.popPose();
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }

    private void transform(PoseStack stack) {
        applyYaw(stack);
        if (screen.pitch() == Direction.DOWN) stack.mulPose(Axis.XP.rotationDegrees(90));
        else if (screen.pitch() == Direction.UP) stack.mulPose(Axis.XP.rotationDegrees(-90));
        stack.translate(-0.5f, -0.5f, 0.5f);
        stack.translate(0, screen.height(), 0);
        RenderState.mirrorScale(stack, 1, -1, 1);
    }

    private void transformHolo(PoseStack stack) {
        applyYaw(stack);
        stack.translate(-0.5f, -0.5f, 0.5f);
        stack.translate(0, screen.height(), 0);
        RenderState.mirrorScale(stack, 1, -1, 1);
    }

    private void applyYaw(PoseStack stack) {
        if (screen.yaw() == Direction.WEST) stack.mulPose(Axis.YP.rotationDegrees(-90));
        else if (screen.yaw() == Direction.NORTH) stack.mulPose(Axis.YP.rotationDegrees(180));
        else if (screen.yaw() == Direction.EAST) stack.mulPose(Axis.YP.rotationDegrees(90));
    }

    private void renderLateHolo(Screen screen, PoseStack stack, MultiBufferSource buffer) {
        this.screen = screen;
        renderHolo(stack, buffer);
    }

    private HoloHit projectedHoloHit(Screen screen, Vec3 start, Vec3 end) {
        this.screen = screen;
        int screenWidth = screen.width();
        int screenHeight = screen.height();
        var pos = screen.getBlockPos();
        PoseStack stack = new PoseStack();
        stack.translate(pos.getX(), pos.getY(), pos.getZ());
        stack.translate(0.5, 0.5, 0.5);
        transformHolo(stack);
        HologramLayout layout = createHologramLayout(screenWidth, screenHeight);
        stack.translate(layout.surface().left(), layout.surface().top(), layout.surface().depth());

        Matrix4f inverse = new Matrix4f(stack.last().pose()).invert();
        Vector4f a = transform(inverse, start);
        Vector4f b = transform(inverse, end);
        float dz = b.z - a.z;
        if (Math.abs(dz) < 1.0e-5f) return null;
        float t = -a.z / dz;
        if (t < 0 || t > 1) return null;
        float x = a.x + (b.x - a.x) * t;
        float y = a.y + (b.y - a.y) * t;
        return x >= 0 && x <= screenWidth && y >= 0 && y <= screenHeight ? new HoloHit(t, x, y) : null;
    }

    private static Vector4f transform(Matrix4f matrix, Vec3 value) {
        return matrix.transform(new Vector4f((float) value.x, (float) value.y, (float) value.z, 1f));
    }

    private Direction resizeSideForHit(Screen screen, float x, float y) {
        float edge = 0.35f;
        if (y >= screen.height() - edge) return Direction.UP;
        if (y <= edge) return Direction.DOWN;
        if (x >= screen.width() - edge) return Direction.EAST;
        if (x <= edge) return Direction.WEST;
        return Direction.UP;
    }

    private boolean isScreen(net.minecraft.world.item.ItemStack stack) {
        return Block.byItem(stack.getItem()) instanceof li.cil.oc.common.block.Screen;
    }

    private boolean isFlatScreen() {
        return screen.getBlockState().getBlock() instanceof FlatScreen;
    }

    private boolean isBackFlatScreen() {
        return screen.getBlockState().getBlock() instanceof FlatScreen flatScreen && flatScreen.isBack();
    }

    private void drawOverlay(PoseStack matrix, VertexConsumer consumer) {
        if (screen.facing() != Direction.UP && screen.facing() != Direction.DOWN) return;
        var player = Minecraft.getInstance().player;
        var heldStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (heldStack.isEmpty() || (!Wrench.holdsApplicableWrench(player, screen.getBlockPos()) && !isScreen(heldStack))) return;

        matrix.pushPose();
        transform(matrix);
        matrix.translate(screen.width() / 2f - 0.5f, screen.height() / 2f - 0.5f,
            isBackFlatScreen() ? -0.935f : 0.05f);
        var icon = Textures.getSprite(Textures.Block.ScreenUpIndicator);
        consumer.addVertex(matrix.last().pose(), 0, 1, 0).setUv(icon.getU0(), icon.getV1());
        consumer.addVertex(matrix.last().pose(), 1, 1, 0).setUv(icon.getU1(), icon.getV1());
        consumer.addVertex(matrix.last().pose(), 1, 0, 0).setUv(icon.getU1(), icon.getV0());
        consumer.addVertex(matrix.last().pose(), 0, 0, 0).setUv(icon.getU0(), icon.getV0());
        matrix.popPose();
    }

    private void draw(PoseStack stack, float alpha, MultiBufferSource buffer) {
        RenderState.checkError(getClass().getName() + ".draw: entering (aka: wasntme)");
        int screenWidth = screen.width();
        int screenHeight = screen.height();
        float textureWidth = screenWidth * 16f;
        float textureHeight = screenHeight * 16f;
        transform(stack);

        float border = isFlatScreen() ? 0.5f : 2.25f;
        stack.translate(screenWidth * border / textureWidth, screenHeight * border / textureHeight, 0);
        float innerWidth = screenWidth - border / 8;
        float innerHeight = screenHeight - border / 8;
        int renderWidth = screen.buffer().renderWidth();
        int renderHeight = screen.buffer().renderHeight();
        float scaleX = innerWidth / renderWidth;
        float scaleY = innerHeight / renderHeight;
        if (scaleX > scaleY) {
            stack.translate(renderWidth * 0.5f * (scaleX - scaleY), 0, 0);
            stack.scale(scaleY, scaleY, 1);
        } else {
            stack.translate(0, renderHeight * 0.5f * (scaleY - scaleX), 0);
            stack.scale(scaleX, scaleX, 1);
        }

        stack.translate(0, 0, (isBackFlatScreen() ? -0.94f : 0) + 0.01f);
        RenderState.checkError(getClass().getName() + ".draw: setup");
        if (screen.buffer() instanceof TextBuffer componentBuffer) componentBuffer.renderText(stack, buffer);
        else screen.buffer().renderText(stack);
        RenderState.checkError(getClass().getName() + ".draw: text");
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldRenderOffScreen(Screen screen) {
        return screen.isOrigin() && (screen.width() > 1 || screen.height() > 1);
    }

    private void renderHolo(PoseStack stack, MultiBufferSource buffer) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        stack.pushPose();
        stack.translate(0.5, 0.5, 0.5);
        transformHolo(stack);

        HologramLayout layout = createHologramLayout(screen.width(), screen.height());
        HologramColor color = hologramColor();
        VertexConsumer quad = buffer.getBuffer(RenderTypes.HOLOGRAM);
        renderProjectionBeam(stack, quad, color, layout.beam());
        renderHologramSurface(stack, quad, color, layout.surface(), layout.content(), buffer);
        stack.popPose();
    }

    private HologramLayout createHologramLayout(int width, int height) {
        HoloScreen holo = (HoloScreen) screen;
        HologramSurfaceLayout surface = new HologramSurfaceLayout(
            holo.projectionPlaneLeft(width), holo.projectionPlaneTop(height), holo.projectionDepth(), width, height);
        Optional<ProjectionBeamLayout> beam = width > 1
            ? Optional.of(new ProjectionBeamLayout(holo.projectionBeamAnchor(height), holo.projectionDepth(),
                holo.projectionBeamProjectorEdgeOffset(), width))
            : Optional.empty();
        return new HologramLayout(surface, beam, new MonitorContentLayout(0.5f, 0.01f));
    }

    private HologramColor hologramColor() {
        int color = screen.getColor();
        if (color == 11250603 || color == 4473924) color = 0;
        return new HologramColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF);
    }

    private void renderProjectionBeam(PoseStack stack, VertexConsumer quad, HologramColor color,
                                      Optional<ProjectionBeamLayout> beam) {
        if (beam.isEmpty()) return;
        ProjectionBeamLayout layout = beam.get();
        stack.pushPose();
        stack.translate(0, layout.anchor(), layout.depth());
        drawProjectionQuad(stack, quad, color, 64, layout);
        stack.popPose();
    }

    private void renderHologramSurface(PoseStack stack, VertexConsumer quad, HologramColor color,
                                       HologramSurfaceLayout surface, MonitorContentLayout content,
                                       MultiBufferSource buffer) {
        stack.pushPose();
        stack.translate(surface.left(), surface.top(), surface.depth());
        drawColoredQuad(stack, quad, color, 128, 0, 0, surface.width(), surface.height());
        stack.translate(0, 0, 0.001f);
        drawColoredQuad(stack, quad, color, 192, 0, 0, 0.01f, surface.height());
        drawColoredQuad(stack, quad, color, 192, surface.width() - 0.01f, 0, surface.width(), surface.height());
        drawColoredQuad(stack, quad, color, 192, 0, 0, surface.width(), 0.01f);
        if (surface.width() == 1) {
            drawColoredQuad(stack, quad, color, 192, 0, surface.height() - 0.01f, surface.width(), surface.height());
        }
        renderMonitorContent(stack, buffer, surface, content);
        stack.popPose();
    }

    private void renderMonitorContent(PoseStack stack, MultiBufferSource buffer, HologramSurfaceLayout surface,
                                      MonitorContentLayout content) {
        if (!screen.buffer().isRenderingEnabled()) return;
        float border = content.border();
        float textureWidth = surface.width() * 16f;
        float textureHeight = surface.height() * 16f;
        stack.translate(surface.width() * border / textureWidth, surface.height() * border / textureHeight,
            content.liftFromSurface());
        float innerWidth = surface.width() - border / 8;
        float innerHeight = surface.height() - border / 8;
        int renderWidth = screen.buffer().renderWidth();
        int renderHeight = screen.buffer().renderHeight();
        float scaleX = innerWidth / renderWidth;
        float scaleY = innerHeight / renderHeight;
        if (scaleX > scaleY) {
            stack.translate(renderWidth * 0.5f * (scaleX - scaleY), 0, 0);
            stack.scale(scaleY, scaleY, 1);
        } else {
            stack.translate(0, renderHeight * 0.5f * (scaleY - scaleX), 0);
            stack.scale(scaleX, scaleX, 1);
        }
        if (screen.buffer() instanceof TextBuffer componentBuffer) componentBuffer.renderText(stack, buffer);
        else screen.buffer().renderText(stack);
    }

    private void drawProjectionQuad(PoseStack stack, VertexConsumer quad, HologramColor color, int alpha,
                                    ProjectionBeamLayout layout) {
        quad.addVertex(stack.last().pose(), 0, layout.projectorEdgeOffset(), 0).setColor(color.red(), color.green(), color.blue(), alpha);
        quad.addVertex(stack.last().pose(), 1, layout.projectorEdgeOffset(), 0).setColor(color.red(), color.green(), color.blue(), alpha);
        quad.addVertex(stack.last().pose(), (layout.width() + 1) / 2f, 0, 0).setColor(color.red(), color.green(), color.blue(), alpha);
        quad.addVertex(stack.last().pose(), -((layout.width() - 1) / 2f), 0, 0).setColor(color.red(), color.green(), color.blue(), alpha);
    }

    private void drawColoredQuad(PoseStack stack, VertexConsumer quad, HologramColor color, int alpha,
                                 float x1, float y1, float x2, float y2) {
        drawColoredQuad(stack, quad, color.red(), color.green(), color.blue(), alpha, x1, y1, x2, y2);
    }

    private void drawColoredQuad(PoseStack stack, VertexConsumer quad, int red, int green, int blue, int alpha,
                                 float x1, float y1, float x2, float y2) {
        quad.addVertex(stack.last().pose(), x1, y2, 0).setColor(red / 255f, green / 255f, blue / 255f, alpha / 255f);
        quad.addVertex(stack.last().pose(), x2, y2, 0).setColor(red / 255f, green / 255f, blue / 255f, alpha / 255f);
        quad.addVertex(stack.last().pose(), x2, y1, 0).setColor(red / 255f, green / 255f, blue / 255f, alpha / 255f);
        quad.addVertex(stack.last().pose(), x1, y1, 0).setColor(red / 255f, green / 255f, blue / 255f, alpha / 255f);
    }

    private double playerDistanceSq() {
        var player = Minecraft.getInstance().player;
        AABB bounds = getRenderBoundingBox(screen);
        Vec3[] corners = new Vec3[8];
        int index = 0;
        for (double x : new double[]{bounds.minX, bounds.maxX}) {
            for (double y : new double[]{bounds.minY, bounds.maxY}) {
                for (double z : new double[]{bounds.minZ, bounds.maxZ}) {
                    corners[index++] = SableCompat.physicalPosition(screen.getLevel(), new Vec3(x, y, z));
                }
            }
        }
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 corner : corners) {
            minX = Math.min(minX, corner.x); minY = Math.min(minY, corner.y); minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x); maxY = Math.max(maxY, corner.y); maxZ = Math.max(maxZ, corner.z);
        }
        Vec3 playerPosition = SableCompat.physicalPosition(screen.getLevel(), new Vec3(player.getX(), player.getY(), player.getZ()));
        double extentX = maxX - minX, extentY = maxY - minY, extentZ = maxZ - minZ;
        double dx = playerPosition.x - (minX + extentX * 0.5);
        double dy = playerPosition.y - (minY + extentY * 0.5);
        double dz = playerPosition.z - (minZ + extentZ * 0.5);
        return distanceOutsideExtent(dx, extentX) + distanceOutsideExtent(dy, extentY) + distanceOutsideExtent(dz, extentZ);
    }

    private static double distanceOutsideExtent(double delta, double extent) {
        if (delta < -extent) { double distance = delta + extent; return distance * distance; }
        if (delta > extent) { double distance = delta - extent; return distance * distance; }
        return 0.0;
    }
}
