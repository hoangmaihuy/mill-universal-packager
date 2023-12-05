package io.github.hoangmaihuy.mill.packager.archetypes.scripts

import java.io.File
import java.net.URL

import mill.api.Logger

import io.github.hoangmaihuy.mill.packager.archetypes.TemplateWriter

trait CommonStartScriptGenerator {

  /** Script destination in final package
    */
  protected[this] val scriptTargetFolder: String = "bin"

  /** Suffix to append to the generated script name (such as ".bat")
    */
  protected[this] val scriptSuffix: String

  /** Name of the forwarder template if user wants to provide custom one
    */
  protected[this] val forwarderTemplateName: String

  /** Line separator for generated scripts
    */
  protected[this] val eol: String

  /** keySurround for TemplateWriter.generateScript()
    */
  protected[this] val keySurround: String => String

  /** Set executable bit of the generated scripts to this value
    * @todo Does it work when building archives on hosts that do not support such permission?
    */
  protected[this] val executableBitValue: Boolean

  protected[this] def createReplacementsForMainScript(
    mainClass: String,
    mainClasses: Seq[String],
    config: SpecializedScriptConfig
  ): Seq[(String, String)]

  protected[this] trait ScriptConfig {
    val executableScriptName: String
    val scriptClasspath: Seq[String]
    val replacements: Seq[(String, String)]
    val templateLocation: File

    def withScriptName(scriptName: String): SpecializedScriptConfig
  }

  /** The type of specialized ScriptConfig.
    * This enables callback methods of the concrete plugin implementations
    * to use fields of config that only exist in their ScriptConfig specialization.
    */
  protected[this] type SpecializedScriptConfig <: ScriptConfig

  protected[this] def generateStartScripts(
    config: SpecializedScriptConfig,
    mainClass: Option[String],
    discoveredMainClasses: Seq[String],
    targetDir: os.Path,
    log: Logger
  ): Seq[(os.Path, os.SubPath)] =
    StartScriptMainClassConfig.from(mainClass, discoveredMainClasses) match {
      case NoMain =>
        log.error("You have no main class in your project. No start script will be generated.")
        Seq.empty
      case SingleMain(main) =>
        Seq(
          createMainScript(
            main,
            config,
            targetDir,
            Seq(main)
          )
        )
      case MultipleMains(mains) =>
        generateMainScripts(
          mains,
          config,
          targetDir,
          log
        )
      case ExplicitMainWithAdditional(main, additional) =>
        createMainScript(
          main,
          config,
          targetDir,
          discoveredMainClasses
        ) +:
          createForwarderScripts(
            config.executableScriptName,
            additional,
            targetDir,
            config,
            log
          )
    }

  private[this] def generateMainScripts(
    discoveredMainClasses: Seq[String],
    config: SpecializedScriptConfig,
    targetDir: os.Path,
    log: Logger
  ): Seq[(os.Path, os.SubPath)] = {
    val classAndScriptNames = ScriptUtils.createScriptNames(discoveredMainClasses)
    ScriptUtils.warnOnScriptNameCollision(classAndScriptNames, log)

    classAndScriptNames
      .find { case (_, script) =>
        script == config.executableScriptName
      }
      .map(_ => classAndScriptNames)
      .getOrElse(
        classAndScriptNames ++ Seq("" -> config.executableScriptName)
      ) // empty string to enforce the custom class in scripts
      .map { case (qualifiedClassName, scriptName) =>
        val newConfig = config.withScriptName(scriptName)
        createMainScript(
          qualifiedClassName,
          newConfig,
          targetDir,
          discoveredMainClasses
        )
      }
  }

  private[this] def mainScriptName(config: ScriptConfig): String =
    config.executableScriptName + scriptSuffix

  /** @param mainClass - Main class added to the java command
    * @param config - Config data for this script
    * @param targetDir - Target directory for this script
    * @return File pointing to the created main script
    */
  private[this] def createMainScript(
    mainClass: String,
    config: SpecializedScriptConfig,
    targetDir: os.Path,
    mainClasses: Seq[String]
  ): (os.Path, os.SubPath) = {
    val template = resolveTemplate(config.templateLocation)
    val replacements = createReplacementsForMainScript(
      mainClass,
      mainClasses,
      config
    )
    val scriptContent = TemplateWriter.generateScript(
      template,
      replacements,
      eol,
      keySurround
    )
    val scriptNameWithSuffix = mainScriptName(config)
    val script = targetDir / scriptTargetFolder / scriptNameWithSuffix

    os.write.over(script, scriptContent, createFolders = true)
    // TODO - Better control over this!
    script.toIO.setExecutable(executableBitValue)
    script -> os.sub / scriptTargetFolder / scriptNameWithSuffix
  }

  private[this] def resolveTemplate(defaultTemplateLocation: File): URL = {
    getClass.getResource(s"/packager/scripts/${defaultTemplateLocation.getName}")
  }

  private[this] def createForwarderScripts(
    executableScriptName: String,
    discoveredMainClasses: Seq[String],
    targetDir: os.Path,
    config: ScriptConfig,
    log: Logger
  ): Seq[(os.Path, os.SubPath)] = {
    val tmp = targetDir / scriptTargetFolder
    val forwarderTemplate = getClass.getResource(s"/packager/scripts/$forwarderTemplateName")
    val classAndScriptNames = ScriptUtils.createScriptNames(discoveredMainClasses)
    ScriptUtils.warnOnScriptNameCollision(classAndScriptNames :+ ("<main script>" -> mainScriptName(config)), log)
    classAndScriptNames.map { case (qualifiedClassName, scriptNameWithoutSuffix) =>
      val scriptName = scriptNameWithoutSuffix + scriptSuffix
      val file = tmp / scriptName

      val replacements = Seq("startScript" -> executableScriptName, "qualifiedClassName" -> qualifiedClassName)
      val scriptContent = TemplateWriter.generateScript(
        forwarderTemplate,
        replacements,
        eol,
        keySurround
      )

      os.write.over(file, scriptContent, createFolders = true)
      file.toIO.setExecutable(executableBitValue)
      file -> os.sub / scriptTargetFolder / scriptName
    }
  }

}
