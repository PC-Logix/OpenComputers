package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.blockentity
import li.cil.oc.common.blockentity.BlockEntityTypes
import li.cil.oc.common.entity.TrainRobot
import li.cil.oc.common.init.OCBlocks
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.menu.MenuTypes
import li.cil.oc.server.{agent, PacketSender}
import li.cil.oc.server.loot.LootFunctions
import li.cil.oc.util.{BlockPosition, InventoryUtils, Tooltip}
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.{Component => ITextComponent}
import net.minecraft.server.level.{ServerPlayer => ServerPlayerEntity}
import net.minecraft.world.{InteractionHand => Hand}
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.{Player => PlayerEntity}
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.level.{LevelReader, BlockGetter => IBlockReader, Level => World}
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.shapes.{VoxelShape, CollisionContext => ISelectionContext, Shapes => VoxelShapes}

import java.util

class RobotProxy(props: Properties) extends RedstoneAware(props) with traits.StateAware with traits.Tickable {
  val shape = VoxelShapes.box(0.1, 0.1, 0.1, 0.9, 0.9, 0.9)

  override val getDescriptionId = "robot"

  var moving = new ThreadLocal[Option[blockentity.Robot]] {
    override protected def initialValue = None
  }

  // ----------------------------------------------------------------------- //

  override def getCloneItemStack(world: LevelReader, pos: BlockPos, state: BlockState): ItemStack =
    world.getBlockEntity(pos) match {
      case proxy: blockentity.RobotProxy => proxy.robot.info.copyItemStack(world.registryAccess())
      case _ => ItemStack.EMPTY
    }

