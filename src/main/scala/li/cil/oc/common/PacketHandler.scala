package li.cil.oc.common

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.OCBlocks
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedLevel._
import net.minecraft.core.{BlockPos, Direction, Registry}
import net.minecraft.nbt.{CompoundTag, NbtIo}
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

import java.io.{ByteArrayInputStream, DataInputStream, InputStream}
import java.util.zip.InflaterInputStream
import scala.reflect.{ClassTag, classTag}

object PacketHandler {
  var clientHandler: PacketHandler = _

  var serverHandler: PacketHandler = _

  private[oc] def handlePacket(isClientSide: Boolean, arr: Array[Byte], player: Player): Unit = {
    var stream: InputStream = null
    try {
      val handler = if (isClientSide) clientHandler else serverHandler

      if (handler != null) {
        stream = new ByteArrayInputStream(arr)
        if (stream.read() != 0) stream = new InflaterInputStream(stream)
        handler.dispatch(handler.createParser(stream, player))
      }
    } catch {
      case e: Throwable =>
        OpenComputers.log.warn("Received a badly formatted packet.", e)
    } finally {
      if (stream != null) {
        stream.close()
      }
    }

    player match {
      case mp: ServerPlayer => mp.resetLastActionTime()
      case _ =>
    }
  }
}

abstract class PacketHandler {
  /**
    * Gets the world for the specified dimension.
    *
    * For clients this returns the client's world if it is the specified
    * dimension; None otherwise. For the server it returns the world for the
    * specified dimension, if such a dimension exists; None otherwise.
    */
  protected def world(player: Player, dimension: ResourceLocation): Option[Level]

  protected def dispatch(p: PacketParser): Unit

  protected def createParser(stream: InputStream, player: Player): PacketParser

  private[oc] class PacketParser(stream: InputStream, val player: Player) extends DataInputStream(stream) {
    val packetType = PacketType(readByte())

    def readRegistryEntry[T](registry: Registry[T]): T = {
      val id = readUTF()
      val location = ResourceLocation.tryParse(id)
      if (location != null) {
        registry.get(location)
      } else {
        registry.get(ResourceLocation.parse("minecraft:air"))
      }
    }

    def getBlockEntity[T: ClassTag](dimension: ResourceLocation, x: Int, y: Int, z: Int): Option[T] = {
      world(player, dimension) match {
        case Some(world) if world.blockExists(BlockPosition(x, y, z)) =>
          val t = world.getBlockEntity(BlockPosition(x, y, z))
          if (t != null && classTag[T].runtimeClass.isAssignableFrom(t.getClass)) {
            return Some(t.asInstanceOf[T])
          }
          // In case a robot moved away before the packet arrived. This is
          // mostly used when the robot *starts* moving while the client sends
          // a request to the server.
          OCBlocks.RobotAfterimage.get().findMovingRobot(world, new BlockPos(x, y, z)) match {
            case Some(robot) if classTag[T].runtimeClass.isAssignableFrom(robot.proxy.getClass) =>
              return Some(robot.proxy.asInstanceOf[T])
            case _ =>
          }
        case _ => // Invalid dimension.
      }
      None
    }

    def getEntity[T: ClassTag](dimension: ResourceLocation, id: Int): Option[T] = {
      world(player, dimension) match {
        case Some(world) =>
          val e = world.getEntity(id)
          if (e != null && classTag[T].runtimeClass.isAssignableFrom(e.getClass)) {
            return Some(e.asInstanceOf[T])
          }
        case _ =>
      }
      None
    }

    def readBlockEntity[T: ClassTag](): Option[T] = {
      val dimension = ResourceLocation.tryParse(readUTF())
      val x = readInt()
      val y = readInt()
      val z = readInt()
      getBlockEntity(dimension, x, y, z)
    }

    def readEntity[T: ClassTag](): Option[T] = {
      val dimension = ResourceLocation.tryParse(readUTF())
      val id = readInt()
      getEntity[T](dimension, id)
    }

    def readDirection(): Option[Direction] = readByte() match {
      case id if id < 0 => None
      case id => Option(Direction.from3DDataValue(id))
    }

    def readItemStack(): ItemStack = {
      val haveStack = readBoolean()
      if (haveStack) {
        ItemStack.parseOptional(player.level.registryAccess(), readNBT())
      }
      else ItemStack.EMPTY
    }

    def readNBT(): CompoundTag = {
      val haveNbt = readBoolean()
      if (haveNbt) {
        NbtIo.read(this)
      }
      else null
    }

    def readMedium(): Int = {
      val c0 = readUnsignedByte()
      val c1 = readUnsignedByte()
      val c2 = readUnsignedByte()
      (c0) | (c1 << 8) | (c2 << 16)
    }

    def readBlockPosCoords(): BlockPosition = {
      val x = readInt()
      val y = readInt()
      val z = readInt()
      new BlockPosition(x, y, z)
    }

    def readByteArray(): Array[Byte] = {
      val len = readInt()
      val arr = new Array[Byte](len)
      readFully(arr)
      arr
    }

    def readPacketType() = PacketType(readByte())
  }
}
