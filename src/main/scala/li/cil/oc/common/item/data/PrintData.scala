package li.cil.oc.common.item.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import li.cil.oc.{api, Constants, Settings}
import li.cil.oc.common.IMC
import li.cil.oc.common.datacomponents.{OCComponents, ScalaCodec, ScalaStreamCodec}
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedDataComponentHolder._
import net.minecraft.core.component.DataComponentHolder
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.codec.{ByteBufCodecs, StreamCodec}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.neoforged.neoforge.common.MutableDataComponentHolder

import java.lang.reflect.Method
import scala.collection.mutable

case class PrintData(var label: Option[String] = None,
                     var tooltip: Option[String] = None,
                     var isButtonMode: Boolean = false,
                     var redstoneLevel: Int = 0,
                     var pressurePlate: Boolean = false,
                     val stateOff: mutable.Set[PrintData.Shape] = mutable.Set.empty[PrintData.Shape],
                     val stateOn: mutable.Set[PrintData.Shape] = mutable.Set.empty[PrintData.Shape],
                     @Deprecated
                     var isBeaconBase: Boolean = false,
                     var lightLevel: Int = 0,
                     var noclipOff: Boolean = false,
                     var noclipOn: Boolean = false) extends ItemData(Constants.BlockName.Print) {
  def this(holder: DataComponentHolder) = {
    this()
    loadData(holder)
  }

  def complexity: Int = stateOn.size max stateOff.size

  def hasActiveState: Boolean = stateOn.nonEmpty

  def emitLight: Boolean = lightLevel > 0

  def emitRedstone: Boolean = redstoneLevel > 0

  def emitRedstone(state: Boolean): Boolean = if (state) emitRedstoneWhenOn else emitRedstoneWhenOff

  def emitRedstoneWhenOff: Boolean = emitRedstone && !hasActiveState

  def emitRedstoneWhenOn: Boolean = emitRedstone && hasActiveState

  def opacity: Float = {
    if (opacityDirty) {
      opacityDirty = false
      opacity_ = PrintData.computeApproximateOpacity(stateOn) min PrintData.computeApproximateOpacity(stateOff)
    }
    opacity_
  }

  // lazily computed and stored, because potentially slow
  private var opacity_ = 0f
  private var opacityDirty = true

  private final val LabelTag = "label"
  private final val TooltipTag = "tooltip"
  private final val IsButtonModeTag = "isButtonMode"
  private final val RedstoneLevelTag = "redstoneLevel"
  private final val RedstoneLevelTagCompat = "emitRedstone"
  private final val PressurePlateTag = "pressurePlate"
  private final val StateOffTag = "stateOff"
  private final val StateOnTag = "stateOn"
  private final val IsBeaconBaseTag = "isBeaconBase"
  private final val LightLevelTag = "lightLevel"
  private final val NoclipOffTag = "noclipOff"
  private final val NoclipOnTag = "noclipOn"

  override def loadData(holder: DataComponentHolder): Unit = {
    for (print <- holder.getComponent(OCComponents.PRINT)) {
      this.stateOff.clear()
      this.stateOn.clear()

      this.label = print.label
      this.tooltip = print.tooltip
      this.isButtonMode = print.isButtonMode
      this.redstoneLevel = print.redstoneLevel
      this.pressurePlate = print.pressurePlate
      this.stateOff ++= print.stateOff
      this.stateOn ++= print.stateOn
      this.isBeaconBase = print.isBeaconBase
      this.lightLevel = print.lightLevel
      this.noclipOff = print.noclipOff
      this.noclipOn = print.noclipOn

      opacityDirty = true
    }
  }

  override def saveData(holder: MutableDataComponentHolder): Unit = {
    // how convenient!
    holder.setComponent(OCComponents.PRINT, this)
  }
}

object PrintData {
  // The following logic is used to approximate the opacity of a print, for
  // which we use the volume as a heuristic. Because computing the actual
  // volume is a) expensive b) not necessarily a good heuristic (e.g. a
  // "dotted grid") we take a shortcut and divide the space into a few
  // sub-sections, for each of which we check if there's anything in it.
  // If so, we consider that area "opaque". To compensate, prints can never
  // be fully light-opaque. This gives a little bit of shading as a nice
  // effect, but avoid it looking derpy when there are only a few sparse
  // shapes in the model.
  private val stepping = 4
  private val step = stepping / 16f
  private val invMaxVolume = 1f / (stepping * stepping * stepping)

  private val inkProviders = mutable.LinkedHashSet.empty[Method]

