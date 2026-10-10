package li.cil.oc.common.datacomponents

import li.cil.oc.api.ImmutableFluidStack

import li.cil.oc.Settings
import li.cil.oc.api.ImmutableItemStack
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.datacomponents.MachineData.Signal
import li.cil.oc.common.datacomponents.TextBufferContents.ShortArray
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.server.component.DebugCard.AccessContext
import li.cil.oc.server.machine.Machine
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.{Color, ItemUtils, NbtDataStream}
import net.minecraft.core.component.{DataComponentType, DataComponents}
import net.minecraft.core.{BlockPos, Direction, HolderLookup, UUIDUtil}
import net.minecraft.nbt.{ByteArrayTag, ByteTag, CompoundTag, DoubleTag, FloatTag, IntArrayTag, IntTag, ListTag, LongTag, NbtOps, StringTag, TagTypes, Tag => NbtTag}
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ColorRGBA
import net.minecraft.world.item.{DyeColor, ItemStack}
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.templates.FluidTank

import java.nio.ByteBuffer
import java.util.function.Supplier
import java.util.{NoSuchElementException, UUID}
import scala.collection.mutable
import scala.jdk.OptionConverters._
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.reflect.ClassTag

private object Migrators {
  val map: mutable.Map[DataComponentType[_], Migrator[_]] = new mutable.HashMap()

  implicit class Identifier(val string: String)

  implicit def identifier(string: (String, String)): Identifier = {
    var (ns, id) = string
    if (!ns.endsWith(":")) ns += ":"
    new Identifier(ns + id)
  }

  trait SimpleValue[T] {
    val value: Option[T]
    def andRemove: Option[T]

    def optional: SimpleValue[Option[T]] = {
      val orig = this
      new SimpleValue[Option[T]] {
        override val value: Option[Option[T]] = Some(orig.value)
        override def andRemove: Option[Option[T]] = Some(orig.andRemove)
      }
    }
  }

  object SimpleValue {
    def empty[T]: SimpleValue[T] with Composable[T] = new SimpleValue[T] with Composable[T] {
      override val value: Option[T] = None
      override def andRemove: Option[T] = None
      override def unwrap: Iterable[SimpleValue[_]] = Iterable.empty
    }

  }

  trait Composable[T] extends SimpleValue[T] {
    def unwrap: Iterable[SimpleValue[_]]

    override def optional: Composable[Option[T]] = {
      val orig = this
      new Composable[Option[T]] {
        override val value: Option[Option[T]] = Some(orig.value)
        override def andRemove: Option[Option[T]] = Some(orig.andRemove)
        override def unwrap: Iterable[SimpleValue[_]] = orig.unwrap
      }
    }
  }

  trait Value[T] extends SimpleValue[T] with Composable[T] {
    val tag: CompoundTag
    val name: String

    override def unwrap: Iterable[SimpleValue[_]] = Array(this)

    override def andRemove: Option[T] = {
      if(value.isDefined) tag.remove(name)
      value
    }

    def map[U](fn: T => Option[U]): Value[U] = new Mapped(this, fn)

    def mustBeExactly(size: Int)(implicit ex: T <:< Array[_]): Value[T] = {
      map(v => Option.when(ex.apply(v).length == size) { v })
    }

    def mustBeSized(size: Range)(implicit ex: T <:< Array[_]): Value[T] = {
      map(v => Option.when(size.contains(ex.apply(v).length)) { v })
    }

    def mustBeWithin(size: Range)(implicit ex: T =:= Int): Value[T] = {
      map(v => Option.when({
        val cmp = ex.apply(v)
        size.contains(cmp)
      }) { v })
    }

    def orElse(other: T): Value[T] =
      optional.map(v => Some(v getOrElse other))

    override def optional: Value[Option[T]] = {
      val orig = this
      new Value[Option[T]] {
        override val value: Option[Option[T]] = Some(orig.value)
        override val tag: CompoundTag = orig.tag
        override val name: String = orig.name
      }
    }
  }

  class CompositionContext[U](val list: mutable.ListBuffer[Composable[_]]) {
    def apply[T](value: Composable[T]): T = {
      list += value
      apply(value.value)
    }

    def apply[T](value: Option[T]): T = {
      value.get
    }

