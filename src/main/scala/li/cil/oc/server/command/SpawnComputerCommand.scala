package li.cil.oc.server.command

import com.mojang.brigadier.CommandDispatcher
import li.cil.oc.{Constants, api}
import li.cil.oc.common.blockentity.{Case => CaseBlockEntity}
import li.cil.oc.common.blockentity.traits.Rotatable
import li.cil.oc.server.machine.luac.LuaStateFactory
import net.minecraft.commands.{CommandSourceStack, Commands}
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.{BlockHitResult, HitResult}

object SpawnComputerCommand {
  final val MaxDistance = 16.0

  def register(dispatcher: CommandDispatcher[CommandSourceStack]): Unit = {
    def command(name: String) = Commands.literal(name)
      .requires(CommandHandler.canUse(_, 2))
      .executes(context => execute(context.getSource))

    dispatcher.register(command("oc_spawnComputer"))
    dispatcher.register(command("oc_spawncomputer"))
    dispatcher.register(command("oc_sc"))
  }

  private def execute(source: CommandSourceStack): Int = {
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

        def place(pos: net.minecraft.core.BlockPos, name: String): Unit =
          level.setBlockAndUpdate(pos, api.Items.get(name).block().defaultBlockState())

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

        place(casePos, Constants.BlockName.CaseCreative)
        rotateProperly(casePos)

        place(screenPos, Constants.BlockName.ScreenTier4)
        rotateProperly(screenPos).foreach { rotatable =>
          if (rotatable.pitch == Direction.UP || rotatable.pitch == Direction.DOWN) {
            rotatable.pitch = Direction.NORTH
          }
        }

        place(keyboardPos, Constants.BlockName.Keyboard)
        level.getBlockEntity(keyboardPos) match {
          case rotatable: Rotatable =>
            rotatable.setFromEntityPitchAndYaw(player)
            rotatable.setFromFacing(Direction.UP)
          case _ =>
        }

        api.Network.joinOrCreateNetwork(level.getBlockEntity(casePos))

        val apu = api.Items.get(Constants.ItemName.APUCreative).createItemStack(1)
        LuaStateFactory.setDefaultArch(apu)
        level.getBlockEntity(casePos) match {
          case computer: CaseBlockEntity =>
            val components = Seq(
              apu,
              api.Items.get(Constants.ItemName.RAMCreative).createItemStack(1),
              api.Items.get(Constants.ItemName.SSDTier3).createItemStack(1),
              api.Items.get(Constants.ItemName.LuaBios).createItemStack(1),
              api.Items.get(Constants.ItemName.OpenOS).createItemStack(1)
            )

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
