package li.cil.oc.integration.multipart

import li.cil.oc.OpenComputers
import li.cil.oc.integration.{ModProxy, Mods}
import net.neoforged.api.distmarker.Dist
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.level.BlockGetter
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.bus.api.IEventBus

/** Kept free of CB Multipart API types so OpenComputers can load without CB Multipart. */
object ModCBMultipart extends ModProxy {
  override def getMod = Mods.CBMultipart

  override def preInitialize(): Unit = {
    val bus = OpenComputers.proxy.modBus
    MultipartIntegration.register(bus)
    if (FMLEnvironment.dist == Dist.CLIENT) registerClient(bus)
  }

  def cableColor(world: BlockGetter, pos: BlockPos): Option[Int] = MultipartColorLookup.cableColor(world, pos)
  def isCable(world: BlockGetter, pos: BlockPos): Boolean =
    Mods.CBMultipart.isModAvailable && MultipartColorLookup.cablePart(world, pos).nonEmpty
  def canConnectFromSide(world: BlockGetter, pos: BlockPos, side: Direction): Boolean =
    !Mods.CBMultipart.isModAvailable || MultipartColorLookup.canConnectFromSide(world, pos, side)
  def canAudioConnectFromSide(world: BlockGetter, pos: BlockPos, side: Direction): Boolean =
    !Mods.CBMultipart.isModAvailable || MultipartColorLookup.canAudioConnectFromSide(world, pos, side)
  def isAudioCable(world: BlockGetter, pos: BlockPos): Boolean =
    Mods.CBMultipart.isModAvailable && MultipartColorLookup.isAudioCable(world, pos)

  private def registerClient(bus: IEventBus): Unit = {
    val clientIntegration = Class.forName("li.cil.oc.integration.multipart.MultipartClientIntegration$")
    val instance = clientIntegration.getField("MODULE$").get(null)
    clientIntegration.getMethod("register", classOf[IEventBus]).invoke(instance, bus)
  }
}