    def remaining(value: Deserializer[U]): CompoundTag = {
      for(item <- list) {
        item.andRemove
      }

      list.clear()

      value.tag
    }
  }

  def compose[U](fn: CompositionContext[U] => U): Composable[U] = {
    try {
      val ctx = new CompositionContext[U](new mutable.ListBuffer())
      val value = fn(ctx)
      new Composed[U](ctx.list.toArray, () => Some(value))
    } catch {
      case _: NoSuchElementException => SimpleValue.empty
      case _: scala.MatchError => SimpleValue.empty
    }
  }

  def composeValue[U](fn: CompositionContext[U] => U): Option[U] =
    compose(fn).value

  def composeAndRemove[U](fn: CompositionContext[U] => U): Option[U] =
    compose(fn).andRemove

  class TagValue[T](override val tag: CompoundTag, override val name: String, converter: PartialFunction[NbtTag, Option[T]]) extends Value[T] {
    override val value = tag.get(name) match {
      case null => None
      case tag => converter.isDefinedAt(tag) match {
        case true => converter.apply(tag)
        case false => None
      }
    }
  }

  class Mapped[T, U](orig: Value[T], fn: T => Option[U]) extends Value[U] {
    override val tag: CompoundTag = orig.tag
    override val name: String = orig.name
    override lazy val value: Option[U] = orig.value match {
      case Some(v) => fn(v)
      case None => None
    }
  }

  class Composed[U](orig: Array[Composable[_]], fn: () => Option[U]) extends SimpleValue[U] with Composable[U] {
    override lazy val value: Option[U] = fn()

    override def unwrap: Iterable[SimpleValue[_]] = orig

    override def andRemove: Option[U] = {
      for(value <- orig.flatMap(v => v.unwrap)) {
        value.andRemove
      }

      value
    }
  }

  class Deserializer[T](val tag: CompoundTag, val provider: HolderLookup.Provider) {
    def compound[U](name: Identifier)(fn: Deserializer[U] => Option[U]): Option[U] = tag.get(name.string) match {
      case value: CompoundTag => fn(new Deserializer[U](value, provider))
      case _ => None
    }

    def value[U](name: Identifier)(converter: PartialFunction[NbtTag, Option[U]]) =
      new TagValue[U](tag, name.string, converter)

    def tag(name: Identifier): TagValue[NbtTag] = value[NbtTag](name) {
      case tag: NbtTag => Some(tag)
    }

    def compoundTag(name: Identifier) = value[CompoundTag](name) {
      case tag: CompoundTag => Some(tag)
    }

    def self = new SimpleValue[CompoundTag] {
      override val value: Option[CompoundTag] = Some(tag)
      override def andRemove: Option[CompoundTag] = ???
    }

    def string(name: Identifier) = value[String](name) {
      case tag: StringTag => Some(tag.getAsString)
    }

    def stringList(name: Identifier) = value[List[String]](name) {
      case tag: ListTag if TagTypes.getType(tag.getElementType) == StringTag.TYPE =>
        Some((0 until tag.size()).map(tag.getString).toList)
    }

    def resourceLocation(name: Identifier) = value[ResourceLocation](name) {
      case tag: StringTag => ResourceLocation.tryParse(tag.getAsString) match {
        case id: ResourceLocation => Some(id)
        case _ => None
      }
    }

    def byte(name: Identifier) = value[Byte](name) {
      case tag: ByteTag => Some(tag.getAsByte)
    }

    def boolean(name: Identifier) = byte(name).map(b => Some(b != 0))

    def int(name: Identifier) = value[Int](name) {
      case tag: IntTag => Some(tag.getAsInt)
    }

    def long(name: Identifier) = value[Long](name) {
      case tag: LongTag => Some(tag.getAsLong)
    }

    def stringEnum[E](name: Identifier, values: Map[String, E]) =
      string(name).map(values.get)

    def enumeration[E](name: Identifier, values: Array[E]) =
      int(name).map(i => Option.when(i < values.length) { values(i) })

    def enumerationArray[E: ClassTag](name: Identifier, values: Array[E]): Value[Array[E]] =
      intArray(name).map(array => Some(array.flatMap(i => Option.when(i < values.length) { values(i) })))

