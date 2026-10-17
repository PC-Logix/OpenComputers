package li.cil.oc.integration.minecraft

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedBlockEntity
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.{Block, Blocks}
import net.minecraft.world.level.block.entity.{AbstractFurnaceBlockEntity, FurnaceBlockEntity}

object DriverFurnace extends DriverSidedBlockEntity {
  override def getBlockEntityClass: Class[_] = classOf[FurnaceBlockEntity]

  override def createEnvironment(world: Level, pos: BlockPos, side: Direction): ManagedEnvironment =
    new EnvironmentFurnace(world.getBlockEntity(pos).asInstanceOf[AbstractFurnaceBlockEntity], "furnace")

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (!stack.isEmpty && Block.byItem(stack.getItem) == Blocks.FURNACE)
        classOf[EnvironmentFurnace]
      else null
    }
  }

}
