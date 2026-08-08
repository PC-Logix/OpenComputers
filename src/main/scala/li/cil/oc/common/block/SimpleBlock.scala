package li.cil.oc.common.block

import com.mojang.serialization.MapCodec

import java.util
import li.cil.oc.common.blockentity
import li.cil.oc.common.blockentity.traits.Colored
import li.cil.oc.common.blockentity.traits.Inventory
import li.cil.oc.common.blockentity.traits.Rotatable
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.server.loot.LootFunctions
import li.cil.oc.util.Color
import li.cil.oc.util.Tooltip
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.{Block, BaseEntityBlock => ContainerBlock, Mirror, RenderShape => BlockRenderType, Rotation}
import net.minecraft.world.item.{TooltipFlag => ITooltipFlag, Item}
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.{Player => PlayerEntity}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.level.block.entity.{BlockEntity => TileEntity}
import net.minecraft.core.Direction
import net.minecraft.world.{InteractionHand, InteractionResult, ItemInteractionResult}
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.{Level => World}
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

import scala.jdk.CollectionConverters._

abstract class SimpleBlock(props: Properties) extends ContainerBlock(props) {
  override protected def codec(): com.mojang.serialization.MapCodec[_ <: SimpleBlock] = com.mojang.serialization.MapCodec.unit(this)

  @Deprecated
  private var unlocalizedName = super.getDescriptionId

  @Deprecated
  private[oc] def setUnlocalizedName(name: String): Unit = unlocalizedName = "blockentity." + name

  @Deprecated
  override def getDescriptionId: String = unlocalizedName

  protected val validRotations_ : Array[Direction] = Array(Direction.UP, Direction.DOWN)

  def createItemStack(amount: Int = 1) = new ItemStack(this, amount)

  override def newBlockEntity(pos: BlockPos, state: BlockState): TileEntity = null

  override def getRenderShape(state: BlockState): BlockRenderType = BlockRenderType.MODEL

