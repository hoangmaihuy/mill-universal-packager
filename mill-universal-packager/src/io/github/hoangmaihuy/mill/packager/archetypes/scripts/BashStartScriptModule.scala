package io.github.hoangmaihuy.mill.packager.archetypes.scripts

import java.io.File
import mill.*

import io.github.hoangmaihuy.mill.packager.archetypes.{JavaAppPackagingModule, TemplateWriter}

/** ==Bash StartScript Plugin==
  *
  * This plugins creates a start bash script to run an application built with the
  * [[io.github.hoangmaihuy.mill.packager.archetypes.JavaAppPackagingModule]].
  */
trait BashStartScriptModule extends Module with CommonStartScriptGenerator { self: JavaAppPackagingModule =>

  /** Name of the bash template if user wants to provide custom one
    */
  val bashTemplate = "bash-template"

  /** Name of the bash forwarder template if user wants to provide custom one
    */
  override protected val forwarderTemplateName = "bash-forwarder-template"

  val appIniLocation = "${app_home}/../conf/application.ini"

  override protected val scriptSuffix: String = ""
  override protected val eol: String = "\n"
  override protected val keySurround: String => String = TemplateWriter.bashFriendlyKeySurround
  override protected val executableBitValue: Boolean = true

  protected case class BashScriptConfig(
    override val executableScriptName: String,
    override val scriptClasspath: Seq[String],
    override val replacements: Seq[(String, String)],
    override val templateLocation: File
  ) extends ScriptConfig {
    override def withScriptName(scriptName: String): BashScriptConfig = copy(executableScriptName = scriptName)
  }

  override protected type SpecializedScriptConfig = BashScriptConfig

  def bashScriptTemplates = Task.Source(moduleDir / "templates" / bashTemplate)

  def bashScriptConfigLocation: T[Option[String]] = Task { Some(appIniLocation) }

  def bashScriptExtraDefines: T[Seq[String]] = Task { Seq.empty[String] }

  def bashScriptDefines: T[Seq[String]] = Task {
    Defines(
      scriptClasspath(),
      bashScriptConfigLocation(),
      bundledJvmLocation()
    ) ++ bashScriptExtraDefines()
  }

  def bashScriptReplacements: T[Seq[(String, String)]] = Task {
    generateScriptReplacements(bashScriptDefines())
  }

  def bashScriptEnvConfigLocation: T[Option[String]] = Task { Option.empty[String] }

  def bashScriptMappings: T[Seq[(PathRef, os.SubPath)]] = Task {
    generateStartScripts(
      BashScriptConfig(
        executableScriptName = executableScriptName(),
        scriptClasspath = scriptClasspath(),
        replacements = bashScriptReplacements(),
        templateLocation = bashScriptTemplates().path.toIO
      ),
      mainClass(),
      allLocalMainClasses(),
      Task.dest,
      Task.log
    )
  }

  private def generateScriptReplacements(defines: Seq[String]): Seq[(String, String)] = {
    val defineString = defines mkString "\n"
    Seq("template_declares" -> defineString)
  }

  /** Bash defines
    */
  object Defines {

    /** Creates the block of defines for a script.
      *
      * @param appClasspath
      *   A sequence of relative-locations (to the lib/ folder) of jars to include on the classpath.
      * @param configFile
      *   An (optional) filename from which the script will read arguments.
      */
    def apply(appClasspath: Seq[String], configFile: Option[String], bundledJvm: Option[String]): Seq[String] =
      (configFile map configFileDefine).toSeq ++
        Seq(makeClasspathDefine(appClasspath)) ++
        (bundledJvm map bundledJvmDefine).toSeq

    private def makeClasspathDefine(cp: Seq[String]): String = {
      val fullString = cp map (n =>
        if (n.startsWith("/")) n
        else "$lib_dir/" + n
      ) mkString ":"
      "declare -r app_classpath=\"" + fullString + "\"\n"
    }

    private def configFileDefine(configFile: String) =
      "declare -r script_conf_file=\"%s\"" format configFile

    private def bundledJvmDefine(bundledJvm: String) =
      """declare -r bundled_jvm="$(dirname "$app_home")/%s"""" format bundledJvm

  }

  private def usageMainClassReplacement(mainClasses: Seq[String]): String =
    if (mainClasses.nonEmpty)
      mainClasses.mkString(
        "Available main classes:\n\t",
        "\n\t",
        ""
      )
    else
      ""

  override protected def createReplacementsForMainScript(
    mainClass: String,
    mainClasses: Seq[String],
    config: SpecializedScriptConfig
  ): Seq[(String, String)] =
    Seq(
      "app_mainclass" -> mainClass,
      "available_main_classes" -> usageMainClassReplacement(mainClasses)
    ) ++ config.replacements

}
