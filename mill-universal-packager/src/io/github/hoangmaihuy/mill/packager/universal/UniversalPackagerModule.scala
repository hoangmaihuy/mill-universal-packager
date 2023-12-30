package io.github.hoangmaihuy.mill.packager.universal

import mill._
import io.github.hoangmaihuy.mill.packager._
import os.Path

trait UniversalPackagerModule extends PackagerModule {

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

  def universalPackage: T[PathRef] = T {
    val zip = T.dest / (packageName() + ".zip")
    val mappings = universalMappings()
    // add top level directory if defined
    val m2 = topLevelDirectory().map { dir =>
      mappings.map { case (f, p) => f -> (os.sub / dir / p) }
    } getOrElse (mappings)
    ZipHelper.zip(m2, zip)
    PathRef(zip)
  }

  // Create a local directory with all the files laid out as they would be in the final distribution
  def stage: T[PathRef] = T {
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

}
