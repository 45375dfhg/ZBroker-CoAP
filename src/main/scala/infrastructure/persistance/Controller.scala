package infrastructure.persistance

import zio.console._

// TODO: SETUP!
// runtime args need to be able to skip this and load from memory!
object Controller {

  val boot = putStrLn("The application is starting. Settings need to be configured.")
}