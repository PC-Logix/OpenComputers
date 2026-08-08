package li.cil.oc.server.machine

import java.util
import java.util.concurrent.TimeUnit
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.Driver
import li.cil.oc.api.Network
import li.cil.oc.api.detail.MachineAPI
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.item.CallBudget
import li.cil.oc.api.driver.item.Processor
import li.cil.oc.api.machine
import li.cil.oc.api.machine.Architecture
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.machine.ExecutionResult
import li.cil.oc.api.machine.LimitReachedException
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.machine.Value
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.ComponentConnector
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.client.ClientUtil
import li.cil.oc.common.EventHandler
import li.cil.oc.common.SaveHandler
import li.cil.oc.common.Slot
import li.cil.oc.common.blockentity
import li.cil.oc.common.datacomponents.MachineData.{Signal => DataSignal}
import li.cil.oc.common.datacomponents.{MachineData, OCComponents}
import li.cil.oc.server.PacketSender
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.fs.FileSystem
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ExtendedDataComponentHolder._
import li.cil.oc.util.ResultWrapper
import li.cil.oc.util.ResultWrapper.result
import li.cil.oc.util.ThreadPoolFactory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt._
import net.neoforged.neoforge.server.ServerLifecycleHooks

import scala.collection.JavaConverters.mapAsJavaMap
import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._
import scala.jdk.CollectionConverters._
import scala.collection.mutable
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.LongTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.ByteArrayTag
import net.minecraft.nbt.ListTag
import net.minecraft.client.server.IntegratedServer
import net.minecraft.core.{BlockPos, HolderLookup}
import net.minecraft.core.component.DataComponentHolder
import net.minecraft.world.level.ChunkPos
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.common.MutableDataComponentHolder

import java.nio.ByteBuffer

class Machine(val host: MachineHost) extends AbstractManagedEnvironment with machine.Machine with Runnable with DeviceInfo {
  override val node: ComponentConnector = Network.newNode(this, Visibility.Network).
    withComponent("computer", Visibility.Neighbors).
    withConnector(Settings.get.bufferComputer).
    create()

  val tmp = if (Settings.get.tmpSize > 0) {
    Option(FileSystem.asManagedEnvironment(FileSystem.
      fromMemory(Settings.get.tmpSize * 1024), "tmpfs", null, null, 5))
  } else None

  var architecture: Architecture = _

  private[machine] val state = mutable.Stack(Machine.State.Stopped)

  private val _components = mutable.Map.empty[String, String]

  private val addedComponents = mutable.Set.empty[Component]

  // A Create contraption places its captured block entities one at a time.
  // Keep already-known components alive while those tiny temporary networks
  // split and merge, then reconcile them against the finished structure.
  private var componentRestoreGrace = 0
  private val deferredRemovedComponents = mutable.Map.empty[String, String]

  private val _users = mutable.Set.empty[String]

  private val signals = mutable.Queue.empty[Machine.Signal]

  var maxComponents = 0

  private var maxCallBudget = 1.0

  private var hasMemory = false

  @volatile private var callBudget = 0.0

  // We want to ignore the call limit in synchronized calls to avoid errors.
  private var inSynchronizedCall = false

  // ----------------------------------------------------------------------- //

  var worldTime = 0L // Game-world time for os.time().

  private var uptime = 0L // Game-world time [ticks] for os.uptime().

  private var cpuTotal = 0L // Pseudo-real-world time [ns] for os.clock().

  private var cpuStart = 0L // Pseudo-real-world time [ns] for os.clock().

  private var remainIdle = 0 // Ticks left to sleep before resuming.

  private var remainingPause = 0 // Ticks left to wait before resuming.

  private var usersChanged = false // Send updated users list to clients?

  private var message: Option[String] = None // For error messages.

  private var cost = Settings.get.computerCost * Settings.get.tickFrequency

  private val maxSignalQueueSize = Settings.get.maxSignalQueueSize

  // ----------------------------------------------------------------------- //

  override def onHostChanged(): Unit = {
    val components = host.internalComponents
    maxComponents = components.foldLeft(0)((sum, item) => sum + (Option(item) match {
      case Some(stack) => Option(Driver.driverFor(stack, host.getClass)) match {
        case Some(driver: Processor) => driver.supportedComponents(stack)
        case _ => 0
      }
      case _ => 0
    }))
    val callBudgets = components.map(stack => (stack, Option(Driver.driverFor(stack, host.getClass)))).collect({
      case (stack, Some(driver: CallBudget)) => driver.getCallBudget(stack)
    })
    maxCallBudget = if (callBudgets.isEmpty) 1.0 else callBudgets.sum / callBudgets.size
    var newArchitecture: Architecture = null
    components.find {
      case stack: ItemStack => Option(Driver.driverFor(stack, host.getClass)) match {
        case Some(driver: Processor) if driver.slot(stack) == Slot.CPU =>
          Option(driver.architecture(stack)) match {
            case Some(clazz) =>
              if (architecture == null || architecture.getClass != clazz) try {
                newArchitecture = clazz.getConstructor(classOf[machine.Machine]).newInstance(this)
              }
              catch {
                case t: Throwable => OpenComputers.log.warn("Failed instantiating a CPU architecture.", t)
              }
              else {
                newArchitecture = architecture
              }
              true
            case _ => false
          }
        case _ => false
      }
      case _ => false
    }
    // This needs to operate synchronized against the worker thread, to avoid the
    // architecture changing while it is currently being executed.
    if (newArchitecture != architecture) this.synchronized {
      architecture = newArchitecture
      if (architecture != null && node.network != null) architecture.onConnect()
    }
    hasMemory = Option(architecture).fold(false)(_.recomputeMemory(components))
  }

