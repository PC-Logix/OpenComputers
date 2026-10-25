package li.cil.oc.server.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.util.ExtendedArgumentsJava;
import net.minecraft.core.Direction;

public abstract class UpgradeStickyPiston extends UpgradePiston {
    protected UpgradeStickyPiston(EnvironmentHost host) {
        super(host);
    }

    @Override
    public boolean isSticky() { return true; }

    @Callback(doc = "function([side:number]):boolean -- Tries to reach out to the side given (default front) and pull a block similar to a vanilla sticky piston.")
    public Object[] pull(Context context, Arguments args) {
        return doPistonAction(context, pushDirection(args, 0), false);
    }

    public static class Drone extends UpgradeStickyPiston {
        public Drone(li.cil.oc.api.internal.Drone drone) { super(drone); }
        @Override protected Direction pushDirection(Arguments args, int index) {
            return new ExtendedArgumentsJava(args).optSideAny(index, Direction.SOUTH);
        }
    }

    public static class Rotatable extends UpgradeStickyPiston {
        protected final li.cil.oc.api.internal.Rotatable rotatable;
        public Rotatable(li.cil.oc.api.internal.Rotatable rotatable) {
            super((EnvironmentHost) rotatable);
            this.rotatable = rotatable;
        }
        @Override protected Direction pushDirection(Arguments args, int index) {
            return rotatable.toGlobal(new ExtendedArgumentsJava(args).optSideForAction(index, Direction.SOUTH));
        }
    }

    public static class Tablet extends Rotatable {
        private final li.cil.oc.api.internal.Tablet tablet;
        public Tablet(li.cil.oc.api.internal.Tablet tablet) {
            super(tablet);
            this.tablet = tablet;
        }
        @Override protected li.cil.oc.util.BlockPosition pushOrigin(Direction side) {
            if (side == Direction.DOWN && tablet.player().getEyeHeight() > 1.0f) {
                return super.pushOrigin(side).offset(Direction.DOWN);
            }
            return super.pushOrigin(side);
        }
    }
}




