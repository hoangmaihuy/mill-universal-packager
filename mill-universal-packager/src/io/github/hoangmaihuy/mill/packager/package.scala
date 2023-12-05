package io.github.hoangmaihuy.mill

package object packager {

  implicit val subPathRw: upickle.default.ReadWriter[os.SubPath] =
    upickle.default.readwriter[String].bimap(_.toString, os.SubPath(_))

}
