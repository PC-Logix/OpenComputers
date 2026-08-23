package li.cil.oc.common.block

import li.cil.oc.{api, Constants, Settings}
import li.cil.oc.client.gui
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.blockentity
import li.cil.oc.common.blockentity.BlockEntityTypes
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.{PackedColor, Tooltip}
import net.minecraft.client.Minecraft
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.network.chat.{Component => ITextComponent}
import net.minecraft.world.{InteractionHand => Hand}
import net.minecraft.world.entity.{Entity, LivingEntity}
import net.minecraft.world.entity.player.{Player => PlayerEntity}
import net.minecraft.world.entity.projectile.{Arrow => ArrowEntity}
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.level.{Level => World}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.{BlockState, StateDefinition => StateContainer}
import net.neoforged.api.distmarker.{Dist, OnlyIn}

import java.util

class Screen(props: Properties, val tier: Int) extends RedstoneAware(props) with traits.Tickable {
  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw)

  // ----------------------------------------------------------------------- //

  override protected def tooltipBody(stack: ItemStack, context: TooltipContext, tooltip: util.List[ITextComponent], flag: TooltipFlag): Unit = {
    val (w, h) = Settings.screenResolutionsByTier(tier)
    val depth = PackedColor.Depth.bits(Settings.screenDepthsByTier(tier))
    Tooltip.add(tooltip, flag, getClass.getSimpleName.toLowerCase, w, h, depth)
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(pos: BlockPos, state: BlockState) = new blockentity.Screen(pos, state, tier)

  // ----------------------------------------------------------------------- //

  override def setPlacedBy(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity, stack: ItemStack): Unit = {
    super.setPlacedBy(world, pos, state, placer, stack)
    world.getBlockEntity(pos) match {
      case screen: blockentity.Screen => screen.delayUntilCheckForMultiBlock = 0
      case _ =>
    }
  }

  override def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = rightClick(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ, force = false)

  def rightClick(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack,
                 side: Direction, hitX: Float, hitY: Float, hitZ: Float, force: Boolean) = {
    if (Wrench.holdsApplicableWrench(player, pos) && getValidRotations(world, pos).contains(side) && !force) false
    else if (api.Items.get(heldItem) == api.Items.get(Constants.ItemName.Analyzer)) false
    else world.getBlockEntity(pos) match {
      case screen: blockentity.Screen if screen.hasKeyboard && (force || player.isCrouching == screen.origin.invertTouchMode) =>
        // Yep, this GUI is actually purely client side (to trigger it from
        // the server we would have to give screens a "container", which we
        // do not want).
        if (world.isClientSide) showGui(screen)
        true
      case screen: blockentity.Screen if screen.tier > 0 && side == screen.facing =>
        if (world.isClientSide && player == Minecraft.getInstance.player) {
          screen.click(hitX, hitY, hitZ)
        }
        else true
      case _ => false
    }
  }

  @OnlyIn(Dist.CLIENT)
  private def showGui(screen: blockentity.Screen): Unit = {
    Minecraft.getInstance.pushGuiLayer(new gui.Screen(screen.origin.buffer, screen.tier > 0, () => screen.origin.hasKeyboard, () => screen.origin.buffer.isRenderingEnabled))
  }

  override def stepOn(world: World, pos: BlockPos, state: BlockState, entity: Entity): Unit =
    if (!world.isClientSide) world.getBlockEntity(pos) match {
      case screen: blockentity.Screen if screen.tier > 0 && screen.facing == Direction.UP => screen.walk(entity)
      case _ => super.stepOn(world, pos, state, entity)
    }

  override def entityInside(state: BlockState, world: World, pos: BlockPos, entity: Entity): Unit =
    if (world.isClientSide) (entity, world.getBlockEntity(pos)) match {
      case (arrow: ArrowEntity, screen: blockentity.Screen) if screen.tier > 0 =>
        val hitX = math.max(0, math.min(1, arrow.getX - pos.getX))
        val hitY = math.max(0, math.min(1, arrow.getY - pos.getY))
        val hitZ = math.max(0, math.min(1, arrow.getZ - pos.getZ))
        val absX = math.abs(hitX - 0.5)
        val absY = math.abs(hitY - 0.5)
        val absZ = math.abs(hitZ - 0.5)
        val side = if (absX > absY && absX > absZ) {
          if (hitX < 0.5) Direction.WEST
          else Direction.EAST
        }
        else if (absY > absZ) {
          if (hitY < 0.5) Direction.DOWN
          else Direction.UP
        }
        else {
          if (hitZ < 0.5) Direction.NORTH
          else Direction.SOUTH
        }
        if (side == screen.facing) {
          screen.shot(arrow)
        }
      case _ =>
    }

  // ----------------------------------------------------------------------- //

  override def getValidRotations(world: World, pos: BlockPos) =
    world.getBlockEntity(pos) match {
      case screen: blockentity.Screen =>
        if (screen.facing == Direction.UP || screen.facing == Direction.DOWN) Direction.values
        else Direction.values.filter {
          d => d != screen.facing && d != screen.facing.getOpposite
        }
      case _ => super.getValidRotations(world, pos)
    }

  override def getBlockEntityType: BlockEntityType[_ <: BlockEntity] = BlockEntityTypes.SCREEN.get()
}
