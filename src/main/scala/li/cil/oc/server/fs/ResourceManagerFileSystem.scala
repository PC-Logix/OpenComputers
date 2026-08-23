package li.cil.oc.server.fs

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, FileNotFoundException, InputStream}

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager

import scala.collection.mutable
import scala.jdk.CollectionConverters._

/** A read-only filesystem snapshot backed by server resources. */
final class ResourceManagerFileSystem (files: Map[String, Array[Byte]], directories: Set[String]) extends InputStreamFileSystem {
  override def spaceTotal: Long = spaceUsed

  override def spaceUsed: Long = files.valuesIterator.map(_.length.toLong).sum

  override def exists(path: String): Boolean = {
    val clean = ResourceManagerFileSystem.clean(path)
    files.contains(clean) || directories.contains(clean)
  }

  override def size(path: String): Long = files.get(ResourceManagerFileSystem.clean(path)).fold(0L)(_.length.toLong)

  override def isDirectory(path: String): Boolean = directories.contains(ResourceManagerFileSystem.clean(path))

  override def lastModified(path: String): Long = 0L

  override def list(path: String): Array[String] = {
    val clean = ResourceManagerFileSystem.clean(path)
    if (!exists(clean)) throw new FileNotFoundException("no such file or directory: " + path)
    if (!isDirectory(clean)) return null

    val prefix = if (clean.isEmpty) "" else clean + "/"
    val children = mutable.LinkedHashSet.empty[String]
    for (entry <- files.keysIterator ++ directories.iterator if entry.startsWith(prefix) && entry != clean) {
      val relative = entry.substring(prefix.length)
      val slash = relative.indexOf('/')
      children += (if (slash < 0) relative else relative.substring(0, slash) + "/")
    }
    children.toArray
  }

  override protected def openInputChannel(path: String): Option[InputChannel] =
    files.get(ResourceManagerFileSystem.clean(path)).map(bytes => new InputStreamChannel(new ByteArrayInputStream(bytes)))
}

object ResourceManagerFileSystem {
  def readResource(manager: ResourceManager, location: ResourceLocation): Option[Array[Byte]] = {
    val resource = manager.getResource(location)
    if (resource.isPresent) Some(read(resource.get.open())) else None
  }

  def fromResource(manager: ResourceManager, root: ResourceLocation): ResourceManagerFileSystem = {
    val directory = root.getPath.stripSuffix("/")
    val prefix = directory + "/"
    val resources = manager.listResources(directory, location =>
      location.getNamespace == root.getNamespace && location.getPath.startsWith(prefix))
    if (resources.isEmpty) return null

    val files = resources.asScala.map { case (location, resource) =>
      val relative = location.getPath.substring(prefix.length)
      relative -> read(resource.open())
    }.toMap

    val directories = mutable.Set.empty[String]
    directories += ""
    for (path <- files.keys; parts = path.split("/").toSeq.dropRight(1)) {
      for (count <- 1 to parts.size) directories += parts.take(count).mkString("/")
    }
    new ResourceManagerFileSystem(files, directories.toSet)
  }

  private def read(input: InputStream): Array[Byte] = {
    try {
      val output = new ByteArrayOutputStream()
      val buffer = new Array[Byte](8192)
      var count = input.read(buffer)
      while (count >= 0) {
        if (count > 0) output.write(buffer, 0, count)
        count = input.read(buffer)
      }
      output.toByteArray
    }
    finally input.close()
  }

  private def clean(path: String): String = path.replace("\\", "/").stripPrefix("/").stripSuffix("/")
}
