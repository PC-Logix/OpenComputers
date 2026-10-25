package li.cil.oc.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.tileentity.RobotRenderer;
import li.cil.oc.common.entity.TrainRobot;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.lang.reflect.Method;

public final class TrainRobotRenderer extends EntityRenderer<TrainRobot> {
    private final Method createHatRenderer;

    public TrainRobotRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
        Method method;
        try {
            var rendererClass = Class.forName("li.cil.oc.client.renderer.entity.CreateTrainRobotHatRenderer");
            method = rendererClass.getMethod("render", TrainRobot.class, PoseStack.class,
                    MultiBufferSource.class, Integer.TYPE);
        } catch (Throwable ignored) {
            method = null;
        }
        createHatRenderer = method;
    }

    @Override
    public void render(TrainRobot entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        pose.pushPose();
        float renderYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        pose.mulPose(Axis.YP.rotationDegrees(renderYaw));
        pose.translate(-0.5, 0, -0.5);
        RobotRenderer.renderChassis(pose, buffer, light, entity.tickCount + partialTick, true, entity.flag());
        if (createHatRenderer != null) {
            try {
                createHatRenderer.invoke(null, entity, pose, buffer, light);
            } catch (Throwable ignored) {
            }
        }
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TrainRobot entity) { return Textures.Model.Robot; }
}
