package io.github.hoangmaihuy.mill.packager.archetypes

import mill._

import io.github.hoangmaihuy.mill.packager.archetypes.scripts.BashStartScriptModule
import io.github.hoangmaihuy.mill.packager.universal.UniversalPackagerModule
import io.github.hoangmaihuy.mill.packager._

trait JavaAppPackagingModule extends UniversalPackagerModule with BashStartScriptModule {

  def bundledJvmLocation: T[Option[String]] = T { Option.empty[String] }

  private def moduleDepMappings: T[Seq[(os.Path, os.SubPath)]] = T.traverse(transitiveModuleDeps.distinct) { module =>
    T.task {
      module.jar().path -> os.sub / "lib" / (module.artifactName() + ".jar")
    }
  }

  private def ivyDepMappings: T[Seq[(os.Path, os.SubPath)]] = T {
    resolvedRunIvyDeps().toSeq.map { ivyDep =>
      ivyDep.path -> (os.sub / "lib" / ivyDep.path.last)
    }
  }

  /** The order of the classpath used at runtime for the bat/bash scripts. */
  def classpathMappings: T[Seq[(os.Path, os.SubPath)]] = T {
    ivyDepMappings() ++ moduleDepMappings()
  }

  /** A list of relative filenames (to the lib/ folder in the distribution) of what to include on the classpath. */
  def scriptClasspath: T[Seq[String]] = T {
    classpathMappings()
      .map { case (_, path) =>
        if (path.startsWith(os.sub / "lib")) {
          path.subRelativeTo(os.sub / "lib").relativeTo(os.sub)
        } else {
          path.relativeTo(os.sub)
        }
      }
      .map(_.toString())
  }

  override def universalMappings = T {
    super.universalMappings() ++ classpathMappings() ++ bashScriptMappings()
  }

  /** Constructs a jar name from components...(ModuleID/Artifact)
    */
  def makeJarName(
    artifactName: String,
    revision: String
  ): String = {
    artifactName + "-" +
      revision +
      ".jar"
  }

}
