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

  /** Create an zip package file. The task will run universalStage() first, and then zip the stage output directory as
    * the result.
    */
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
