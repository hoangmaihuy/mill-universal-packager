import zio.*

object SecondMainApp extends ZIOAppDefault {

  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] = {
    ZIO.succeed(println("Hello, Second World!"))
  }

}
