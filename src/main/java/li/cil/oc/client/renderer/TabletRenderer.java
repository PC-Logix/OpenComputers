package li.cil.oc.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.item.TabletCache;
import li.cil.oc.common.item.TabletWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;
import java.util.Optional;

public final class TabletRenderer {
    public static final TabletRenderer INSTANCE = new TabletRenderer();
    private static final int FRAME_BORDER = 12;
    private static final int BOTTOM_BORDER = 20;
    private static final float OFFHAND_FACE_SIZE = 1.26f;
    private static final float CENTER_FACE_SIZE = 1f;

    private TabletRenderer() {}

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        if (Items.get(event.getItemStack()) != Items.get(Constants.ItemName$.MODULE$.Tablet())) return;

        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        Optional<TabletWrapper> wrapper = TabletCache.getClientOptional(event.getItemStack());
        if (wrapper.isEmpty() && player != null && TabletCache.getIdOptional(event.getItemStack()).isPresent()) {
            wrapper = Optional.ofNullable(TabletCache.getClient(event.getItemStack(), player));
        }

        Optional<TextBuffer> textBuffer = Optional.empty();
        boolean powered = false;
        if (wrapper.isPresent()) {
            for (Optional<li.cil.oc.api.network.ManagedEnvironment> slot : ComponentInventorySlotAccessJava.snapshot(wrapper.get())) {
                if (slot.isPresent() && slot.get() instanceof TextBuffer buffer) {
                    textBuffer = Optional.of(buffer);
                    break;
                }
            }
            powered = wrapper.get().data().isRunning;
        }

