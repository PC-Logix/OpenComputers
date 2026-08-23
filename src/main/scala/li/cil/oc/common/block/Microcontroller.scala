package li.cil.oc.common.block

import li.cil.oc.{api, Constants, Settings}
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.{blockentity, Tier}
import li.cil.oc.common.blockentity.BlockEntityTypes
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.integration.util.Wrench
import li.cil.oc.server.loot.LootFunctions
import li.cil.oc.util.StackOption._
import li.cil.oc.util.{InventoryUtils, Tooltip}
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.network.chat.{Component => ITextComponent}
import net.minecraft.world.{InteractionHand => Hand}
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.{Player => PlayerEntity}
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.{ItemStack, TooltipFlag}
import net.minecraft.world.level.{LevelReader, Level => World}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.{BlockState, StateDefinition => StateContainer}
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams

import java.util

class Microcontroller(props: Properties)
  extends RedstoneAware(props) with traits.PowerAcceptor with traits.StateAware with traits.Tickable {

  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Facing)

  // ----------------------------------------------------------------------- //

  override def getCloneItemStack(world: LevelReader, pos: BlockPos, state: BlockState): ItemStack =
    world.getBlockEntity(pos) match {
      case mcu: blockentity.Microcontroller => mcu.info.copyItemStack()
      case _ => ItemStack.EMPTY
    }

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(stack: ItemStack, context: TooltipContext, tooltip: util.List[ITextComponent], flag: TooltipFlag): Unit = {
    super.tooltipTail(stack, context, tooltip, flag)
    if (Tooltip.showExtendedTooltip(flag)) {
      val info = new MicrocontrollerData(stack)
      for (component <- info.components if !component.isEmpty) {
        tooltip.add(ITextComponent.literal("- " + component.getHoverName.getString).setStyle(Tooltip.DefaultStyle))
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput: Double = Settings.get.caseRate(Tier.One)

  override def newBlockEntity(pos: BlockPos, state: BlockState) = new blockentity.Microcontroller(pos, state)

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (!Wrench.holdsApplicableWrench(player, pos)) {
      if (!player.isCrouching) {
        if (!world.isClientSide) {
          world.getBlockEntity(pos) match {
            case mcu: blockentity.Microcontroller =>
              if (mcu.machine.isRunning) mcu.machine.stop()
              else mcu.machine.start()
            case _ =>
          }
        }
        true
      }
      else if (api.Items.get(heldItem) == api.Items.get(Constants.ItemName.EEPROM)) {
        if (!world.isClientSide) {
          world.getBlockEntity(pos) match {
            case mcu: blockentity.Microcontroller =>
              val newEeprom = player.getInventory.removeItem(player.getInventory.selected, 1)
              mcu.changeEEPROM(newEeprom) match {
                case SomeStack(oldEeprom) => InventoryUtils.addToPlayerInventory(oldEeprom, player)
                case _ =>
              }
          }
        }
        true
      }
      else false
    }
    else false
  }

  override def setPlacedBy(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity, stack: ItemStack): Unit = {
    super.setPlacedBy(world, pos, state, placer, stack)
    world.getBlockEntity(pos) match {
      case tileEntity: blockentity.Microcontroller if !world.isClientSide => {
        tileEntity.info.loadData(stack)
        tileEntity.snooperNode.changeBuffer(tileEntity.info.storedEnergy - tileEntity.snooperNode.localBuffer)
      }
      case _ =>
    }
  }

  override def getDrops(state: BlockState, ctx: LootParams.Builder): util.List[ItemStack] = {
    val newCtx = ctx.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, f => {
      ctx.getOptionalParameter(LootContextParams.BLOCK_ENTITY) match {
        case tileEntity: blockentity.Microcontroller =>
          tileEntity.saveComponents()
          tileEntity.info.storedEnergy = tileEntity.snooperNode.localBuffer.toInt
          f.accept(tileEntity.info.createItemStack())
        case _ =>
      }
    })
    super.getDrops(state, newCtx)
  }

  override def playerWillDestroy(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity): BlockState = {
    if (!world.isClientSide && player.isCreative) {
      world.getBlockEntity(pos) match {
        case tileEntity: blockentity.Microcontroller =>
          Block.dropResources(state, world, pos, tileEntity, player, player.getMainHandItem)
        case _ =>
      }
    }
    super.playerWillDestroy(world, pos, state, player)
  }

  override def getBlockEntityType: BlockEntityType[_ <: BlockEntity] = BlockEntityTypes.MICROCONTROLLER.get()
}