    def float(name: Identifier) = value[Float](name) {
      case tag: FloatTag => Some(tag.getAsFloat)
    }

    def double(name: Identifier) = value[Double](name) {
      case tag: DoubleTag => Some(tag.getAsDouble)
    }

    def byteBuffer(name: Identifier) = value[ByteBuffer](name) {
      case string: StringTag => Some(ByteBuffer.wrap(string.getAsString.getBytes))
      case bytes: ByteArrayTag => Some(ByteBuffer.wrap(bytes.getAsByteArray))
    }

    def intArray(name: Identifier) = value[Array[Int]](name) {
      case intArray: IntArrayTag => Some(intArray.getAsIntArray)
    }

    def itemStack(name: Identifier) = value[ImmutableItemStack](name) {
      case compoundTag: CompoundTag => ImmutableItemStack.parse(provider, compoundTag).toScala
    }

    def fluidStack(name: Identifier) = value[FluidStack](name) {
      case compoundTag: CompoundTag => FluidStack.parse(provider, compoundTag).toScala
    }

    def itemStackList(name: Identifier) = value[List[ImmutableItemStack]](name) {
      case tag: ListTag if TagTypes.getType(tag.getElementType) == CompoundTag.TYPE =>
        Some((0 until tag.size()).map(i => ImmutableItemStack.parse(provider, tag.get(i))).filter(_.isPresent).map(_.get).toList)
    }

    def uuid(name: Identifier) = value[UUID](name) {
      case intArrayTag: IntArrayTag if intArrayTag.size() == 4 => Some(UUIDUtil.uuidFromIntArray(intArrayTag.getAsIntArray))
    }

    def list[U](name: Identifier)(fn: Deserializer[U] => Option[U]) = value[List[U]](name) {
      case list: ListTag if TagTypes.getType(list.getElementType) == CompoundTag.TYPE =>
        Some((0 until list.size).flatMap(i => fn(new Deserializer[U](list.getCompound(i), provider))).toList)
    }

    def array[U: ClassTag](name: Identifier)(fn: Deserializer[U] => Option[U]) = value[Array[U]](name) {
      case list: ListTag if TagTypes.getType(list.getElementType) == CompoundTag.TYPE =>
        Some((0 until list.size).flatMap(i => fn(new Deserializer[U](list.getCompound(i), provider))).toArray)
    }
  }

  def register[T](component: Supplier[DataComponentType[T]])(fn: Deserializer[T] => Option[T]): Unit = {
    map.put(component.get, new Migrator[T] {
      override def fromNBT(tag: CompoundTag, provider: HolderLookup.Provider): Option[T] = {
        fn(new Deserializer(tag, provider)) match {
          case Some(value) => Some(value)
          case None if tag.contains(oc + "data") => fn(new Deserializer(tag.getCompound(oc + "data"), provider))
          case _ => None
        }
      }
    })
  }

  def first[T](values: (() => Option[T])*): Option[T] = {
    for(fn <- values) {
      for(value <- fn()) return Some(value)
    }

    None
  }

  private val oc = Settings.namespace

  register(OCComponents.ID) { _.uuid(oc -> "uuid").andRemove }
  register(() => DataComponents.CUSTOM_NAME) { de => ItemUtils.getDisplayName(de.tag).map(Component.literal) }

  register(OCComponents.OWNER) { de =>
    first(
      () => composeAndRemove[Owner] { by =>
        val name = by(de.string(oc -> "owner"))
        val id = by(de.uuid(oc -> "ownerUuid"))
        new Owner(name, id)
      },

      // alternative to help migrate block entities
      () => composeAndRemove[Owner] { by =>
        val name = by(de.string("owner"))
        val id = by(de.uuid("ownerUuid"))
        new Owner(name, id)
      }
    )
  }

  register(OCComponents.LABEL) { de =>
    first(
      () => de.compound[String](oc -> "data") { _.string(oc -> "label").andRemove },
      () => de.compound[String](oc -> "data") { _.string(oc -> "fs.label").andRemove },
      () => de.string(oc -> "label").andRemove,
      () => de.string(oc -> "fs.label").andRemove,
    )
  }

