package io.github.hoangmaihuy.mill.packager.universal

import java.io._
import java.io.InputStream

import scala.util.Using

import os.Path
import os.SubPath
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
import org.apache.commons.compress.compressors.CompressorOutputStream

/** Helper methods to package up files into compressed archives. */
object TarArchives {

  // buffer size used for reading and writing
  private val BUFFER_SIZE = 8192

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
  def mkTarball(
    input: Path,
    output: Path,
    wrappedIn: Option[SubPath]
  )(
    compress: OutputStream => CompressorOutputStream
  ): Unit = {
    Using.resource(compress(new BufferedOutputStream(new FileOutputStream(output.toIO)))) { bos =>
      addPathsToTarball(input, bos, wrappedIn)
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
  def mkTarball(
    mappings: Seq[(Path, SubPath)],
    out: Path,
    wrappedIn: Option[SubPath]
  )(
    compress: OutputStream => CompressorOutputStream
  ): Unit = {
    Using.resource(new TarArchiveOutputStream(compress(new BufferedOutputStream(new FileOutputStream(out.toIO))))) {
      tos =>
        mappings.foreach { case (from, to) =>
          addPathToTar(from, to, tos, wrappedIn)
        }
    }
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
        case _ =>
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
    // add tar ArchiveEntry
    tarArchive.putArchiveEntry(new TarArchiveEntry(file, entryPath.toString))
    if (os.isFile(path)) {
      Using.resource(new BufferedInputStream(new FileInputStream(file))) { inputStream =>
        // Write file content to archive
        copyInputStreamToOutputStream(inputStream, tarArchive)
      }
    }
    tarArchive.closeArchiveEntry()
  }

  /** Reads all bytes from an input stream and writes them to an output stream.
    *
    * TODO: use in.transferTo(out) when we dropped JAVA 8 support.
    */
  private def copyInputStreamToOutputStream(in: InputStream, out: OutputStream): Long = {
    var nread = 0L
    val buf = new Array[Byte](BUFFER_SIZE)
    var n = 0
    while ({ n = in.read(buf); n } > 0) {
      out.write(buf, 0, n)
      nread += n
    }
    nread
  }

}
