package li.cil.oc.client.renderer.tileentity;

import com.google.common.base.Strings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.item.UpgradeRenderer;
import li.cil.oc.api.driver.item.UpgradeRenderer.MountPointName;
import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.RobotFlags;
import li.cil.oc.common.blockentity.RobotProxy;
import li.cil.oc.util.RenderState;
import li.cil.oc.common.blockentity.Robot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import scala.Option;

import java.util.Optional;
import scala.collection.Iterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Renders robot chassis, mounted upgrades, flags, and equipped tools. */
public final class RobotRenderer implements BlockEntityRenderer<RobotProxy> {
    public static final ResourceLocation RainbowFlag = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "rainbow_flag");
    public static final ResourceLocation TransFlag = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "trans_flag");

    private static final float GAP = 1.0f / 28.0f;
    private static final float TOP = 0.5f + GAP;
    private static final float BOTTOM = 0.5f - GAP;
    private static final float HALF_SIZE = 0.4f;
    private static final float LOW = 0.5f - HALF_SIZE;
    private static final float HIGH = 0.5f + HALF_SIZE;
    private static final Map<String, Integer> SLOT_INDEX = Map.of(
        MountPointName.TopLeft, 0, MountPointName.TopRight, 1, MountPointName.TopBack, 2,
        MountPointName.BottomLeft, 3, MountPointName.BottomRight, 4,
        MountPointName.BottomBack, 5, MountPointName.BottomFront, 6);

    private static final RobotRenderer CHASSIS_RENDERER = new RobotRenderer();
    public static final BlockEntityRendererProvider<RobotProxy> INSTANCE = context -> new RobotRenderer();

    private final RobotRenderEvent.MountPoint[] mountPoints = new RobotRenderEvent.MountPoint[7];

    public RobotRenderer() {
        SLOT_INDEX.forEach((name, index) -> mountPoints[index] = new RobotRenderEvent.MountPoint(name));
    }

    public static void renderChassis(PoseStack stack, MultiBufferSource buffer, int light, double offset,
                                     boolean isRunningOverride, Option<ResourceLocation> flag) {
        CHASSIS_RENDERER.renderChassisInternal(stack, buffer, light, null, offset, isRunningOverride, flag);
    }

    @Override
    public AABB getRenderBoundingBox(RobotProxy entity) {
        return new AABB(entity.getBlockPos()).inflate(0.5);
    }

    private void resetMountPoints(boolean running) {
        float offset = running ? 0 : -0.06f;
        for (int i = 0; i < 3; i++) mountPoints[i].offset.set(0, 0.2f, 0.24f);
        mountPoints[0].rotation.set(0, 1, 0, 90);
        mountPoints[1].rotation.set(0, 1, 0, -90);
        mountPoints[2].rotation.set(0, 1, 0, 180);
        for (int i = 3; i < 7; i++) mountPoints[i].offset.set(0, -0.2f - offset, 0.24f);
        mountPoints[3].rotation.set(0, 1, 0, 90);
        mountPoints[4].rotation.set(0, 1, 0, -90);
        mountPoints[5].rotation.set(0, 1, 0, 180);
        mountPoints[6].rotation.set(0, 1, 0, 0);
    }

    private static void vertex(VertexConsumer out, PoseStack stack, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz,
                               int red, int green, int blue, int light) {
        out.addVertex(stack.last().pose(), x, y, z).setColor(red, green, blue, 0xFF)
            .setUv(u, v).setLight(light).setNormal(stack.last(), nx, ny, nz);
    }

    private static void drawMesh(PoseStack stack, MultiBufferSource source, int light,
                                 int red, int green, int blue, float[] vertices) {
        VertexConsumer out = source.getBuffer(RenderTypes.ROBOT_CHASSIS);
        for (int i = 0; i < vertices.length; i += 8) {
            vertex(out, stack, vertices[i], vertices[i + 1], vertices[i + 2], vertices[i + 3], vertices[i + 4],
                vertices[i + 5], vertices[i + 6], vertices[i + 7], red, green, blue, light);
        }
    }

    private void drawTop(PoseStack stack, MultiBufferSource buffer, int light, int red, int green, int blue) {
        drawMesh(stack, buffer, light, red, green, blue, TOP_MESH);
    }

    private void drawBottom(PoseStack stack, MultiBufferSource buffer, int light, int red, int green, int blue) {
        drawMesh(stack, buffer, light, red, green, blue, BOTTOM_MESH);
    }

    // Each vertex stores position, UV, and its original face normal.
    private static final float[] TOP_MESH = {
        .5f,1,.5f,.25f,.25f,0,.196116135f,.980580676f, LOW,TOP,HIGH,0,.5f,0,.196116135f,.980580676f, HIGH,TOP,HIGH,.5f,.5f,0,.196116135f,.980580676f,
        .5f,1,.5f,.25f,.25f,0,.196116135f,.980580676f, HIGH,TOP,HIGH,.5f,.5f,0,.196116135f,.980580676f, HIGH,TOP,LOW,.5f,0,.980580676f,.196116135f,0,
        .5f,1,.5f,.25f,.25f,0,.196116135f,.980580676f, HIGH,TOP,LOW,.5f,0,.980580676f,.196116135f,0, LOW,TOP,LOW,0,0,0,.196116135f,-.980580676f,
        .5f,1,.5f,.25f,.25f,0,.196116135f,.980580676f, LOW,TOP,LOW,0,0,0,.196116135f,-.980580676f, LOW,TOP,HIGH,0,.5f,-.980580676f,.196116135f,0,
        LOW,TOP,HIGH,0,1,0,-1,0, LOW,TOP,LOW,0,.5f,0,-1,0, HIGH,TOP,LOW,.5f,.5f,0,-1,0,
        LOW,TOP,HIGH,0,1,0,-1,0, HIGH,TOP,LOW,.5f,.5f,0,-1,0, HIGH,TOP,HIGH,.5f,1,0,-1,0
    };
    private static final float[] BOTTOM_MESH = {
        .5f,.03f,.5f,.75f,.25f,0,-.196116135f,.980580676f, LOW,BOTTOM,LOW,.5f,0,0,-.196116135f,.980580676f, HIGH,BOTTOM,LOW,1,0,0,-.196116135f,.980580676f,
        .5f,.03f,.5f,.75f,.25f,0,-.196116135f,.980580676f, HIGH,BOTTOM,LOW,1,0,0,-.196116135f,.980580676f, HIGH,BOTTOM,HIGH,1,.5f,.980580676f,-.196116135f,0,
        .5f,.03f,.5f,.75f,.25f,0,-.196116135f,.980580676f, HIGH,BOTTOM,HIGH,1,.5f,.980580676f,-.196116135f,0, LOW,BOTTOM,HIGH,.5f,.5f,0,-.196116135f,-.980580676f,
        .5f,.03f,.5f,.75f,.25f,0,-.196116135f,.980580676f, LOW,BOTTOM,HIGH,.5f,.5f,0,-.196116135f,-.980580676f, LOW,BOTTOM,LOW,.5f,0,-.980580676f,-.196116135f,0,
        LOW,BOTTOM,LOW,0,.5f,0,1,0, LOW,BOTTOM,HIGH,0,1,0,1,0, HIGH,BOTTOM,HIGH,.5f,1,0,1,0,
        LOW,BOTTOM,LOW,0,.5f,0,1,0, HIGH,BOTTOM,HIGH,.5f,1,0,1,0, HIGH,BOTTOM,LOW,.5f,.5f,0,1,0
    };

    private void drawFlag(PoseStack stack, MultiBufferSource buffer, int light, ResourceLocation flag) {
        Optional<RobotFlags.Flag> maybeDefinition = RobotFlags.byIdOptional(flag);
        if (maybeDefinition.isEmpty()) return;
        RobotFlags.Flag definition = maybeDefinition.get();
        VertexConsumer out = buffer.getBuffer(RenderTypes.robotFlag(definition.id()));
        float x = 2f / 16f;
        float flagBottom = 13.5f / 16f;
        float flagTop = (13.5f + definition.height() / 2f) / 16f;
        float flagFront = 10.5f / 16f;
        float flagBack = 15.5f / 16f;
        float poleBottom = 10.5f / 16f;
        float poleTop = 13.5f / 16f;
        float poleBack = 11f / 16f;

        stack.pushPose();
        stack.translate(x, 11f / 16f, 10.75f / 16f);
        stack.mulPose(Axis.XP.rotationDegrees(22.5f));
        stack.translate(-x, -11f / 16f, -10.75f / 16f);
        quad(out, stack, x, flagBottom, flagTop, flagFront, flagBack, 0, 0, 7, definition.height(), light);
        quad(out, stack, x, poleBottom, poleTop, flagFront, poleBack, 13, 0, 14, 6, light);
        stack.popPose();
    }

    private static void quad(VertexConsumer out, PoseStack stack, float x, float y0, float y1, float z0, float z1,
                             float u0, float v0, float u1, float v1, int light) {
        float inv = 1f / 16f;
        var pose = stack.last();
        out.addVertex(pose.pose(), x, y0, z0).setColor(255,255,255,255).setUv(u0*inv,v1*inv).setLight(light).setNormal(pose,1,0,0);
        out.addVertex(pose.pose(), x, y1, z0).setColor(255,255,255,255).setUv(u0*inv,v0*inv).setLight(light).setNormal(pose,1,0,0);
        out.addVertex(pose.pose(), x, y1, z1).setColor(255,255,255,255).setUv(u1*inv,v0*inv).setLight(light).setNormal(pose,1,0,0);
        out.addVertex(pose.pose(), x, y0, z1).setColor(255,255,255,255).setUv(u1*inv,v1*inv).setLight(light).setNormal(pose,1,0,0);
    }

    private void renderChassisInternal(PoseStack stack, MultiBufferSource buffer, int light, Robot robot,
                                       double offset, boolean isRunningOverride, Option<ResourceLocation> flag) {
        boolean isRunning = robot == null ? isRunningOverride : robot.isRunning();
        float size = .3f, left = .5f - size, right = .5f + size, vStep = 1f / 32f;
        float offsetV = (int) ((offset - (int) offset) * 16) * vStep;
        float u0 = isRunning ? .5f : .25f - vStep;
        float u1 = isRunning ? 1f : .25f + vStep;
        float v0 = isRunning ? .5f + offsetV : .75f - vStep;
        float v1 = isRunning ? .5f + vStep + offsetV : .75f + vStep;

        resetMountPoints(robot != null && robot.isRunning());
        RobotRenderEvent event = new RobotRenderEvent(robot, mountPoints);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return;

        int color = event.getColorMultiplier();
        int cr = color >> 16 & 0xFF, cg = color >> 8 & 0xFF, cb = color & 0xFF;
        if (!isRunning) stack.translate(0, -2 * GAP, 0);
        drawBottom(stack, buffer, light, cr, cg, cb);
        if (!isRunning) stack.translate(0, -2 * GAP, 0);
        drawTop(stack, buffer, light, cr, cg, cb);

        Option<ResourceLocation> flagToDraw = robot != null ? robot.info().flag() : flag;
        if (flagToDraw != null && flagToDraw.isDefined()) drawFlag(stack, buffer, light, flagToDraw.get());

        if (isRunning) {
            int lightColor = event.lightColor < 0
                ? (robot != null && robot.info() != null ? robot.info().lightColor() : 0xF23030)
                : event.lightColor & 0xFFFFFF;
            int red = lightColor >>> 16 & 0xFF, green = lightColor >>> 8 & 0xFF, blue = lightColor & 0xFF;
            VertexConsumer out = buffer.getBuffer(RenderTypes.ROBOT_LIGHT);
            float[][] vertices = {
                {left,TOP,left,u0,v0,-1,0,0},{left,BOTTOM,left,u0,v1,-1,0,0},{left,BOTTOM,right,u1,v1,-1,0,0},{left,TOP,right,u1,v0,-1,0,0},
                {left,TOP,right,u0,v0,0,0,1},{left,BOTTOM,right,u0,v1,0,0,1},{right,BOTTOM,right,u1,v1,0,0,1},{right,TOP,right,u1,v0,0,0,1},
                {right,TOP,right,u0,v0,1,0,0},{right,BOTTOM,right,u0,v1,1,0,0},{right,BOTTOM,left,u1,v1,1,0,0},{right,TOP,left,u1,v0,1,0,0},
                {right,TOP,left,u0,v0,0,0,-1},{right,BOTTOM,left,u0,v1,0,0,-1},{left,BOTTOM,left,u1,v1,0,0,-1},{left,TOP,left,u1,v0,0,0,-1}
            };
            for (float[] v : vertices) vertex(out, stack, v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],red,green,blue,light);
        }
    }

    @Override
    public void render(RobotProxy proxy, float partialTick, PoseStack matrix, MultiBufferSource buffer,
                       int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");
        Robot robot = proxy.robot();
        double worldTime = proxy.getLevel().getGameTime() + partialTick;
        matrix.pushPose();
        matrix.translate(.5, .5, .5);

        if (robot.proxy() != proxy) {
            BlockPos robotPos = robot.proxy().getBlockPos(), proxyPos = proxy.getBlockPos();
            matrix.translate((double) (robotPos.getX() - proxyPos.getX()),
                (double) (robotPos.getY() - proxyPos.getY()), (double) (robotPos.getZ() - proxyPos.getZ()));
        }
        if (robot.isAnimatingMove()) {
            double remaining = (robot.animationTicksLeft() - partialTick) / robot.animationTicksTotal();
            Vec3i delta = robot.moveFrom().get().subtract(robot.getBlockPos());
            matrix.translate(delta.getX() * remaining, delta.getY() * remaining, delta.getZ() * remaining);
        }

        int timeJitter = robot.hashCode() ^ 0xFF;
        float hover = robot.isRunning() ? (float) (Math.sin(timeJitter + worldTime / 20.0) * .03) : -.03f;
        matrix.translate(0, hover, 0);
        matrix.pushPose();
        if (robot.isAnimatingTurn()) {
            float remaining = (robot.animationTicksLeft() - partialTick) / (float) robot.animationTicksTotal();
            matrix.mulPose((robot.turnAxis() < 0 ? Axis.YN : Axis.YP).rotationDegrees(90 * remaining));
        }
        switch (robot.yaw()) {
            case WEST -> matrix.mulPose(Axis.YP.rotationDegrees(-90));
            case NORTH -> matrix.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> matrix.mulPose(Axis.YP.rotationDegrees(90));
            default -> { }
        }
        matrix.translate(-.5f, -.5f, -.5f);
        renderChassisInternal(matrix, buffer, light, robot, timeJitter + worldTime / 20.0, false, Option.empty());

        BlockPos pos = proxy.getBlockPos();
        double distance = Minecraft.getInstance().player.position().distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        if (!robot.renderingErrored() && distance < 24 * 24) {
            ItemStack held = robot.getItem(0);
            if (held != null && !held.isEmpty()) renderHeldItem(proxy, robot, held, partialTick, matrix, buffer, light, overlay);
            renderUpgrades(robot, partialTick, matrix, buffer, light);
        }
        matrix.popPose();

        String name = robot.name();
        if (Settings.get().robotLabels() && !Strings.isNullOrEmpty(name) && distance < 64 * 64) {
            var font = Minecraft.getInstance().font;
            float scale = 1.6f / 60f;
            int halfWidth = font.width(name) / 2;
            int background = (int) (255f * Minecraft.getInstance().options.getBackgroundOpacity(.25F)) << 24;
            matrix.translate(0, .8, 0);
            matrix.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            RenderState.mirrorScale(matrix, -scale, -scale, scale);
            font.drawInBatch((EventHandler.isItTime() ? ChatFormatting.OBFUSCATED.toString() : "") + name,
                -halfWidth, 0, -1, false, matrix.last().pose(), buffer, Font.DisplayMode.NORMAL, background, light);
        }
        matrix.popPose();
        RenderState.checkError(getClass().getName() + ".render: leaving");
    }

    private static void renderHeldItem(RobotProxy proxy, Robot robot, ItemStack stack, float partialTick,
                                       PoseStack matrix, MultiBufferSource buffer, int light, int overlay) {
        matrix.pushPose();
        try {
            RenderState.mirrorScale(matrix, 1, -1, -1);
            matrix.translate(0, -8 * .0625F - .0078125F, -.5F);
            if (robot.isAnimatingSwing()) {
                int cycles = Math.max(robot.animationTicksTotal() / 10, 1);
                int ticksPerCycle = robot.animationTicksTotal() / cycles;
                double remaining = (robot.animationTicksLeft() - partialTick) / ticksPerCycle;
                matrix.mulPose(Axis.XP.rotationDegrees((float) (Math.sin((remaining - (int) remaining) * Math.PI) * 45)));
            }
            if (stack.getItem() instanceof BlockItem) {
                matrix.mulPose(Axis.XP.rotationDegrees(180));
                matrix.mulPose(Axis.YP.rotationDegrees(90));
                matrix.scale(.625f, .625f, .625f);
            } else if (stack.getItem() == Items.BOW) {
                matrix.translate(0, -3f / 16f, -.125F);
                matrix.mulPose(Axis.ZP.rotationDegrees(170));
                matrix.scale(.625f, .625f, .625f);
            } else {
                matrix.translate(1f / 16f, 1f / 16f, -2f / 16f);
                matrix.scale(.625f, .625f, .625f);
                matrix.mulPose(Axis.ZP.rotationDegrees(180));
            }
            Minecraft.getInstance().getItemRenderer().renderStatic(Minecraft.getInstance().player, stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, matrix, buffer, proxy.getLevel(), light, overlay,
                (int) proxy.getBlockPos().asLong());
        } catch (Throwable error) {
            OpenComputers.log().warn("Failed rendering equipped item.", error);
            robot.renderingErrored_$eq(true);
        }
        matrix.popPose();
    }

    private void renderUpgrades(Robot robot, float partialTick, PoseStack matrix, MultiBufferSource buffer, int light) {
        Set<String> availableSlots = new HashSet<>(SLOT_INDEX.keySet());
        List<UpgradeMount> wildcards = new ArrayList<>();
        UpgradeRenderer[] slotRenderers = new UpgradeRenderer[mountPoints.length];
        ItemStack[] slotItems = new ItemStack[mountPoints.length];
        Iterator<Object> componentSlots = robot.componentSlotRange().iterator();
        while (componentSlots.hasNext()) collectUpgrade(robot.getItem(((Number) componentSlots.next()).intValue()), robot, availableSlots, wildcards, slotItems, slotRenderers);
        for (int slot : robot.containerSlots()) collectUpgrade(robot.getItem(slot), robot, availableSlots, wildcards, slotItems, slotRenderers);

        int firstEmpty = firstEmpty(slotRenderers);
        for (UpgradeMount upgrade : wildcards) {
            if (firstEmpty < 0) break;
            slotItems[firstEmpty] = upgrade.stack;
            slotRenderers[firstEmpty] = upgrade.renderer;
            firstEmpty = firstEmpty(slotRenderers);
        }
        for (int i = 0; i < slotRenderers.length; i++) {
            UpgradeRenderer renderer = slotRenderers[i];
            if (renderer == null) continue;
            try {
                matrix.pushPose();
                matrix.translate(.5f, .5f, .5f);
                renderer.render(matrix, buffer, light, slotItems[i], mountPoints[i], robot, partialTick);
                matrix.popPose();
            } catch (Throwable error) {
                OpenComputers.log().warn("Failed rendering equipped upgrade.", error);
                robot.renderingErrored_$eq(true);
            }
        }
    }

    private void collectUpgrade(ItemStack stack, Robot robot, Set<String> availableSlots, List<UpgradeMount> wildcards,
                                ItemStack[] slotItems, UpgradeRenderer[] slotRenderers) {
        if (stack.isEmpty() || !(stack.getItem() instanceof UpgradeRenderer renderer)) return;
        String preferred = renderer.computePreferredMountPoint(stack, robot, availableSlots);
        if (availableSlots.remove(preferred)) {
            Integer index = SLOT_INDEX.get(preferred);
            slotItems[index] = stack;
            slotRenderers[index] = renderer;
        } else if (MountPointName.Any.equals(preferred)) wildcards.add(new UpgradeMount(stack, renderer));
    }

    private static int firstEmpty(UpgradeRenderer[] renderers) {
        for (int i = 0; i < renderers.length; i++) if (renderers[i] == null) return i;
        return -1;
    }

    private record UpgradeMount(ItemStack stack, UpgradeRenderer renderer) { }
}
