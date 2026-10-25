package li.cil.oc.common.block;

import li.cil.oc.common.block.property.PropertyCableConnection;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

// cable blockstate helper for f**king shit scala compiler
public class CableHelper {
    public static BlockState helperRegisterDefaultState(StateDefinition<Block, BlockState> def) {
        return def.any().
                setValue(PropertyCableConnection.DOWN , PropertyCableConnection.Shape.NONE).
                setValue(PropertyCableConnection.UP   , PropertyCableConnection.Shape.NONE).
                setValue(PropertyCableConnection.NORTH, PropertyCableConnection.Shape.NONE).
                setValue(PropertyCableConnection.SOUTH, PropertyCableConnection.Shape.NONE).
                setValue(PropertyCableConnection.WEST , PropertyCableConnection.Shape.NONE).
                setValue(PropertyCableConnection.EAST , PropertyCableConnection.Shape.NONE);
    }
    
    public static PropertyCableConnection.Shape getCableShape(final BlockState state, Direction side) {
        return state.getValue(PropertyCableConnection.BY_DIRECTION.get(side));
    }
    
    public static BlockState helperSetCableShapeState(BlockState state, Direction fromSide, PropertyCableConnection.Shape type) {
        return state.setValue(PropertyCableConnection.BY_DIRECTION.get(fromSide), type);
    }
}
