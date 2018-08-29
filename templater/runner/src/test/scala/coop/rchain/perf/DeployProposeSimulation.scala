package coop.rchain.perf

import java.nio.file.Paths

import collection.JavaConverters._
import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef.{Simulation, atOnceUsers, scenario}
import io.gatling.core.Predef._

import scala.concurrent.duration._

import scala.io.Source

class DeployProposeSimulation extends Simulation {
  import RNodeActionDSL._
  val defaultTerm =
    """
      |contract @"dupe"(@depth) = {
      |  if (depth <= 0) { Nil } else { @"dupe"!(depth-1) | @"dupe"!(depth-1) | @"dupe"!(depth-1) | @"dupe"!(depth-1) | @"dupe"!(depth-1) | @"dupe"!(depth-1) | @"dupe"!(depth-1) | @"dupe"!(depth-1) | @"dupe"!(depth-1) | @"dupe"!(depth-1) }
      |} |
      |@"dupe"!(5)
    """.stripMargin

  val conf = ConfigFactory.load()
  val rnodes = conf.getStringList("rnodes").asScala.toList

  val contractsFromPath = Option(System.getProperty("path"))
    .map(Paths.get(_))
    .map(ContinuousRunner.getAllRhosFromPath)

  val contract = contractsFromPath.getOrElse(
    Option(System.getProperty("contract"))
      .map { s =>
        (Paths.get(s).getFileName.toString, Source.fromFile(s).mkString)
      }
      .getOrElse(("sum-list", defaultTerm)))

  println(s"will run simulation on ${rnodes.mkString(", ")}, contracts:")
  println("-------------------------------")
  println(contract)
  println("-------------------------------")

  val protocol = RNodeProtocol.createFor(rnodes)

  val scn = scenario("DeployProposeSimulation")
    .foreach(List(contract), "contract") {
      repeat(1) {
        repeat(1) {
          exec(deploy())
        }.exec(propose())
      }
    }

  setUp(
    scn.inject(rampUsers(1) over (5 seconds))
  ).protocols(protocol)
}
