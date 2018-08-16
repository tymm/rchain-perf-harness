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
      |new loop, primeCheck in {
      |  contract loop(@x) = {
      |    match x {
      |      [] => Nil
      |      [head ...tail] => {
      |        new ret in {
      |          for (_ <- ret) {
      |            loop!(tail)
      |          } | primeCheck!(head, *ret)
      |        }
      |      }
      |    }
      |  } |
      |  contract primeCheck(@x, ret) = {
      |    match x {
      |      Nil => @"stdoutAck"!("Nil", *ret)
      |      ~{~Nil | ~Nil} => @"stdoutAck"!("Prime", *ret)
      |      _ => @"stdoutAck"!("Composite", *ret)
      |    }
      |  } |
      |  loop!([Nil, 7, 7 | 8, 9 | Nil, 9 | 10, Nil, 9])
      |}
      |
    """.stripMargin

  val conf = ConfigFactory.load()
  val rnodes = conf.getStringList("rnodes").asScala.toList

  val contract = Option(System.getProperty("contract"))
    .map { s =>
      (Paths.get(s).getFileName.toString, Source.fromFile(s).mkString)
    }
    .getOrElse(("check-prime", defaultTerm))

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
    scn.inject(atOnceUsers(10))
  ).protocols(protocol)
}
