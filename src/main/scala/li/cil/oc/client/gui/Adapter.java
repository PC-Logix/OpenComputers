package li.cil.oc.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Adapter extends DynamicGuiContainer<li.cil.oc.common.menu.Adapter> {
    public Adapter(li.cil.oc.common.menu.Adapter state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }
}
