package li.cil.oc.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.common.entity.Drone;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class ModelQuadcopter extends EntityModel<Drone> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("opencomputers", "drone"), "main");

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 23).addBox(-3, 1, -3, 6, 1, 6)
                .texOffs(0, 1).addBox(-1, 0, -1, 2, 1, 2)
                .texOffs(0, 17).addBox(-2, -1, -2, 4, 1, 4),
                PartPose.rotation(0, (float) Math.toRadians(45), 0));

        float[][] wingOffsets = {
                {1, -7, 2, -3}, {1, 1, 2, 2}, {-7, 1, -3, 2}, {-7, -7, -3, -3}
        };
        for (int i = 0; i < 4; i++) {
            var offset = wingOffsets[i];
            float wx = offset[0], wz = offset[1], px = offset[2], pz = offset[3];
            root.addOrReplaceChild("wing" + i, CubeListBuilder.create()
                    .texOffs(0, 9).addBox(wx, 0, wz, 6, 1, 6)
                    .texOffs(0, 27).addBox(px, -1, pz, 1, 3, 1), PartPose.ZERO);
            root.addOrReplaceChild("light" + i, CubeListBuilder.create()
                    .texOffs(24, 0).addBox(wx, 0, wz, 6, 1, 6), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    private final ModelPart body;
    private final ModelPart[] wings = new ModelPart[4];
    private final ModelPart[] lights = new ModelPart[4];
    private final Vec3 up = new Vec3(0, 1, 0);
    private Drone cachedEntity;
    private float cachedDt;

    public ModelQuadcopter(ModelPart root) {
        body = root.getChild("body");
        for (int i = 0; i < 4; i++) {
            wings[i] = root.getChild("wing" + i);
            lights[i] = root.getChild("light" + i);
        }
    }

    @Override
    public void setupAnim(Drone drone, float f1, float f2, float f3, float f4, float f5) {}

    @Override
    public void prepareMobModel(Drone drone, float f1, float f2, float dt) {
        cachedEntity = drone;
        cachedDt = dt;
    }

    @Override
    public void renderToBuffer(PoseStack stack, VertexConsumer builder, int light, int overlay, int color) {
        if (cachedEntity != null) doRender(cachedEntity, cachedDt, stack, builder, light, overlay, color);
    }

    private void doRender(Drone drone, float dt, PoseStack stack, VertexConsumer builder,
                          int light, int overlay, int color) {
        float a = ((color >>> 24) & 0xFF) / 255f;
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        stack.pushPose();
        if (drone.isRunning()) {
            int jitter = drone.hashCode() ^ 0xFF;
            stack.translate(0, (float) (Math.sin(jitter + (drone.getEnvironmentLevel().getGameTime() + dt) / 20.0)
                    * (1 / 16f)), 0);
        }

        var direction = drone.getDeltaMovement().normalize();
        if (direction.dot(up) < 0.99) {
            var rotationAxis = direction.cross(up);
            float relativeSpeed = (float) drone.getDeltaMovement().length() / drone.maxVelocity();
            float degrees = relativeSpeed * -20f;
            var rotation = new Quaternionf().setAngleAxis((float) Math.toRadians(degrees),
                    (float) rotationAxis.x(), (float) rotationAxis.y(), (float) rotationAxis.z());
            stack.mulPose(rotation);
        }

        stack.mulPose(Axis.YP.rotationDegrees(drone.bodyAngle()));
        body.render(stack, builder, light, overlay, color);
        for (int i = 0; i < 4; i++) {
            wings[i].xRot = drone.flapAngles()[i][0];
            wings[i].zRot = drone.flapAngles()[i][1];
            wings[i].render(stack, builder, light, overlay, color);
        }

        if (drone.isRunning()) {
            int lightColor = drone.lightColor();
            int rr = (int) (r * ((lightColor >>> 16) & 0xFF)) & 0xFF;
            int gg = (int) (g * ((lightColor >>> 8) & 0xFF)) & 0xFF;
            int bb = (int) (b * (lightColor & 0xFF)) & 0xFF;
            int aa = (int) (a * 255) & 0xFF;
            int packedColor = (aa << 24) | (rr << 16) | (gg << 8) | bb;
            int fullLight = LightTexture.pack(15, 15);
            for (int i = 0; i < 4; i++) {
                lights[i].xRot = drone.flapAngles()[i][0];
                lights[i].zRot = drone.flapAngles()[i][1];
                lights[i].render(stack, builder, fullLight, OverlayTexture.NO_OVERLAY, packedColor);
            }
        }
        stack.popPose();
    }
}
