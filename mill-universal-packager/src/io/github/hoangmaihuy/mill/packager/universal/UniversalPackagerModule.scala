package io.github.hoangmaihuy.mill.packager.universal

import mill._
import io.github.hoangmaihuy.mill.packager._

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

}
