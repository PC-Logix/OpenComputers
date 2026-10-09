package li.cil.oc.server.machine

import li.cil.oc.OpenComputers
import li.cil.oc.api.network.{FilteredEnvironment, ManagedPeripheral}
import li.cil.oc.server.driver.CompoundBlockEnvironment

import scala.collection.immutable
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object Callbacks {
  type Callback = CallbackAnalyzer.Callback
  type ComponentCallback = CallbackAnalyzer.ComponentCallback
  type PeripheralCallback = CallbackAnalyzer.PeripheralCallback

  private val analyzer = new CallbackAnalyzer(OpenComputers.log)
  private val cache = mutable.Map.empty[Class[_], immutable.Map[String, Callback]]

  def apply(host: Any): immutable.Map[String, Callback] = host match {
    case multi: CompoundBlockEnvironment => analyzer.analyze(multi.environments.map(_._2).asJava).asScala.toMap
    case _: ManagedPeripheral | _: FilteredEnvironment => analyzer.analyze(List(host).asJava).asScala.toMap
    case _ => cache.getOrElseUpdate(host.getClass, analyzer.analyze(List(host).asJava).asScala.toMap)
  }

  def clear(): Unit = cache.clear()

  def fromClass(environment: Class[_]): immutable.Map[String, Callback] = analyzer.fromClass(environment).asScala.toMap
}
