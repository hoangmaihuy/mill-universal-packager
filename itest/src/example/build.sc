import $file.plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`

import mill._
import mill.scalalib._
import de.tobiasroeser.mill.vcs.version.VcsVersion

import io.github.hoangmaihuy.mill.packager.archetypes._

object example extends RootModule with ScalaModule with JavaAppPackagingModule {

  override def artifactName = "example"

  override def scalaVersion = "3.3.1"

  override def mainClass = Some("MainApp")

  override def packageVersion = VcsVersion.vcsState().format()

  override def ivyDeps = super.ivyDeps() ++ Seq(
    ivy"dev.zio::zio:2.0.19"
  )

  override def topLevelDirectory = T { Some("example-app") }

}