  register(OCComponents.COMPONENTS) { de =>
    first(
      () => de.itemStackList(oc -> "components").andRemove,
      () => de.itemStackList("components").andRemove,
      () => de.itemStackList(oc -> "disks").andRemove,
      () => de.itemStackList("disks").andRemove,
    )
  }
  register(OCComponents.CONTENTS) { _.itemStackList(oc -> "items").andRemove }

  // loot disks
  register(OCComponents.LOOT_DISK) { _.resourceLocation(oc -> "lootFactory").andRemove }
  register(OCComponents.DISK_COLOR) { _.int(oc -> "color").map(i => Some(DyeColor.byId(i))).andRemove }

  // generic energy storage
  register(OCComponents.CHARGE) { de =>
    first(
      () => de.double(oc -> "charge").andRemove,
      () => de.double(oc -> "energy").andRemove,
    )
  }

  register(OCComponents.MAX_CHARGE) { de =>
    de.double(oc -> "maxEnergy").andRemove
  }

  // disks & drives
  register(OCComponents.UNMANAGED) { _.boolean(oc -> "unmanaged").andRemove }
  register(OCComponents.LOCK) { _.string(oc -> "lock").andRemove }
  register(OCComponents.HEAD_POS) { _.int("headPos").andRemove }

  // eeprom stuff
  register(OCComponents.EEPROM_CODE) { _.compound[ByteBuffer](oc -> "data") { _.byteBuffer(oc -> "eeprom").andRemove } }
  register(OCComponents.EEPROM_DATA) { _.compound[ByteBuffer](oc -> "data") { _.byteBuffer(oc -> "userdata").andRemove } }
  register(OCComponents.READONLY) { _.compound[Boolean](oc -> "data") { _.boolean(oc -> "readonly").andRemove } }

  // map things
  register(OCComponents.MF_COORD) { de =>
    composeAndRemove[MFCoords] { by =>
      val dimension = by(de.resourceLocation(oc -> "dimension"))
      val Array(x, y, z, side) = by(de.intArray(oc -> "coord") mustBeExactly 4)
      new MFCoords(dimension, new BlockPos(x, y, z), Direction.from3DDataValue(side))
    }
  }

  register(OCComponents.SOURCE_MAP_ITEM) { _.itemStack(oc -> "map").andRemove }

  // nanomachines
  register(OCComponents.NANOMACHINES_NETWORK_INFO) { _.compound[CompoundTag](oc -> "configuration") { _.self.value } }

  // network
  register(OCComponents.ADDRESS) { _.compound[String]("node") { _.string("address").andRemove } }
  register(OCComponents.VISIBILITY) { de =>
    first(
      () => de.compound[Visibility](oc -> "node") { _.enumeration("visibility", Visibility.values()).andRemove },
      () => de.compound[Visibility](oc -> "node") { _.enumeration(oc -> "visibility", Visibility.values()).andRemove },
      () => de.compound[Visibility]("node") { _.enumeration("visibility", Visibility.values()).andRemove },
      () => de.enumeration(oc -> "visibility", Visibility.values()).andRemove,
      () => de.enumeration("visibility", Visibility.values()).andRemove
    )
  }

  // network cards
  register(OCComponents.OPEN_PORTS) { _.intArray("openPorts").map(v => Some(v.toList)).andRemove }

  register(OCComponents.WAKE_MESSAGE) { de =>
    composeAndRemove[WakeMessage] { by =>
      val message = by(de.string("wakeMessage"))
      val fuzzy = by(de.boolean("wakeMessageFuzzy"))

      new WakeMessage(message, fuzzy)
    }
  }

  register(OCComponents.TUNNEL) { _.string(oc -> "tunnel").andRemove }
  register(OCComponents.STRENGTH) { _.double("strength").andRemove }

