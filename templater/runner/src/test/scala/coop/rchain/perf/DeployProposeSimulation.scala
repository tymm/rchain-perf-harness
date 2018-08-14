package coop.rchain.perf

import collection.JavaConversions._
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef.{Simulation, atOnceUsers, scenario}
import io.gatling.core.Predef._

import scala.io.Source

class DeployProposeSimulation extends Simulation {
  import RNodeActionDSL._
  val defaultTerm =
    """
      |// This benchmark example runs N iterations recursively.
      |// Useful to measure RSpace performance.
      |
      |new LoopRecursive, stdout(`rho:io:stdout`) in {
      |  contract LoopRecursive(@count) = {
      |    match count {
      |    0 => stdout!("Done!")
      |    x => {
      |        stdout!("Step")
      |         | LoopRecursive!(x - 1)
      |      }
      |    }
      |  } |
      |  new myChannel in {
      |    LoopRecursive!(10000)
      |  }
      |}
    """.stripMargin

  val conf = ConfigFactory.load();
  val rnodes = conf.getStringList("rnodes").toList

  val contract = Option(System.getProperty("contract"))
    .map(Source.fromFile(_).mkString)
    .getOrElse(defaultTerm)

  println(s"will run simulation on ${rnodes.mkString(", ")}, contract:")
  println("-------------------------------")
  println(contract)
  println("-------------------------------")

  val protocols = rnodes.map(RNodeProtocol(_))

  val scn = scenario("DeployProposeSimulation")
    .repeat(2) {
      exec(deploy(contract))
        .pause(1)
        .exec(propose())
        .pause(1)
    }

  setUp(
    scn.inject(atOnceUsers(2))
  ).protocols(protocols)
}
