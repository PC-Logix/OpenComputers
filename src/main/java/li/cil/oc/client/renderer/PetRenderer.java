package li.cil.oc.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.client.renderer.tileentity.RobotRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class PetRenderer {
    public static final PetRenderer INSTANCE = new PetRenderer();
    public static final Set<String> hidden = new java.util.HashSet<>();
    public static boolean isInitialized;

    private static final Map<String, Color> entitledPlayers = new HashMap<>();
    private static final Cache<Entity, PetLocation> petLocations = CacheBuilder.newBuilder()
        .expireAfterAccess(5, TimeUnit.SECONDS)
        .build();
    private static Color rendering;

    static {
        entitledPlayers.put("9f1f262f-0d68-4e13-9161-9eeaf4a0a1a8", new Color(0.3, 0.9, 0.6)); // Sangar
        entitledPlayers.put("18f8bed4-f027-44af-8947-6a3a2317645a", new Color(1.0, 0.0, 0.0)); // Jodarion
        entitledPlayers.put("36123742-2cf6-4cfc-8b65-278581b3caeb", new Color(0.5, 0.7, 1.0)); // DaKaTotal
        entitledPlayers.put("2c0c214b-96f4-4565-b513-de90d5fbc977", new Color(1.0, 0.25, 0.55)); // MichiRavencroft
        entitledPlayers.put("f3ba6ec8-c280-4950-bb08-1fcb2eab3a9c", new Color(0.18, 0.95, 0.922)); // Vexatos
        entitledPlayers.put("9d636bdd-b9f4-4b80-b9ce-586ca04bd4f3", new Color(0.8, 0.77, 0.75)); // StoneNomad
        entitledPlayers.put("23c7ed71-fb13-4abe-abe7-f355e1de6e62", new Color(0.3, 0.3, 1.0)); // LizzyTheSiren
        entitledPlayers.put("076541f1-f10a-46de-a127-dfab8adfbb75", new Color(0.2, 1.0, 0.1)); // vifino
        entitledPlayers.put("e7e90198-0ccf-4662-a827-192ec8f4419d", new Color(0.0, 0.2, 0.6)); // Izaya
        entitledPlayers.put("f514ee69-7bbb-4e46-9e94-d8176324cec2", new Color(0.098, 0.471, 0.784)); // Wobbo
        entitledPlayers.put("f812c043-78ba-4324-82ae-e8f05c52ae6e", new Color(0.1, 0.8, 0.5)); // payonel
        entitledPlayers.put("1db17ee7-8830-4bac-8018-de154340aae6", new Color(0.0, 0.5, 1.0)); // Kosmos
        entitledPlayers.put("02775c6b-c529-4c4e-ad3e-a2654d60328d", new Color(0.0353, 0.5686, 1.0)); // SoraFirestorm
        entitledPlayers.put("4485b0cf-e1d5-4e82-a45a-2dc0270b3bb8", new Color(0.600, 0.329, 0.769)); // TheSandromatic
        entitledPlayers.put("f5cd509e-fa84-4479-ad9e-f318454517e2", new Color(0.078, 0.000, 1.000)); // Forecaster
        entitledPlayers.put("8df90a8c-a554-4886-86b0-71b1d36dfcc1", new Color(0.000, 0.498, 1.000)); // NaomiRavencroft
        entitledPlayers.put("594f4191-90e3-459e-8ff7-06014b4dedd5", new Color(0.20, 0.85, 0.80)); // CutieRavencroft
        entitledPlayers.put("927b049f-2397-4d31-a1db-e89c62482329", new Color(0.8156, 0.0, 1.0)); // ben_mkiv
        entitledPlayers.put("f629f865-55dd-423a-a0fc-3b8ae6fbeeb8", new Color(1.0, 1.0, 1.0)); // Kodos
        entitledPlayers.put("563ad54b-6d9f-4cc0-9872-5528854015d8", new Color(1.0, 0.0, 0.0)); // Babo357
    }

    private PetRenderer() {}

    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Pre event) {
        Entity entity = event.getEntity();
        String uuid = entity.getUUID().toString();
        Color color = entitledPlayers.get(uuid);
        if (hidden.contains(uuid) || color == null) return;
        rendering = color;

        long worldTime = entity.level().getGameTime();
        int timeJitter = entity.hashCode() ^ 0xFF;
        double offset = timeJitter + worldTime / 20.0;
        float hover = (float) (Math.sin(timeJitter + (worldTime + event.getPartialTick()) / 20.0) * 0.03);
        PetLocation location;
        try {
            location = petLocations.get(entity, (Callable<PetLocation>) () -> new PetLocation(entity));
        } catch (ExecutionException exception) {
            throw new IllegalStateException(exception);
        }

        PoseStack stack = event.getPoseStack();
        stack.pushPose();
        Entity self = Minecraft.getInstance().player;
        double px = entity.xOld + (entity.getX() - entity.xOld) * event.getPartialTick();
        double py = entity.yOld + (entity.getY() - entity.yOld) * event.getPartialTick() + entity.getEyeHeight(entity.getPose());
        double pz = entity.zOld + (entity.getZ() - entity.zOld) * event.getPartialTick();
        stack.translate(px - self.getX(), py - self.getY(), pz - self.getZ());
        location.applyInterpolatedTransformations(stack, event.getPartialTick());
        stack.scale(0.3f, 0.3f, 0.3f);
        stack.translate(0, hover, 0);
        li.cil.oc.client.renderer.tileentity.RobotRenderer.renderChassis(
            stack, event.getMultiBufferSource(), event.getPackedLight(), offset, true, scala.Option.empty());
        stack.popPose();
        rendering = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRobotRender(RobotRenderEvent event) {
        Color color = rendering;
        if (color != null) {
            event.setLightColor((float) color.r, (float) color.g, (float) color.b);
            event.multiplyColors((float) color.r, (float) color.g, (float) color.b);
        }
    }

    @SubscribeEvent
    public void tickStart(ClientTickEvent.Pre event) {
        petLocations.cleanUp();
        for (PetLocation pet : petLocations.asMap().values()) pet.update();
    }

    private record Color(double r, double g, double b) {}

    private static final class PetLocation {
        private final Entity owner;
        private double x, y, z;
        private double yaw;
        private double lastX, lastY, lastZ, lastYaw;

        private PetLocation(Entity owner) {
            this.owner = owner;
            yaw = owner.getYRot();
            lastYaw = yaw;
        }

        private void update() {
            double dx = owner.xOld - owner.getX();
            double dy = owner.yOld - owner.getY();
            double dz = owner.zOld - owner.getZ();
            double dYaw = owner.getYRot() - yaw;
            lastX = x;
            lastY = y;
            lastZ = z;
            lastYaw = yaw;
            x = (x + dx) * 0.05;
            y = (y + dy) * 0.05;
            z = (z + dz) * 0.05;
            yaw += dYaw * 0.2f;
        }

        private void applyInterpolatedTransformations(PoseStack stack, float delta) {
            double ix = lastX + (x - lastX) * delta;
            double iy = lastY + (y - lastY) * delta;
            double iz = lastZ + (z - lastZ) * delta;
            double interpolatedYaw = lastYaw + (yaw - lastYaw) * delta;
            stack.translate(ix, iy, iz);
            boolean inventory = Minecraft.getInstance().screen != null && owner == Minecraft.getInstance().player;
            stack.mulPose(Axis.YP.rotationDegrees((float) -(inventory ? owner.getYRot() : interpolatedYaw)));
            stack.translate(0.3, -0.1, -0.2);
        }
    }
}
