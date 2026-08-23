package li.cil.oc.integration.opencomputers

import li.cil.oc.{api, Constants, Settings}
import li.cil.oc.api.network.{Component, EnvironmentHost, ManagedEnvironment, Visibility}
import li.cil.oc.common.Slot
import li.cil.oc.common.item.Tablet
import li.cil.oc.common.item.data.TabletData
import net.minecraft.nbt.{CompoundTag, Tag}
import net.minecraft.world.item.ItemStack

object DriverTablet extends Item {
  override def worksWith(stack: ItemStack): Boolean = isOneOf(stack,
    api.Items.get(Constants.ItemName.Tablet))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost): ManagedEnvironment =
    if (host.getEnvironmentLevel != null && host.getEnvironmentLevel.isClientSide) null
    else {
      Tablet.Server.cache.invalidate(Tablet.getOrCreateId(stack))
      val data = new TabletData(stack)
      val index = fileSystemIndex(data)
      if (index < 0) null
      else {
        val fsStack = data.items(index)
        Option(DriverFileSystem.createEnvironment(fsStack, host)) match {
          case Some(environment) => environment.node match {
            case component: Component =>
              component.setVisibility(Visibility.Network)
              // Creating a filesystem environment may initialize data on the
              // embedded disk stack (most importantly its address). Persist the
              // modified embedded stack back into the tablet, but never save the
              // filesystem environment itself onto the outer tablet stack.
              data.items(index) = fsStack
              data.saveData(stack)
              environment
            case _ => null
          }
          case _ => null
        }
      }
    }


  private def fileSystemIndex(data: TabletData): Int =
    data.items.indexWhere(fs => !fs.isEmpty && DriverFileSystem.worksWith(fs))

  /**
   * Run persistence code against the filesystem ItemStack embedded in a tablet.
   *
   * A tablet is a proxy component: the environment exposed to the host is the
   * tablet's internal filesystem, not the tablet item itself. Persisting that
   * environment directly to the outer tablet would overwrite tablet-level data
   * components such as CHARGE.
   */
  def withFileSystemStack(tabletStack: ItemStack)(f: ItemStack => Unit): Boolean = {
    val data = new TabletData(tabletStack)
    val index = fileSystemIndex(data)
    if (index < 0) false
    else {
      val fsStack = data.items(index)
      f(fsStack)
      data.items(index) = fsStack
      data.saveData(tabletStack)
      true
    }
  }

  override def slot(stack: ItemStack) = Slot.Tablet

  def mapToDataTag(stack: ItemStack, tag: CompoundTag): CompoundTag = {
    val data = new TabletData(stack)
    val index = data.items.indexWhere {
      case fs if !fs.isEmpty => DriverFileSystem.worksWith(fs)
      case _ => false
    }
    if (index >= 0 && tag != null && tag.contains(Settings.namespace + "items")) {
      val baseTag = tag.getList(Settings.namespace + "items", Tag.TAG_COMPOUND).getCompound(index)
      if (!baseTag.contains("item")) {
        baseTag.put("item", new CompoundTag())
      }
      val itemTag = baseTag.getCompound("item")
      if (!itemTag.contains("tag")) {
        itemTag.put("tag", new CompoundTag())
      }
      val stackTag = itemTag.getCompound("tag")
      if (!stackTag.contains(Settings.namespace + "data")) {
        stackTag.put(Settings.namespace + "data", new CompoundTag())
      }
      stackTag.getCompound(Settings.namespace + "data")
    }
    else new CompoundTag()
  }
}
