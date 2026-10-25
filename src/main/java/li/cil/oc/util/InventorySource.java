package li.cil.oc.util;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.items.IItemHandler;

public interface InventorySource {
    Direction side();
    IItemHandler inventory();
}
