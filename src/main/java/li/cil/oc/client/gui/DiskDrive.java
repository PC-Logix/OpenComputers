package li.cil.oc.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DiskDrive extends DynamicGuiContainer<li.cil.oc.common.menu.DiskDrive> {
    public DiskDrive(li.cil.oc.common.menu.DiskDrive state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }
}
