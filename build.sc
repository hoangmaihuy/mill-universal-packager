import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`

import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalalib.api.ZincWorkerUtil.scalaNativeBinaryVersion

import de.tobiasroeser.mill.integrationtest._
import io.kipp.mill.ci.release.CiReleaseModule
import io.kipp.mill.ci.release.SonatypeHost

def millVersionFile = T.source(PathRef(os.pwd / ".mill-version"))

def millVersion = T {
  os.read(millVersionFile().path).trim
}

object Versions {
  lazy val scala = "2.13.12"
}

object `mill-universal-packager` extends ScalaModule with CiReleaseModule {

  override def scalaVersion = Versions.scala

  override def sonatypeHost = Some(SonatypeHost.s01)

  override def versionScheme: T[Option[VersionScheme]] = T(Option(VersionScheme.EarlySemVer))

  override def pomSettings = PomSettings(
    description = "Universal packaging for Mill",
    organization = "io.github.hoangmaihuy",
    url = "https://github.com/hoangmaihuy/mill-universal-packager",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github(owner = "hoangmaihuy", repo = "mill-universal-packager"),
    developers = Seq(Developer("hoangmaihuy", "Hoang Mai", "https://github.com/hoangmaihuy"))
  )

  override def artifactName = "mill-universal-packager"

  override def artifactSuffix =
    "_mill" + scalaNativeBinaryVersion(millVersion()) +
      super.artifactSuffix()

  override def scalacOptions = Seq("-Ywarn-unused", "-deprecation")

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-scalalib:${millVersion()}"
  )

}

object itest extends MillIntegrationTestModule {

  override def millTestVersion = millVersion

  override def pluginsUnderTest = Seq(`mill-universal-packager`)

  def testBase = millSourcePath / "src"

  override def testInvocations = Seq(
    PathRef(testBase / "example") -> Seq(
      TestInvocation.Targets(Seq("universalStage")),
      TestInvocation.Targets(Seq("universalPackage"))
    )
  )

}
