package coop.rchain.perf

import akka.actor.ActorSystem
import com.google.protobuf.empty.Empty
import com.typesafe.config.ConfigException.ValidationFailed
import coop.rchain.casper.protocol.{DeployData, DeployServiceGrpc, DeployServiceResponse}
import coop.rchain.casper.protocol.DeployServiceGrpc.DeployServiceBlockingClient
import io.gatling.app.Gatling
import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.CoreComponents
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.{GatlingConfiguration, GatlingPropertiesBuilder}
import io.gatling.core.protocol.{Protocol, ProtocolComponents, ProtocolKey}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.gatling.http.protocol.HttpProtocol
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

import scala.io.Source
import scala.util.{Failure, Success, Try}

object Runner {

}

object GatlingRunner {
  def main(args: Array[String]) {
    val simClass = classOf[RNodeSimulation].getName

    val props = new GatlingPropertiesBuilder
    props.simulationClass(simClass)
    Gatling.fromMap(props.build)
  }
}

import io.gatling.core.Predef._
import io.gatling.http.Predef._


class RNodeSimulation extends Simulation {
  import RNodeActionDSL._
  val protocol = RNodeProtocol()

  val scn = scenario("DeployProposeSimulation")
    .exec(deploy())
    .pause(1)
    .exec(propose())
    .pause(1)

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}

case class RNodeProtocol() extends Protocol {

}

object Propose {
  def a(client: DeployServiceBlockingClient): DeployServiceResponse = {
    client.createBlock(Empty())
  }
}

object Deploy {
  val term =
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

  def a(client: DeployServiceBlockingClient): DeployServiceResponse = {
    val d = DeployData()
                .withTimestamp(System.currentTimeMillis())
                .withTerm(term)
                .withFrom("0x1")
                .withPhloLimit(0)
                .withPhloPrice(0)
                .withNonce(0)
    client.doDeploy(d)
  }
}

class RNodeRequestAction(val actionName: String, val execute: DeployServiceBlockingClient => DeployServiceResponse, val statsEngine: StatsEngine, val next: Action, val client: DeployServiceBlockingClient) extends ExitableAction with NameGen {
  override def name: String = genName(s"rnodeRequest-$actionName")

  override def execute(session: Session): Unit = recover(session) {
    val start = System.currentTimeMillis()
    io.gatling.commons.validation.Success("").map { _ =>
      val r = Try { execute(client) }
      val timings = ResponseTimings(start, System.currentTimeMillis())

      r match {
        case Failure(exception) =>
          statsEngine.logResponse(session, name, timings, KO, None, Some(exception.getMessage))
          next ! session.markAsFailed

        case Success(DeployServiceResponse(false, msg)) =>
          statsEngine.logResponse(session, name, timings, KO, None, Some(msg))
          next ! session.markAsFailed

        case Success(DeployServiceResponse(true, msg)) =>
          statsEngine.logResponse(session, name, timings, OK, None, Some(msg))
          next ! session.markAsSucceeded
      }
    }
  }
}

object RNodeActionDSL {
  def propose(): RNodeActionBuilder = {
    new RNodeActionBuilder {
      override val execute = Propose.a
      override val actionName: String = "propose"
    }
  }

  def deploy(): RNodeActionBuilder = {
    new RNodeActionBuilder {
      override val execute = Deploy.a
      override val actionName: String = "deploy"
    }
  }
}
abstract class RNodeActionBuilder extends ActionBuilder {
  val execute: DeployServiceBlockingClient => DeployServiceResponse
  val actionName: String

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val rnodeComponents = protocolComponentsRegistry.components(RNodeProtocol.RNodeProtocolKey)
    new RNodeRequestAction(actionName, execute, coreComponents.statsEngine, next, rnodeComponents.client)
  }
}

object RNodeProtocol {
  val RNodeProtocolKey = new ProtocolKey {
    type Protocol = RNodeProtocol
    type Components = RNodeComponents

    def protocolClass: Class[io.gatling.core.protocol.Protocol] = classOf[RNodeProtocol].asInstanceOf[Class[io.gatling.core.protocol.Protocol]]

    def defaultProtocolValue(configuration: GatlingConfiguration): RNodeProtocol =
      throw new IllegalStateException("Can't provide a default value for RNodeProtocol")

    def newComponents(system: ActorSystem, coreComponents: CoreComponents): RNodeProtocol => RNodeComponents = {
      val channel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 40401).usePlaintext(true).build
      val client = DeployServiceGrpc.blockingStub(channel)
      rnodeProtocol => RNodeComponents(rnodeProtocol, client)
    }
  }
}

case class RNodeComponents(rnodeProtocol: RNodeProtocol, client: DeployServiceBlockingClient) extends ProtocolComponents {

  def onStart: Option[Session => Session] = {
    Some(s => {
      println("staring session")
      s
    })
  }
  def onExit: Option[Session => Unit] = {
    Some(s => {
      println("stopping session")
    })
  }
}