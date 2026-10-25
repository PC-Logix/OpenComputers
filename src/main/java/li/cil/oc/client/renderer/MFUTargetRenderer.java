package li.cil.oc.client.renderer;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.Constants;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.RenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public final class MFUTargetRenderer {
    public static final MFUTargetRenderer INSTANCE = new MFUTargetRenderer();
    private static final float RED = 0;
    private static final float GREEN = 1;
    private static final float BLUE = 0;

    private MFUTargetRenderer() {}

    @SubscribeEvent
    public void onRenderWorldLastEvent(RenderLevelStageEvent event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) return;
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        var item = li.cil.oc.api.Items.get(stack);
        var mfu = li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.MFU());
        var component = OCComponentsJava.MF_COORD.get();
        if (item != mfu || !stack.has(component)) return;
        var coords = stack.get(component);
        var dimension = coords.dimension();
        var blockPos = coords.blockPos();
        Direction side = coords.side();
        if (!player.level().dimension().location().equals(dimension)) return;
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();
        if (player.distanceToSqr(x, y, z) > 64 * 64) return;
        var bounds = new BlockPosition(x, y, z).bounds().inflate(0.1, 0.1, 0.1);

        RenderState.checkError(getClass().getName() + ".onRenderWorldLastEvent: entering (aka: wasntme)");
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        var camera = minecraft.gameRenderer.getMainCamera().getPosition();
        pose.translate(-camera.x, -camera.y, -camera.z);
        RenderSystem.disableDepthTest();
        var buffer = minecraft.renderBuffers().bufferSource();
        drawBox(pose.last().pose(), buffer.getBuffer(RenderTypes.MFU_LINES),
            (float) bounds.minX, (float) bounds.minY, (float) bounds.minZ,
            (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ, RED, GREEN, BLUE);
        drawFace(pose.last().pose(), buffer.getBuffer(RenderTypes.MFU_QUADS),
            (float) bounds.minX, (float) bounds.minY, (float) bounds.minZ,
            (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ, side, RED, GREEN, BLUE);
        buffer.endBatch();
        pose.popPose();
        RenderState.checkError(getClass().getName() + ".onRenderWorldLastEvent: leaving");
    }

    public static void drawBox(Matrix4f matrix, VertexConsumer builder,
                               float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                               float red, float green, float blue) {
        vertex(matrix, builder, minX, minY, minZ, red, green, blue, 0.5f);
        vertex(matrix, builder, minX, minY, maxZ, red, green, blue, 0.5f);
        vertex(matrix, builder, minX, minY, maxZ, red, green, blue, 0.5f);
        vertex(matrix, builder, maxX, minY, maxZ, red, green, blue, 0.5f);
        vertex(matrix, builder, maxX, minY, maxZ, red, green, blue, 0.5f);
        vertex(matrix, builder, maxX, minY, minZ, red, green, blue, 0.5f);
        vertex(matrix, builder, maxX, minY, minZ, red, green, blue, 0.5f);
        vertex(matrix, builder, minX, minY, minZ, red, green, blue, 0.5f);
        vertex(matrix, builder, minX, minY, minZ, red, green, blue, 0.5f);
        vertex(matrix, builder, minX, maxY, minZ, red, green, blue, 0.5f);
        vertex(matrix, builder, maxX, minY, minZ, red, green, blue, 0.5f);
        vertex(matrix, builder, maxX, maxY, minZ, red, green, blue, 0.5f);
        vertex(matrix, builder, maxX, minY, maxZ, red, green, blue, 0.5f);
        vertex(matrix, builder, maxX, maxY, maxZ, red, green, blue, 0.5f);
        vertex(matrix, builder, minX, minY, maxZ, red, green, blue, 0.5f);
        vertex(matrix, builder, minX, maxY, maxZ, red, green, blue, 0.5f);
        vertex(matrix, builder, maxX, maxY, minZ, red, green, blue, 0.5f);
        vertex(matrix, builder, maxX, maxY, maxZ, red, green, blue, 0.5f);
        vertex(matrix, builder, maxX, maxY, maxZ, red, green, blue, 0.5f);
        vertex(matrix, builder, minX, maxY, maxZ, red, green, blue, 0.5f);
        vertex(matrix, builder, minX, maxY, maxZ, red, green, blue, 0.5f);
        vertex(matrix, builder, minX, maxY, minZ, red, green, blue, 0.5f);
        vertex(matrix, builder, minX, maxY, minZ, red, green, blue, 0.5f);
        vertex(matrix, builder, maxX, maxY, minZ, red, green, blue, 0.5f);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer builder, float x, float y, float z,
                               float red, float green, float blue, float alpha) {
        builder.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
    }

    private static void drawFace(Matrix4f matrix, VertexConsumer builder,
                                 float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                 Direction side, float red, float green, float blue) {
        switch (side) {
            case DOWN -> quad(matrix, builder, red, green, blue, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ);
            case UP -> quad(matrix, builder, red, green, blue, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
            case NORTH -> quad(matrix, builder, red, green, blue, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ);
            case SOUTH -> quad(matrix, builder, red, green, blue, maxX, maxY, maxZ, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ);
            case EAST -> quad(matrix, builder, red, green, blue, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ);
            case WEST -> quad(matrix, builder, red, green, blue, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        }
    }

    private static void quad(Matrix4f matrix, VertexConsumer builder, float red, float green, float blue,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4) {
        builder.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, 0.25f);
        builder.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, 0.25f);
        builder.addVertex(matrix, x3, y3, z3).setColor(red, green, blue, 0.25f);
        builder.addVertex(matrix, x4, y4, z4).setColor(red, green, blue, 0.25f);
    }
}
