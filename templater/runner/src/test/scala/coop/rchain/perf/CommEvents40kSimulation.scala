package coop.rchain.perf
import java.nio.file.{Files, Paths}

import collection.JavaConverters._
import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef.{Simulation, atOnceUsers, scenario}
import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.io.Source

/*
 We expect to reach 40k comm events per second one day
 */
class CommEvents40kSimulation extends Simulation {

  import RNodeActionDSL._

  val iterationsCount = 100
  val path = Paths.get(Paths.get("").toAbsolutePath().normalize().toString(),
    "../../simulations/mvcepp/loop-mvcepp.rho")

  val conf = ConfigFactory.load()
  val rnodes = conf.getStringList("rnodes").asScala.toList

  val rhoContent = Source.fromFile(path.toUri).mkString
  val contracts = List((path.getFileName.toString, rhoContent))

  println(
    s"will run ${iterationsCount} iterations of minimal comm event producing program on ${rnodes
      .mkString(", ")}")
  println("-------------------------------")
  println(contracts)
  println("-------------------------------")

  val protocol = RNodeProtocol.createFor(rnodes)

  val scn = scenario("CommEvents40kSimulation")
    .foreach(contracts, "contract") {
      repeat(iterationsCount) {
        exec(deploy())
      }.exec(propose())
    }

  setUp(
    scn.inject(rampUsers(1) over (5 seconds))
  ).protocols(protocol)
}
