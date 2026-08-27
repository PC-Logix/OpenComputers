package li.cil.oc.server.command

import com.mojang.brigadier.CommandDispatcher
import li.cil.oc.{Constants, api}
import li.cil.oc.common.Loot
import li.cil.oc.common.blockentity.{Case => CaseBlockEntity}
import li.cil.oc.common.blockentity.traits.Rotatable
import li.cil.oc.common.init.{OCBlocks, OCItems}
import li.cil.oc.server.machine.luac.LuaStateFactory
import net.minecraft.commands.{Commands, CommandSourceStack}
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.{BlockHitResult, HitResult}
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext

object SpawnComputerCommand {
  final val MaxDistance = 16.0

  def register(dispatcher: CommandDispatcher[CommandSourceStack]): Unit = {
    def command(name: String) = Commands.literal(name)
      .requires(CommandHandler.canUse(_, 2))
      .then(
        Commands.argument("TierScreen (1-4)", IntegerArgumentType.integer(1, 4))
          .executes(context => execute(context))
      )

    dispatcher.register(command("oc_spawnComputer"))
    dispatcher.register(command("oc_spawncomputer"))
    dispatcher.register(command("oc_sc"))
  }

  private def execute(context: CommandContext[CommandSourceStack]): Int = {
    val TierScreen = IntegerArgumentType.getInteger(context, "TierScreen (1-4)")

    val source = context.getSource
    val player = source.getPlayerOrException
    val level = player.serverLevel()

    player.pick(MaxDistance, 0.0f, false) match {
      case hit: BlockHitResult if hit.getType == HitResult.Type.BLOCK =>
        val casePos = (hit.getBlockPos.relative(hit.getDirection): net.minecraft.core.BlockPos)
        val screenPos = (casePos.above(): net.minecraft.core.BlockPos)
        val keyboardPos = (screenPos.above(): net.minecraft.core.BlockPos)

        if (!level.isEmptyBlock(casePos) || !level.isEmptyBlock(screenPos) || !level.isEmptyBlock(keyboardPos)) {
          source.sendFailure(Component.literal("Target position obstructed."))
          return 0
        }

        val apu = Option(api.Items.get(Constants.ItemName.APUCreative)).map(_.createItemStack(1))
        val components = Seq(
          apu,
          Option(api.Items.get(Constants.ItemName.RAMCreative)).map(_.createItemStack(1)),
          Option(api.Items.get(Constants.ItemName.RAMCreative)).map(_.createItemStack(1)),
          Option(api.Items.get(Constants.ItemName.SSDTier3)).map(_.createItemStack(1)),
          Option(Loot.defaultEEPROM).filter(stack => !stack.isEmpty),
          Option(Loot.defaultOpenOS).filter(stack => !stack.isEmpty)
        ).flatten

        if (components.size != 6 || components.exists(_.isEmpty)) {
          source.sendFailure(Component.literal("OpenComputers default EEPROM/OpenOS data is not loaded; reload the server resources and try again."))
          return 0
        }

        val apuStack = components.head

        def rotateProperly(pos: net.minecraft.core.BlockPos): Option[Rotatable] =
          level.getBlockEntity(pos) match {
            case rotatable: Rotatable =>
              rotatable.setFromEntityPitchAndYaw(player)
              if (!rotatable.validFacings.contains(rotatable.pitch)) {
                rotatable.pitch = rotatable.validFacings.headOption.getOrElse(Direction.NORTH)
              }
              rotatable.invertRotation()
              Some(rotatable)
            case _ => None
          }

        level.setBlockAndUpdate(casePos, OCBlocks.CaseCreative.get().defaultBlockState())
        rotateProperly(casePos)

        if (TierScreen == 1) {
          level.setBlockAndUpdate(screenPos, OCBlocks.ScreenTier1.get().defaultBlockState())
        } else if (TierScreen == 2) {
          level.setBlockAndUpdate(screenPos, OCBlocks.ScreenTier2.get().defaultBlockState())
        } else if (TierScreen == 3) {
          level.setBlockAndUpdate(screenPos, OCBlocks.ScreenTier3.get().defaultBlockState())
        } else if (TierScreen == 4) {
          level.setBlockAndUpdate(screenPos, OCBlocks.ScreenTier4.get().defaultBlockState())
        }

        rotateProperly(screenPos).foreach { rotatable =>
          if (rotatable.pitch == Direction.UP || rotatable.pitch == Direction.DOWN) {
            rotatable.pitch = Direction.NORTH
          }
        }

        level.setBlockAndUpdate(keyboardPos, OCBlocks.Keyboard.get().defaultBlockState())
        level.getBlockEntity(keyboardPos) match {
          case rotatable: Rotatable =>
            rotatable.setFromEntityPitchAndYaw(player)
            rotatable.setFromFacing(Direction.UP)
          case _ =>
        }

        api.Network.joinOrCreateNetwork(level.getBlockEntity(casePos))

        LuaStateFactory.setDefaultArch(apuStack)
        level.getBlockEntity(casePos) match {
          case computer: CaseBlockEntity =>
            for (component <- components) {
              val slot = (0 until computer.getContainerSize)
                .find(i => computer.getItem(i).isEmpty && computer.canPlaceItem(i, component))
                .getOrElse(throw new IllegalStateException(s"No compatible case slot for ${component.getHoverName.getString}"))
              computer.setItem(slot, component)
            }
            computer.setChanged()
          case _ =>
            throw new IllegalStateException("Creative case block entity was not created.")
        }

        source.sendSuccess(() => Component.literal("Spawned a configured OpenComputers computer."), true)
        1
      case _ =>
        source.sendFailure(Component.literal("You need to be looking at a nearby block."))
        0
    }
  }
}
