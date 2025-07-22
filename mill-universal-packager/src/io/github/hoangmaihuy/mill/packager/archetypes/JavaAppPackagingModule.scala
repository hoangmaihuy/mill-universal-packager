package io.github.hoangmaihuy.mill.packager.archetypes

import mill.*

import io.github.hoangmaihuy.mill.packager.archetypes.scripts.BashStartScriptModule
import io.github.hoangmaihuy.mill.packager.universal.UniversalPackagerModule

trait JavaAppPackagingModule extends UniversalPackagerModule with BashStartScriptModule {

  def bundledJvmLocation: T[Option[String]] = Task { Option.empty[String] }

  private def moduleDepMappings: T[Seq[(PathRef, os.SubPath)]] = Task {
    Task.traverse(transitiveModuleDeps.distinct) { module =>
      Task.Anon {
        module.jar() -> os.sub / "lib" / (module.artifactName() + ".jar")
      }
    }()
  }

  private def mvnDepMappings: T[Seq[(PathRef, os.SubPath)]] = Task {
    resolvedRunMvnDeps().map { mvnDep =>
      mvnDep -> (os.sub / "lib" / mvnDep.path.last)
    }
  }

  /** The order of the classpath used at runtime for the bat/bash scripts. */
  def classpathMappings: T[Seq[(PathRef, os.SubPath)]] = Task {
    mvnDepMappings() ++ moduleDepMappings()
  }

  /** A list of relative filenames (to the lib/ folder in the distribution) of what to include on the classpath. */
  def scriptClasspath: T[Seq[String]] = Task {
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

  override def universalMappings = Task {
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
