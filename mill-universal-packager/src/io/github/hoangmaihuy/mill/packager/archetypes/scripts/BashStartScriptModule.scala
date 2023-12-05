package io.github.hoangmaihuy.mill.packager.archetypes.scripts

import java.io.File
import mill._

import io.github.hoangmaihuy.mill.packager.archetypes.{JavaAppPackagingModule, TemplateWriter}
import io.github.hoangmaihuy.mill.packager._

/** == Bash StartScript Plugin ==
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
  override protected[this] val forwarderTemplateName = "bash-forwarder-template"

  val appIniLocation = "${app_home}/../conf/application.ini"

  override protected[this] val scriptSuffix: String = ""
  override protected[this] val eol: String = "\n"
  override protected[this] val keySurround: String => String = TemplateWriter.bashFriendlyKeySurround
  override protected[this] val executableBitValue: Boolean = true

  protected[this] case class BashScriptConfig(
    override val executableScriptName: String,
    override val scriptClasspath: Seq[String],
    override val replacements: Seq[(String, String)],
    override val templateLocation: File
  ) extends ScriptConfig {
    override def withScriptName(scriptName: String): BashScriptConfig = copy(executableScriptName = scriptName)
  }

  override protected[this] type SpecializedScriptConfig = BashScriptConfig

  def bashScriptTemplates = T.source(millSourcePath / "templates" / bashTemplate)

  def bashScriptConfigLocation: T[Option[String]] = T { Some(appIniLocation) }

  def bashScriptExtraDefines: T[Seq[String]] = T { Seq.empty[String] }

  def bashScriptDefines: T[Seq[String]] = T {
    Defines(
      scriptClasspath(),
      bashScriptConfigLocation(),
      bundledJvmLocation()
    ) ++ bashScriptExtraDefines()
  }

  def bashScriptReplacements: T[Seq[(String, String)]] = T {
    generateScriptReplacements(bashScriptDefines())
  }

  def bashScriptEnvConfigLocation: T[Option[String]] = T { Option.empty[String] }

  def bashScriptMappings: T[Seq[(os.Path, os.SubPath)]] = T {
    val discoveredMainClasses = zincWorker().worker().discoverMainClasses(compile())
    generateStartScripts(
      BashScriptConfig(
        executableScriptName = executableScriptName(),
        scriptClasspath = scriptClasspath(),
        replacements = bashScriptReplacements(),
        templateLocation = bashScriptTemplates().path.toIO
      ),
      mainClass(),
      discoveredMainClasses,
      T.dest,
      T.log
    )
  }

  private[this] def generateScriptReplacements(defines: Seq[String]): Seq[(String, String)] = {
    val defineString = defines mkString "\n"
    Seq("template_declares" -> defineString)
  }

  /** Bash defines
    */
  object Defines {

    /** Creates the block of defines for a script.
      *
      * @param appClasspath A sequence of relative-locations (to the lib/ folder) of jars
      *                     to include on the classpath.
      * @param configFile An (optional) filename from which the script will read arguments.
      */
    def apply(appClasspath: Seq[String], configFile: Option[String], bundledJvm: Option[String]): Seq[String] =
      (configFile map configFileDefine).toSeq ++
        Seq(makeClasspathDefine(appClasspath)) ++
        (bundledJvm map bundledJvmDefine).toSeq

    private[this] def makeClasspathDefine(cp: Seq[String]): String = {
      val fullString = cp map (n =>
        if (n.startsWith("/")) n
        else "$lib_dir/" + n
      ) mkString ":"
      "declare -r app_classpath=\"" + fullString + "\"\n"
    }

    private[this] def configFileDefine(configFile: String) =
      "declare -r script_conf_file=\"%s\"" format configFile

    private[this] def bundledJvmDefine(bundledJvm: String) =
      """declare -r bundled_jvm="$(dirname "$app_home")/%s"""" format bundledJvm

  }

  private[this] def usageMainClassReplacement(mainClasses: Seq[String]): String =
    if (mainClasses.nonEmpty)
      mainClasses.mkString(
        "Available main classes:\n\t",
        "\n\t",
        ""
      )
    else
      ""

  override protected[this] def createReplacementsForMainScript(
    mainClass: String,
    mainClasses: Seq[String],
    config: SpecializedScriptConfig
  ): Seq[(String, String)] =
    Seq(
      "app_mainclass" -> mainClass,
      "available_main_classes" -> usageMainClassReplacement(mainClasses)
    ) ++ config.replacements

}
