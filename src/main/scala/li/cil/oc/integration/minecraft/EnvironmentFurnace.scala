package li.cil.oc.integration.minecraft

import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.integration.ManagedBlockEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity

final class EnvironmentFurnace(tileEntity: AbstractFurnaceBlockEntity, name: String) extends ManagedBlockEntityEnvironment[AbstractFurnaceBlockEntity](tileEntity, name) with NamedBlock {
  override def preferredName: String = name

  override def priority = 0

  @Callback(doc = "function():number -- The number of ticks that the furnace will keep burning from the last consumed fuel.")
  def getBurnTime(context: Context, args: Arguments): Array[AnyRef] = {
    result(tileEntity.litTime)
  }

  @Callback(doc = "function():number -- The number of ticks that the currently burning fuel lasts in total.")
  def getCurrentItemBurnTime(context: Context, args: Arguments): Array[AnyRef] = {
    result(tileEntity.litDuration)
  }

  @Callback(doc = "function():number -- The number of ticks that the current item has been cooking for.")
  def getCookTime(context: Context, args: Arguments): Array[AnyRef] = {
    result(tileEntity.cookingProgress)
  }

  @Callback(doc = "function():number -- The number of ticks that the current item needs to cook.")
  def getTotalCookTime(context: Context, args: Arguments): Array[AnyRef] = {
    result(tileEntity.cookingTotalTime)
  }

  @Callback(doc = "function():boolean -- Get whether the furnace is currently active.")
  def isBurning(context: Context, args: Arguments): Array[AnyRef] = {
    result(tileEntity.litTime > 0)
  }
}