  val CODEC: Codec[PrintData] = RecordCodecBuilder.create(inst => inst.group(
    ScalaCodec.optionFieldOf("label", Codec.STRING).forGetter(_.label),
    ScalaCodec.optionFieldOf("tooltip", Codec.STRING).forGetter(_.tooltip),
    ScalaCodec.BOOL.fieldOf("button").forGetter(_.isButtonMode),
    ScalaCodec.INT.fieldOf("redstone_level").forGetter(_.redstoneLevel),
    ScalaCodec.BOOL.fieldOf("pressure_plate").forGetter(_.pressurePlate),
    ScalaCodec.mutableSet(Shape.CODEC).fieldOf("state_off").forGetter(_.stateOff),
    ScalaCodec.mutableSet(Shape.CODEC).fieldOf("state_on").forGetter(_.stateOn),
    ScalaCodec.BOOL.fieldOf("beacon_base").forGetter(_.isBeaconBase),
    ScalaCodec.INT.fieldOf("light_level").forGetter(_.lightLevel),
    ScalaCodec.BOOL.fieldOf("noclip_off").forGetter(_.noclipOff),
    ScalaCodec.BOOL.fieldOf("noclip_on").forGetter(_.noclipOn)
  ).apply(inst, PrintData.apply _))

  // this codec is too long for composite :(
  val STREAM_CODEC: StreamCodec[ByteBuf, PrintData] = new StreamCodec[ByteBuf, PrintData] {
    override def decode(buffer: ByteBuf): PrintData = PrintData(
      label = ScalaStreamCodec.option(ByteBufCodecs.STRING_UTF8).decode(buffer),
      tooltip = ScalaStreamCodec.option(ByteBufCodecs.STRING_UTF8).decode(buffer),
      isButtonMode = ScalaStreamCodec.BOOL.decode(buffer),
      redstoneLevel = ScalaStreamCodec.VAR_INT.decode(buffer),
      pressurePlate = ScalaStreamCodec.BOOL.decode(buffer),
      stateOff = ScalaStreamCodec.mutableSet(Shape.STREAM_CODEC).decode(buffer),
      stateOn = ScalaStreamCodec.mutableSet(Shape.STREAM_CODEC).decode(buffer),
      isBeaconBase = ScalaStreamCodec.BOOL.decode(buffer),
      lightLevel = ScalaStreamCodec.INT.decode(buffer),
      noclipOff = ScalaStreamCodec.BOOL.decode(buffer),
      noclipOn = ScalaStreamCodec.BOOL.decode(buffer)
    )

    override def encode(buffer: ByteBuf, value: PrintData): Unit = {
      ScalaStreamCodec.option(ByteBufCodecs.STRING_UTF8).encode(buffer, value.label)
      ScalaStreamCodec.option(ByteBufCodecs.STRING_UTF8).encode(buffer, value.tooltip)
      ScalaStreamCodec.BOOL.encode(buffer, value.isButtonMode)
      ScalaStreamCodec.VAR_INT.encode(buffer, value.redstoneLevel)
      ScalaStreamCodec.BOOL.encode(buffer, value.pressurePlate)
      ScalaStreamCodec.mutableSet(Shape.STREAM_CODEC).encode(buffer, value.stateOff)
      ScalaStreamCodec.mutableSet(Shape.STREAM_CODEC).encode(buffer, value.stateOn)
      ScalaStreamCodec.BOOL.encode(buffer, value.isBeaconBase)
      ScalaStreamCodec.INT.encode(buffer, value.lightLevel)
      ScalaStreamCodec.BOOL.encode(buffer, value.noclipOff)
      ScalaStreamCodec.BOOL.encode(buffer, value.noclipOn)
    }
  }

  def addInkProvider(provider: Method): Unit = inkProviders += provider

  def computeApproximateOpacity(shapes: Iterable[PrintData.Shape]): Float = {
    var volume = 1f
    if (shapes.nonEmpty) for (x <- 0 until 16 / stepping; y <- 0 until 16 / stepping; z <- 0 until 16 / stepping) {
      val bounds = new AABB(
        x * step, y * step, z * step,
        (x + 1) * step, (y + 1) * step, (z + 1) * step)
      if (!shapes.exists(_.bounds.intersects(bounds))) {
        volume -= invMaxVolume
      }
    }
    volume
  }

  def computeCosts(data: PrintData): Option[(Int, Int)] = {
    val totalVolume = data.stateOn.foldLeft(0)((acc, shape) => acc + shape.bounds.volume) + data.stateOff.foldLeft(0)((acc, shape) => acc + shape.bounds.volume)
    val totalSurface = data.stateOn.foldLeft(0)((acc, shape) => acc + shape.bounds.surface) + data.stateOff.foldLeft(0)((acc, shape) => acc + shape.bounds.surface)
    val multiplier = if (data.noclipOff || data.noclipOn) Settings.get.noclipMultiplier else 1

    if (totalVolume > 0) {
      val baseMaterialRequired = (totalVolume / 2) max 1
      val materialRequired =
        if (data.redstoneLevel > 0 && data.redstoneLevel < 15) baseMaterialRequired + Settings.get.printCustomRedstone
        else baseMaterialRequired
      val inkRequired = (totalSurface / 6) max 1

      Option(((materialRequired * multiplier).toInt, inkRequired))
    }
    else None
  }

