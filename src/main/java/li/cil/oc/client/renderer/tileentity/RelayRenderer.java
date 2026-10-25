package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.blockentity.Relay;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class RelayRenderer implements BlockEntityRendererProvider<Relay> {
    public static final RelayRenderer INSTANCE = new RelayRenderer();

    private RelayRenderer() {}

    @Override
    public BlockEntityRenderer<Relay> create(Context context) {
        return new RelayBlockEntityRenderer();
    }
}

final class RelayBlockEntityRenderer implements BlockEntityRenderer<Relay> {
    @Override
    public void render(Relay relay, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");
        RenderSystem.setShaderColor(1, 1, 1, 1);

        double activity = Math.max(0, 1 - (System.currentTimeMillis() - relay.lastMessage()) / 1000.0);
        if (activity > 0) {
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            RenderState.mirrorScale(pose, 1.0025f, -1.0025f, 1.0025f);
            pose.translate(-0.5f, -0.5f, -0.5f);

            var vertices = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
            var icon = Textures.getSprite(Textures.Block.SwitchSideOn);
            var matrix = pose.last().pose();
            vertices.addVertex(matrix, 1, 1, 0).setUv(icon.getU0(), icon.getV1());
            vertices.addVertex(matrix, 0, 1, 0).setUv(icon.getU1(), icon.getV1());
            vertices.addVertex(matrix, 0, 0, 0).setUv(icon.getU1(), icon.getV0());
            vertices.addVertex(matrix, 1, 0, 0).setUv(icon.getU0(), icon.getV0());

            vertices.addVertex(matrix, 0, 1, 1).setUv(icon.getU0(), icon.getV1());
            vertices.addVertex(matrix, 1, 1, 1).setUv(icon.getU1(), icon.getV1());
            vertices.addVertex(matrix, 1, 0, 1).setUv(icon.getU1(), icon.getV0());
            vertices.addVertex(matrix, 0, 0, 1).setUv(icon.getU0(), icon.getV0());

            vertices.addVertex(matrix, 1, 1, 1).setUv(icon.getU0(), icon.getV1());
            vertices.addVertex(matrix, 1, 1, 0).setUv(icon.getU1(), icon.getV1());
            vertices.addVertex(matrix, 1, 0, 0).setUv(icon.getU1(), icon.getV0());
            vertices.addVertex(matrix, 1, 0, 1).setUv(icon.getU0(), icon.getV0());

            vertices.addVertex(matrix, 0, 1, 0).setUv(icon.getU0(), icon.getV1());
            vertices.addVertex(matrix, 0, 1, 1).setUv(icon.getU1(), icon.getV1());
            vertices.addVertex(matrix, 0, 0, 1).setUv(icon.getU1(), icon.getV0());
            vertices.addVertex(matrix, 0, 0, 0).setUv(icon.getU0(), icon.getV0());
            pose.popPose();
        }

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
