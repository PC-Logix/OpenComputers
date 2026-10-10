package li.cil.oc.common.block.property;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public final class PropertyRotatable {
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final DirectionProperty Mount = DirectionProperty.create("mount", direction -> direction == Direction.UP || direction == Direction.DOWN);
    public static final DirectionProperty Pitch = DirectionProperty.create("pitch", direction -> direction.getAxis() == Direction.Axis.Y || direction == Direction.NORTH);
    public static final DirectionProperty Yaw = DirectionProperty.create("yaw", Direction.Plane.HORIZONTAL);

    private PropertyRotatable() {
    }
}