  // text buffers
  register(OCComponents.TEXT_BUFFER) { de =>
    composeAndRemove[TextBufferContents] { by =>
      val width = by(de.int("width") orElse 0)
      val height = by(de.int("height") orElse 0)
      val buffer = by(de.stringList("buffer"))
      val depth = by(de.int("depth") orElse 0)
      val foreground = by(de.int("foreground") orElse 0)
      val foregroundIsPalette = by(de.boolean("foregroundIsPalette") orElse false)
      val background = by(de.int("background") orElse 0)
      val backgroundIsPalette = by(de.boolean("backgroundIsPalette") orElse false)

      val color = Array.fill[Short](height, width)(0)
      if (!NbtDataStream.getShortArray(de.tag, "colors", color, width, height)) {
        NbtDataStream.getIntArrayLegacy(de.tag, "color", color, width, height)
        by(de.tag("color"))
      } else {
        by(de.tag("colors"))
      }

      TextBufferContents(width, height, depth, foreground, foregroundIsPalette, background, backgroundIsPalette, buffer, new ShortArray(color))
    }
  }
  register(OCComponents.IS_ON) { _.boolean(oc -> "isOn").andRemove }
  register(OCComponents.IS_POWERED) { de =>
    first(
      () => de.boolean(oc -> "hasPower").andRemove,
      () => de.boolean("hasPower").andRemove
    )
  }
  register(OCComponents.IS_PRECISE) { _.boolean(oc -> "precise").andRemove }

  register(OCComponents.MAX_VIDEO_MODE) { de =>
    composeAndRemove[MaximumVideoMode] { by =>
      val maxWidth = by(de.int(oc -> "maxWidth"))
      val maxHeight = by(de.int(oc -> "maxHeight"))
      val maxDepth = by(de.int(oc -> "maxDepth"))
      new MaximumVideoMode(maxWidth, maxHeight, maxDepth)
    }
  }

  register(OCComponents.VIDEO_MODE) { de =>
    first(
      () => composeAndRemove[VideoMode] { by =>
        val width = by(de.int(oc -> "viewportWidth"))
        val height = by(de.int(oc -> "viewportHeight"))
        new VideoMode(width, height)
      },
      () => composeAndRemove[VideoMode] { by =>
        val width = by(de.int(oc -> "configWidth"))
        val height = by(de.int(oc -> "configHeight"))
        new VideoMode(width, height)
      }
    )
  }

  // terminal
  register(OCComponents.KEYS) { _.stringList(oc -> "keys").andRemove }

  register(OCComponents.TERMINAL_REFERENCE) { de =>
    composeAndRemove[TerminalReference] { by =>
      val key = by(de.string(oc -> "key"))
      val server = by(de.string(oc -> "server"))
      new TerminalReference(key, server)
    }
  }

  // computers
  register(OCComponents.IS_RUNNING) { _.boolean(oc -> "isRunning").andRemove }
  register(OCComponents.IS_ERRORED) { _.boolean(oc -> "hasErrored").map(b => Option.when(b) { () }).andRemove }
  register(OCComponents.USERS) { _.stringList(oc -> "users").andRemove.map(_.toSet) }

  // the complicated one
  register(OCComponents.MACHINE) { de =>
    def decomposeMachine(de: Deserializer[MachineData]) = {
      composeAndRemove[MachineData] { by =>
        val state = by(de.enumerationArray("state", Machine.State.values.toArray))
        val users = by(de.stringList("users") orElse List.empty)
        val message = by(de.string("message").optional)

        val components = by(de.list[MachineData.Component]("components") { de =>
          composeValue[MachineData.Component] { by =>
            val address = by(de.string("address"))
            val name = by(de.string("name"))
            MachineData.Component(address, name)
          }
        })

        val signals = by(de.list[Signal]("signals") { de =>
          composeValue[Signal] { by =>
            val name = by(de.string("name"))
            val args = by(de.compound[List[Signal.Value]]("args") { de =>
              composeValue[List[Signal.Value]] { by =>
                // the encoding of this is weird
                //
                // it stores a "length" tag with the number of items, then each
                // item is stored with the name "arg" + i
                //
                // who knows why the author didn't just use an array, but oh well
                val length = by(de.int("length"))
                val args = (0 until length).map(i => by(de.value[Signal.Value]("arg" + i) {
                  case tag: ByteTag if tag.getAsByte == -1 => Some(Signal.Null)
                  case tag: ByteTag => Some(Signal.Boolean(tag.getAsByte == 1))
                  case tag: LongTag => Some(Signal.Long(tag.getAsLong))
                  case tag: DoubleTag => Some(Signal.Double(tag.getAsDouble))
                  case tag: StringTag => Some(Signal.StringValue(tag.getAsString))
                  case tag: ByteArrayTag => Some(Signal.ByteArray(ByteBuffer.wrap(tag.getAsByteArray)))
                  case tag: ListTag =>
                    // contrary to the tag name, this is a string map
                    val data = mutable.Map.empty[String, String]
                    for (i <- 0 until tag.size by 2) {
                      data += tag.getString(i) -> tag.getString(i + 1)
                    }
                    Some(Signal.StringMap(data.toMap))
                  case tag: CompoundTag => Some(Signal.Compound(tag))
                  case _ => Some(Signal.Null)
                }))

                args.toList
              }
            })

            Signal(name, args)
          }
        })

        val uptime = by(de.long("uptime") orElse 0)
        val cpuTotal = by(de.long("cpuTotal") orElse 0)
        val remainingPause = by(de.int("remainingPause") orElse 0)

        // must be last as this will remove the already consumed data
        val architecture = by.remaining(de)
        val legacyNode = architecture.getCompound("node")
        val nodeAddress = Option(legacyNode.getString("address")).filter(_.nonEmpty)
        architecture.remove("node")

        MachineData(
          state.toList,
          users.toSet,
          message,
          components,
          nodeAddress,
          architecture,
          signals,
          uptime,
          cpuTotal,
          remainingPause
        )
      }
    }

    first(
      () => decomposeMachine(de),
      () => de.compound(oc -> "machine")(decomposeMachine),
      () => de.compound("machine")(decomposeMachine),
    )
  }

