package li.cil.oc.integration.create;

import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import li.cil.oc.api.Network;
import li.cil.oc.common.block.Case;
import li.cil.oc.common.block.Keyboard;
import li.cil.oc.common.block.Microcontroller;
import li.cil.oc.common.block.Screen;
import li.cil.oc.common.block.SimpleBlock;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.blockentity.traits.Computer;
import li.cil.oc.common.blockentity.traits.NetworkBlockEntity;
import li.cil.oc.common.blockentity.traits.RedstoneAware;
import li.cil.oc.common.blockentity.traits.Rotatable;
import li.cil.oc.common.blockentity.traits.Tickable;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.util.BlockPosHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;

/** Create transformations for OC blocks. This got a bit out of hand. */
final class CreateContraptionTransformers {
    private static final String PITCH = "pitch";
    private static final String YAW = "yaw";
    private static final String FACING = "facing";
    private static final String MOUNT = "mount";

    private CreateContraptionTransformers() {
    }

    static void register() {
        // I tried to make a list of these. The registry loop was less typing.
        // Maybe this works.
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof SimpleBlock) {
                MovedBlockTransformerRegistries.BLOCK_TRANSFORMERS.register(
                        block, CreateContraptionTransformers::transformBlock);
                if (block.defaultBlockState().hasBlockEntity()) {
                    // Any OC block entity can be an external component. Give it a
                    // live moving host instead of only supporting the handful of
                    // blocks that happened to be used by the first test rig.
                    MovementBehaviour.REGISTRY.register(block, OC_MOVEMENT);
                }
            }
            if (block instanceof Case || block instanceof Microcontroller || block instanceof Screen
                    || block instanceof Keyboard) {
                // Chests get this for free. OC needs a special invitation.
                // FUCK YOU MINECRAFT! Apparently moving blocks have a second click system.
                MovingInteractionBehaviour.REGISTRY.register(block, OC_INTERACTION);
            }
        }

        registerBlockEntity(BlockEntityTypes.HOLOGRAM.get());
        registerBlockEntity(BlockEntityTypes.PRINT.get());
        registerBlockEntity(BlockEntityTypes.ROBOT.get());
    }

    private static void registerBlockEntity(final BlockEntityType<?> type) {
        MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(
                type, CreateContraptionTransformers::transformBlockEntity);
    }

    private static final MovementBehaviour OC_MOVEMENT = new MovementBehaviour() {
        @Override
        public void startMoving(final MovementContext context) {
            // Create does not keep a live BlockEntity around while it is moving.
            // So we make one. This sounds worse when written down.
            if (!context.world.isClientSide) {
                context.temporaryData = makeEnvironment(context);
            }
        }

        @Override
        public void tick(final MovementContext context) {
            if (context.world.isClientSide) {
                registerMovingScreen(context);
                return;
            }

            NetworkBlockEntity environment = movingEnvironment(context);
            if (environment == null) {
                // Train carriages can deserialize their actor contexts without
                // temporaryData. Rebuild OC's off-world block entity on demand.
                environment = makeEnvironment(context);
                context.temporaryData = environment;
                if (environment == null) {
                    return;
                }
            }

            // The position is the center of the actor block. OC wants that
            // position for wireless range, sounds, events, and packet logs.
            Vec3 position = context.position;
            if (position == null) {
                position = new Vec3(
                        context.localPos.getX() + 0.5,
                        context.localPos.getY() + 0.5,
                        context.localPos.getZ() + 0.5);
            }
            environment.updateMovingPosition(position, context.rotation, context.state);
            if (environment instanceof Computer computer) {
                computer.tickMoving();
            } else if (environment instanceof Tickable tickable) {
                tickable.tick();
            }

            // Vanilla redstone is not ticking inside Create's little magic box.
            // So if the OC output is next to a captured lamp, fake the one bit
            // of redstone simulation we actually need. Hmmm. Like this?
            syncMovingLamp(context, environment);

            // Hmmm. The real cable network is also off-world. Reconnect the
            // fake nodes whenever both ends of an OC connection exist.
            connectMovingNeighbors(context, environment);
        }

        @Override
        public void stopMoving(final MovementContext context) {
            if (context.world.isClientSide) {
                unregisterMovingScreen(context);
                return;
            }

            stopMovingEnvironments(context);
        }

        @Override
        public void writeExtraData(final MovementContext context) {
            // Hmmm. Like this? Create may save a running contraption before it stops.
            saveEnvironment(context);
        }
    };

    private static final MovingInteractionBehaviour OC_INTERACTION = new MovingInteractionBehaviour() {
        @Override
        public boolean handlePlayerInteraction(final Player player, final InteractionHand hand,
                                               final BlockPos localPos,
                                               final AbstractContraptionEntity contraptionEntity) {
            if (contraptionEntity.level().isClientSide) {
                // Case menus are opened by the server. Screens are the weird
                // exception: their terminal GUI is deliberately client-only.
                BlockEntity renderEntity = contraptionEntity.getContraption()
                        .getOrCreateClientContraptionLazy().getBlockEntity(localPos);
                StructureBlockInfo capturedInfo = contraptionEntity.getContraption().getBlocks().get(localPos);
                if (renderEntity == null || capturedInfo == null) {
                    return true;
                }
                BlockState capturedState = capturedInfo.state();
                if (renderEntity instanceof li.cil.oc.common.blockentity.Screen screen
                        && capturedState.getBlock() instanceof Screen screenBlock) {
                    screen.registerMovingClientBuffer(contraptionEntity.level());
                    screenBlock.rightClick(renderEntity.getLevel(), localPos, player, hand,
                            player.getItemInHand(hand), screen.facing(), 0.5f, 0.5f, 0.5f, true);
                } else if (renderEntity instanceof li.cil.oc.common.blockentity.Keyboard keyboard
                        && capturedState.getBlock() instanceof Keyboard keyboardBlock) {
                    keyboardBlock.localOnBlockActivated(renderEntity.getLevel(), localPos, player, hand,
                            player.getItemInHand(hand), keyboard.facing(), 0.5f, 0.5f, 0.5f);
                }
                return true;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return false;
            }

            var actor = contraptionEntity.getContraption().getActorAt(localPos);
            if (actor == null || actor.right == null
                    || !(actor.right.temporaryData instanceof NetworkBlockEntity environment)) {
                return false;
            }

            if (environment instanceof li.cil.oc.common.blockentity.Case computer
                    && computer.stillValid(player)) {
                MenuTypes.openCaseGui(serverPlayer, computer);
                return true;
            }

            if (environment instanceof li.cil.oc.common.blockentity.Microcontroller microcontroller) {
                if (microcontroller.machine().isRunning()) {
                    microcontroller.machine().stop();
                } else {
                    microcontroller.machine().start();
                }
                return true;
            }

            return false;
        }
    };

    private static NetworkBlockEntity makeEnvironment(final MovementContext context) {
        if (!(context.state.getBlock() instanceof EntityBlock entityBlock)) {
            return null;
        }

        BlockEntity blockEntity = entityBlock.newBlockEntity(context.localPos, context.state);
        if (!(blockEntity instanceof NetworkBlockEntity environment)) {
            return null;
        }

        blockEntity.setLevel(context.world);

        // Screen buffers live in SaveHandler files named by their world chunk.
        // Make the fake block entity look like it is still at its pre-assembly
        // position while loadWithComponents reads that file. If we let it use
        // localPos here, it finds nothing and builds a blank T1 buffer instead.
        BlockPos originalPos = context.localPos.offset(context.contraption.anchor);
        Vec3 savedPosition = context.position != null
                ? context.position
                : new Vec3(
                        originalPos.getX() + 0.5,
                        originalPos.getY() + 0.5,
                        originalPos.getZ() + 0.5);
        environment.beginMoving(
                context.world,
                savedPosition,
                context.rotation,
                context.state);
        if (context.blockEntityData != null) {
            blockEntity.loadWithComponents(context.blockEntityData, context.world.registryAccess());
        }

        // The fake BE is not in Level.blockEntities, so the normal neighbor scan
        // cannot find it. A private network still lets its machine and cards run.
        Network.joinNewNetwork(environment.node());
        if (environment instanceof Computer computer
                && context.contraption.entity instanceof CarriageContraptionEntity carriageEntity
                && carriageEntity.getCarriage() != null) {
            CreateTrainEnvironment.attach(computer, carriageEntity.getCarriage().train, context.world);
        }
        return environment;
    }

    private static NetworkBlockEntity movingEnvironment(final MovementContext context) {
        return context.temporaryData instanceof NetworkBlockEntity environment ? environment : null;
    }

    private static void unregisterMovingScreen(final MovementContext context) {
        if (context.contraption == null || context.contraption.entity == null) {
            return;
        }

        BlockEntity renderEntity = context.contraption.getOrCreateClientContraptionLazy()
                .getBlockEntity(context.localPos);
        if (renderEntity instanceof li.cil.oc.common.blockentity.Screen screen) {
            screen.unregisterMovingClientBuffer();
        }
    }

    private static void registerMovingScreen(final MovementContext context) {
        if (context.contraption == null || context.contraption.entity == null) {
            return;
        }

        BlockEntity renderEntity = context.contraption.getOrCreateClientContraptionLazy()
                .getBlockEntity(context.localPos);
        if (renderEntity instanceof li.cil.oc.common.blockentity.Screen screen) {
            screen.registerMovingClientBuffer(context.contraption.entity.level());
        }
    }

    private static void saveEnvironment(final MovementContext context) {
        NetworkBlockEntity environment = movingEnvironment(context);
        if (environment != null && context.blockEntityData != null) {
            environment.saveMovingState(context.blockEntityData, context.world.registryAccess());
        }
    }

    private static void stopMovingEnvironments(final MovementContext context) {
        if (movingEnvironment(context) == null || context.contraption == null) {
            return;
        }

        // Create stops actors one at a time. If the floppy drive happens to go
        // first, OpenOS gets told its boot filesystem vanished before Create
        // reaches the computer. Save the entire OC network while it still
        // exists, then tear it down ourselves in an order that cannot do that.
        for (var actor : context.contraption.getActors()) {
            if (actor.right.temporaryData instanceof NetworkBlockEntity) {
                saveEnvironment(actor.right);
            }
        }

        // Close every VM before removing any external component host. Their
        // state is safely in NBT now, and a dead listener cannot queue a fake
        // component_removed while the remaining actors disappear.
        for (var actor : context.contraption.getActors()) {
            if (actor.right.temporaryData instanceof Computer computer) {
                CreateTrainEnvironment.detach(computer);
                computer.disposeMoving();
                actor.right.temporaryData = null;
            }
        }

        for (var actor : context.contraption.getActors()) {
            if (actor.right.temporaryData instanceof NetworkBlockEntity environment) {
                environment.disposeMoving();
                actor.right.temporaryData = null;
            }
        }
    }

    private static void connectMovingNeighbors(final MovementContext context, final NetworkBlockEntity environment) {
        for (Direction side : Direction.values()) {
            BlockPos neighborPos = BlockPosHelper.relative(context.localPos, side);
            for (var actor : context.contraption.getActors()) {
                if (!neighborPos.equals(actor.left.pos())) {
                    continue;
                }
                NetworkBlockEntity neighbor = actor.right.temporaryData instanceof NetworkBlockEntity other ? other : null;
                if (neighbor != null && neighbor.node() != null && environment.node() != null) {
                    environment.node().connect(neighbor.node());
                }
            }
        }
    }

    private static void syncMovingLamp(final MovementContext context, final NetworkBlockEntity environment) {
        if (!(environment instanceof RedstoneAware redstone)
                || context.contraption == null
                || context.contraption.entity == null) {
            return;
        }

        for (Direction localSide : Direction.values()) {
            Direction worldSide = redstone.toGlobal(localSide);
            if (worldSide == null) {
                continue;
            }

            BlockPos lampPos = BlockPosHelper.relative(context.localPos, localSide);
            StructureBlockInfo lampInfo = context.contraption.getBlocks().get(lampPos);
            if (lampInfo == null || !lampInfo.state().is(Blocks.REDSTONE_LAMP)) {
                continue;
            }

            boolean lit = redstone.getOutput(worldSide) > 0;
            boolean alreadyLit = lampInfo.state().getValue(BlockStateProperties.LIT);
            if (lit == alreadyLit) {
                continue;
            }

            // FUCK YOU MINECRAFT! A moving lamp is not really a lamp right now.
            BlockState newState = lampInfo.state().setValue(BlockStateProperties.LIT, lit);
            context.contraption.entity.setBlock(
                    lampPos,
                    new StructureBlockInfo(lampInfo.pos(), newState, lampInfo.nbt()));
        }
    }

    static BlockState transformBlock(final BlockState state, final StructureTransform transform) {
        Direction pitch = getDirection(state, PITCH);
        Direction yaw = getDirection(state, YAW);
        if (pitch != null && yaw != null) {
            return transformPitchAndYaw(state, pitch, yaw, transform);
        }

        Direction facing = getDirection(state, FACING);
        if (facing != null) {
            Direction transformed = transformDirection(facing, transform);
            if (hasDirection(state, FACING, transformed)) {
                return setDirection(state, FACING, transformed);
            }
        }

        Direction mount = getDirection(state, MOUNT);
        if (mount != null) {
            Direction transformed = transformDirection(mount, transform);
            if (hasDirection(state, MOUNT, transformed)) {
                return setDirection(state, MOUNT, transformed);
            }
        }

        return state;
    }

    private static BlockState transformPitchAndYaw(final BlockState state, final Direction pitch,
                                                    final Direction yaw, final StructureTransform transform) {
        // Pitch is either actually pitch or NORTH pretending not to be pitch.
        // Hmmm. Like this?
        Direction forward = yaw;
        Direction up = Direction.UP;
        if (pitch.getAxis().isVertical()) {
            forward = pitch;
            up = yaw;
        }
        forward = transformDirection(forward, transform);
        up = transformDirection(up, transform);

        if (forward.getAxis().isVertical()) {
            BlockState result = setDirection(state, PITCH, forward);
            if (hasDirection(result, YAW, up)) {
                result = setDirection(result, YAW, up);
            }
            return result;
        }

        // PropertyRotatable.Pitch only allows UP, DOWN, or NORTH. Horizontal
        // orientation lives in Yaw, so NORTH is the "no pitch" sentinel here.
        // FUCK YOU MINECRAFT! Why is NORTH the "not really pitch" direction?
        BlockState result = setDirection(state, PITCH, Direction.NORTH);
        if (hasDirection(result, YAW, forward)) {
            result = setDirection(result, YAW, forward);
        }
        return result;
    }

    private static Direction transformDirection(final Direction direction, final StructureTransform transform) {
        Direction result = direction;
        if (transform.mirror != null && transform.mirror != Mirror.NONE) {
            result = transform.mirrorFacing(result);
        }
        // Create calls this with Rotation.NONE sometimes, which is fine.
        if (transform.rotation != null && transform.rotationAxis != null
                && transform.rotation.ordinal() != 0) {
            result = transform.rotateFacing(result);
        }
        return result;
    }

    private static void transformBlockEntity(final BlockEntity blockEntity, final StructureTransform transform) {
        if (!(blockEntity instanceof Rotatable rotatable)) {
            return;
        }

        Direction oldPitch = rotatable.pitch();
        Direction oldYaw = rotatable.yaw();
        Direction forward = oldYaw;
        Direction up = Direction.UP;
        if (oldPitch.getAxis().isVertical()) {
            forward = oldPitch;
            up = oldYaw;
        }
        forward = transformDirection(forward, transform);
        up = transformDirection(up, transform);

        // There is probably a nicer way to do this. This is the way that is
        // least likely to make the old block orientation code angry.
        if (forward.getAxis().isVertical()) {
            rotatable.setFromPitchAndYaw(forward,
                    up.getAxis().isHorizontal() ? up : oldYaw);
        } else {
            rotatable.setFromPitchAndYaw(Direction.NORTH, forward);
        }
    }

    private static Direction getDirection(final BlockState state, final String name) {
        final Property<?> property = findProperty(state, name);
        if (property == null) {
            return null;
        }
        return state.getValue(directionProperty(property));
    }

    private static boolean hasDirection(final BlockState state, final String name, final Direction value) {
        final Property<?> property = findProperty(state, name);
        return property != null && property.getPossibleValues().contains(value);
    }

    private static BlockState setDirection(final BlockState state, final String name, final Direction value) {
        return state.setValue(directionProperty(findProperty(state, name)), value);
    }

    private static Property<?> findProperty(final BlockState state, final String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Property<Direction> directionProperty(final Property<?> property) {
        return (Property<Direction>) property;
    }
}
