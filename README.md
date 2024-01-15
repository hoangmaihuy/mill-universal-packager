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

## Tasks

| Task | Output destination | Description |
|------|-------------|--------------------------|
| `universalStage` | universalStage.dest | For making unarchived universal package. This is usefull for checking the contents before the final packaging and publishing. |
| `universalPackageZip` | universalPackageZip.dest | For making zip compressed universal package without `universalStage`. |
| `universalPackageTarZstd` | universalPackageTarZstd.dest | For making zstd compressed tarball universal package without `universalStage`. The zstd compression level can be configured by `universalZstdCompressLevel`. And the tarball file name extension can be configured by `universalTarZstdExt` with default value `.tar.zstd`.  |
| `universalPackageTarGzip` | universalPackageTarGzip.dest | For making gzip compressed tarball universal package, without `universalStage`. The gzip compression level can be configured by `universalGzipCompressLevel`. And the tarball file name extension can be configured by `universalTarGzipExt` with default value `.tar.gz`.  |
| `universalPackageTarBzip2` | universalPackageTarBzip2.dest | For making bzip2 compressed tarball universal package, without `universalStage`. The tarball file name extension can be configured by `universalTarBzip2Ext` with default value `.tar.bz2`.  |
| `universalPackageTarXz` | universalPackageTarXz.dest | For making xz compressed tarball universal package, without `universalStage`. The tarball file name extension can be configured by `universalTarXzExt` with default value `.tar.Xz`.  |
| `universalPackageStageZip` | universalPackageStageZip.dest | For making zip compressed universal package from the `universalStage`. |
| `universalPackageStageTarZstd` | universalPackageStageTarZstd.dest | For making zstd compressed tarball universal package from the `universalStage`. The zstd compression level can be configured by `universalZstdCompressLevel`. And the tarball file name extension can be configured by `universalTarZstdExt` with default value `.tar.zstd`.  |
| `universalPackageStageTarGzip` | universalPackageStageTarGzip.dest | For making gzip compressed tarball universal package from the `universalStage`. The gzip compression level can be configured by `universalGzipCompressLevel`. And the tarball file name extension can be configured by `universalTarGzipExt` with default value `.tar.gz`.  |
| `universalPackageStageTarBzip2` | universalPackageStageTarBzip2.dest | For making bzip2 compressed tarball universal package from the `universalStage`. The tarball file name extension can be configured by `universalTarBzip2Ext` with default value `.tar.bz2`.  |
| `universalPackageStageTarXz` | universalPackageStageTarXz.dest | For making xz compressed tarball universal package from the `universalStage`. The tarball file name extension can be configured by `universalTarXzExt` with default value `.tar.xz`.  |

## Configuration

`mill-universal-packager` configurations are similar to `JavaAppPackaging` and `Universal` plugin keys
from `sbt-native-packager`.

| Key                             | Type                       | Default                                   | Description                                                            |
|----------------------           |----------------------------|-------------------------------------------|------------------------------------------------------------------------|
| packageVersion                  | String                     |                                           | Package version to use in `packageName`                                |
| packageName                     | String                     | `artifactName() + "-" + packageVersion()` | Package file name                                                      |
| maintainer                      | String                     |                                           | Maintainer name
| executableScriptName            | String                     | `artifactName()`                          | Executable script file name                                            |
| topLevelDirectory               | Option[String]             | `None`                                    | Top level directory in archive file                                    |
| universalSources                | PathRef                    | `millSourcePath / "universal"`            | Files to be included in archive, for example `.conf`, `.ini` files,... |
| universalMappings               | Seq[(os.Path, os.SubPath)] |                                           | A list of mappings from original path to archive path                  |
| universalZstdCompressLevel      | Int, 1-22 (inclusive)      | 9                                         | Compression level for zstd tarball                                 |
| universalZipCompressLevel       | Int, 1-9 (inclusive)       | 9                                         | Compression level for gzip                                         |
| universalTarZstdExt             | String                     | `.tar.zstd`                               | The Zstd compressed tarbal file extension, can be `.tar.zstd`, .`tar.zst` or `.tzst` or any other else as you specified. |
| universalTarGZExt               | String                     | `.tar.gz`                                 | The Gzip compressed tarbal file extension, can be `.tar.gz` or `.tgz` or any other else as you specified.                |
| universalTarBzip2Ext            | String                     | `.tar.bz2`                                | The Bzip2 compressed tarbal file extension, can be `.tar.bz2` or `.tbz` or any other else as you specified.              |
| universalTarXZExt               | String                     | `.tar.xz`                                 | The XZ compressed tarbal file extension, can be `.tar.xz` or `.txz` or any other else as you specified.                  |

## More information

This plugin was ported from ported from [sbt-native-packager](https://github.com/sbt/sbt-native-packager). Core functions were copied from `sbt-native-packager` with some modifications to work with Mill.

## Licenses

This software is released under the Apache License 2.0. More information in the file LICENSE distributed with this project.
