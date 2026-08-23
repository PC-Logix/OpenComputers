package li.cil.oc.integration.create;

import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlock;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyBlockEntity;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class CreateContraptionEnvironments {
    private CreateContraptionEnvironments() {
    }

    public static final class ElevatorPulley extends CreateEnvironment<ElevatorPulleyBlockEntity> {
        ElevatorPulley(final ElevatorPulleyBlockEntity blockEntity) {
            super(blockEntity, "Create_ElevatorPulley");
        }

        private ElevatorContraption contraption() {
            if (blockEntity.movedContraption == null) return null;
            if (blockEntity.movedContraption.getContraption() == null) return null;
            if (!(blockEntity.movedContraption.getContraption() instanceof ElevatorContraption contraption)) return null;
            return contraption;
        }

        @Callback(direct = true, doc = "function():number -- Get the current offset of the pulley rope.")
        public Object[] getPulleyDistance(final Context context, final Arguments args) {
            return result(blockEntity.getInterpolatedOffset(.5f));
        }

        @Callback(direct = true, doc = "function():number -- Get the index of the floor the elevator is currently at.")
        public Object[] getElevatorFloor(final Context context, final Arguments args) {
            final ElevatorContraption contraption = contraption();
            if (contraption == null) return result(0);
            for (int i = 0; i < contraption.namesList.size(); ++i) {
                if ((int) contraption.namesList.get(i).getFirst() == contraption.getCurrentTargetY(blockEntity.getLevel())) {
                    return result(i);
                }
            }
            return result(0);
        }

        @Callback(direct = true, doc = "function():number -- Get the number of floors configured on this elevator.")
        public Object[] getElevatorFloors(final Context context, final Arguments args) {
            final ElevatorContraption contraption = contraption();
            return result(contraption == null ? 0 : contraption.namesList.size());
        }

        @Callback(direct = true, doc = "function(index:number):string -- Get the name of the given floor index.")
        public Object[] getElevatorFloorName(final Context context, final Arguments args) {
            final int index = args.checkInteger(0);
            final ElevatorContraption contraption = contraption();
            if (contraption == null || index < 0 || index >= contraption.namesList.size()) {
                return result(String.valueOf(index));
            }
            return result(contraption.namesList.get(index).getSecond().getFirst());
        }

        @Callback(direct = true, doc = "function():boolean -- Whether the elevator has arrived at its target floor.")
        public Object[] hasElevatorArrived(final Context context, final Arguments args) {
            final ElevatorContraption contraption = contraption();
            return result(contraption != null && contraption.arrived);
        }

        @Callback(doc = "function(index:number):number -- Send the elevator to the given floor index. Returns the change in target Y.")
        public Object[] gotoElevatorFloor(final Context context, final Arguments args) {
            final int index = args.checkInteger(0);
            final ElevatorContraption contraption = contraption();
            if (contraption == null || index < 0 || index >= contraption.namesList.size()) {
                return result(0);
            }

            final Level level = blockEntity.getLevel();
            final int oldTargetY = contraption.getCurrentTargetY(level);
            final int targetY = contraption.namesList.get(index).getFirst();

            final ElevatorColumn column = ElevatorColumn.get(level, contraption.getGlobalColumn());
            if (!contraption.isTargetUnreachable(targetY)) {
                final BlockPos contactPos = column.contactAt(targetY);
                final BlockState contactState = level.getBlockState(contactPos);
                final Block contactBlock = contactState.getBlock();
                if (contactBlock instanceof ElevatorContactBlock elevatorContactBlock) {
                    elevatorContactBlock.callToContactAndUpdate(column, contactState, level, contactPos, false);
                }
            }

            return result(targetY - oldTargetY);
        }
    }

    public static final class MechanicalBearing extends CreateEnvironment<MechanicalBearingBlockEntity> {
        MechanicalBearing(final MechanicalBearingBlockEntity blockEntity) {
            super(blockEntity, "Create_MechanicalBearing");
        }

        @Callback(direct = true, doc = "function():number -- Get the current angle of the bearing.")
        public Object[] getBearingAngle(final Context context, final Arguments args) {
            return result(blockEntity.getInterpolatedAngle(.5f));
        }
    }

    // PulleyBlockEntity is the rope pulley's own class; ElevatorPulleyBlockEntity
    // extends it, so this driver is registered with a predicate excluding
    // elevator pulleys (they already get their own, more specific driver
    // above) to avoid exposing both Create_RopePulley and Create_ElevatorPulley
    // on the same block.
    public static final class RopePulley extends CreateEnvironment<PulleyBlockEntity> {
        RopePulley(final PulleyBlockEntity blockEntity) {
            super(blockEntity, "Create_RopePulley");
        }

        @Callback(direct = true, doc = "function():number -- Get the current offset of the pulley rope.")
        public Object[] getPulleyDistance(final Context context, final Arguments args) {
            return result(blockEntity.getInterpolatedOffset(.5f));
        }
    }

    public static final class HosePulley extends CreateEnvironment<HosePulleyBlockEntity> {
        HosePulley(final HosePulleyBlockEntity blockEntity) {
            super(blockEntity, "Create_HosePulley");
        }

        @Callback(direct = true, doc = "function():number -- Get the current offset of the hose pulley.")
        public Object[] getPulleyDistance(final Context context, final Arguments args) {
            return result(blockEntity.getInterpolatedOffset(.5f));
        }
    }

    public static final class MechanicalPiston extends CreateEnvironment<MechanicalPistonBlockEntity> {
        MechanicalPiston(final MechanicalPistonBlockEntity blockEntity) {
            super(blockEntity, "Create_MechanicalPiston");
        }

        @Callback(direct = true, doc = "function():number -- Get the current offset of the piston.")
        public Object[] getPistonDistance(final Context context, final Arguments args) {
            return result(blockEntity.getInterpolatedOffset(.5f));
        }

        // Sticky vs. regular mechanical pistons share this exact block entity
        // class; the distinction lives on the Block instance instead.
        @Callback(direct = true, doc = "function():boolean -- Whether this is the sticky variant of the mechanical piston.")
        public Object[] isSticky(final Context context, final Arguments args) {
            return result(MechanicalPistonBlock.isStickyPiston(blockEntity.getBlockState()));
        }
    }
}
