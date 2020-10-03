package utility

import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.Service
import zio.ZLayer
import zio.console.putStrLn

object ControllerStrings {

  val bootFromMemory = "Program is loading configuration from memory ..."

  val bootFromConsole = "No user selection detected. User will be prompted to configure settings ..."


}