        renderFirstPerson(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
            event.getHand(), event.getInterpolatedPitch(), event.getEquipProgress(), event.getSwingProgress(),
            textBuffer, powered);
        event.setCanceled(true);
    }

    private static void renderFirstPerson(PoseStack stack, MultiBufferSource renderBuffer, int packedLight,
                                          InteractionHand hand, float pitch, float equipProgress, float swingProgress,
                                          Optional<TextBuffer> textBuffer, boolean powered) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        stack.pushPose();
        if (hand == InteractionHand.MAIN_HAND && player.getOffhandItem().isEmpty()) {
            renderCentered(stack, renderBuffer, packedLight, pitch, equipProgress, swingProgress, textBuffer, powered);
        } else {
            HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            renderAtSide(stack, renderBuffer, packedLight, arm, equipProgress, swingProgress, textBuffer, powered, OFFHAND_FACE_SIZE);
        }
        stack.popPose();
    }

    private static void renderAtSide(PoseStack stack, MultiBufferSource renderBuffer, int packedLight,
                                    HumanoidArm arm, float equipProgress, float swingProgress,
                                    Optional<TextBuffer> textBuffer, boolean powered, float faceSize) {
        Minecraft minecraft = Minecraft.getInstance();
        float side = arm == HumanoidArm.RIGHT ? 1f : -1f;

        stack.translate(side * 0.125f, -0.125f, 0f);
        if (!minecraft.player.isInvisible()) {
            stack.pushPose();
            stack.mulPose(Axis.ZP.rotationDegrees(side * 10f));
            renderPlayerArm(stack, renderBuffer, packedLight, equipProgress, swingProgress, arm);
            stack.popPose();
        }

        stack.pushPose();
        stack.translate(side * 0.51f, -0.08f + equipProgress * -1.2f, -0.75f);
        float swingRoot = Mth.sqrt(swingProgress);
        float swing = Mth.sin(swingRoot * (float) Math.PI);
        stack.translate(side * (-0.5f * swing),
            0.4f * Mth.sin(swingRoot * (float) Math.PI * 2f) - 0.3f * swing,
            -0.3f * Mth.sin(swingProgress * (float) Math.PI));
        stack.mulPose(Axis.XP.rotationDegrees(swing * -45f));
        stack.mulPose(Axis.YP.rotationDegrees(side * swing * -30f));
        renderTablet(stack, renderBuffer, textBuffer, powered, faceSize);
        stack.popPose();
    }

    private static void renderCentered(PoseStack stack, MultiBufferSource renderBuffer, int packedLight,
                                      float pitch, float equipProgress, float swingProgress,
                                      Optional<TextBuffer> textBuffer, boolean powered) {
        Minecraft minecraft = Minecraft.getInstance();
        float swingRoot = Mth.sqrt(swingProgress);

        stack.translate(0f, -(-0.2f * Mth.sin(swingProgress * (float) Math.PI)) / 2f,
            -0.4f * Mth.sin(swingRoot * (float) Math.PI));
        float tilt = calculateMapTilt(pitch);
        stack.translate(0f, 0.04f + equipProgress * -1.2f + tilt * -0.5f, -0.72f);
        stack.mulPose(Axis.XP.rotationDegrees(tilt * -85f));

        if (!minecraft.player.isInvisible()) {
            stack.pushPose();
            stack.mulPose(Axis.YP.rotationDegrees(90f));
            renderMapHand(stack, renderBuffer, packedLight, HumanoidArm.RIGHT);
            renderMapHand(stack, renderBuffer, packedLight, HumanoidArm.LEFT);
            stack.popPose();
        }

        stack.mulPose(Axis.XP.rotationDegrees(Mth.sin(swingRoot * (float) Math.PI) * 20f));
        stack.scale(2f, 2f, 2f);
        renderTablet(stack, renderBuffer, textBuffer, powered, CENTER_FACE_SIZE);
    }

    private static float calculateMapTilt(float pitch) {
        float tilt = Mth.clamp(1f - pitch / 45f + 0.1f, 0f, 1f);
        return -Mth.cos(tilt * (float) Math.PI) * 0.5f + 0.5f;
    }

    private static void renderMapHand(PoseStack stack, MultiBufferSource renderBuffer, int packedLight, HumanoidArm arm) {
        Minecraft minecraft = Minecraft.getInstance();
        PlayerRenderer playerRenderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(minecraft.player);
        float side = arm == HumanoidArm.RIGHT ? 1f : -1f;

        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(92f));
        stack.mulPose(Axis.XP.rotationDegrees(45f));
        stack.mulPose(Axis.ZP.rotationDegrees(side * -41f));
        stack.translate(side * 0.3f, -1.1f, 0.45f);
        if (arm == HumanoidArm.RIGHT) playerRenderer.renderRightHand(stack, renderBuffer, packedLight, minecraft.player);
        else playerRenderer.renderLeftHand(stack, renderBuffer, packedLight, minecraft.player);
        stack.popPose();
    }

    private static void renderPlayerArm(PoseStack stack, MultiBufferSource renderBuffer, int packedLight,
                                        float equipProgress, float swingProgress, HumanoidArm arm) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean right = arm != HumanoidArm.LEFT;
        float side = right ? 1f : -1f;
        float swingRoot = Mth.sqrt(swingProgress);
        float swingX = -0.3f * Mth.sin(swingRoot * (float) Math.PI);
        float swingY = 0.4f * Mth.sin(swingRoot * (float) Math.PI * 2f);
        float swingZ = -0.4f * Mth.sin(swingProgress * (float) Math.PI);

        stack.translate(side * (swingX + 0.64000005f), swingY - 0.6f + equipProgress * -0.6f, swingZ - 0.71999997f);
        stack.mulPose(Axis.YP.rotationDegrees(side * 45f));
        stack.mulPose(Axis.YP.rotationDegrees(side * Mth.sin(swingRoot * (float) Math.PI) * 70f));
        stack.mulPose(Axis.ZP.rotationDegrees(side * Mth.sin(swingProgress * swingProgress * (float) Math.PI) * -20f));
        stack.translate(side * -1f, 3.6f, 3.5f);
        stack.mulPose(Axis.ZP.rotationDegrees(side * 120f));
        stack.mulPose(Axis.XP.rotationDegrees(200f));
        stack.mulPose(Axis.YP.rotationDegrees(side * -135f));
        stack.translate(side * 5.6f, 0f, 0f);

        PlayerRenderer playerRenderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(minecraft.player);
        if (right) playerRenderer.renderRightHand(stack, renderBuffer, packedLight, minecraft.player);
        else playerRenderer.renderLeftHand(stack, renderBuffer, packedLight, minecraft.player);
    }

    private static void renderTablet(PoseStack stack, MultiBufferSource renderBuffer, Optional<TextBuffer> textBuffer,
                                    boolean powered, float faceSize) {
        int width = textBuffer.isPresent() ? textBuffer.get().renderWidth() : 160;
        int height = textBuffer.isPresent() ? textBuffer.get().renderHeight() : 90;
        if (width <= 0 || height <= 0) return;

        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(180f));
        stack.mulPose(Axis.ZP.rotationDegrees(180f));
        stack.scale(0.5f, 0.5f, 0.5f);

        float scale = faceSize / Math.max(width + FRAME_BORDER * 2, height + FRAME_BORDER + BOTTOM_BORDER);
        stack.scale(scale, scale, -1f);
        stack.translate(-width * 0.5, -height * 0.5, 0);
        renderFrame(stack, renderBuffer, width, height, powered);

        if (textBuffer.isPresent() && textBuffer.get().isRenderingEnabled()) {
            TextBuffer buffer = textBuffer.get();
            stack.translate(0, 0, 0.002f);
            if (buffer instanceof li.cil.oc.common.component.TextBuffer componentBuffer) componentBuffer.renderText(stack, renderBuffer);
            else buffer.renderText(stack);
        }
        stack.popPose();
    }

    private static void renderFrame(PoseStack stack, MultiBufferSource renderBuffer, int width, int height, boolean powered) {
        VertexConsumer builder = renderBuffer.getBuffer(RenderTypes.FONT_QUAD);
        var matrix = stack.last().pose();
        int frameWidth = width + FRAME_BORDER * 2;
        int frameHeight = height + FRAME_BORDER + BOTTOM_BORDER;
        quad(builder, matrix, -FRAME_BORDER, -FRAME_BORDER, frameWidth, frameHeight, 0f, 0x25262D);
        quad(builder, matrix, -FRAME_BORDER + 2, -FRAME_BORDER + 2, frameWidth - 4, 2, 0.0005f, 0x4A4C57);
        quad(builder, matrix, -FRAME_BORDER + 2, -FRAME_BORDER + 4, 2, frameHeight - 8, 0.0005f, 0x3A3C46);
        quad(builder, matrix, -FRAME_BORDER + 2, height + BOTTOM_BORDER - 4, frameWidth - 4, 2, 0.0005f, 0x17181D);
        quad(builder, matrix, width + FRAME_BORDER - 4, -FRAME_BORDER + 4, 2, frameHeight - 8, 0.0005f, 0x17181D);
        quad(builder, matrix, 0, 0, width, height, 0.001f, 0x000000);
        quad(builder, matrix, width - 8, height + 5, 5, 5, 0.001f, powered ? 0x66DD55 : 0x333333);
    }

    private static void quad(VertexConsumer builder, org.joml.Matrix4f matrix, int x, int y, int width, int height,
                             float z, int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        builder.addVertex(matrix, x, y + height, z).setUv(0f, 1f).setColor(red, green, blue, 255);
        builder.addVertex(matrix, x + width, y + height, z).setUv(1f, 1f).setColor(red, green, blue, 255);
        builder.addVertex(matrix, x + width, y, z).setUv(1f, 0f).setColor(red, green, blue, 255);
        builder.addVertex(matrix, x, y, z).setUv(0f, 0f).setColor(red, green, blue, 255);
    }
}
