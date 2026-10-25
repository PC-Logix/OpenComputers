package li.cil.oc.client.renderer.tileentity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.client.gui.Screen;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.blockentity.Projector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import scala.Function0;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class ProjectorRenderer implements BlockEntityRendererProvider<Projector>, BlockEntityRenderer<Projector> {
    private static final double PROJECTION_WIDTH = 13.0;
    private static final double PROJECTION_HEIGHT = 8.125;
    private static final double MAX_PROJECTION_DISTANCE = 12.5;
    private static final Map<Long, Projector> RENDERED_PROJECTORS = new LinkedHashMap<>();
    private static final Cache<Projector, ProjectorTexture> TEXTURES = createTextureCache();

    public static final ProjectorRenderer INSTANCE = new ProjectorRenderer();

    private ProjectorRenderer() {}

    private static Cache<Projector, ProjectorTexture> createTextureCache() {
        RemovalListener<Projector, ProjectorTexture> listener = notification -> notification.getValue().close();
        return CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .removalListener(listener)
            .build();
    }

    @Override
    public BlockEntityRenderer<Projector> create(BlockEntityRendererProvider.Context context) {
        return new ProjectorRenderer();
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        TEXTURES.cleanUp();
    }

    public static ProjectorTexture textureFor(Projector projector) {
        try {
            return TEXTURES.get(projector, ProjectorTexture::new);
        } catch (ExecutionException exception) {
            throw new RuntimeException(exception.getCause());
        }
    }

    private static void markRendered(Projector projector) {
        RENDERED_PROJECTORS.put(projector.getBlockPos().asLong(), projector);
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) RENDERED_PROJECTORS.clear();
    }

    /** Opens the regular OC screen when the player uses the projected screen. */
    @SubscribeEvent
    public void onInteractionKeyMapping(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || RENDERED_PROJECTORS.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.level == null || player == null || minecraft.screen != null || minecraft.gameMode == null) return;

        float partialTicks = minecraft.getTimer().getGameTimeDeltaTicks();
        Vec3 start = player.getEyePosition(partialTicks);
        double reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue();
        Vec3 end = start.add(player.getViewVector(partialTicks).scale(reach));

        Projector nearestProjector = null;
        ProjectorHit nearestHit = null;
        for (Projector projector : RENDERED_PROJECTORS.values()) {
            if (projector.getLevel() != minecraft.level || projector.isRemoved() || !projector.isScreenMode()) continue;
            ProjectorHit hit = projectedHit(projector, start, end);
            if (hit != null && (nearestHit == null || hit.t() < nearestHit.t())) {
                nearestProjector = projector;
                nearestHit = hit;
            }
        }

        if (nearestProjector != null) {
            Projector projector = nearestProjector;
            Function0<Object> hasKeyboard = () -> Boolean.TRUE;
            Function0<Object> hasPower = () -> projector.screenBuffer().isRenderingEnabled();
            minecraft.pushGuiLayer(new Screen(projector.screenBuffer(), true, hasKeyboard, hasPower));
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    private static ProjectorHit projectedHit(Projector projector, Vec3 start, Vec3 end) {
        PoseStack stack = new PoseStack();
        var pos = projector.getBlockPos();
        stack.translate(pos.getX(), pos.getY(), pos.getZ());
        stack.translate(0.5, 0.5, 0.5);
        Direction direction = projector.getProjectionDirection();
        if (direction == Direction.WEST) stack.mulPose(Axis.YP.rotationDegrees(-90));
        else if (direction == Direction.NORTH) stack.mulPose(Axis.YP.rotationDegrees(180));
        else if (direction == Direction.EAST) stack.mulPose(Axis.YP.rotationDegrees(90));
        stack.translate(0, 0, projectionDistance(projector));

        Matrix4f inverse = new Matrix4f(stack.last().pose()).invert();
        Vector4f a = transform(inverse, start);
        Vector4f b = transform(inverse, end);
        float dz = b.z - a.z;
        if (Math.abs(dz) < 1.0e-5f) return null;

        float t = -a.z / dz;
        if (t < 0 || t > 1) return null;

        float x = a.x + (b.x - a.x) * t;
        float y = a.y + (b.y - a.y) * t;
        double projectionScale = projectionDistance(projector) / MAX_PROJECTION_DISTANCE;
        double scaledPlaneWidth = PROJECTION_WIDTH * projectionScale;
        double scaledPlaneHeight = PROJECTION_HEIGHT * projectionScale;
        int renderWidth = Math.max(1, projector.screenBuffer().renderWidth());
        int renderHeight = Math.max(1, projector.screenBuffer().renderHeight());
        double scale = Math.min(scaledPlaneWidth / renderWidth, scaledPlaneHeight / renderHeight);
        double contentWidth = renderWidth * scale;
        double contentHeight = renderHeight * scale;
        double left = (scaledPlaneWidth - contentWidth) * 0.5;
        double top = (scaledPlaneHeight - contentHeight) * 0.5;
        double localX = x + scaledPlaneWidth * 0.5;
        double localY = scaledPlaneHeight * 0.5 - y;

        if (localX < left || localX >= left + contentWidth || localY < top || localY >= top + contentHeight) return null;
        double bufferX = (localX - left) / contentWidth * projector.screenBuffer().getViewportWidth();
        double bufferY = (localY - top) / contentHeight * projector.screenBuffer().getViewportHeight();
        return new ProjectorHit(t, bufferX, bufferY);
    }

    private static Vector4f transform(Matrix4f matrix, Vec3 value) {
        return matrix.transform(new Vector4f((float) value.x, (float) value.y, (float) value.z, 1f));
    }

    private static float projectionDistance(Projector projector) {
        double rayStartOffset = 0.501;
        double surfaceOffset = 0.001;
        var blockPos = projector.getBlockPos();
        Direction direction = projector.getProjectionDirection();
        Vec3 origin = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        Vec3 step = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        Vec3 rayStart = origin.add(step.scale(rayStartOffset));
        Vec3 rayEnd = origin.add(step.scale(MAX_PROJECTION_DISTANCE));
        HitResult hit = projector.getLevel().clip(new ClipContext(
            rayStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return (float) Math.max(rayStartOffset, hit.getLocation().distanceTo(origin) - surfaceOffset);
        }
        return (float) MAX_PROJECTION_DISTANCE;
    }

    @Override
    public boolean shouldRenderOffScreen(Projector projector) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(Projector projector) {
        return projector.projectionBounds();
    }

    @Override
    public void render(Projector projector, float partialTick, PoseStack stack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!projector.isOn()) return;
        if (!projector.isScreenMode() && !projector.hasPower()) return;

        stack.pushPose();
        stack.translate(0.5, 0.5, 0.5);
        Direction direction = projector.getProjectionDirection();
        if (direction == Direction.WEST) stack.mulPose(Axis.YP.rotationDegrees(-90));
        else if (direction == Direction.NORTH) stack.mulPose(Axis.YP.rotationDegrees(180));
        else if (direction == Direction.EAST) stack.mulPose(Axis.YP.rotationDegrees(90));
        stack.translate(0, 0, projectionDistance(projector));

        if (projector.isScreenMode()) {
            markRendered(projector);
            renderScreen(projector, stack, buffer);
        } else {
            renderPixels(projector, stack, buffer);
        }
        stack.popPose();
    }

    private static void renderPixels(Projector projector, PoseStack stack, MultiBufferSource buffer) {
        ProjectorTexture texture = textureFor(projector);
        texture.update(projector);

        double projectionScale = projectionDistance(projector) / MAX_PROJECTION_DISTANCE;
        float halfWidth = (float) (PROJECTION_WIDTH * projectionScale * 0.5);
        float halfHeight = (float) (PROJECTION_HEIGHT * projectionScale * 0.5);
        var matrix = stack.last().pose();
        var vertices = buffer.getBuffer(texture.renderType());
        vertices.addVertex(matrix, -halfWidth, halfHeight, 0).setColor(255, 255, 255, 255).setUv(1, 0);
        vertices.addVertex(matrix, halfWidth, halfHeight, 0).setColor(255, 255, 255, 255).setUv(0, 0);
        vertices.addVertex(matrix, halfWidth, -halfHeight, 0).setColor(255, 255, 255, 255).setUv(0, 1);
        vertices.addVertex(matrix, -halfWidth, -halfHeight, 0).setColor(255, 255, 255, 255).setUv(1, 1);
    }

    private static void renderScreen(Projector projector, PoseStack stack, MultiBufferSource buffer) {
        var screenBuffer = projector.screenBuffer();
        if (!screenBuffer.isRenderingEnabled()) return;

        double projectionScale = projectionDistance(projector) / MAX_PROJECTION_DISTANCE;
        float planeWidth = (float) (PROJECTION_WIDTH * projectionScale);
        float planeHeight = (float) (PROJECTION_HEIGHT * projectionScale);
        int renderWidth = Math.max(1, screenBuffer.renderWidth());
        int renderHeight = Math.max(1, screenBuffer.renderHeight());
        float scale = Math.min(planeWidth / renderWidth, planeHeight / renderHeight);

        stack.pushPose();
        stack.translate(renderWidth * scale * 0.5f, renderHeight * scale * 0.5f, 0);
        stack.scale(-scale, -scale, 1);
        screenBuffer.renderText(stack, buffer);
        stack.popPose();
    }
}

final class ProjectorTexture implements AutoCloseable {
    private final DynamicTexture texture = new DynamicTexture(320, 200, false);
    private final String name = "oc_projector_" + System.nanoTime();
    private final ResourceLocation location = Minecraft.getInstance().getTextureManager().register(name, texture);
    private final RenderType renderType = RenderTypes.createFontTex(texture.getId());

    public RenderType renderType() { return renderType; }

    @Override
    public void close() {
        texture.close();
        Minecraft.getInstance().getTextureManager().release(location);
    }

    public void update(Projector projector) {
        if (!projector.clientPixelsDirty()) return;
        synchronized (projector) {
            var image = texture.getPixels();
            for (int y = 0; y < projector.height(); y++) {
                for (int x = 0; x < projector.width(); x++) {
                    int color = projector.pixels()[x + y * projector.width()];
                    int alpha = (color >>> 24) & 0xFF;
                    int red = (color >>> 16) & 0xFF;
                    int green = (color >>> 8) & 0xFF;
                    int blue = color & 0xFF;
                    image.setPixelRGBA(x, y, (alpha << 24) | (blue << 16) | (green << 8) | red);
                }
            }
            texture.upload();
            projector.clientPixelsDirty_$eq(false);
        }
    }
}
