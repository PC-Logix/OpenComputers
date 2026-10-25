package li.cil.oc.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HoloScreen extends DynamicGuiContainer<li.cil.oc.common.menu.HoloScreen> {
    public HoloScreen(li.cil.oc.common.menu.HoloScreen state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }
}
