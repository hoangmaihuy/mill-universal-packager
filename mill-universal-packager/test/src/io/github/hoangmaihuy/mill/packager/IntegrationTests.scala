package io.github.hoangmaihuy.mill.packager

import mill.testkit.IntegrationTester
import utest.*

import scala.runtime.stdLibPatches.Predef.assert

object IntegrationTests extends TestSuite {

  private def eval(tester: IntegrationTester)(command: String) = {
    val res = tester.eval(command)
    println(res.out)
    println(res.err)
    assert(res.isSuccess)
  }

  def tests: Tests = Tests {
    println("initializing mill-universal-packager.IntegrationTest.tests")

    test("integration") {
      val resourceFolder = os.Path(sys.env("MILL_TEST_RESOURCE_DIR"))
      val tester = new IntegrationTester(
        daemonMode = true,
        workspaceSourcePath = resourceFolder / "integration",
        millExecutable = os.Path(sys.env("MILL_EXECUTABLE_PATH"))
      )

      eval(tester)("example.universalStage")
      eval(tester)("example.universalPackage")
      eval(tester)("example.universalPackageZip")
      eval(tester)("example.universalPackageTarZstd")
      eval(tester)("example.universalPackageTarGzip")
      eval(tester)("example.universalPackageTarBzip2")
      eval(tester)("example.universalPackageTarXz")
      eval(tester)("example.universalStagePackageZip")
      eval(tester)("example.universalStagePackageTarZstd")
      eval(tester)("example.universalStagePackageTarGzip")
      eval(tester)("example.universalStagePackageTarBzip2")
      eval(tester)("example.universalStagePackageTarXz")
    }
  }

}
