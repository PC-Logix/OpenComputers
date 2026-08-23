package li.cil.oc.common.block

import li.cil.oc.{Localization, Settings}
import li.cil.oc.common.blockentity
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.server.loot.LootFunctions
import li.cil.oc.util.Tooltip
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.network.chat.{Component => ITextComponent}
import net.minecraft.server.level.{ServerLevel => ServerWorld}
import net.minecraft.util.RandomSource
import net.minecraft.world.{InteractionResult => ActionResultType}
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.{Player => PlayerEntity}
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.level.{BlockGetter => IBlockReader, Level => World}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.{BlockHitResult => BlockRayTraceResult}
import net.minecraft.world.phys.shapes.{VoxelShape, CollisionContext => ISelectionContext}

import java.util
import scala.jdk.CollectionConverters._

class Print(props: Properties) extends RedstoneAware(props) {
  @Deprecated
  override def propagatesSkylightDown(state: BlockState, world: IBlockReader, pos: BlockPos) = false

  // ----------------------------------------------------------------------- //

  override protected def tooltipBody(stack: ItemStack, context: TooltipContext, tooltip: util.List[ITextComponent], flag: TooltipFlag): Unit = {
    super.tooltipBody(stack, context, tooltip, flag)
    val data = new PrintData(stack)
    data.tooltip.foreach(s => tooltip.addAll(s.linesIterator.map(ITextComponent.literal(_).setStyle(Tooltip.DefaultStyle)).toList.asJava))
  }

  override protected def tooltipTail(stack: ItemStack, context: TooltipContext, tooltip: util.List[ITextComponent], flag: TooltipFlag): Unit = {
    super.tooltipTail(stack, context, tooltip, flag)
    val data = new PrintData(stack)
    if (data.isBeaconBase) {
      tooltip.add(ITextComponent.literal(Localization.Tooltip.PrintBeaconBase).setStyle(Tooltip.DefaultStyle))
    }
    if (data.emitRedstone) {
      tooltip.add(ITextComponent.literal(Localization.Tooltip.PrintRedstoneLevel(data.redstoneLevel)).setStyle(Tooltip.DefaultStyle))
    }
    if (data.emitLight) {
      tooltip.add(ITextComponent.literal(Localization.Tooltip.PrintLightValue(data.lightLevel)).setStyle(Tooltip.DefaultStyle))
    }
  }

  override def getLightEmission(state: BlockState, world: IBlockReader, pos: BlockPos): Int =
    world match {
      case world: World if world.isLoaded(pos) => world.getBlockEntity(pos) match {
        case print: blockentity.Print => print.data.lightLevel
        case _ => super.getLightEmission(state, world, pos)
      }
      case _ => super.getLightEmission(state, world, pos)
    }

  @Deprecated
  override def getLightBlock(state: BlockState, world: IBlockReader, pos: BlockPos): Int =
    world match {
      case world: World if world.isLoaded(pos) => world.getBlockEntity(pos) match {
        case print: blockentity.Print if Settings.get.printsHaveOpacity => (print.data.opacity * 4).toInt
        case _ => super.getLightBlock(state, world, pos)
      }
      case _ => super.getLightBlock(state, world, pos)
    }

  override def getShape(state: BlockState, world: IBlockReader, pos: BlockPos, ctx: ISelectionContext): VoxelShape = {
    world.getBlockEntity(pos) match {
      case print: blockentity.Print => print.shape
      case _ => super.getShape(state, world, pos, ctx)
    }
  }

  def tickRate(world: World) = 20

  override def tick(state: BlockState, world: ServerWorld, pos: BlockPos, rand: RandomSource): Unit = {
    if (!world.isClientSide) world.getBlockEntity(pos) match {
      case print: blockentity.Print =>
        if (print.state) print.toggleState()
      case _ =>
    }
  }

  @Deprecated
  def isBeaconBase(world: IBlockReader, pos: BlockPos, beacon: BlockPos): Boolean = {
    world.getBlockEntity(pos) match {
      case print: blockentity.Print => print.data.isBeaconBase
      case _ => false
    }
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(pos: BlockPos, state: BlockState) = new blockentity.Print(pos, state)

  // ----------------------------------------------------------------------- //

  override def useWithoutItem(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hitResult: BlockRayTraceResult): ActionResultType = {
    world.getBlockEntity(pos) match {
      case print: blockentity.Print => if (print.activate()) ActionResultType.sidedSuccess(world.isClientSide) else ActionResultType.PASS
      case _ => super.useWithoutItem(state, world, pos, player, hitResult)
    }
  }

  override def onRemove(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean): Unit = {
    world.getBlockEntity(pos) match {
      case print: blockentity.Print if print.data.emitRedstone(print.state) =>
        world.updateNeighborsAt(pos, this)
        for (side <- Direction.values) {
          world.updateNeighborsAt(pos.relative(side), this)
        }
      case _ =>
    }
    super.onRemove(state, world, pos, newState, moved)
  }

  override def setPlacedBy(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity, stack: ItemStack): Unit = {
    super.setPlacedBy(world, pos, state, placer, stack)
    world.getBlockEntity(pos) match {
      case tileEntity: blockentity.Print => {
        tileEntity.data.loadData(stack)
        tileEntity.updateShape()
        tileEntity.updateRedstone()
        tileEntity.getLevel.getLightEngine.checkBlock(tileEntity.getBlockPos)
      }
      case _ =>
    }
  }

  override def getDrops(state: BlockState, ctx: LootParams.Builder): util.List[ItemStack] = {
    val newCtx = ctx.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, f => {
      ctx.getOptionalParameter(LootContextParams.BLOCK_ENTITY) match {
        case tileEntity: blockentity.Print => f.accept(tileEntity.data.createItemStack())
        case _ =>
      }
    })
    super.getDrops(state, newCtx)
  }
}
