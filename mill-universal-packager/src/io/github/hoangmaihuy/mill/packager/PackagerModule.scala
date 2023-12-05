package io.github.hoangmaihuy.mill.packager

import mill._
import mill.scalalib._

trait PackagerModule extends JavaModule {

  def packageName: T[String] = artifactName() + "-" + packageVersion()
  def packageVersion: T[String]

  def maintainer: T[String] = ""

  def executableScriptName: T[String] = artifactName()

}
