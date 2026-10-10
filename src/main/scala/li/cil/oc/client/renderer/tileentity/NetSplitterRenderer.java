package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.client.Textures;
import li.cil.oc.client.Textures$;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.blockentity.NetSplitter;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

public class NetSplitterRenderer implements BlockEntityRenderer<NetSplitter> {
    public NetSplitterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NetSplitter splitter, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering");
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        boolean hasVisibleSide = false;
        for (boolean open : splitter.openSides()) {
            if (open == !splitter.isInverted()) {
                hasVisibleSide = true;
                break;
            }
        }
        if (hasVisibleSide) {
            stack.pushPose();
            stack.translate(0.5, 0.5, 0.5);
            RenderState.mirrorScale(stack, 1.0025f, -1.0025f, 1.0025f);
            stack.translate(-0.5, -0.5, -0.5);
            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);

            VertexConsumer vertices = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
            TextureAtlasSprite icon = Textures$.MODULE$.getSprite(Textures.Block$.MODULE$.NetSplitterOn());
            Matrix4f matrix = stack.last().pose();

            if (splitter.isSideOpen(Direction.DOWN)) {
                vertex(vertices, matrix, 0, 1, 0, icon.getU1(), icon.getV0());
                vertex(vertices, matrix, 1, 1, 0, icon.getU0(), icon.getV0());
                vertex(vertices, matrix, 1, 1, 1, icon.getU0(), icon.getV1());
                vertex(vertices, matrix, 0, 1, 1, icon.getU1(), icon.getV1());
            }
            if (splitter.isSideOpen(Direction.UP)) {
                vertex(vertices, matrix, 0, 0, 0, icon.getU1(), icon.getV1());
                vertex(vertices, matrix, 0, 0, 1, icon.getU1(), icon.getV0());
                vertex(vertices, matrix, 1, 0, 1, icon.getU0(), icon.getV0());
                vertex(vertices, matrix, 1, 0, 0, icon.getU0(), icon.getV1());
            }
            if (splitter.isSideOpen(Direction.NORTH)) {
                vertex(vertices, matrix, 1, 1, 0, icon.getU0(), icon.getV1());
                vertex(vertices, matrix, 0, 1, 0, icon.getU1(), icon.getV1());
                vertex(vertices, matrix, 0, 0, 0, icon.getU1(), icon.getV0());
                vertex(vertices, matrix, 1, 0, 0, icon.getU0(), icon.getV0());
            }
            if (splitter.isSideOpen(Direction.SOUTH)) {
                vertex(vertices, matrix, 0, 1, 1, icon.getU0(), icon.getV1());
                vertex(vertices, matrix, 1, 1, 1, icon.getU1(), icon.getV1());
                vertex(vertices, matrix, 1, 0, 1, icon.getU1(), icon.getV0());
                vertex(vertices, matrix, 0, 0, 1, icon.getU0(), icon.getV0());
            }
            if (splitter.isSideOpen(Direction.WEST)) {
                vertex(vertices, matrix, 0, 1, 0, icon.getU0(), icon.getV1());
                vertex(vertices, matrix, 0, 1, 1, icon.getU1(), icon.getV1());
                vertex(vertices, matrix, 0, 0, 1, icon.getU1(), icon.getV0());
                vertex(vertices, matrix, 0, 0, 0, icon.getU0(), icon.getV0());
            }
            if (splitter.isSideOpen(Direction.EAST)) {
                vertex(vertices, matrix, 1, 1, 1, icon.getU0(), icon.getV1());
                vertex(vertices, matrix, 1, 1, 0, icon.getU1(), icon.getV1());
                vertex(vertices, matrix, 1, 0, 0, icon.getU1(), icon.getV0());
                vertex(vertices, matrix, 1, 0, 1, icon.getU0(), icon.getV0());
            }
            stack.popPose();
        }
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, int x, int y, int z, float u, float v) {
        vertices.addVertex(matrix, x, y, z).setUv(u, v);
    }
}
