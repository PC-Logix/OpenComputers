package li.cil.oc.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.client.Textures;
import li.cil.oc.common.entity.Drone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class DroneRenderer extends EntityRenderer<Drone> {
    private final ModelQuadcopter model;

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new ModelQuadcopter(context.bakeLayer(ModelQuadcopter.LAYER_LOCATION));
    }

    @Override
    public void render(Drone entity, float yaw, float partialTick, PoseStack stack, MultiBufferSource buffer, int light) {
        var renderType = getRenderType(entity);
        if (renderType != null) {
            stack.pushPose();
            stack.translate(0, 2f / 16f, 0);
            var builder = buffer.getBuffer(renderType);
            model.prepareMobModel(entity, 0, 0, partialTick);
            float xRot = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());
            float yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            model.setupAnim(entity, 0, 0, entity.tickCount, yRot, xRot);
            model.renderToBuffer(stack, builder, light, OverlayTexture.NO_OVERLAY);
            stack.popPose();
        }
        super.render(entity, yaw, partialTick, stack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Drone entity) { return Textures.Model.Drone; }

    public RenderType getRenderType(Drone entity) {
        var minecraft = Minecraft.getInstance();
        var texture = getTextureLocation(entity);
        if (!entity.isInvisible()) return model.renderType(texture);
        if (!entity.isInvisibleTo(minecraft.player)) return RenderType.itemEntityTranslucentCull(texture);
        if (minecraft.shouldEntityAppearGlowing(entity)) return RenderType.outline(texture);
        return null;
    }
}
