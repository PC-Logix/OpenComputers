package li.cil.oc.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.Settings;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class HoverBootRenderer extends HumanoidModel<LivingEntity> {
    public static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
        Settings.resourceDomain(), "textures/model/drone.png");
    private final ModelPart bootLeft;
    private final ModelPart bootRight;
    private final ModelPart light0;
    private final ModelPart light1;
    private final ModelPart light2;
    private final ModelPart light3;
    private final List<ModelPart> allLightParts;
    public int lightColor = 0x66DD55;

    public HoverBootRenderer(ModelPart root) {
        super(root);
        bootLeft = leftLeg.getChild("boot_left");
        bootRight = rightLeg.getChild("boot_right");
        light0 = bootLeft.getChild("wing0").getChild("light0");
        light1 = bootLeft.getChild("wing1").getChild("light1");
        light2 = bootRight.getChild("wing2").getChild("light2");
        light3 = bootRight.getChild("wing3").getChild("light3");
        allLightParts = List.of(light0, light1, light2, light3);
        head.visible = false;
        hat.visible = false;
        body.visible = false;
        rightArm.visible = false;
        leftArm.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9f, 12f, 0f));
        var leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9f, 12f, 0f));
        var bootRight = rightLeg.addOrReplaceChild("boot_right", CubeListBuilder.create(), PartPose.offset(0f, 10.1f, 0f));
        var bootLeft = leftLeg.addOrReplaceChild("boot_left", CubeListBuilder.create(), PartPose.offset(0f, 10.11f, 0f));
        addDroneBody(bootLeft, "drone_body_left");
        addDroneBody(bootRight, "drone_body_right");

        var wing0 = bootLeft.addOrReplaceChild("wing0", CubeListBuilder.create()
            .texOffs(0, 9).addBox(-1, 0, -7, 6, 1, 6)
            .texOffs(0, 27).addBox(0, -1, -3, 1, 3, 1), PartPose.ZERO);
        var wing1 = bootLeft.addOrReplaceChild("wing1", CubeListBuilder.create()
            .texOffs(0, 9).addBox(-1, 0, 1, 6, 1, 6)
            .texOffs(0, 27).addBox(0, -1, 2, 1, 3, 1), PartPose.ZERO);
        var wing2 = bootRight.addOrReplaceChild("wing2", CubeListBuilder.create()
            .texOffs(0, 9).addBox(-5, 0, 1, 6, 1, 6)
            .texOffs(0, 27).addBox(-1, -1, 2, 1, 3, 1), PartPose.ZERO);
        var wing3 = bootRight.addOrReplaceChild("wing3", CubeListBuilder.create()
            .texOffs(0, 9).addBox(-5, 0, -7, 6, 1, 6)
            .texOffs(0, 27).addBox(-1, -1, -3, 1, 3, 1), PartPose.ZERO);
        wing0.addOrReplaceChild("light0", CubeListBuilder.create().texOffs(24, 0).addBox(-1, 0, -7, 6, 1, 6), PartPose.ZERO);
        wing1.addOrReplaceChild("light1", CubeListBuilder.create().texOffs(24, 0).addBox(-1, 0, 1, 6, 1, 6), PartPose.ZERO);
        wing2.addOrReplaceChild("light2", CubeListBuilder.create().texOffs(24, 0).addBox(-5, 0, 1, 6, 1, 6), PartPose.ZERO);
        wing3.addOrReplaceChild("light3", CubeListBuilder.create().texOffs(24, 0).addBox(-5, 0, -7, 6, 1, 6), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void addDroneBody(net.minecraft.client.model.geom.builders.PartDefinition parent, String name) {
        parent.addOrReplaceChild(name, CubeListBuilder.create()
            .texOffs(0, 23).addBox(-3, 1, -3, 6, 1, 6)
            .texOffs(0, 1).addBox(-1, 0, -1, 2, 1, 2)
            .texOffs(0, 17).addBox(-2, -1, -2, 4, 1, 4),
            PartPose.offsetAndRotation(0, 0, 0, 0, (float) Math.toRadians(45), 0));
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        crouching = entity.isCrouching();
        young = false;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, int color) {
        for (var part : allLightParts) part.visible = false;
        super.renderToBuffer(pose, consumer, light, overlay, color);
        for (var part : allLightParts) part.visible = true;

        float alpha = ((color >>> 24) & 0xFF) / 255f;
        float red = ((color >>> 16) & 0xFF) / 255f;
        float green = ((color >>> 8) & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;
        float redMultiplier = ((lightColor >>> 16) & 0xFF) / 255f;
        float greenMultiplier = ((lightColor >>> 8) & 0xFF) / 255f;
        float blueMultiplier = (lightColor & 0xFF) / 255f;
        int litColor = (int) (alpha * 255) << 24
            | ((int) (red * redMultiplier * 255) & 0xFF) << 16
            | ((int) (green * greenMultiplier * 255) & 0xFF) << 8
            | ((int) (blue * blueMultiplier * 255) & 0xFF);
        int fullBright = LightTexture.pack(15, 15);

        renderLight(pose, consumer, fullBright, overlay, litColor, leftLeg, bootLeft, "wing0", light0);
        renderLight(pose, consumer, fullBright, overlay, litColor, leftLeg, bootLeft, "wing1", light1);
        renderLight(pose, consumer, fullBright, overlay, litColor, rightLeg, bootRight, "wing2", light2);
        renderLight(pose, consumer, fullBright, overlay, litColor, rightLeg, bootRight, "wing3", light3);
    }

    private static void renderLight(PoseStack pose, VertexConsumer consumer, int light, int overlay, int color,
                                    ModelPart leg, ModelPart boot, String wingName, ModelPart lightPart) {
        pose.pushPose();
        leg.translateAndRotate(pose);
        boot.translateAndRotate(pose);
        boot.getChild(wingName).translateAndRotate(pose);
        lightPart.render(pose, consumer, light, overlay, color);
        pose.popPose();
    }
}
