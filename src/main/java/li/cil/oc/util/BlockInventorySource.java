package li.cil.oc.util;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.items.IItemHandler;

public record BlockInventorySource(BlockPosition position, Direction side, IItemHandler inventory) implements InventorySource {}
