package li.cil.oc.integration.multipart

import java.util.Collections

import codechicken.multipart.api.{MultipartType, PartConverter, SimpleMultipartType}
import codechicken.multipart.api.part.MultiPart
import codechicken.multipart.block.TileMultipart
import codechicken.multipart.util.MultipartPlaceContext
import li.cil.oc.{OpenComputers, Settings}
import li.cil.oc.common.init.OCBlocks
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.util.SableCompat
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.bus.api.IEventBus

import scala.jdk.CollectionConverters._

object MultipartIntegration {
  private val multipartTypes: DeferredRegister[MultipartType[_]] =
    DeferredRegister.create(MultipartType.MULTIPART_TYPES, OpenComputers.ID)
  private val partConverters: DeferredRegister[PartConverter] =
    DeferredRegister.create(PartConverter.PART_CONVERTERS, OpenComputers.ID)

  private val cableType = multipartTypes.register[MultipartType[MultipartCablePart]](
    "cable",
    new java.util.function.Supplier[MultipartType[MultipartCablePart]] {
      override def get(): MultipartType[MultipartCablePart] = new SimpleMultipartType[MultipartCablePart](_ => new MultipartCablePart())
    }
  )

  private val audioCableType = multipartTypes.register[MultipartType[MultipartAudioCablePart]](
    "audio_cable",
    new java.util.function.Supplier[MultipartType[MultipartAudioCablePart]] {
      override def get(): MultipartType[MultipartAudioCablePart] = new SimpleMultipartType[MultipartAudioCablePart](_ => new MultipartAudioCablePart())
    }
  )

  private val printType = multipartTypes.register[MultipartType[MultipartPrintPart]](
    "print",
    new java.util.function.Supplier[MultipartType[MultipartPrintPart]] {
      override def get(): MultipartType[MultipartPrintPart] = new SimpleMultipartType[MultipartPrintPart](_ => new MultipartPrintPart())
    }
  )

  private val cableConverter = partConverters.register(
    "cable",
    () => new PartConverter {
      override def convert(level: LevelAccessor, pos: BlockPos, state: BlockState) = {
        if (state.getBlock != OCBlocks.Cable.get) PartConverter.emptyResultList()
        else {
          level.getBlockEntity(pos) match {
            case cable: li.cil.oc.common.blockentity.Cable =>
              PartConverter.ConversionResult.success(Collections.singleton[MultiPart](new MultipartCablePart(state, Some(cable))))
            case _ => PartConverter.ConversionResult.success(Collections.singleton[MultiPart](new MultipartCablePart(state)))
          }
        }
      }

      override def convert(context: MultipartPlaceContext) = {
        val stack = context.getItemInHand
        if (stack.getItem != OCBlocks.Cable.get().asItem()) PartConverter.emptyResult()
        else {
          val state = OCBlocks.Cable.get().getStateForPlacement(context)
          if (state == null) PartConverter.emptyResult()
          else {
            val part = new MultipartCablePart(state)
            if (li.cil.oc.util.ItemColorizer.hasColor(stack)) part.copyColor(li.cil.oc.util.ItemColorizer.getColor(stack))
            PartConverter.ConversionResult.success(part)
          }
        }
      }
    }
  )

  private val audioCableConverter = partConverters.register(
    "audio_cable",
    () => new PartConverter {
      override def convert(level: LevelAccessor, pos: BlockPos, state: BlockState) = {
        if (state.getBlock != OCBlocks.AudioCable.get) PartConverter.emptyResultList()
        else PartConverter.ConversionResult.success(Collections.singleton[MultiPart](new MultipartAudioCablePart(state)))
      }

      override def convert(context: MultipartPlaceContext) = {
        val stack = context.getItemInHand
        if (stack.getItem != OCBlocks.AudioCable.get().asItem()) PartConverter.emptyResult()
        else {
          val state = OCBlocks.AudioCable.get().getStateForPlacement(context)
          if (state == null) PartConverter.emptyResult()
          else PartConverter.ConversionResult.success(new MultipartAudioCablePart(state))
        }
      }
    }
  )

  private val printConverter = partConverters.register(
    "print",
    () => new PartConverter {
      override def convert(level: LevelAccessor, pos: BlockPos, state: BlockState) = {
        if (state.getBlock != OCBlocks.Print.get) PartConverter.emptyResultList()
        else level.getBlockEntity(pos) match {
          case print: li.cil.oc.common.blockentity.Print =>
            PartConverter.ConversionResult.success(Collections.singleton[MultiPart](new MultipartPrintPart(state, Some(print))))
          case _ => PartConverter.emptyResultList()
        }
      }

      override def convert(context: MultipartPlaceContext) = {
        val stack = context.getItemInHand
        if (stack.getItem != OCBlocks.Print.get().asItem()) PartConverter.emptyResult()
        else {
          val state = OCBlocks.Print.get().getStateForPlacement(context)
          val data = new PrintData(stack)
          if (state == null || !withinPrintLimit(context.getLevel, context.getClickedPos, data)) PartConverter.emptyResult()
          else {
            val part = new MultipartPrintPart(state)
            part.data = data
            val player = context.getPlayer
            if (player != null) {
              val clickPos = Vec3.atCenterOf(context.getClickedPos)
              val forward = Vec3.directionFromRotation(player.getXRot, player.getYRot).reverse()
              val side = Vec3.directionFromRotation(0, player.getYRot + 90).reverse()
              val yaw = SableCompat.localHeading(context.getLevel, clickPos, forward, side)
              val pitch = SableCompat.localPitch(context.getLevel, clickPos, forward)
              val orientation = new li.cil.oc.common.blockentity.Print(context.getClickedPos, state)
              orientation.setFromPitchAndYaw(pitch.toFloat, yaw.toFloat)
              orientation.invertRotation()
              part.facing = orientation.facing
            }
            PartConverter.ConversionResult.success(part)
          }
        }
      }
    }
  )

  private def withinPrintLimit(level: LevelAccessor, pos: BlockPos, incoming: PrintData): Boolean = {
    val others: Iterable[PrintData] = level.getBlockEntity(pos) match {
      case tile: TileMultipart => tile.getPartList.asScala.collect { case print: MultipartPrintPart => print.data }
      case print: li.cil.oc.common.blockentity.Print => Iterable(print.data)
      case _ => Iterable.empty
    }
    val offCount = incoming.stateOff.size + others.map(_.stateOff.size).sum
    val onCount = incoming.stateOn.size + others.map(_.stateOn.size).sum
    offCount <= Settings.get.maxPrintComplexity && onCount <= Settings.get.maxPrintComplexity
  }

  def register(bus: IEventBus): Unit = {
    multipartTypes.register(bus)
    partConverters.register(bus)
  }

  def cableMultipartType: MultipartType[MultipartCablePart] = cableType.get()
  def audioCableMultipartType: MultipartType[MultipartAudioCablePart] = audioCableType.get()
  def printMultipartType: MultipartType[MultipartPrintPart] = printType.get()
}
