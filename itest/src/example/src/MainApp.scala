import zio.*

object MainApp extends ZIOAppDefault {

  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] = {
    ZIO.succeed(println("Hello, World!"))
  }

}