  override def components: util.Map[String, String] = mapAsJavaMap(_components)

  def componentCount: Int = (_components.foldLeft(0.0)((acc, entry) => entry match {
    case (_, name) => acc + (if (name != "filesystem") 1.0 else 0.25)
  }) + addedComponents.foldLeft(0.0)((acc, component) => acc + (if (component.name != "filesystem") 1 else 0.25)) - 1).toInt // -1 = this computer

  override def tmpAddress: String = tmp.fold(null: String)(_.node.address)

  def lastError: String = message.orNull

  override def setCostPerTick(value: Double): Unit = cost = value * Settings.get.tickFrequency

  override def getCostPerTick: Double = cost / Settings.get.tickFrequency

  override def users: Array[String] = _users.synchronized(_users.toArray)

  override def upTime(): Double = {
    // Convert from old saves (set to -timeStarted on load).
    if (uptime < 0) {
      uptime = worldTime + uptime
    }
    // World time is in ticks, and each second has 20 ticks. Since we
    // want uptime() to return real seconds, though, we'll divide it
    // accordingly.
    uptime / 20.0
  }

  override def cpuTime: Double = (cpuTotal + (System.nanoTime() - cpuStart)) * 10e-10

  // ----------------------------------------------------------------------- //

  override def getDeviceInfo: util.Map[String, String] = host match {
    case deviceInfo: DeviceInfo => deviceInfo.getDeviceInfo
    case _ => null
  }

  // ----------------------------------------------------------------------- //

  override def canInteract(player: String): Boolean = !Settings.get.canComputersBeOwned ||
    _users.synchronized(_users.isEmpty || _users.contains(player)) ||
    ServerLifecycleHooks.getCurrentServer == null ||
    ServerLifecycleHooks.getCurrentServer.isSingleplayer || {
    val config = ServerLifecycleHooks.getCurrentServer.getPlayerList
    val entity = config.getPlayerByName(player)
    entity != null && config.isOp(entity.getGameProfile)
  }

  override def isRunning: Boolean = state.synchronized(state.top != Machine.State.Stopped && state.top != Machine.State.Stopping)

  override def isPaused: Boolean = state.synchronized(state.top == Machine.State.Paused && remainingPause > 0)

  override def start(): Boolean = state.synchronized(state.top match {
    case Machine.State.Stopped if node.network != null =>
      onHostChanged()
      processAddedComponents()
      verifyComponents()
      if (!Settings.get.ignorePower && node.globalBuffer < cost) {
        // No beep! We have no energy after all :P
        crash("gui.Error.NoEnergy")
        false
      }
      else if (architecture == null || maxComponents == 0) {
        beep("-")
        crash("gui.Error.NoCPU")
        false
      }
      else if (componentCount > maxComponents) {
        beep("-..")
        crash("gui.Error.ComponentOverflow")
        false
      }
      else if (!hasMemory) {
        beep("-.")
        crash("gui.Error.NoRAM")
        false
      }
      else if (!init()) {
        beep("--")
        false
      }
      else {
        switchTo(Machine.State.Starting)
        uptime = 0
        node.sendToReachable("computer.started")
        true
      }
    case Machine.State.Paused if remainingPause > 0 =>
      remainingPause = 0
      host.markChanged()
      true
    case Machine.State.Stopping =>
      switchTo(Machine.State.Restarting)
      EventHandler.unscheduleClose(this)
      true
    case _ =>
      false
  })

  override def pause(seconds: Double): Boolean = {
    val ticksToPause = math.max((seconds * 20).toInt, 0)
    def shouldPause(state: Machine.State.Value) = state match {
      case Machine.State.Stopping | Machine.State.Stopped => false
      case Machine.State.Paused if ticksToPause <= remainingPause => false
      case _ => true
    }
    if (shouldPause(state.synchronized(state.top))) {
      // Check again when we get the lock, might have changed since.
      Machine.this.synchronized(state.synchronized(if (shouldPause(state.top)) {
        if (state.top != Machine.State.Paused) {
          assert(!state.contains(Machine.State.Paused))
          state.push(Machine.State.Paused)
        }
        remainingPause = ticksToPause
        host.markChanged()
        return true
      }))
    }
    false
  }

  override def stop(): Boolean = state.synchronized(state.headOption match {
    case Some(Machine.State.Stopped | Machine.State.Stopping) =>
      false
    case _ =>
      state.push(Machine.State.Stopping)
      EventHandler.scheduleClose(this)
      true
  })

  override def consumeCallBudget(callCost: Double): Unit = {
    if (architecture.isInitialized && !inSynchronizedCall) {
      val clampedCost = math.max(0.0, callCost)
      if (clampedCost > callBudget) {
        throw new LimitReachedException()
      }
      callBudget -= clampedCost
    }
  }