  // print data (also complicated)
  register(OCComponents.PRINT) { de =>
    composeAndRemove[PrintData] { by =>
      val label = by(de.string("label").optional)
      val tooltip = by(de.string("tooltip").optional)
      val isButtonMode = by(de.boolean("isButtonMode") orElse false)
      val redstoneLevel = by(first[Int](
        // compatibility
        () => de.boolean("emitRedstone").map(v => if(v) Some(15) else None).andRemove,
        () => (de.int("redstoneLevel") orElse 0).andRemove
      ))
      val pressurePlate = by(de.boolean("pressurePlate") orElse false)
      val stateOff = by(de.value[mutable.Set[PrintData.Shape]]("stateOff") {
        case tag: ListTag if TagTypes.getType(tag.getElementType) == CompoundTag.TYPE =>
          Some(tag.map(PrintData.nbtToShape).to(mutable.Set))
      })
      val stateOn = by(de.value[mutable.Set[PrintData.Shape]]("stateOn") {
        case tag: ListTag if TagTypes.getType(tag.getElementType) == CompoundTag.TYPE =>
          Some(tag.map(PrintData.nbtToShape).to(mutable.Set))
      })
      val isBeaconBase = by(de.boolean("isBeaconBase") orElse false)
      val lightLevel = by(de.int("lightLevel") mustBeWithin (0 to 15) orElse 0)
      val noclipOn = by(de.boolean("noclipOn") orElse false)
      val noclipOff = by(de.boolean("noclipOff") orElse false)

      PrintData(label, tooltip, isButtonMode, redstoneLevel, pressurePlate, stateOff, stateOn, isBeaconBase, lightLevel, noclipOff, noclipOn)
    }
  }

  // redstone stuff
  register(OCComponents.WAKE_THRESHOLD) { _.int("wakeThreshold").andRemove }

  register(OCComponents.WIRELESS_REDSTONE_STATE) { de =>
    composeAndRemove[WirelessRedstoneState] { by =>
      val frequency = by(de.int("wirelessFrequency"))
      val input = by(de.boolean("wirelessInput"))
      val output = by(de.boolean("wirelessOutput"))
      new WirelessRedstoneState(frequency, input, output)
    }
  }

  // dron
  register(OCComponents.DRONE_STATE) { de =>
    composeAndRemove[DroneState] { by =>
      val targetX = by(de.float("targetX"))
      val targetY = by(de.float("targetY"))
      val targetZ = by(de.float("targetZ"))
      val targetAcceleration = by(de.float("targetAcceleration"))
      val selectedSlot = by(de.byte("selectedSlot"))
      val selectedTank = by(de.byte("selectedTank"))
      new DroneState(targetX, targetY, targetZ, targetAcceleration, selectedSlot, selectedTank)
    }
  }

