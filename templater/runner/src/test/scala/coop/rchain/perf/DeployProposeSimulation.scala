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
      |new orExample in {
      |  contract orExample(@{record /\ {{@"name"!(_) | @"age"!(_) | _} \/ {@"nombre"!(_) | @"edad"!(_)}}}) = {
      |    match record {
      |      {@"name"!(name) | @"age"!(age) | _} => @"stdout"!(["Hello, ", name, " aged ", age])
      |      {@"nombre"!(nombre) | @"edad"!(edad) | _} => @"stdout"!(["Hola, ", nombre, " con ", edad, " años."])
      |    }
      |  } |
      |  orExample!(@"name"!("Joe") | @"age"!(40)) |
      |  orExample!(@"nombre"!("Jose") | @"edad"!(41))
      |}
      |
    """.stripMargin
//    """
//      |// This benchmark example runs N iterations recursively.
//      |// Useful to measure RSpace performance.
//      |
//      |new LoopRecursive, stdout(`rho:io:stdout`) in {
//      |  contract LoopRecursive(@count) = {
//      |    match count {
//      |    0 => stdout!("Done!")
//      |    x => {
//      |        stdout!("Step")
//      |         | LoopRecursive!(x - 1)
//      |      }
//      |    }
//      |  } |
//      |  new myChannel in {
//      |    LoopRecursive!(10000)
//      |  }
//      |}
//    """.stripMargin

  val conf = ConfigFactory.load()
  val rnodes = conf.getStringList("rnodes").asScala.toList

  val contract = Option(System.getProperty("contract"))
    .map { s =>
      (Paths.get(s).getFileName.toString, Source.fromFile(s).mkString)
    }
    .getOrElse(("recursive-loop", defaultTerm))

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
