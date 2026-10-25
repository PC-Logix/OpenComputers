package li.cil.oc.util;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.items.IItemHandler;

public record EntityInventorySource(Entity entity, Direction side, IItemHandler inventory) implements InventorySource {}
