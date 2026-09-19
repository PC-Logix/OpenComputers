package li.cil.oc.common.blockentity

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.EventHandler
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.server.network.Waypoints
import li.cil.oc.util.ExtendedDataComponentHolder._
import net.minecraft.core.component.DataComponentHolder
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.{BlockPos, Direction, HolderLookup}
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.MutableDataComponentHolder
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension

class Waypoint(pos: BlockPos, state: BlockState)
  extends BlockEntity(BlockEntityTypes.WAYPOINT.get(), pos, state) with traits.Environment with traits.Rotatable with traits.RedstoneAware with traits.Tickable with IBlockEntityExtension {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("waypoint").
    create()

  var label = ""

  override def validFacings: Array[Direction]  = Direction.values

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(): string -- Get the current label of this waypoint.""")
  def getLabel(context: Context, args: Arguments): Array[Object] = result(label)

  @Callback(doc = """function(value:string) -- Set the label for this waypoint.""")
  def setLabel(context: Context, args: Arguments): Array[Object] = {
    label = args.checkString(0).take(32)
    setChanged()
    context.pause(0.5)
    null
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity(): Unit = {
    super.updateEntity()
    if (isClient) {
      val origin = position.toVec3.add(facing.getStepX * 0.5, facing.getStepY * 0.5, facing.getStepZ * 0.5)
      val dx = (getLevel.random.nextFloat() - 0.5f) * 0.8f
      val dy = (getLevel.random.nextFloat() - 0.5f) * 0.8f
      val dz = (getLevel.random.nextFloat() - 0.5f) * 0.8f
      val vx = (getLevel.random.nextFloat() - 0.5f) * 0.2f + facing.getStepX * 0.3f
      val vy = (getLevel.random.nextFloat() - 0.5f) * 0.2f + facing.getStepY * 0.3f - 0.5f
      val vz = (getLevel.random.nextFloat() - 0.5f) * 0.2f + facing.getStepZ * 0.3f
      getLevel.addParticle(ParticleTypes.PORTAL, origin.x + dx, origin.y + dy, origin.z + dz, vx, vy, vz)
    }
  }

  override protected def initialize(): Unit = {
    super.initialize()
    EventHandler.scheduleServer(() => Waypoints.add(this))
  }

  override def dispose(): Unit = {
    super.dispose()
    Waypoints.remove(this)
  }

  // ----------------------------------------------------------------------- //

  override def loadComponentsCommon(holder: DataComponentHolder): Unit = {
    super.loadComponentsCommon(holder)
    for(label <- holder.getComponent(OCComponents.LABEL))
      this.label = label
  }

  override def saveComponentsCommon(holder: MutableDataComponentHolder): Unit = {
    super.saveComponentsCommon(holder)
    holder.setComponent(OCComponents.LABEL, label)
  }
}
