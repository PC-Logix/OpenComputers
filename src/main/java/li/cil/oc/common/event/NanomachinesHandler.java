package li.cil.oc.common.event;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.nanomachines.ControllerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import scala.runtime.BoxedUnit;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public final class NanomachinesHandler {
    private NanomachinesHandler() {}

    @OnlyIn(Dist.CLIENT)
    public static final class Client {
        public static final Client INSTANCE = new Client();

        private static final RenderType TEXTURE = RenderTypes.createTexturedQuad("nanomachines",
                Textures.GUI.Nanomachines, DefaultVertexFormat.POSITION_TEX, false);
        private static final RenderType BAR = RenderTypes.createTexturedQuad("nanomachines_bar",
                Textures.GUI.NanomachinesBar, DefaultVertexFormat.POSITION_TEX, false);

        private Client() {}

        @SubscribeEvent
        public void onRenderGameOverlay(RenderGuiLayerEvent.Post event) {
            if (!VanillaGuiLayers.DEBUG_OVERLAY.equals(event.getName())) return;
            var mc = Minecraft.getInstance();
            Controller controller = Nanomachines.getController(mc.player);
            if (controller == null) return;

            var graphics = event.getGuiGraphics();
            var window = mc.getWindow();
            int sizeX = 8;
            int sizeY = 12;
            int width = window.getGuiScaledWidth();
            int height = window.getGuiScaledHeight();
            var hudPos = Settings.get().nanomachineHudPos();
            double x = ((Number) hudPos._1()).doubleValue();
            double y = ((Number) hudPos._2()).doubleValue();
            int left = Math.min(width - sizeX,
                    x < 0 ? width / 2 - 91 - 12 : x < 1 ? (int) (width * x) : (int) x);
            int top = Math.min(height - sizeY,
                    y < 0 ? height - 39 : y < 1 ? (int) (y * height) : (int) y);
            float fill = (float) (controller.getLocalBuffer() / controller.getLocalBufferSize());

            try (var byteBuffer = new ByteBufferBuilder(786432)) {
                var buffer = MultiBufferSource.immediate(byteBuffer);
                drawRect(graphics.pose(), buffer.getBuffer(TEXTURE), left, top, sizeX, sizeY, sizeX, sizeY, 1);
                drawRect(graphics.pose(), buffer.getBuffer(BAR), left, top, sizeX, sizeY, sizeX, sizeY, fill);
                buffer.endBatch();
            }
        }

        private static void drawRect(PoseStack stack, VertexConsumer renderer, int x, int y,
                                     int width, int height, int textureWidth, int textureHeight, float fill) {
            float sx = 1f / textureWidth;
            float sy = 1f / textureHeight;
            renderer.addVertex(stack.last().pose(), x, y + height, 0f).setUv(0f, height * sy);
            renderer.addVertex(stack.last().pose(), x + width, y + height, 0f).setUv(width * sx, height * sy);
            renderer.addVertex(stack.last().pose(), x + width, y + height * (1 - fill), 0f)
                    .setUv(width * sx, 1 - fill);
            renderer.addVertex(stack.last().pose(), x, y + height * (1 - fill), 0f).setUv(0f, 1 - fill);
        }
    }

    public static final class Common {
        public static final Common INSTANCE = new Common();

        private Common() {}

        @SubscribeEvent
        public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            Controller controller = Nanomachines.getController(event.getEntity());
            if (controller != null) {
                controller.changeBuffer(-controller.getLocalBuffer());
            }
        }

        @SubscribeEvent
        public void onLivingUpdate(EntityTickEvent.Post event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!(Nanomachines.getController(player) instanceof ControllerImpl controller)) return;

            if (controller.player() == player) {
                controller.update();
            } else {
                CompoundTag nbt = new CompoundTag();
                controller.saveData(nbt);
                Nanomachines.uninstallController(controller.player());
                if (Nanomachines.installController(player) instanceof ControllerImpl replacement) {
                    replacement.loadData(nbt);
                    replacement.reset();
                }
            }
        }

        @SubscribeEvent
        public void onPlayerSave(PlayerEvent.SaveToFile event) {
            if (!(Nanomachines.getController(event.getEntity()) instanceof ControllerImpl controller)) return;
            try (var output = new FileOutputStream(event.getPlayerFile("ocnm"))) {
                CompoundTag nbt = new CompoundTag();
                controller.saveData(nbt);
                NbtIo.writeCompressed(nbt, output);
            } catch (Throwable error) {
                OpenComputers.log().warn("Error saving nanomachine state.", error);
            }
        }

        @SubscribeEvent
        public void onPlayerLoad(PlayerEvent.LoadFromFile event) {
            var file = event.getPlayerFile("ocnm");
            if (!file.exists()) return;
            if (!(Nanomachines.getController(event.getEntity()) instanceof ControllerImpl controller)) return;
            try (var input = new FileInputStream(file)) {
                controller.loadData(NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap()));
            } catch (Throwable error) {
                OpenComputers.log().warn("Error loading nanomachine state.", error);
            }
        }

        @SubscribeEvent
        public void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
            if (Nanomachines.getController(event.getEntity()) instanceof ControllerImpl) {
                EventHandler.scheduleServer(() -> {
                    Nanomachines.uninstallController(event.getEntity());
                    return BoxedUnit.UNIT;
                });
            }
        }
    }
}