  override def beep(frequency: Short, duration: Short): Unit = {
    PacketSender.sendSound(host.getEnvironmentLevel, host.xPosition, host.yPosition, host.zPosition, frequency, duration)
  }

  override def beep(pattern: String): Unit = {
    PacketSender.sendSound(host.getEnvironmentLevel, host.xPosition, host.yPosition, host.zPosition, pattern)
  }

  override def crash(message: String): Boolean = {
    this.message = Option(message)
    state.synchronized {
      val result = stop()
      if (state.top == Machine.State.Stopping) {
        // When crashing, make sure there's no "Running" left in the stack.
        state.clear()
        state.push(Machine.State.Stopping)
      }
      result
    }
  }

  def convertArg(param: Any): AnyRef = {
    param match {
      case arg: java.lang.Boolean => arg
      case arg: java.lang.Character => Integer.valueOf(arg.toInt)
      case arg: java.lang.Byte => arg
      case arg: java.lang.Short => arg
      case arg: java.lang.Integer => arg
      case arg: java.lang.Long => arg
      case arg: java.lang.Number => Double.box(arg.doubleValue)
      case arg: java.lang.String => arg
      case arg: Array[Byte] => arg
      case arg: CompoundTag => arg
      case arg: java.util.HashMap[_, _] => arg.asScala
      case arg =>
        OpenComputers.log.warn("Trying to push signal with an unsupported argument of type " + arg.getClass.getName)
        null
    }
  }

  override def signal(name: String, args: AnyRef*): Boolean = {
    state.synchronized(state.top match {
      case Machine.State.Stopped | Machine.State.Stopping => return false
      case _ => signals.synchronized {
        if (signals.size >= maxSignalQueueSize) return false
        else if (args == null) {
          signals.enqueue(new Machine.Signal(name, Array.empty))
        }
        else {
          signals.enqueue(new Machine.Signal(name, args.map {
            case null | ResultWrapper.unit | None => null
            case arg: Map[_, _] if arg.isEmpty || arg.head._1.isInstanceOf[String] && arg.head._2.isInstanceOf[String] => arg
            case arg: mutable.Map[_, _] if arg.isEmpty || arg.head._1.isInstanceOf[String] && arg.head._2.isInstanceOf[String] => arg.toMap
            case arg: java.util.Map[_, _] => {
              val convertedMap = new mutable.HashMap[AnyRef, AnyRef]
              for ((key, value) <- arg) {
                val convertedKey = convertArg(key)
                if (convertedKey != null) {
                  val convertedValue = convertArg(value)
                  if (convertedValue != null) {
                    convertedMap += convertedKey -> convertedValue
                  }
                }
              }
              convertedMap
            }
            case arg => convertArg(arg)
          }.toArray[AnyRef]))
        }
      }
    })

    if (architecture != null) architecture.onSignal()
    true
  }

  override def popSignal(): Machine.Signal = signals.synchronized(if (signals.isEmpty) null else signals.dequeue().convert())

  override def methods(value: scala.AnyRef): util.Map[String, Callback] = Callbacks(value).map(entry => {
    val (name, callback) = entry
    name -> callback.annotation
  })

  override def invoke(address: String, method: String, args: Array[AnyRef]): Array[AnyRef] = {
    if (method == "setForeground" || method == "setBackground") {
      if (args.nonEmpty && args(0).isInstanceOf[Array[Byte]]) {
        OpenComputers.log.warn(s"[Debug] Bad argument passed to $method! Array[Byte] detected.")
      }
    }
    if (node != null && node.network != null) {
      Option(node.network.node(address)) match {
        case Some(component: li.cil.oc.server.network.Component) if component.canBeSeenFrom(node) || component == node =>
          val annotation = component.annotation(method)
          if (annotation.direct) {
            consumeCallBudget(1.0 / annotation.limit)
          }
          component.invoke(method, this, args: _*)
        case _ => throw new IllegalArgumentException("no such component")
      }
    }
    else {
      // Not really, but makes the VM stop, which is what we want in this case,
      // because it means we've been disconnected / disposed already.
      throw new LimitReachedException()
    }
  }

  override def invoke(value: Value, method: String, args: Array[AnyRef]): Array[AnyRef] = {
    Callbacks(value).get(method) match {
      case Some(callback) =>
        val annotation = callback.annotation
        if (annotation.direct) {
          consumeCallBudget(1.0 / annotation.limit)
        }
        val arguments = new ArgumentsImpl(Seq(args: _*))
        Registry.convert(callback(value, this, arguments))
      case _ => throw new NoSuchMethodException()
    }
  }

  override def addUser(name: String): Unit = {
    if (_users.size >= Settings.get.maxUsers)
      throw new Exception("too many users")

    if (_users.contains(name))
      throw new Exception("user exists")
    if (name.length > Settings.get.maxUsernameLength)
      throw new Exception("username too long")
    if (!ServerLifecycleHooks.getCurrentServer.getPlayerNames.contains(name))
      throw new Exception("player must be online")

    _users.synchronized {
      _users += name
      usersChanged = true
    }
  }

