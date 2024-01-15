package io.github.hoangmaihuy.mill.packager.universal

import mill._
import io.github.hoangmaihuy.mill.packager._
import os.{Path, SubPath}

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.{GzipParameters, GzipCompressorOutputStream}
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream

trait UniversalPackagerModule extends PackagerModule {

  // the zstd compressed tarbal file extension, can be .tar.zstd, .tar.zst or .tzst
  def universalTarZstdExt: T[String] = T(".tar.zstd")
  // the gzip compressed tarbal file extension, can be .tar.gz or .tgz
  def universalTarGZExt: T[String] = T(".tar.gz")
  // the bzip2 compressed tarbal file extension, can be .tar.bz2 or .tbz
  def universalTarBzip2Ext: T[String] = T(".tar.bz2")
  // the xz compressed tarbal file extension, can be .tar.xz or .txz
  def universalTarXZExt: T[String] = T(".tar.xz")

  // the compression level for gzip
  def universalGzipCompressLevel: T[Int] = T(9)
  // the compression level for zstd
  def universalZstdCompressLevel: T[Int] = T(10)

  def topLevelDirectory: T[Option[String]] = T { Option.empty[String] }

  def universalSources: T[PathRef] = T.source(millSourcePath / "universal")

  def universalMappings: T[Seq[(os.Path, os.SubPath)]] = T {
    val sourceDirectory = universalSources().path
    if (os.exists(sourceDirectory)) {
      os
        .walk(sourceDirectory)
        .map { source =>
          source -> source.subRelativeTo(sourceDirectory)
        }
    } else {
      Seq.empty
    }
  }

  // mappings with or without the configured topLevelDirectory
  private def universalPackageMappings: T[Seq[(os.Path, os.SubPath)]] = T {
    val mappings = universalMappings()
    topLevelDirectory().map { dir =>
      mappings.map { case (f, p) => f -> (os.sub / dir / p) }
    } getOrElse (mappings)
  }

  private def universalTopLevelPath: T[Option[SubPath]] = T(topLevelDirectory().map(os.sub / _))

  /** Create an zip package file. The task will run universalStage() first, and then zip the stage output directory as
    * the result.
    */
  @deprecated("use universalStageZip instead", "v0.1.2")
  def universalPackage: T[PathRef] = T {
    val stagePath = universalStage().path
    val m2 = os.walk(stagePath).map { case p =>
      val targetSubPath = p.relativeTo(stagePath).asSubPath
      topLevelDirectory() match {
        case None      => p -> targetSubPath
        case Some(dir) => p -> os.sub / dir / targetSubPath
      }
    }
    val zip = T.dest / (packageName() + ".zip")
    ZipHelper.zip(m2, zip)
    PathRef(zip)
  }

  /** Create a local directory with all the files laid out as they would be in the final distribution.
    *
    * Note: the output "stage" directory should always have no top level directory.
    */
  def universalStage: T[PathRef] = T {
    // let's clean the T.dest directory first
    os.remove.all(T.dest)
    universalMappings().foreach { case (f, p) =>
      os.copy(
        from = f,
        to = Path(p, T.dest),
        createFolders = true,
        followLinks = true,
        replaceExisting = true,
        mergeFolders = true
      )
    }
    PathRef(T.dest)
  }

  /** Create an .zip package file. The task will run universalStage() first, and then zip the stage output directory as
    * the result.
    */
  def universalStagePackageZip: T[PathRef] = T {
    val stagePath = universalStage().path
    val m2 = os.walk(stagePath).map { case p =>
      val targetSubPath = p.relativeTo(stagePath).asSubPath
      topLevelDirectory() match {
        case None      => p -> targetSubPath
        case Some(dir) => p -> os.sub / dir / targetSubPath
      }
    }
    val zip = T.dest / (packageName() + ".zip")
    ZipHelper.zip(m2, zip)
    PathRef(zip)
  }

