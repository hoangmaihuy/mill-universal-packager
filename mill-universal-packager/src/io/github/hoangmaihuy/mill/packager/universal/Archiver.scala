package io.github.hoangmaihuy.mill.packager.universal

import java.io._
import java.io.InputStream
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit

import scala.util.Using

import mill.define.TaskCtx
import os.{Path, SubPath}
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
import org.apache.commons.compress.compressors.CompressorOutputStream

import io.github.hoangmaihuy.mill.packager.permissions.OctalString

object Archiver {

  def apply(
  )(
    implicit ctx: TaskCtx
  ): Archiver = new Archiver()

}

/** Helper methods to package up files into compressed archives.
  */
class Archiver(
)(
  implicit ctx: TaskCtx
) {

  // buffer size used for reading and writing
  private val BUFFER_SIZE = 8192

  /** source data epoch, in seconds. should not use `sys.env`, as Mill's long-lived server process means that `sys.env`
    * variables may not be up to date.
    */
  private val SOURCE_DATE_EPOCH: Option[Long] = ctx.env.get("SOURCE_DATE_EPOCH").flatMap(_.toLongOption).filter(_ > 0L)

  // for setting the permissions for normal files
  private val DEAFULT_PERMSET: Int = oct"0644"

  // for setting the permissions for directories and excutable files
  private val EXEXCUTABLE_PERMSET: Int = oct"0755"

  /** make a compressed tarball file with the compression function from the given path, will put all contents under the
    * path to the tarball file. might throw exceptions when the input path is not existing, since we do not check if the
    * input path exists or not.
    * @param input
    *   the path as input
    * @param output
    *   the path as output
    * @param wrappedIn
    *   the optional sub path to wrap the contents in the tarball file. eg: for mapping (/tmp/xyz/1.conf -> conf/1.conf)
    *   and wrappedIn = archived/simple-app-v0.0.1-SNAPSHOT, it will produce
    *   archived/simple-app-v0.0.1-SNAPSHOT/conf/1.conf in the tarball file
    * @param compress
    *   the compression function
    */
  def mkTarball[T <: OutputStream](
    input: Path,
    output: Path,
    wrappedIn: Option[SubPath]
  )(
    compress: OutputStream => CompressorOutputStream[T]
  ): Unit = {
    Using.Manager { use =>
      val fos = use(new FileOutputStream(output.toIO))
      val bos = use(new BufferedOutputStream(fos))
      addPathsToTarball(input, compress(bos), wrappedIn)
    }
  }

  /** make a compressed tarball file with the compression function from the given mappings
    * @param mappings
    *   the file mappings as input
    * @param out
    *   the output path
    * @param wrappedIn
    *   the optional sub path to wrap the contents in the tarball file. eg: for mapping (/tmp/xyz/1.conf -> conf/1.conf)
    *   and wrappedIn = archived/simple-app-v0.0.1-SNAPSHOT, it will produce
    *   archived/simple-app-v0.0.1-SNAPSHOT/conf/1.conf in the tarball file
    * @param compress
    *   the compression function
    */
  def mkTarball[T <: OutputStream](
    mappings: Seq[(Path, SubPath)],
    output: Path,
    wrappedIn: Option[SubPath]
  )(
    compress: OutputStream => CompressorOutputStream[T]
  ): Unit =
    Using.Manager { use =>
      val fos = use(new FileOutputStream(output.toIO))
      val bos = use(new BufferedOutputStream(fos))
      val tos = use(new TarArchiveOutputStream(compress(bos)))
      mappings.foreach { case (from, to) => addPathToTar(from, to, tos, wrappedIn) }
    }

  // recursively add a Path which can be a file or a directory to the given TarArchiveOutputStream.
  // if the path is a single file, just add it to the output stream,
  // if the path is a directory, add all of the contents under the path to the output stream
  private def addPathsToTarball(path: Path, outputStream: OutputStream, wrappedIn: Option[SubPath]): Unit = {
    Using.resource(new TarArchiveOutputStream(outputStream)) { tarArchive =>
      path match {
        case _ if os.isDir(path) =>
          os.walk(path).foreach { p =>
            addPathToTar(
              path = p,
              target = p.subRelativeTo(path),
              tarArchive = tarArchive,
              wrappedIn = wrappedIn
            )
          }
        case _ if os.isFile(path) =>
          addPathToTar(
            path = path,
            target = path.subRelativeTo(path / os.up),
            tarArchive = tarArchive,
            wrappedIn = wrappedIn
          )
        // this should not happen
        case _ => throw new IllegalStateException(s"path $path is not directory nor file.")
      }
    }
  }

  // add single Path which can be a file or a directory to the given TarArchiveOutputStream, not recursively
  private def addPathToTar(
    path: Path,
    target: SubPath,
    tarArchive: TarArchiveOutputStream,
    wrappedIn: Option[SubPath]
  ): Unit = {
    val file: File = path.toIO
    // Create entry name relative to parent file path
    val entryPath = wrappedIn.map(_ / target).getOrElse(target)
    val tarEntry = new TarArchiveEntry(file, entryPath.toString)
    SOURCE_DATE_EPOCH.foreach { d =>
      // set the last modified time as needed
      tarEntry.setLastModifiedTime(FileTime.from(d, TimeUnit.SECONDS))
    }
    // set the executable permission for directires and files under the /bin directory
    // the source file might have "0700" permissions, so adjust them to the pre set permissions
    // TODO: or use (path / os.up).baseName == "bin" ?
    if (file.canExecute || os.isDir(path)) {
      tarEntry.setMode(EXEXCUTABLE_PERMSET)
    } else {
      // for normal files
      tarEntry.setMode(DEAFULT_PERMSET)
    }
    // add tar ArchiveEntry
    tarArchive.putArchiveEntry(tarEntry)
    if (os.isFile(path)) {
      Using.resource(new BufferedInputStream(new FileInputStream(file))) { inputStream =>
        // Write file content to archive
        transfer(inputStream, tarArchive, false)
      }
    }
    tarArchive.closeArchiveEntry()
  }

  /** Reads all bytes from an input stream and writes them to an output stream.
    *
    * TODO: use in.transferTo(out) once we drop JAVA 8 support.
    */
  private def transfer(in: InputStream, out: OutputStream, close: Boolean) = {
    try {
      val buffer = new Array[Byte](BUFFER_SIZE)

      @scala.annotation.tailrec
      def read(): Unit = {
        val byteCount = in.read(buffer)
        if (byteCount >= 0) {
          out.write(
            buffer,
            0,
            byteCount
          )
          read()
        }
      }

      read()
    } finally {
      if (close) in.close()
    }
  }

}
