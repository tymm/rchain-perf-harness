package coop.rchain.perf

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

  val host = Option(System.getProperty("host")).getOrElse("localhost")
  val port = Integer.getInteger("port", 40401)
  val contract = Option(System.getProperty("contract"))
    .map(Source.fromFile(_).mkString)
    .getOrElse(defaultTerm)

  println(s"will run simulation on $host:$port, contract:")
  println("-------------------------------")
  println(contract)
  println("-------------------------------")

  val protocol = RNodeProtocol(host, port)

  val scn = scenario("DeployProposeSimulation")
    .repeat(100) {
      exec(deploy(contract))
        .pause(1)
        .exec(propose())
        .pause(1)
    }

  setUp(
    scn.inject(atOnceUsers(40))
  ).protocols(protocol)
}