  // ----------------------------------------------------------------------- //
  // BlockItem
  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], flag: ITooltipFlag): Unit = {
    tooltipHead(stack, context, tooltip, flag)
    tooltipBody(stack, context, tooltip, flag)
    tooltipTail(stack, context, tooltip, flag)
  }

  protected def tooltipHead(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], flag: ITooltipFlag): Unit = {
  }

  protected def tooltipBody(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], flag: ITooltipFlag): Unit = {
    for (curr <- Tooltip.get(getClass.getSimpleName.toLowerCase).asScala) {
      tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle))
    }
  }

  protected def tooltipTail(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], flag: ITooltipFlag): Unit = {
  }

  // ----------------------------------------------------------------------- //
  // Rotation
  // ----------------------------------------------------------------------- //

  def getFacing(world: BlockGetter, pos: BlockPos): Direction =
    world.getBlockEntity(pos) match {
      case tileEntity: Rotatable => tileEntity.facing
      case _ => Direction.SOUTH
    }

  def setFacing(world: World, pos: BlockPos, value: Direction): Boolean =
    world.getBlockEntity(pos) match {
      case rotatable: Rotatable => rotatable.setFromFacing(value); true
      case _ => false
    }

  def setRotationFromEntityPitchAndYaw(world: World, pos: BlockPos, value: Entity): Boolean =
    world.getBlockEntity(pos) match {
      case rotatable: Rotatable => rotatable.setFromEntityPitchAndYaw(value); true
      case _ => false
    }

  def toLocal(world: BlockGetter, pos: BlockPos, value: Direction): Direction =
    world.getBlockEntity(pos) match {
      case rotatable: Rotatable => rotatable.toLocal(value)
      case _ => value
    }

  // ----------------------------------------------------------------------- //
  // Block
  // ----------------------------------------------------------------------- //

  override def canHarvestBlock(state: BlockState, world: BlockGetter, pos: BlockPos, player: PlayerEntity) = true

  override def canBeReplaced(state: BlockState, ctx: BlockPlaceContext): Boolean = false

  /**
   * Vanilla/Create transform block states directly. This is separate from the
   * legacy wrench path below, which rotates the block entity in-place.
   */
  @SuppressWarnings(Array("deprecation"))
  override protected def rotate(state: BlockState, rotation: Rotation): BlockState = {
    var result = state
    if (state.hasProperty(PropertyRotatable.Facing)) {
      result = result.setValue(PropertyRotatable.Facing, rotation.rotate(state.getValue(PropertyRotatable.Facing)))
    }
    if (state.hasProperty(PropertyRotatable.Yaw)) {
      result = result.setValue(PropertyRotatable.Yaw, rotation.rotate(state.getValue(PropertyRotatable.Yaw)))
    }
    result
  }

  @SuppressWarnings(Array("deprecation"))
  override protected def mirror(state: BlockState, mirror: Mirror): BlockState = {
    var result = state
    if (state.hasProperty(PropertyRotatable.Facing)) {
      result = result.setValue(PropertyRotatable.Facing, mirror.mirror(state.getValue(PropertyRotatable.Facing)))
    }
    if (state.hasProperty(PropertyRotatable.Yaw)) {
      result = result.setValue(PropertyRotatable.Yaw, mirror.mirror(state.getValue(PropertyRotatable.Yaw)))
    }
    result
  }
  
  def getValidRotations(world: World, pos: BlockPos): Array[Direction] = validRotations_

  override def getDrops(state: BlockState, ctx: LootParams.Builder): util.List[ItemStack] = {
    val newCtx = ctx.getOptionalParameter(LootContextParams.BLOCK_ENTITY) match {
      case _: Inventory => ctx.withDynamicDrop(LootFunctions.DYN_VOLATILE_CONTENTS, f => {
        ctx.getOptionalParameter(LootContextParams.BLOCK_ENTITY) match {
          case inventory: Inventory => inventory.forAllLoot(f)
          case _ =>
        }
      })
      case _ => ctx
    }
    super.getDrops(state, newCtx)
  }

  override def playerWillDestroy(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity): BlockState = {
    if (!world.isClientSide && player.isCreative) world.getBlockEntity(pos) match {
      case inventory: Inventory => inventory.dropAllSlots()
      case _ => // Ignore.
    }
    super.playerWillDestroy(world, pos, state, player)
  }

  // ----------------------------------------------------------------------- //

  @Deprecated
  def rotateBlock(world: World, pos: BlockPos, axis: Direction): Boolean =
    world.getBlockEntity(pos) match {
      case rotatable: blockentity.traits.Rotatable if rotatable.rotate(axis) =>
        world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
        true
      case _ => false
    }

  def applyColor(colored: Colored, world: World, pos: BlockPos, player: PlayerEntity, dyeItem: ItemStack) = {
    colored.setColor(Color.rgbValues(Color.dyeColor(dyeItem)))
    world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
    if (!player.isCreative && colored.consumesDye) {
      dyeItem.split(1)
    }
  }

  // ----------------------------------------------------------------------- //
  override def useItemOn(stack: ItemStack, state: BlockState, level: World, pos: BlockPos, player: PlayerEntity, hand: InteractionHand, hitResult: BlockHitResult): ItemInteractionResult = {
    level.getBlockEntity(pos) match {
      case colored: Colored if Color.isDye(stack) =>
        applyColor(colored, level, pos, player, stack)
        ItemInteractionResult.sidedSuccess(level.isClientSide)
      case _ => {
        val result = localOnBlockActivated(level, pos, player, hand, stack, hitResult.getDirection,
          (hitResult.getLocation.x - pos.getX).toFloat, (hitResult.getLocation.y - pos.getY).toFloat, (hitResult.getLocation.z - pos.getZ).toFloat)
        if (result) ItemInteractionResult.sidedSuccess(level.isClientSide) else ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
      }
    }
  }

  override def useWithoutItem(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hitResult: BlockHitResult): InteractionResult = {
    val heldItem = player.getItemInHand(InteractionHand.MAIN_HAND)
    world.getBlockEntity(pos) match {
      case colored: Colored if Color.isDye(heldItem) =>
        applyColor(colored, world, pos, player, heldItem)
        InteractionResult.sidedSuccess(world.isClientSide)
      case _ => {
        val loc = hitResult.getLocation
        val bPos = hitResult.getBlockPos
        val x = loc.x.toFloat - bPos.getX
        val y = loc.y.toFloat - bPos.getY
        val z = loc.z.toFloat - bPos.getZ
        if (localOnBlockActivated(world, bPos, player, InteractionHand.MAIN_HAND, heldItem, hitResult.getDirection, x, y, z))
          InteractionResult.sidedSuccess(world.isClientSide) else InteractionResult.PASS
      }
    }
  }

  def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: InteractionHand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = false
}
