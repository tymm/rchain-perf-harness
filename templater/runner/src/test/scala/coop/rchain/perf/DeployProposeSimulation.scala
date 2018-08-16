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
      |@["LinkedList", "range"]!(1, 100, "myList") |
      |contract @"double"(@x, ret) = { ret!(2 * x) } |
      |contract @"sum"(@x, @y, ret) = { ret!(x + y) } |
      |for(@myList <- @"myList"){
      |  @["LinkedList", "map"]!(myList, "double", "newList") |
      |  for(@newList <- @"newList") {
      |    @["LinkedList", "fold"]!(newList, 0, "sum", "result") |
      |    for(@result <- @"result"){ @"stdout"!(result) }
      |  }
      |}
    """.stripMargin

  val conf = ConfigFactory.load()
  val rnodes = conf.getStringList("rnodes").asScala.toList

  val contract = Option(System.getProperty("contract"))
    .map { s =>
      (Paths.get(s).getFileName.toString, Source.fromFile(s).mkString)
    }
    .getOrElse(("sum-list", defaultTerm))

  println(s"will run simulation on ${rnodes.mkString(", ")}, contract:")
  println("-------------------------------")
  println(contract)
  println("-------------------------------")

  val protocol = RNodeProtocol.createFor(rnodes)

  val scn = scenario("DeployProposeSimulation")
    .foreach(List(contract), "contract") {
      repeat(1) {
        repeat(5) {
          exec(deploy())
        }
          .exec(propose())
      }
    }

  setUp(
    scn.inject(rampUsers(10) over (5 seconds))
  ).protocols(protocol)
}
