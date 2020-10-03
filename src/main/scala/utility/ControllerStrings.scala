package utility

import zio.console.putStrLn

object ControllerStrings {

  val print = putStrLn _

  val bootFromMemory = "Program is loading configuration from memory ..."

  val bootFromConsole = "No user selection detected. User will be prompted to configure settings ..."

  print(bootFromConsole)
}