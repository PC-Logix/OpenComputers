package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.blockentity.Transposer;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class TransposerRenderer implements BlockEntityRendererProvider<Transposer> {
    public static final TransposerRenderer INSTANCE = new TransposerRenderer();
    private TransposerRenderer() {}

    @Override
    public BlockEntityRenderer<Transposer> create(Context context) {
        return new TransposerBlockEntityRenderer();
    }
}

final class TransposerBlockEntityRenderer implements BlockEntityRenderer<Transposer> {
    @Override
    public void render(Transposer transposer, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");
        RenderSystem.setShaderColor(1, 1, 1, 1);
        float activity = Math.max(0, 1 - (System.currentTimeMillis() - transposer.lastOperation()) / 1000.0f);
        if (activity > 0) {
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            RenderState.mirrorScale(pose, 1.0025f, -1.0025f, 1.0025f);
            pose.translate(-0.5f, -0.5f, -0.5f);
            var vertices = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY_COLOR);
            var icon = Textures.getSprite(Textures.Block.TransposerOn);
            var matrix = pose.last().pose();
            vertices.addVertex(matrix, 0, 1, 0).setColor(1f, 1f, 1f, activity).setUv(icon.getU1(), icon.getV0());
            vertices.addVertex(matrix, 1, 1, 0).setColor(1f, 1f, 1f, activity).setUv(icon.getU0(), icon.getV0());
            vertices.addVertex(matrix, 1, 1, 1).setColor(1f, 1f, 1f, activity).setUv(icon.getU0(), icon.getV1());
            vertices.addVertex(matrix, 0, 1, 1).setColor(1f, 1f, 1f, activity).setUv(icon.getU1(), icon.getV1());
            vertices.addVertex(matrix, 0, 0, 0).setColor(1f, 1f, 1f, activity).setUv(icon.getU1(), icon.getV1());
            vertices.addVertex(matrix, 0, 0, 1).setColor(1f, 1f, 1f, activity).setUv(icon.getU1(), icon.getV0());
            vertices.addVertex(matrix, 1, 0, 1).setColor(1f, 1f, 1f, activity).setUv(icon.getU0(), icon.getV0());
            vertices.addVertex(matrix, 1, 0, 0).setColor(1f, 1f, 1f, activity).setUv(icon.getU0(), icon.getV1());
            vertices.addVertex(matrix, 1, 1, 0).setColor(1f, 1f, 1f, activity).setUv(icon.getU0(), icon.getV1());
            vertices.addVertex(matrix, 0, 1, 0).setColor(1f, 1f, 1f, activity).setUv(icon.getU1(), icon.getV1());
            vertices.addVertex(matrix, 0, 0, 0).setColor(1f, 1f, 1f, activity).setUv(icon.getU1(), icon.getV0());
            vertices.addVertex(matrix, 1, 0, 0).setColor(1f, 1f, 1f, activity).setUv(icon.getU0(), icon.getV0());
            vertices.addVertex(matrix, 0, 1, 1).setColor(1f, 1f, 1f, activity).setUv(icon.getU0(), icon.getV1());
            vertices.addVertex(matrix, 1, 1, 1).setColor(1f, 1f, 1f, activity).setUv(icon.getU1(), icon.getV1());
            vertices.addVertex(matrix, 1, 0, 1).setColor(1f, 1f, 1f, activity).setUv(icon.getU1(), icon.getV0());
            vertices.addVertex(matrix, 0, 0, 1).setColor(1f, 1f, 1f, activity).setUv(icon.getU0(), icon.getV0());
            vertices.addVertex(matrix, 0, 1, 0).setColor(1f, 1f, 1f, activity).setUv(icon.getU0(), icon.getV1());
            vertices.addVertex(matrix, 0, 1, 1).setColor(1f, 1f, 1f, activity).setUv(icon.getU1(), icon.getV1());
            vertices.addVertex(matrix, 0, 0, 1).setColor(1f, 1f, 1f, activity).setUv(icon.getU1(), icon.getV0());
            vertices.addVertex(matrix, 0, 0, 0).setColor(1f, 1f, 1f, activity).setUv(icon.getU0(), icon.getV0());
            vertices.addVertex(matrix, 1, 1, 1).setColor(1f, 1f, 1f, activity).setUv(icon.getU0(), icon.getV1());
            vertices.addVertex(matrix, 1, 1, 0).setColor(1f, 1f, 1f, activity).setUv(icon.getU1(), icon.getV1());
            vertices.addVertex(matrix, 1, 0, 0).setColor(1f, 1f, 1f, activity).setUv(icon.getU1(), icon.getV0());
            vertices.addVertex(matrix, 1, 0, 1).setColor(1f, 1f, 1f, activity).setUv(icon.getU0(), icon.getV0());
            pose.popPose();
        }
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