  private val materialPerItem = Settings.get.printMaterialValue

  def materialValue(stack: ItemStack): Int = {
    if (api.Items.get(stack) == api.Items.get(Constants.ItemName.Chamelium))
      materialPerItem
    else if (api.Items.get(stack) == api.Items.get(Constants.BlockName.Print)) {
      val data = new PrintData(stack)
      computeCosts(data) match {
        case Some((materialRequired, inkRequired)) => (materialRequired * Settings.get.printRecycleRate).toInt
        case _ => 0
      }
    }
    else 0
  }

  def inkValue(stack: ItemStack): Int = {
    for (provider <- inkProviders) {
      val value = IMC.tryInvokeStatic(provider, stack)(0)
      if (value > 0) {
        return value
      }
    }
    0
  }

  def nbtToShape(nbt: CompoundTag): Shape = {
    val aabb =
      if (nbt.contains("minX")) {
        // Compatibility with shapes created with earlier dev-builds.
        val minX = nbt.getByte("minX") / 16f
        val minY = nbt.getByte("minY") / 16f
        val minZ = nbt.getByte("minZ") / 16f
        val maxX = nbt.getByte("maxX") / 16f
        val maxY = nbt.getByte("maxY") / 16f
        val maxZ = nbt.getByte("maxZ") / 16f
        new AABB(minX, minY, minZ, maxX, maxY, maxZ)
      }
      else {
        val bounds = nbt.getByteArray("bounds").padTo(6, 0.toByte)
        val minX = bounds(0) / 16f
        val minY = bounds(1) / 16f
        val minZ = bounds(2) / 16f
        val maxX = bounds(3) / 16f
        val maxY = bounds(4) / 16f
        val maxZ = bounds(5) / 16f
        new AABB(minX, minY, minZ, maxX, maxY, maxZ)
      }
    val texture = nbt.getString("texture")
    val tint = if (nbt.contains("tint")) Option(nbt.getInt("tint")) else None
    new Shape(aabb, texture, tint)
  }

  def shapeToNBT(shape: Shape): CompoundTag = {
    val nbt = new CompoundTag()
    nbt.putByteArray("bounds", Array(
      (shape.bounds.minX * 16).round.toByte,
      (shape.bounds.minY * 16).round.toByte,
      (shape.bounds.minZ * 16).round.toByte,
      (shape.bounds.maxX * 16).round.toByte,
      (shape.bounds.maxY * 16).round.toByte,
      (shape.bounds.maxZ * 16).round.toByte
    ))
    nbt.putString("texture", shape.texture)
    shape.tint.foreach(nbt.putInt("tint", _))
    nbt
  }

  case class Shape(bounds: AABB, texture: String, tint: Option[Int]) extends Ordered[Shape] {
    override def compare(that: Shape): Int = {
      val (a, b) = (this, that)

      if (a.bounds.minX != b.bounds.minX) return a.bounds.minX compare b.bounds.minX
      if (a.bounds.minY != b.bounds.minY) return a.bounds.minY compare b.bounds.minY
      if (a.bounds.minZ != b.bounds.minZ) return a.bounds.minZ compare b.bounds.minZ
      if (a.bounds.maxX != b.bounds.maxX) return a.bounds.maxX compare b.bounds.maxX
      if (a.bounds.maxY != b.bounds.maxY) return a.bounds.maxY compare b.bounds.maxY
      if (a.bounds.maxZ != b.bounds.maxZ) return a.bounds.maxZ compare b.bounds.maxZ
      if (a.tint != b.tint) return Ordering[Option[Int]].compare(a.tint, b.tint)
      if (a.texture != b.texture) return a.texture compare b.texture
      0
    }
  }

  object Shape {
    val CODEC: Codec[Shape] = RecordCodecBuilder.create(inst => inst.group(
      ScalaCodec.AABB.fieldOf("bounds").forGetter(_.bounds),
      Codec.STRING.fieldOf("texture").forGetter(_.texture),
      ScalaCodec.optionFieldOf("tint", ScalaCodec.INT).forGetter(_.tint),
    ).apply(inst, Shape.apply _))

    val STREAM_CODEC: StreamCodec[ByteBuf, Shape] = StreamCodec.composite(
      ScalaStreamCodec.AABB, _.bounds,
      ByteBufCodecs.STRING_UTF8, _.texture,
      ScalaStreamCodec.option(ScalaStreamCodec.INT), _.tint,
      Shape.apply _
    )
  }
}
