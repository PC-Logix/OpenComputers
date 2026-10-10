package li.cil.oc.client.gui;

import li.cil.oc.Localization;
import li.cil.oc.client.PacketSender$;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.traits.LockedHotbar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class Server extends DynamicGuiContainer<li.cil.oc.common.menu.Server>
    implements LockedHotbar<li.cil.oc.common.menu.Server> {
    protected ImageButton powerButton;

    public Server(li.cil.oc.common.menu.Server state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }

    @Override
    public ItemStack lockedStack() {
        return inventoryContainer().stack();
    }

    @Override
    public void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (slot == null || !ItemStack.isSameItem(slot.getItem(), lockedStack())) {
            super.slotClicked(slot, slotId, mouseButton, clickType);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        powerButton.visible = !inventoryContainer().isItem();
        powerButton.toggled_$eq(inventoryContainer().isRunning());
        powerButton.setTooltip(Tooltip.create(Component.literal(inventoryContainer().isRunning()
            ? Localization.Computer$.MODULE$.TurnOff() : Localization.Computer$.MODULE$.TurnOn())));
        super.render(graphics, mouseX, mouseY, dt);
    }

    @Override
    public void init() {
        super.init();
        powerButton = addRenderableWidget(new ImageButton(leftPos + 48, topPos + 33, 18, 18,
            button -> {
                if (inventoryContainer().rackSlot() >= 0) {
                    PacketSender$.MODULE$.sendServerPower(inventoryContainer(), inventoryContainer().rackSlot(),
                        !inventoryContainer().isRunning());
                }
            }, Textures.GUISprites$.MODULE$.ButtonPower(), Component.empty(), false,
            0xE0E0E0, 0xA0A0A0, 0xFFFFA0, -1, -1, -1));
    }

    @Override
    public void drawSecondaryBackgroundLayer(GuiGraphics graphics) {
        graphics.blit(Textures.GUI$.MODULE$.Server(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
