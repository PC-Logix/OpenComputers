package li.cil.oc.integration.opencomputers

import li.cil.oc
import li.cil.oc.{Constants, OpenComputers, Settings, api}
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.{Loot, Slot}
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.common.item.data.DriveData
import li.cil.oc.common.item.{FloppyDisk, HardDiskDrive, SolidStateDrive}
import li.cil.oc.server.component.Drive
import li.cil.oc.server.fs.FileSystem.{ItemLabel, ReadOnlyLabel}
import li.cil.oc.util.ExtendedDataComponentHolder._
import li.cil.oc.util.ItemUtils
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentHolder
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.common.MutableDataComponentHolder
import net.neoforged.neoforge.server.ServerLifecycleHooks

import java.util.UUID

object DriverFileSystem extends Item {
  val UUIDVerifier = """^([0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12})$""".r

  override def worksWith(stack: ItemStack) = {
    val tag = ItemUtils.getTag(stack)
    isOneOf(stack,
      api.Items.get(Constants.ItemName.HDDTier1),
      api.Items.get(Constants.ItemName.HDDTier2),
      api.Items.get(Constants.ItemName.HDDTier3),
      api.Items.get(Constants.ItemName.HDDTier4),
      api.Items.get(Constants.ItemName.SSDTier1),
      api.Items.get(Constants.ItemName.SSDTier2),
      api.Items.get(Constants.ItemName.SSDTier3),
      api.Items.get(Constants.ItemName.Floppy)) &&
      (tag == null || !tag.contains(Settings.namespace + "lootPath"))
  }

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.getEnvironmentLevel != null && host.getEnvironmentLevel.isClientSide) null
    else stack.getItem match {
      case hdd: HardDiskDrive => createEnvironment(stack, hdd.kiloBytes * 1024, hdd.platterCount, host, hdd.tier + 2)
      case ssd: SolidStateDrive => createEnvironment(stack, ssd.kiloBytes * 1024, 1, host, ssd.tier + 4)
      case disk: FloppyDisk => createEnvironment(stack, Settings.get.floppySize * 1024, 1, host, 1)
      case _ => null
    }

  override def slot(stack: ItemStack) =
    stack.getItem match {
      case ssd: SolidStateDrive => Slot.HDD
      case hdd: HardDiskDrive => Slot.HDD
      case disk: FloppyDisk => Slot.Floppy
      case _ => throw new IllegalArgumentException()
    }

  override def tier(stack: ItemStack) =
    stack.getItem match {
      case hdd: HardDiskDrive => hdd.tier
      case ssd: SolidStateDrive => ssd.tier
      case _ => 0
    }

  private def createEnvironment(stack: ItemStack, capacity: Int, platterCount: Int, host: EnvironmentHost, speed: Int) = if (ServerLifecycleHooks.getCurrentServer != null) {
    val lootFactory = stack.get(OCComponents.LOOT_DISK.get())
    if (lootFactory != null) {
      // Loot disk, create file system using factory callback.
      Loot.factories.get(lootFactory) match {
        case Some(factory) =>
          val label = stack.getComponent(OCComponents.LABEL).orNull
          api.FileSystem.asManagedEnvironment(factory.call(), label, host, Settings.resourceDomain + ":floppy_access")
        case _ => null // Invalid loot disk.
      }
    }
    else {
      // We have a bit of a chicken-egg problem here, because we want to use the
      // node's address as the folder name... so we generate the address here,
      // if necessary. No one will know, right? Right!?
      val address = getOrCreateAddress(stack)
      var label: api.fs.Label = new ReadWriteItemLabel(stack)
      val isFloppy = api.Items.get(stack) == api.Items.get(Constants.ItemName.Floppy)
      val isSSD = stack.getItem.isInstanceOf[SolidStateDrive]
      // ssd_access sound event is intentionally not provided, so they're silent
      val sound = Some(Settings.resourceDomain + ":" + (if (isFloppy) "floppy_access" else if (isSSD) "ssd_access" else "hdd_access"))
      val drive = new DriveData(stack)
      val environment = if (drive.isUnmanaged) {
        new Drive(capacity max 0, platterCount, label, Option(host), sound, speed, drive.isLocked, isSSD)
      }
      else {
        var fs = oc.api.FileSystem.fromSaveDirectory(address, capacity max 0, Settings.get.bufferChanges)
        if (drive.isLocked) {
          fs = oc.api.FileSystem.asReadOnly(fs)
          label = new ReadOnlyLabel(label.getLabel(ServerLifecycleHooks.getCurrentServer.registryAccess()))
        }
        if (isSSD) {
          li.cil.oc.server.fs.FileSystem.asManagedEnvironment(fs, label, host, sound.orNull, speed,
            Settings.get.ssdReadCost, Settings.get.ssdWriteCost)
        }
        else {
          oc.api.FileSystem.asManagedEnvironment(fs, label, host, sound.orNull, speed)
        }
      }
      if (environment != null && environment.node != null) {
        environment.node.asInstanceOf[oc.server.network.Node].address = address
      }
      environment
    }
  }
  else null

  private def getOrCreateAddress(holder: MutableDataComponentHolder): String = {
    holder.getComponent(OCComponents.ADDRESS) match {
      case Some(UUIDVerifier(address)) => address
      case _ =>
        val newAddress = UUID.randomUUID().toString
        holder.setComponent(OCComponents.ADDRESS, newAddress)
        OpenComputers.log.warn(s"Generated new address for disk '${newAddress}'.")
        newAddress
    }
  }

  @deprecated
  private def addressFromTag(tag: CompoundTag) =
    if (tag.contains("node") && tag.getCompound("node").contains("address")) {
      tag.getCompound("node").getString("address") match {
        case UUIDVerifier(address) => address
        case _ => // Invalid disk address.
          val newAddress = UUID.randomUUID().toString
          tag.getCompound("node").putString("address", newAddress)
          OpenComputers.log.warn(s"Generated new address for disk '${newAddress}'.")
          newAddress
      }
    }
    else UUID.randomUUID().toString

  private class ReadWriteItemLabel(stack: ItemStack) extends ItemLabel(stack) {
    var label: Option[String] = None

    override def getLabel(provider: HolderLookup.Provider): String = label.orNull

    override def setLabel(value: String): Unit = {
      label = Option(value).map(_.take(16))
    }

    override def loadData(holder: DataComponentHolder): Unit = {
      for(text <- holder.getComponent(OCComponents.LABEL)) {
        label = Some(text)
      }
    }

    override def saveData(holder: MutableDataComponentHolder): Unit = {
      for(label <- label) {
        holder.setComponent(OCComponents.LABEL, label)
      }
    }
  }
}