  /** Create a .tar.zstd package file. The task will run universalStage() first, and then tarball and compress the stage
    * output directory as the result.
    */
  def universalStagePackageTarZstd: T[PathRef] = T {
    val staged: PathRef = universalStage()
    val out: Path = T.dest / (packageName() + universalTarZstdExt())
    Archiver().mkTarball(staged.path, out, universalTopLevelPath())(
      new ZstdCompressorOutputStream(_, universalZstdCompressLevel())
    )
    T.log.info(s"Generated package: $out")
    PathRef(out)
  }

  /** Create a .tar.gz package file. The task will run universalStage() first, and then tarball and compress the stage
    * output directory as the result.
    */
  def universalStagePackageTarGzip: T[PathRef] = T {
    val staged: PathRef = universalStage()
    val out: Path = T.dest / (packageName() + universalTarGZExt())
    val parameter = new GzipParameters()
    parameter.setCompressionLevel(universalGzipCompressLevel())
    Archiver().mkTarball(staged.path, out, universalTopLevelPath())(new GzipCompressorOutputStream(_, parameter))
    T.log.info(s"Generated package: $out")
    PathRef(out)
  }

  /** Create a .tar.bz2 package file. The task will run universalStage() first, and then tarball and compress the stage
    * output directory as the result.
    */
  def universalStagePackageTarBzip2: T[PathRef] = T {
    val staged: PathRef = universalStage()
    val out: Path = T.dest / (packageName() + universalTarBzip2Ext())
    Archiver().mkTarball(staged.path, out, universalTopLevelPath())(new BZip2CompressorOutputStream(_))
    T.log.info(s"Generated package: $out")
    PathRef(out)
  }

  /** Create a .tar.xz package file. The task will run universalStage() first, and then tarball and compress the stage
    * output directory as the result.
    */
  def universalStagePackageTarXz: T[PathRef] = T {
    val staged: PathRef = universalStage()
    val out: Path = T.dest / (packageName() + universalTarXZExt())
    Archiver().mkTarball(staged.path, out, universalTopLevelPath())(new XZCompressorOutputStream(_))
    T.log.info(s"Generated package: $out")
    PathRef(out)
  }

  /** Create a .zip package file.
    */
  def universalPackageZip: T[PathRef] = T {
    val zip = T.dest / (packageName() + ".zip")
    ZipHelper.zip(universalPackageMappings(), zip)
    T.log.info(s"Generated package: $zip")
    PathRef(zip)
  }

  /** Create a .tar.zstd package file.
    */
  def universalPackageTarZstd: T[PathRef] = T {
    val out: Path = T.dest / (packageName() + universalTarZstdExt())
    Archiver().mkTarball(universalMappings(), out, universalTopLevelPath())(
      new ZstdCompressorOutputStream(_, universalZstdCompressLevel())
    )
    T.log.info(s"Generated package: $out")
    PathRef(out)
  }

  /** Create a .tar.gz package file.
    */
  def universalPackageTarGzip: T[PathRef] = T {
    val out: Path = T.dest / (packageName() + universalTarGZExt())
    val parameter = new GzipParameters()
    parameter.setCompressionLevel(universalGzipCompressLevel())
    Archiver().mkTarball(universalMappings(), out, universalTopLevelPath())(new GzipCompressorOutputStream(_, parameter))
    T.log.info(s"Generated package: $out")
    PathRef(out)
  }

  /** Create a .tar.bz2 package file.
    */
  def universalPackageTarBzip2: T[PathRef] = T {
    val out: Path = T.dest / (packageName() + universalTarBzip2Ext())
    Archiver().mkTarball(universalMappings(), out, universalTopLevelPath())(new BZip2CompressorOutputStream(_))
    PathRef(out)
  }

  /** Create a .tar.xz package file.
    */
  def universalPackageTarXz: T[PathRef] = T {
    val out: Path = T.dest / (packageName() + universalTarXZExt())
    Archiver().mkTarball(universalMappings(), out, universalTopLevelPath())(new XZCompressorOutputStream(_))
    T.log.info(s"Generated package: $out")
    PathRef(out)
  }

}
