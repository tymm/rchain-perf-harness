package coop.rchain.perf

import java.nio.file.Paths

import collection.JavaConverters._
import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef.{Simulation, atOnceUsers, scenario}
import io.gatling.core.Predef._

import scala.io.Source

class DeployProposeSimulation extends Simulation {
  import RNodeActionDSL._
  val defaultTerm =
    """
      |new stdout(`rho:io:stdout`), stdoutAck(`rho:io:stdoutAck`) in {
      |  @"north"!("knife") |
      |  @"south"!("spoon") |
      |  for (@knf <- @"north"; @spn <- @"south") {
      |    new ack in {
      |      stdoutAck!("Philosopher 1 Utensils: ", *ack) |
      |      for (_ <- ack) {
      |        stdoutAck!(knf, *ack) |
      |        for (_ <- ack) {
      |          stdoutAck!(", ", *ack) |
      |          for (_ <- ack) {
      |            stdoutAck!(spn, *ack) |
      |            for (_ <- ack) {
      |              stdout!("\n")
      |            }
      |          }
      |        }
      |      }
      |    } |
      |    @"north"!(knf) |
      |    @"south"!(spn)
      |  } |
      |  for (@spn <- @"south"; @knf <- @"north") {
      |    new ack in {
      |      stdoutAck!("Philosopher 2 Utensils: ", *ack) |
      |      for (_ <- ack) {
      |        stdoutAck!(knf, *ack) |
      |        for (_ <- ack) {
      |          stdoutAck!(", ", *ack) |
      |          for (_ <- ack) {
      |            stdoutAck!(spn, *ack) |
      |            for (_ <- ack) {
      |              stdout!("\n")
      |            }
      |          }
      |        }
      |      }
      |    } |
      |    @"north"!(knf) |
      |    @"south"!(spn)
      |  }
      |}
      |
    """.stripMargin

  val conf = ConfigFactory.load()
  val rnodes = conf.getStringList("rnodes").asScala.toList

  val contract = Option(System.getProperty("contract"))
    .map { s =>
      (Paths.get(s).getFileName.toString, Source.fromFile(s).mkString)
    }
    .getOrElse(("dining-philosophers", defaultTerm))

  println(s"will run simulation on ${rnodes.mkString(", ")}, contract:")
  println("-------------------------------")
  println(contract)
  println("-------------------------------")

  val protocol = RNodeProtocol.createFor(rnodes)

  val scn = scenario("DeployProposeSimulation")
    .foreach(List(contract), "contract") {
      repeat(20) {
        exec(deploy())
          .exec(propose())
      }
    }

  setUp(
    scn.inject(atOnceUsers(5))
  ).protocols(protocol)
}
