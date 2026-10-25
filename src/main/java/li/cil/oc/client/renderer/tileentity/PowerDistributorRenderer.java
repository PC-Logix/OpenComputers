package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.blockentity.PowerDistributor;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class PowerDistributorRenderer implements BlockEntityRendererProvider<PowerDistributor> {
    public static final PowerDistributorRenderer INSTANCE = new PowerDistributorRenderer();

    private PowerDistributorRenderer() {}

    @Override
    public BlockEntityRenderer<PowerDistributor> create(Context context) {
        return new PowerDistributorBlockEntityRenderer();
    }
}

final class PowerDistributorBlockEntityRenderer implements BlockEntityRenderer<PowerDistributor> {
    @Override
    public void render(PowerDistributor distributor, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");
        RenderSystem.setShaderColor(1, 1, 1, 1);
        if (distributor.globalBuffer() > 0) {
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            RenderState.mirrorScale(pose, 1.0025f, -1.0025f, 1.0025f);
            pose.translate(-0.5f, -0.5f, -0.5f);

            var vertices = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
            var matrix = pose.last().pose();
            var top = Textures.getSprite(Textures.Block.PowerDistributorTopOn);
            vertices.addVertex(matrix, 0, 0, 1).setUv(top.getU0(), top.getV1());
            vertices.addVertex(matrix, 1, 0, 1).setUv(top.getU1(), top.getV1());
            vertices.addVertex(matrix, 1, 0, 0).setUv(top.getU1(), top.getV0());
            vertices.addVertex(matrix, 0, 0, 0).setUv(top.getU0(), top.getV0());

            var side = Textures.getSprite(Textures.Block.PowerDistributorSideOn);
            vertices.addVertex(matrix, 1, 1, 0).setUv(side.getU0(), side.getV1());
            vertices.addVertex(matrix, 0, 1, 0).setUv(side.getU1(), side.getV1());
            vertices.addVertex(matrix, 0, 0, 0).setUv(side.getU1(), side.getV0());
            vertices.addVertex(matrix, 1, 0, 0).setUv(side.getU0(), side.getV0());
            vertices.addVertex(matrix, 0, 1, 1).setUv(side.getU0(), side.getV1());
            vertices.addVertex(matrix, 1, 1, 1).setUv(side.getU1(), side.getV1());
            vertices.addVertex(matrix, 1, 0, 1).setUv(side.getU1(), side.getV0());
            vertices.addVertex(matrix, 0, 0, 1).setUv(side.getU0(), side.getV0());
            vertices.addVertex(matrix, 1, 1, 1).setUv(side.getU0(), side.getV1());
            vertices.addVertex(matrix, 1, 1, 0).setUv(side.getU1(), side.getV1());
            vertices.addVertex(matrix, 1, 0, 0).setUv(side.getU1(), side.getV0());
            vertices.addVertex(matrix, 1, 0, 1).setUv(side.getU0(), side.getV0());
            vertices.addVertex(matrix, 0, 1, 0).setUv(side.getU0(), side.getV1());
            vertices.addVertex(matrix, 0, 1, 1).setUv(side.getU1(), side.getV1());
            vertices.addVertex(matrix, 0, 0, 1).setUv(side.getU1(), side.getV0());
            vertices.addVertex(matrix, 0, 0, 0).setUv(side.getU0(), side.getV0());
            pose.popPose();
        }
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