  override def removeUser(name: String): Boolean = _users.synchronized {
    val success = _users.remove(name)
    if (success) {
      usersChanged = true
    }
    success
  }

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():boolean -- Starts the computer. Returns true if the state changed.""")
  def start(context: Context, args: Arguments): Array[AnyRef] =
    result(!isPaused && start())

  @Callback(doc = """function():boolean -- Stops the computer. Returns true if the state changed.""")
  def stop(context: Context, args: Arguments): Array[AnyRef] =
    result(stop())

  @Callback(direct = true, doc = """function():boolean -- Returns whether the computer is running.""")
  def isRunning(context: Context, args: Arguments): Array[AnyRef] =
    result(isRunning)

  @Callback(doc = """function([frequency:string or number[, duration:number[, async:boolean]]]) -- Plays a tone, useful to alert users via audible feedback.""")
  def beep(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.count == 1 && args.isString(0)) {
      beep(args.checkString(0))
    } else {
      val frequency = args.optInteger(0, 440)
      if (frequency < 20 || frequency > 2000) {
        throw new IllegalArgumentException("invalid frequency, must be in [20, 2000]")
      }
      val duration = args.optDouble(1, 0.1)
      val durationInMilliseconds = math.max(50, math.min(5000, (duration * 1000).toInt))
      val async = args.optBoolean(2, false)
      if (!async) {
        context.pause(durationInMilliseconds / 1000.0)
      }
      beep(frequency.toShort, durationInMilliseconds.toShort)
    }
    null
  }

  @Callback(doc = """function():table -- Collect information on all connected devices.""")
  def getDeviceInfo(context: Context, args: Arguments): Array[AnyRef] = {
    context.pause(1) // Iterating all nodes is potentially expensive, and I see no practical reason for having to call this frequently.
    Array[AnyRef](node.network.nodes.map(n => (n, n.host)).collect {
      case (n: Component, deviceInfo: DeviceInfo) =>
        if (n.canBeSeenFrom(node) || n == node) {
          Option(deviceInfo.getDeviceInfo) match {
            case Some(info) => Option(n.address -> info)
            case _ => None
          }
        }
        else None
      case (n, deviceInfo: DeviceInfo) =>
        if (n.canBeReachedFrom(node)) {
          Option(deviceInfo.getDeviceInfo) match {
            case Some(info) => Option(n.address -> info)
            case _ => None
          }
        }
        else None
    }.collect { case Some(kvp) => kvp }.toMap)
  }

  @Callback(doc = """function():table -- Returns a map of program name to disk label for known programs.""")
  def getProgramLocations(context: Context, args: Arguments): Array[AnyRef] =
    result(ProgramLocations.getMappings(Machine.getArchitectureName(architecture.getClass)))

  // ----------------------------------------------------------------------- //

  def isExecuting: Boolean = state.synchronized(state.contains(Machine.State.Running))

  override val canUpdate = true

  override def update(): Unit = if (state.synchronized(state.top != Machine.State.Stopped)) {
    // Add components that were added since the last update to the actual list
    // of components if we can see them. We use this delayed approach to avoid
    // issues with components that have a visibility lower than their
    // reachability, because in that case if they get connected in the wrong
    // order we wouldn't add them (since they'd be invisible in their connect
    // message, and only become visible with a later node-to-node connection,
    // but that wouldn't trigger a connect message anymore due to the higher
    // reachability).
    processAddedComponents()
    tickComponentRestoreGrace()

    // Component overflow check, crash if too many components are connected, to
    // avoid confusion on the user's side due to components not showing up.
    if (componentCount > maxComponents) {
      beep("-..")
      crash("gui.Error.ComponentOverflow")
    }

    // Update world time for time() and uptime().
    worldTime = host.getEnvironmentLevel.getDayTime
    uptime += 1

    if (remainIdle > 0) {
      remainIdle -= 1
    }

    // Reset direct call budget.
    callBudget = maxCallBudget

    // Make sure we have enough power.
    if (host.getEnvironmentLevel.getGameTime % Settings.get.tickFrequency == 0) {
      state.synchronized(state.top match {
        case Machine.State.Paused |
             Machine.State.Restarting |
             Machine.State.Stopping |
             Machine.State.Stopped => // No power consumption.
        case Machine.State.Sleeping if remainIdle > 0 && signals.isEmpty =>
          if (!node.tryChangeBuffer(-cost * Settings.get.sleepCostFactor)) {
            crash("gui.Error.NoEnergy")
          }
        case _ =>
          if (!node.tryChangeBuffer(-cost)) {
            crash("gui.Error.NoEnergy")
          }
      })
    }

    // Avoid spamming user list across the network.
    if (host.getEnvironmentLevel.getGameTime % 20 == 0 && usersChanged) {
      val list = _users.synchronized {
        usersChanged = false
        users
      }
      host match {
        case computer: blockentity.traits.Computer => PacketSender.sendComputerUserList(computer, list)
        case _ =>
      }
    }

    // Check if we should switch states. These are all the states in which we're
    // guaranteed that the executor thread isn't running anymore.
    state.synchronized(state.top) match {
      // Booting up.
      case Machine.State.Starting =>
        verifyComponents()
        switchTo(Machine.State.Yielded)
      // Computer is rebooting.
      case Machine.State.Restarting =>
        close()
        if (Settings.get.eraseTmpOnReboot) {
          tmp.foreach(_.node.remove()) // To force deleting contents.
          tmp.foreach(tmp => node.connect(tmp.node))
        }
        node.sendToReachable("computer.stopped")
        start()
      // Resume from pauses based on sleep or signal underflow.
      case Machine.State.Sleeping if remainIdle <= 0 || signals.nonEmpty =>
        switchTo(Machine.State.Yielded)
      // Resume in case we paused  because the game was paused.
      case Machine.State.Paused =>
        if (remainingPause > 0) {
          remainingPause -= 1
        }
        else {
          verifyComponents() // In case we're resuming after loading.
          state.pop()
          switchTo(state.top) // Trigger execution if necessary.
        }
      // Perform a synchronized call (message sending).
      case Machine.State.SynchronizedCall =>
        // We switch into running state, since we'll behave as though the call
        // were performed from our executor thread.
        switchTo(Machine.State.Running)
        try {
          inSynchronizedCall = true
          architecture.runSynchronized()
          inSynchronizedCall = false
          // Check if the callback called pause() or stop().
          state.top match {
            case Machine.State.Running =>
              switchTo(Machine.State.SynchronizedReturn)
            case Machine.State.Paused =>
              state.pop() // Paused
              state.pop() // Running, no switchTo to avoid new future.
              state.push(Machine.State.SynchronizedReturn)
              state.push(Machine.State.Paused)
            case Machine.State.Stopping =>
              state.clear()
              state.push(Machine.State.Stopping)
            case _ => throw new AssertionError()
          }
        }
        catch {
          case e: java.lang.Error if e.getMessage == "not enough memory" =>
            crash("gui.Error.OutOfMemory")
          case e: Throwable =>
            OpenComputers.log.warn("Faulty architecture implementation for synchronized calls.", e)
            crash("gui.Error.InternalError")
        }
        finally {
          inSynchronizedCall = false
        }
      case _ => // Nothing special to do, just avoid match errors.
    }

    // Finally check if we should stop the computer. We cannot lock the state
    // because we may have to wait for the executor thread to finish, which
    // might turn into a deadlock depending on where it currently is.
    state.synchronized(state.top) match {
      // Computer is shutting down.
      case Machine.State.Stopping => Machine.this.synchronized(state.synchronized(tryClose()))
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message): Unit = {
    message.data match {
      case Array(name: String, args@_*) if message.name == "computer.signal" =>
        signal(name, Seq(message.source.address) ++ args: _*)
      case Array(player: Player, name: String, args@_*) if message.name == "computer.checked_signal" =>
        if (canInteract(player.getName.getString))
          signal(name, Seq(message.source.address) ++ args: _*)
      case _ =>
        if (message.name == "computer.start" && !isPaused) start()
        else if (message.name == "computer.stop") stop()
    }
  }

  override def onConnect(node: Node): Unit = {
    if (node == this.node) {
      _components += this.node.address -> this.node.name
      tmp.foreach(fs => node.connect(fs.node))
      Option(architecture).foreach(_.onConnect())
    }
    else {
      node match {
        case component: Component => addComponent(component)
        case _ =>
      }
    }
    // For computers, to generate the components in their inventory.
    host.onMachineConnect(node)
  }

  override def onDisconnect(node: Node): Unit = {
    if (node == this.node) {
      close()
      tmp.foreach(_.node.remove())
    }
    else {
      node match {
        case component: Component => removeComponent(component)
        case _ =>
      }
    }
    // For computers, to save the components in their inventory.
    host.onMachineDisconnect(node)
  }

  // ----------------------------------------------------------------------- //

  def addComponent(component: Component): Unit = {
    deferredRemovedComponents -= component.address
    if (!_components.contains(component.address)) {
      addedComponents += component
    }
  }

  def removeComponent(component: Component): Unit = {
    if (_components.contains(component.address)) {
      if (componentRestoreGrace > 0) {
        deferredRemovedComponents(component.address) = component.name
      }
      else {
        _components.synchronized(_components -= component.address)
        signal("component_removed", component.address, component.name)
      }
    }
    addedComponents -= component
  }

  /** Suppress transient component removal events while Create restores blocks. */
  def beginComponentRestoreGrace(ticks: Int): Unit = {
    if (isRunning) {
      componentRestoreGrace = math.max(componentRestoreGrace, ticks)
      pause(componentRestoreGrace / 20.0)
    }
  }

  private def tickComponentRestoreGrace(): Unit = if (componentRestoreGrace > 0) {
    componentRestoreGrace -= 1
    if (componentRestoreGrace == 0) {
      val network = node.network
      for ((address, name) <- deferredRemovedComponents) {
        val restored = network != null && (network.node(address) match {
          case component: Component => component.name == name
          case _ => false
        })
        if (!restored && _components.contains(address)) {
          _components.synchronized(_components -= address)
          signal("component_removed", address, name)
        }
      }
      deferredRemovedComponents.clear()
    }
  }

  private def processAddedComponents(): Unit = {
    if (addedComponents.nonEmpty) {
      for (component <- addedComponents) {
        if (component.canBeSeenFrom(node)) {
          _components.synchronized(_components += component.address -> component.name)
          // Skip the signal if we're not initialized yet, since we'd generate a
          // duplicate in the startup script otherwise.
          if (architecture != null && architecture.isInitialized) {
            signal("component_added", component.address, component.name)
          }
        }
      }
      addedComponents.clear()
    }
  }

  private def verifyComponents(): Unit = {
    if (componentRestoreGrace > 0) return

    val invalid = mutable.Set.empty[String]
    for ((address, name) <- _components) {
      node.network.node(address) match {
        case component: Component if component.name == name => // All is well.
        case _ =>
          if (name == "filesystem") {
            OpenComputers.log.trace(s"A component of type '$name' disappeared ($address)! This usually means that it didn't save its node.")
            OpenComputers.log.trace("If this was a file system provided by a ComputerCraft peripheral, this is normal.")
          }
          else OpenComputers.log.warn(s"A component of type '$name' disappeared ($address)! This usually means that it didn't save its node.")
          signal("component_removed", address, name)
          invalid += address
      }
    }
    for (address <- invalid) {
      _components -= address
    }
  }

  // ----------------------------------------------------------------------- //

  private def tmpPath = node.address + "_tmp"

  override def loadData(holder: DataComponentHolder): Unit = Machine.this.synchronized(state.synchronized {
    assert(state.top == Machine.State.Stopped || state.top == Machine.State.Paused)
    close()
    state.clear()

    val machineData = holder.getComponent(OCComponents.MACHINE)
    machineData.flatMap(_.nodeAddress) match {
      case Some(address) => node.asInstanceOf[li.cil.oc.server.network.Node].loadAddress(address)
      // Data written before machine nodes had their own nested address used
      // the shared holder address. Retain that as a migration fallback.
      case _ => super.loadData(holder)
    }

    for(data <- machineData) {
      // Stack iteration/save order is top-to-bottom, while pushAll places each
      // following value on top. Reverse the serialized order, as the 1.12
      // implementation did, so the active machine state remains on top.
      state.pushAll(data.state.reverse)
      _users ++= data.users

      for(msg <- data.message) {
        message = Some(msg)
      }

      _components ++= data.components.map(c => c.address -> c.name)

      for (fs <- tmp) {
        val lvl = host.getEnvironmentLevel
        val cpos = new ChunkPos(new BlockPos(host.xPosition.toInt, host.yPosition.toInt, host.zPosition.toInt))
        fs.loadData(SaveHandler.loadNBT(lvl.dimension.location, cpos, tmpPath), lvl.registryAccess())
      }

      if (state.nonEmpty && isRunning && init()) {
        try {
          architecture.loadData(holder, data.architectureData)
        } catch {
          case t: Throwable =>
            OpenComputers.log.error(
              s"""Unexpected error loading a state of computer at ${host.machinePosition()}. """ +
                s"""State: ${state.headOption.fold("no state")(_.toString)}. Unless you're upgrading/downgrading across a major version, please report this! Thank you.""", t)
            close()
            // Preserve usability when a native VM snapshot is corrupt or
            // incompatible: restart through the BIOS with the persisted
            // components and disks instead of leaving the machine dead.
            start()
        }

        signals ++= data.signals.map(v => Machine.Signal(v.name, v.args.map {
          case DataSignal.Null => null
          case DataSignal.Boolean(v) => Boolean.box(v)
          case DataSignal.Long(v) => Long.box(v)
          case DataSignal.Double(v) => Double.box(v)
          case DataSignal.StringValue(v) => v
          case DataSignal.ByteArray(v) => if (v.hasArray) {
            v.array
          } else {
            val array: Array[Byte] = Array.fill(v.remaining()) {
              0
            }
            v.get(array)
            array
          }
          case DataSignal.StringMap(v) => mutable.Map.from(v)
          case DataSignal.Compound(v) => v
        }.toArray))

        uptime = data.uptime
        cpuTotal = data.cpuTotal
        remainingPause = data.remainingPause

        // Delay execution for a second to allow the world around us to settle.
        if (state.top != Machine.State.Restarting) {
          pause(Settings.get.startupDelay)
        }
      } else {
        // Clean up in case we got a weird state stack.
        onHostChanged()
        close()
      }
    }

    // A newly assembled machine has other item data but no persisted machine
    // component yet. Keep the state stack valid after the unconditional clear
    // above so its first save cannot try to pause an empty stack.
    if (state.isEmpty) state.push(Machine.State.Stopped)
  })

  override def saveData(holder: MutableDataComponentHolder): Unit = Machine.this.synchronized(state.synchronized {
    // The lock on 'this' should guarantee that this never happens regularly.
    // If something other than regular saving tries to save while we are executing code,
    // e.g. SpongeForge saving during robot.move due to block changes being captured,
    // just don't save this at all. What could possibly go wrong?
    if(isExecuting) return

    if(SaveHandler.savingForClients) {
      return
    }

    // Make sure we don't continue running until everything has saved.
    pause(0.05)

    // Make sure the component list is up-to-date.
    processAddedComponents()

    // Do not persist stale component bookkeeping. During modern BlockEntity
    // reconstruction transient component environments may briefly exist while
    // inventories/networks are being rebuilt. If one of those addresses made
    // it into _components, carrying it across saves produces phantom
    // component_removed events (most visibly for robot screens) on every load.
    if (node.network != null) {
      _components.synchronized {
        _components.filterInPlace { case (address, name) =>
          node.network.node(address) match {
            case component: Component => component.name == name
            case _ => false
          }
        }
      }
    }

    for(fs <- tmp) {
      val lvl = host.getEnvironmentLevel
      val cpos = new ChunkPos(new BlockPos(host.xPosition.toInt, host.yPosition.toInt, host.zPosition.toInt))
      SaveHandler.scheduleSave(lvl.dimension.location, cpos, tmpPath, (nbt: CompoundTag) => fs.saveData(nbt, lvl.registryAccess()))
    }

    holder.setComponent(OCComponents.MACHINE, MachineData(
      state = state.toList,
      users = _users.toSet,
      message = message,
      components = _components.map { case (k, v) => MachineData.Component(k, v) }.toList,
      nodeAddress = Option(node.address),
      architectureData = {
        val nbt = new CompoundTag()

        if(architecture != null) {
          try {
            architecture.saveData(holder, nbt)
          } catch {
            case t: Throwable =>
              OpenComputers.log.error(
                s"""Unexpected error saving a state of computer at ${host.machinePosition()}. """ +
                  s"""State: ${state.headOption.fold("no state")(_.toString)}. Unless you're upgrading/downgrading across a major version, please report this! Thank you.""", t)
          }
        }

        nbt
      },
      signals = if(state.top != Machine.State.Stopped) {
        signals.map {
          case Machine.Signal(name, args) =>
            MachineData.Signal(
              name,
              args.map {
                case null => DataSignal.Null
                case arg: java.lang.Boolean => DataSignal.Boolean(arg)
                case arg: java.lang.Long => DataSignal.Long(arg)
                case arg: java.lang.Double => DataSignal.Double(arg)
                case arg: String => DataSignal.StringValue(arg)
                case arg: Array[Byte] => DataSignal.ByteArray(ByteBuffer.wrap(arg))
                case arg: Map[_, _] => DataSignal.StringMap(Map.from(arg.iterator.map { case a -> b => a.toString -> b.toString }))
                case arg: CompoundTag => DataSignal.Compound(arg)
                case _ => DataSignal.Null
              }.toList
            )
        }.toList
      } else List.empty,
      uptime = uptime,
      cpuTotal = cpuTotal,
      remainingPause = remainingPause
    ))
  })

  // ----------------------------------------------------------------------- //

  private def init(): Boolean = {
    onHostChanged()
    if (architecture == null) return false

    // Reset error state.
    message = None

    // Clear any left-over signals from a previous run.
    signals.clear()

    // Connect the `/tmp` node to our owner. We're not in a network in
    // case we're loading, which is why we have to check it here.
    if (node.network != null) {
      tmp.foreach(fs => node.connect(fs.node))
    }

    try {
      return architecture.initialize()
    }
    catch {
      case ex: Throwable =>
        OpenComputers.log.warn("Failed initializing computer.", ex)
        close()
    }
    false
  }

  def tryClose(): Boolean =
    if (isExecuting) false
    else {
      close()
      tmp.foreach(_.node.remove()) // To force deleting contents.
      if (node.network != null) {
        tmp.foreach(tmp => node.connect(tmp.node))
      }
      node.sendToReachable("computer.stopped")
      true
    }

  private def close() =
    if (state.synchronized(state.isEmpty || state.top != Machine.State.Stopped)) {
      // Give up the state lock, then get the more generic lock on this instance first
      // before locking on state again. Always must be in that order to avoid deadlocks.
      this.synchronized(state.synchronized {
        state.clear()
        state.push(Machine.State.Stopped)
        componentRestoreGrace = 0
        deferredRemovedComponents.clear()
        Option(architecture).foreach(_.close())
        signals.clear()
        uptime = 0
        cpuTotal = 0
        cpuStart = 0
        remainIdle = 0
      })

      // Mark state change in owner, to send it to clients.
      host.markChanged()
    }

  // ----------------------------------------------------------------------- //

  private def switchTo(value: Machine.State.Value) = state.synchronized {
    val result = state.pop()
    if (value == Machine.State.Stopping || value == Machine.State.Restarting) {
      state.clear()
    }
    state.push(value)
    if (value == Machine.State.Yielded || value == Machine.State.SynchronizedReturn) {
      remainIdle = 0
      Machine.threadPool.schedule(this, Settings.get.executionDelay, TimeUnit.MILLISECONDS)
    }

    // Mark state change in owner, to send it to clients.
    host.markChanged()

    result
  }

  private def isGamePaused: Boolean = {
    val server = ServerLifecycleHooks.getCurrentServer

    server != null &&
      !server.isDedicatedServer &&
      (FMLEnvironment.dist.isClient && (() => { ClientUtil.isPaused })())
  }

  // This is a really high level lock that we only use for saving and loading.
  override def run(): Unit = Machine.this.synchronized {
    val isSynchronizedReturn = state.synchronized {
      if (state.top != Machine.State.Yielded &&
        state.top != Machine.State.SynchronizedReturn) {
        return
      }
      // See if the game appears to be paused, in which case we also pause.
      if (isGamePaused) {
        state.push(Machine.State.Paused)
        return
      }
      switchTo(Machine.State.Running) == Machine.State.SynchronizedReturn
    }

    cpuStart = System.nanoTime()

    try {
      val result = architecture.runThreaded(isSynchronizedReturn)

      // Check if someone called pause() or stop() in the meantime.
      state.synchronized {
        state.top match {
          case Machine.State.Running =>
            result match {
              case result: ExecutionResult.Sleep =>
                signals.synchronized {
                  // Immediately check for signals to allow processing more than one
                  // signal per game tick.
                  if (signals.isEmpty && result.ticks > 0) {
                    switchTo(Machine.State.Sleeping)
                    remainIdle = result.ticks
                  } else {
                    switchTo(Machine.State.Yielded)
                  }
                }
              case result: ExecutionResult.SynchronizedCall =>
                switchTo(Machine.State.SynchronizedCall)
              case result: ExecutionResult.Shutdown =>
                if (result.reboot) {
                  switchTo(Machine.State.Restarting)
                }
                else {
                  switchTo(Machine.State.Stopping)
                }
              case result: ExecutionResult.Error =>
                beep("--")
                crash(Option(result.message).getOrElse("unknown error"))
            }
          case Machine.State.Paused =>
            state.pop() // Paused
            state.pop() // Running, no switchTo to avoid new future.
            result match {
              case result: ExecutionResult.Sleep =>
                remainIdle = result.ticks
                state.push(Machine.State.Sleeping)
              case result: ExecutionResult.SynchronizedCall =>
                state.push(Machine.State.SynchronizedCall)
              case result: ExecutionResult.Shutdown =>
                if (result.reboot) {
                  state.push(Machine.State.Restarting)
                }
                else {
                  state.push(Machine.State.Stopping)
                }
              case result: ExecutionResult.Error =>
                crash(Option(result.message).getOrElse("unknown error"))
            }
            state.push(Machine.State.Paused)
          case Machine.State.Stopping =>
            state.clear()
            state.push(Machine.State.Stopping)
          case Machine.State.Restarting =>
          // Nothing to do!
          case _ => throw new AssertionError("Invalid state in executor post-processing.")
        }
        assert(!isExecuting)
      }
    }
    catch {
      case e: Throwable =>
        OpenComputers.log.warn("Architecture's runThreaded threw an error. This should never happen!", e)
        crash("gui.Error.InternalError")
    }

    // Keep track of time spent executing the computer.
    cpuTotal += System.nanoTime() - cpuStart
  }
}

