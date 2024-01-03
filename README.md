# mill-universal-packager

Universal packaging for Java application, ported from [sbt-native-packager](https://github.com/sbt/sbt-native-packager).

Currently, `mill-universal-packager` only supports universal `stage` with Bash start script and the corresponding `zip` archive.

## Usage

```scala
// build.sc
import $ivy.`io.github.hoangmaihuy::mill-universal-packager::<latest-version>`

import io.github.hoangmaihuy.mill.packager.archetypes.JavaAppPackagingModule

object example extends JavaAppPackagingModule {
	override def packageVersion = "0.1.0"
}
```

Run `universalStage` command

```bash
> mill example.universalStage
```

The output directory can be found at `universalStage.dest` in Mill's `out` folder.

Run `universalPackage` command

```bash
> mill example.universalPackage
```

Zip package will be saved at `universalPackage.dest` in Mill's `out` folder.

## Configuration

`mill-universal-packager` configurations are similar to `JavaAppPackaging` and `Universal` plugin keys
from `sbt-native-packager`.

| Key                  | Type                       | Default                                   | Description                                                            |
|----------------------|----------------------------|-------------------------------------------|------------------------------------------------------------------------|
| packageVersion       | String                     |                                           | Package version to use in `packageName`                                |
| packageName          | String                     | `artifactName() + "-" + packageVersion()` | Package file name                                                      | 
| maintainer           | String                     |                                           | Maintainer name                                                        
| executableScriptName | String                     | `artifactName()`                          | Executable script file name                                            |
| topLevelDirectory    | Option[String]             | `None`                                    | Top level directory in archive file                                    |
| universalSources     | PathRef                    | `millSourcePath / "universal"`            | Files to be included in archive, for example `.conf`, `.ini` files,... |
| universalMappings    | Seq[(os.Path, os.SubPath)] |                                           | A list of mappings from original path to archive path                  | 

## More information

This plugin was ported from ported from [sbt-native-packager](https://github.com/sbt/sbt-native-packager). Core functions were copied from `sbt-native-packager` with some modifications to work with Mill.

## Licenses

This software is released under the Apache License 2.0. More information in the file LICENSE distributed with this project.
