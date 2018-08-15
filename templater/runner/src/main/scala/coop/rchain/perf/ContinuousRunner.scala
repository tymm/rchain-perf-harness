package coop.rchain.perf

import java.nio.file.{Files, Paths}

import io.gatling.app.Gatling
import io.gatling.core.Predef.{Simulation, scenario}
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingPropertiesBuilder

import scala.collection.JavaConverters._
import scala.io.Source
import scala.concurrent.duration._
import scala.language.postfixOps

object ContinuousRunner {
  def main(args: Array[String]): Unit = {
    val simClass = classOf[ContinuousSimulation].getName

    val props = new GatlingPropertiesBuilder
    props.simulationClass(simClass)
    Gatling.fromMap(props.build)
  }

  class ContinuousSimulation extends Simulation {
    import RNodeActionDSL._

    import java.nio.file.FileSystems

    val rhoMatcher = FileSystems.getDefault.getPathMatcher("glob:**.rho")

    val contractsPath = System.getProperty("path")
    val hosts = System.getProperty("hosts")
    val sessions = Integer.getInteger("sessions", 1)
    val deploy2ProposeRatio: Int = Integer.getInteger("ratio", 1)

    val basePath = Paths.get(contractsPath)
    private val termsWithNames: List[(String, String)] = Files
      .walk(basePath)
      .filter(rhoMatcher.matches(_))
      .iterator()
      .asScala
      .map { p =>
        (basePath.relativize(p).toString, Source.fromFile(p.toFile).mkString)
      }
      .toList

    val protocol: RNodeProtocol =
      RNodeProtocol(hosts.split(" ").map((_, 40401)).toList)

    val scn = scenario("ContinuousSimulation").forever {
      foreach(termsWithNames, "contract") {
        repeat(deploy2ProposeRatio) {
          exec(deploy())
            .pause(1)
        }.exec(propose()).pause(1)
      }
    }

    setUp(
      scn.inject(rampUsers(sessions) over (5 seconds))
    ).protocols(protocol)
  }

}