  override def getShape(state: BlockState, world: IBlockReader, pos: BlockPos, ctx: ISelectionContext): VoxelShape = {
    world.getBlockEntity(pos) match {
      case proxy: blockentity.RobotProxy =>
        val robot = proxy.robot
        if (robot.isAnimatingMove) {
          val remaining = robot.animationTicksLeft.toDouble / robot.animationTicksTotal.toDouble
          val blockPos = robot.moveFrom.get
          val vec = robot.getBlockPos
          val delta = new BlockPos(blockPos.getX - vec.getX, blockPos.getY - vec.getY, blockPos.getZ - vec.getZ)
          shape.move(delta.getX * remaining, delta.getY * remaining, delta.getZ * remaining)
        }
        else shape
      case _ => super.getShape(state, world, pos, ctx)
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def tooltipHead(stack: ItemStack, context: TooltipContext, tooltip: util.List[ITextComponent], flag: TooltipFlag): Unit = {
    super.tooltipHead(stack, context, tooltip, flag)
    addLines(stack, tooltip, flag)
  }

  override protected def tooltipBody(stack: ItemStack, context: TooltipContext, tooltip: util.List[ITextComponent], flag: TooltipFlag): Unit = {
    Tooltip.add(tooltip, flag, "robot")
  }

  override protected def tooltipTail(stack: ItemStack, context: TooltipContext, tooltip: util.List[ITextComponent], flag: TooltipFlag): Unit = {
    super.tooltipTail(stack, context, tooltip, flag)
    if (Tooltip.showExtendedTooltip(flag)) {
      val info = new RobotData(stack)
      val components = info.containers ++ info.components
      if (components.length > 0) {
        Tooltip.add(tooltip, flag, "server.Components")
        for (component <- components if !component.isEmpty) {
          tooltip.add(ITextComponent.literal("- " + component.getHoverName.getString).setStyle(Tooltip.DefaultStyle))
        }
      }
    }
  }

  private def addLines(stack: ItemStack, tooltip: util.List[ITextComponent], flag: TooltipFlag): Unit = {
    if (stack.has(DataComponents.CUSTOM_DATA)) {
      if (stack.get(DataComponents.CUSTOM_DATA).contains(Settings.namespace + "xp")) {
        val xp = stack.get(DataComponents.CUSTOM_DATA).getUnsafe.getDouble(Settings.namespace + "xp")
        val level = Math.min((Math.pow(xp - Settings.get.baseXpToLevel, 1 / Settings.get.exponentialXpGrowth) / Settings.get.constantXpGrowth).toInt, 30)
        if (level > 0) {
          Tooltip.add(tooltip, flag, getDescriptionId + "_level", level)
        }
      }
      if (stack.get(DataComponents.CUSTOM_DATA).contains(Settings.namespace + "storedEnergy")) {
        val energy = stack.get(DataComponents.CUSTOM_DATA).copyTag().getInt(Settings.namespace + "storedEnergy")
        if (energy > 0) {
          Tooltip.add(tooltip, flag, getDescriptionId + "_storedenergy", energy)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(pos: BlockPos, state: BlockState): blockentity.RobotProxy = {
    moving.get match {
      case Some(robot) => new blockentity.RobotProxy(pos, state, robot)
      case _ => new blockentity.RobotProxy(pos, state)
    }
  }

  // ----------------------------------------------------------------------- //

  override def getDrops(state: BlockState, ctx: LootParams.Builder): util.List[ItemStack] = {
    val newCtx = ctx.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, f => {
      ctx.getOptionalParameter(LootContextParams.BLOCK_ENTITY) match {
        case proxy: blockentity.RobotProxy =>
          val robot = proxy.robot
          if (robot.node != null) {
            if (gettingDropsForActualDrop) {
              robot.node.remove()
              robot.saveComponents()
            }
            f.accept(robot.info.createItemStack())
          }
        case _ =>
      }
    })
    super.getDrops(state, newCtx)
  }

  private val getDropForRealDropCallers = Set(
    "appeng.parts.automation.PartAnnihilationPlane.EatBlock"
  )

  private def gettingDropsForActualDrop = new Exception().getStackTrace.exists(element => getDropForRealDropCallers.contains(element.getClassName + "." + element.getMethodName))

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (!player.isCrouching && TrainRobot.isHat(heldItem)) {
      if (!world.isClientSide) {
        world.getBlockEntity(pos) match {
          case proxy: blockentity.RobotProxy if TrainRobot.replaceRobot(world, pos, player.getYRot, proxy) =>
            if (!player.isCreative) heldItem.shrink(1)
          case _ =>
        }
      }
      true
    }
    else if (!player.isCrouching) {
      if (!world.isClientSide) {
        // We only send slot changes to nearby players, so if there was no slot
        // change since this player got into range he might have the wrong one,
        // so we send him the current one just in case.
        (player, world.getBlockEntity(pos)) match {
          case (srvPlr: ServerPlayerEntity, proxy: blockentity.RobotProxy) if proxy.robot.node.network != null =>
            PacketSender.sendRobotSelectedSlotChange(proxy.robot)
            if (proxy.stillValid(player)) {
              MenuTypes.openRobotGui(srvPlr, proxy.robot)
            }
          case _ =>
        }
      }
      true
    }
    else if (heldItem.isEmpty) {
      if (!world.isClientSide) {
        world.getBlockEntity(pos) match {
          case proxy: blockentity.RobotProxy if !proxy.machine.isRunning && proxy.stillValid(player) => proxy.machine.start()
          case _ =>
        }
      }
      true
    }
    else false
  }

  override def setPlacedBy(world: World, pos: BlockPos, state: BlockState, entity: LivingEntity, stack: ItemStack): Unit = {
    super.setPlacedBy(world, pos, state, entity, stack)
    if (!world.isClientSide) ((entity, world.getBlockEntity(pos)) match {
      case (player: agent.Player, proxy: blockentity.RobotProxy) =>
        Some((proxy.robot, player.agent.ownerName, player.agent.ownerUUID))
      case (player: PlayerEntity, proxy: blockentity.RobotProxy) =>
        Some((proxy.robot, player.getName.getString, player.getGameProfile.getId))
      case _ => None
    }) match {
      case Some((robot, owner, uuid)) =>
        robot.ownerName = owner
        robot.ownerUUID = agent.Player.determineUUID(Option(uuid))

        // The BlockEntity is already live by the time setPlacedBy runs, so
        // clearRemoved()/validate may have connected the robot using its empty
        // default component layout. Rebuild those environments from the actual
        // assembled robot ItemStack now that its CPU/EEPROM/upgrades are known.
        robot.disconnectComponents()
        robot.info.loadData(stack)
        robot.bot.node.changeBuffer(robot.info.robotEnergy - robot.bot.node.localBuffer)
        robot.updateInventorySize()
        robot.connectComponents()
        robot.machine.onHostChanged()

      case _ =>
    }
  }

  override def onDestroyedByPlayer(
                                state: BlockState,
                                world: World,
                                pos: BlockPos,
                                player: PlayerEntity,
                                willHarvest: Boolean,
                                fluid: FluidState
                              ): Boolean = {
    Option(world.getBlockEntity(pos)).collect { case proxy: blockentity.RobotProxy => proxy }.foreach { proxy =>
      val robot = proxy.robot
      val playerName = player.getName.getString

      if (robot.isCreative && (!player.isCreative || !robot.canInteract(playerName))) {
        return false
      }

      if (!world.isClientSide) {
        if (robot.player == player) return false

        robot.node.remove()
        robot.saveComponents()

        if (player.isCreative) {
          InventoryUtils.spawnStackInWorld(BlockPosition(pos, world), robot.info.createItemStack())
        }
      }

      robot.moveFrom.foreach(fromPos => {
        val targetState = world.getBlockState(fromPos)
        if (targetState.is(OCBlocks.RobotAfterimage.get())) {
          world.setBlock(fromPos, Blocks.AIR.defaultBlockState, 3)
        }
      })
    }

    super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid)
  }

  override def getBlockEntityType: BlockEntityType[_ <: BlockEntity] = BlockEntityTypes.ROBOT.get()
}