  register(OCComponents.STATUS_TEXT) {
    _.string("statusText")
      .map(v => Some(Component.literal(v): Component))
      .andRemove
  }

  register(OCComponents.LIGHT_COLOR) { de =>
    first(
      () => de.int(oc -> "lightColor").andRemove,
      () => de.int("lightColor").andRemove
    )
      // despite the name this is encoded as ARGB, so it's compatible with existing data
      .map(v => new ColorRGBA(v))
  }

  // robot
  register(OCComponents.ROBOT_CHARGE) { de =>
    composeAndRemove[RobotChargeInfo] { by =>
      new RobotChargeInfo(
        by(de.int(oc -> "storedEnergy")),
        by(de.int(oc -> "robotEnergy"))
      )
    }
  }

  register(OCComponents.CONTAINERS) { de =>
    de.itemStackList(oc -> "containers").andRemove
  }

  register(OCComponents.SELECTED_SLOT) { _.int(oc -> "selectedSlot").andRemove }
  register(OCComponents.SELECTED_TANK) { _.int(oc -> "selectedTank").andRemove }

  register(OCComponents.ROBOT_TOTAL_ANIMATION_TIME) { _.int(oc -> "animationTicksTotal").andRemove }
  register(OCComponents.ROBOT_CURRENT_ANIMATION) { de =>
    composeAndRemove[RobotCurrentAnimation] { by =>
      val animationTicksLeft = by(de.int(oc -> "animationTicksLeft"))
      val moveFrom = by(compose[BlockPos] { by =>
        val moveFromX = by(de.int(oc -> "moveFromX"))
        val moveFromY = by(de.int(oc -> "moveFromY"))
        val moveFromZ = by(de.int(oc -> "moveFromZ"))
        new BlockPos(moveFromX, moveFromY, moveFromZ)
      }.optional)
      val swingShovel = by(de.boolean(oc -> "swingingTool"))
      val turnAxis = by(de.byte(oc -> "turnAxis"))

      RobotCurrentAnimation(animationTicksLeft, moveFrom, swingShovel, turnAxis)
    }
  }

  // compound block thing
  register(OCComponents.COMPOUND_DRIVER) { de =>
    composeAndRemove[(Long, Map[String, CompoundStorage])] { by =>
      val typeHash = by(de.long("typeHash"))
      val remain = by.remaining(de)

      typeHash -> Map.from(remain.getAllKeys.asScala.map(s => s -> new CompoundStorage(remain.getCompound(s))))
    }
  }

  // tablet
  register(OCComponents.ATTACHMENT) { _.itemStack(oc -> "container").andRemove }

  // microcontroller
  register(OCComponents.COMPONENT_NODES) {
    _.array[Option[CompoundStorage]](oc -> "componentNodes") { de =>
      Some(Option.when(!de.tag.isEmpty) { new CompoundStorage(de.tag) })
    }.map(v => Some(v.toList)).andRemove
  }

  // file systems
  register(OCComponents.FILESYSTEM_DATA) { de =>
    first(
      () => de.compound[CompoundTag]("fs") { _.self.andRemove },

      // raid
      () => de.compound[CompoundTag](oc -> "filesystem") { de => de.compound[CompoundTag]("fs") { _.self.andRemove } }
    )
  }

  register(OCComponents.ROBOT_ROM_FILESYSTEM_DATA) { de =>
    first(
      () => de.compoundTag("romRobot").andRemove,
      () => de.compound[CompoundTag](oc -> "robot") { de => de.compoundTag("romRobot").andRemove }
    )
  }

  register(OCComponents.HANDLES) { de =>
    def decomposeOwners(de: Deserializer[(String, Set[Int])]): Option[(String, Set[Int])] = {
      composeAndRemove[(String, Set[Int])] { by =>
        by(de.string("address")) -> by(de.intArray("handles")).toSet
      }
    }

    first(
      () => de.list[(String, Set[Int])]("owners")(decomposeOwners).andRemove.map(Map.from),

      // raid
      () => de.compound[List[(String, Set[Int])]](oc -> "filesystem") { de =>
        de.list[(String, Set[Int])]("owners")(decomposeOwners).andRemove
      }.map(Map.from)
    )
  }

