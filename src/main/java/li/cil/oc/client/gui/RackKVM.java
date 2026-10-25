package li.cil.oc.client.gui;

import li.cil.oc.client.PacketSender;
import li.cil.oc.common.component.TextBuffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import scala.Function0;
import scala.Option;

public class RackKVM extends Screen {
    private final li.cil.oc.common.component.RackKVM kvm;
    private li.cil.oc.api.internal.TextBuffer lastBuffer;

    public RackKVM(li.cil.oc.common.component.RackKVM kvm, Function0<Object> hasPower) {
        super(kvm.buffer(), true, () -> true, hasPower);
        this.kvm = kvm;
    }

    @Override
    public li.cil.oc.api.internal.TextBuffer buffer() {
        return kvm.buffer();
    }

    @Override
    public int topPadding() {
        return 22;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        if (lastBuffer != buffer()) {
            lastBuffer = buffer();
            requestSynchronization(buffer());
        }
        super.render(graphics, mouseX, mouseY, dt);

        int x0 = (width - 66) / 2;
        for (int index = 0; index < 3; index++) {
            boolean available = (kvm.serverMask() & (1 << index)) != 0;
            boolean selected = kvm.selectedRackSlot() == kvm.consoleSlots()[index];
            int left = x0 + index * 22;
            int background = selected && available ? 0xCC2C8FBE : selected ? 0xCC6B4A2D : available ? 0xCC303030 : 0xCC151515;
            int foreground = available ? 0xFFFFFF : 0x666666;
            graphics.fill(left, 4, left + 20, 18, background);
            graphics.drawCenteredString(font, Component.literal(Integer.toString(index + 1)), left + 10, 7, foreground);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseY >= 4 && mouseY < 18) {
            double relativeX = mouseX - (width - 66) / 2;
            int index = (int) (relativeX / 22);
            if (relativeX >= 0 && index >= 0 && index < 3 && relativeX - index * 22 < 20) {
                select(index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int mods) {
        if (net.minecraft.client.gui.screens.Screen.hasControlDown() && keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_3) {
            select(keyCode - GLFW.GLFW_KEY_1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, mods);
    }

    private boolean select(int index) {
        if ((kvm.serverMask() & (1 << index)) == 0) return false;
        if (kvm.rack() instanceof li.cil.oc.common.blockentity.Rack rack) {
            int rackSlot = kvm.consoleSlots()[index];
            PacketSender.sendRackKVMSelection(rack, kvm.slot(), rackSlot);
            Option<li.cil.oc.api.internal.TextBuffer> target = kvm.bufferAtRackSlot(rackSlot);
            if (target.isDefined()) requestSynchronization(target.get());
            return true;
        }
        return false;
    }

    private void requestSynchronization(li.cil.oc.api.internal.TextBuffer target) {
        if (target instanceof TextBuffer concrete) concrete.requestSynchronization();
    }
}