object Machine extends MachineAPI {
  // Keep registration order, to allow deterministic iteration of the architectures.
  val checked: mutable.LinkedHashSet[Class[_ <: Architecture]] = mutable.LinkedHashSet.empty[Class[_ <: Architecture]]

  override def add(architecture: Class[_ <: Architecture]): Unit = {
    if (!checked.contains(architecture)) {
      try {
        architecture.getConstructor(classOf[machine.Machine])
      }
      catch {
        case t: Throwable => throw new IllegalArgumentException("Architecture does not have required constructor.", t)
      }
      checked += architecture
    }
  }

  override def architectures: util.List[Class[_ <: Architecture]] = checked.toSeq

  def getArchitectureName(architecture: Class[_ <: Architecture]): String =
    architecture.getAnnotation(classOf[Architecture.Name]) match {
      case annotation: Architecture.Name => annotation.value
      case _ => architecture.getSimpleName
    }

  override def create(host: MachineHost) = new Machine(host)

  /** Possible states of the computer, and in particular its executor. */
  object State extends Enumeration {
    /** The computer is not running right now and there is no Lua state. */
    val Stopped = Value("Stopped")

    /** Booting up, doing the first run to initialize the kernel and libs. */
    val Starting = Value("Starting")

    /** Computer is currently rebooting. */
    val Restarting = Value("Restarting")

    /** The computer is currently shutting down. */
    val Stopping = Value("Stopping")

    /** The computer is paused and waiting for the game to resume. */
    val Paused = Value("Paused")

    /** The computer executor is waiting for a synchronized call to be made. */
    val SynchronizedCall = Value("SynchronizedCall")

    /** The computer should resume with the result of a synchronized call. */
    val SynchronizedReturn = Value("SynchronizedReturn")

    /** The computer will resume as soon as possible. */
    val Yielded = Value("Yielded")

    /** The computer is yielding for a longer amount of time. */
    val Sleeping = Value("Sleeping")

    /** The computer is up and running, executing Lua code. */
    val Running = Value("Running")
  }

  /** Signals are messages sent to the Lua state from Java asynchronously. */
  private[machine] case class Signal(val name: String, val args: Array[AnyRef]) extends machine.Signal {
    def convert() = new Signal(name, Registry.convert(args))
  }

  private val threadPool = ThreadPoolFactory.create("Computer", Settings.get.threads)
}
