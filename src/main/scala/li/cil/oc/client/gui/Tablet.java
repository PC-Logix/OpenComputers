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

public class Tablet extends DynamicGuiContainer<li.cil.oc.common.menu.Tablet>
    implements LockedHotbar<li.cil.oc.common.menu.Tablet> {
    protected ImageButton powerButton;

    public Tablet(li.cil.oc.common.menu.Tablet state, Inventory playerInventory, Component name) {
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
        powerButton.toggled_$eq(inventoryContainer().isRunning());
        powerButton.setTooltip(Tooltip.create(Component.literal(inventoryContainer().isRunning()
            ? Localization.Computer$.MODULE$.TurnOff() : Localization.Computer$.MODULE$.TurnOn())));
        super.render(graphics, mouseX, mouseY, dt);
    }

    @Override
    public void init() {
        super.init();
        powerButton = addRenderableWidget(new ImageButton(leftPos + 68, topPos + 34, 18, 18,
            button -> PacketSender$.MODULE$.sendTabletPower(inventoryContainer(), !inventoryContainer().isRunning()),
            Textures.GUISprites$.MODULE$.ButtonPower(), Component.empty(), false,
            0xE0E0E0, 0xA0A0A0, 0xFFFFA0, -1, -1, -1));
    }
}
