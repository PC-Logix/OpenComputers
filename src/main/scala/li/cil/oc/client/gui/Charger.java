package li.cil.oc.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Charger extends DynamicGuiContainer<li.cil.oc.common.menu.Charger> {
    public Charger(li.cil.oc.common.menu.Charger state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }
}