  // COLORS!!!
  register(OCComponents.PALETTE) { _.intArray("palette").map(v => Some(v.toList)).andRemove }
  register(OCComponents.RENDER_COLOR) { de =>
    first(
      () => de.int(oc -> "renderColorRGB").map(i => Some(new ColorRGBA(i))).andRemove,
      () => de.int(oc -> "renderColor").map(i => Some(new ColorRGBA(Color.rgbValues(DyeColor.byId(i))))).andRemove
    )
  }

  // screen
  register(OCComponents.HAS_REDSTONE_INPUT) { _.boolean(oc -> "hadRedstoneInput").andRemove }
  register(OCComponents.INVERT_TOUCH) { de =>
    first(
      () => de.boolean(oc -> "invertTouchMode").andRemove,
    ).flatMap(b => Option.when(b) { () })
  }

  // charger
  register(OCComponents.CHARGE_SPEED) { de =>
    first(
      () => de.double(oc -> "chargeSpeed").andRemove,
      () => de.double("chargeSpeed").andRemove
    )
  }

  register(OCComponents.INVERT_SIGNAL) { de =>
    first(
      () => de.boolean(oc -> "invertSignal").andRemove,
      () => de.boolean("invertSignal").andRemove,
    ).flatMap(b => Option.when(b) { () })
  }

  // raid
  register(OCComponents.PRESENCE) { _.byteBuffer(oc -> "presence").andRemove }

  // racks
  register(OCComponents.RACK_DATA) { de =>
    de.array[Option[CompoundStorage]](oc -> "lastData") { de =>
      Some(Option.when(!de.tag.isEmpty) { new CompoundStorage(de.tag) })
    }.map(v => Some(v.toList)).andRemove
  }

  register(OCComponents.RACK_NODE_MAPPING) { de =>
    de.value(oc -> "nodeMapping") {
      case tag: ListTag if TagTypes.getType(tag.getElementType) == IntArrayTag.TYPE =>
        Some(tag.map { (i: IntArrayTag) => i.getAsIntArray.map(Direction.from3DDataValue).toList }.toList)
    }.andRemove
  }

  // graphics card
  register(OCComponents.GRAPHICS_CARD) { de =>
    composeAndRemove[GraphicsCardState] { by =>
      val screen = by(de.string("screen").optional)
      val bufferIndex = by(de.int("bufferIndex"))

      GraphicsCardState(screen, bufferIndex)
    }
  }

  register(OCComponents.VIDEO_RAM) { de =>
    de.compound[List[(Int, CompoundStorage)]]("videoRam") { de =>
      de.list[(Int, CompoundStorage)]("pages") { de =>
        composeValue[(Int, CompoundStorage)] { by =>
          val idx = by(de.int("page_idx"))
          val data = by(de.compoundTag("page_data"))

          idx -> new CompoundStorage(data)
        }
      }.andRemove
    }
  }

  // leash upgrade
  register(OCComponents.LEASHED_ENTITIES) { _.stringList("leashedEntities").andRemove.map(v => v.map(UUID.fromString)) }

  // tank upgrade
  register(OCComponents.TANK) { de => FluidStack.parse(de.provider, de.tag).toScala.map(ImmutableFluidStack.copyOf) }

  // generator upgrade
  register(OCComponents.FUEL_INVENTORY) { _.itemStack("inventory").andRemove }
  register(OCComponents.FUEL_TICKS_REMAINING) { _.int("remainingTicks").andRemove }

  // debug card
  register(OCComponents.DEBUG_CARD_ACCESS_CONTEXT) { de =>
    composeAndRemove[AccessContext] { by =>
      val player = by(de.string(oc -> "player"))
      val accessNonce = by(de.string(oc -> "accessNonce"))
      AccessContext(player, accessNonce)
    }
  }

  register(OCComponents.DEBUG_CARD_REMOTE_NODE_POSITION) { de =>
    composeAndRemove[BlockPos] { by =>
      val x = by(de.int(oc -> "remoteX"))
      val y = by(de.int(oc -> "remoteY"))
      val z = by(de.int(oc -> "remoteZ"))
      new BlockPos(x, y, z)
    }
  }
}
