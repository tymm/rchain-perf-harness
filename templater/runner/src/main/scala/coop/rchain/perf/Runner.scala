package coop.rchain.perf

import akka.actor.ActorSystem
import com.google.protobuf.empty.Empty
import coop.rchain.casper.protocol.DeployServiceGrpc.DeployServiceBlockingClient
import coop.rchain.casper.protocol.{
  DeployData,
  DeployServiceGrpc,
  DeployServiceResponse
}
import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.CoreComponents
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{Protocol, ProtocolComponents, ProtocolKey}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

import scala.util.{Failure, Success, Try}

object Runner {}

import io.gatling.core.Predef._

case class RNodeProtocol(host: String, port: Int) extends Protocol {}

object Propose {
  def a(session: Session)(
      client: DeployServiceBlockingClient): DeployServiceResponse = {
    client.createBlock(Empty())
  }
}

object Deploy {
  def a(session: Session)(
      client: DeployServiceBlockingClient): DeployServiceResponse = {
    val (contractName, contract): (String, String) = session("contract")
      .as[(String, String)]
    val d = DeployData()
      .withTimestamp(System.currentTimeMillis())
      .withTerm(contract)
      .withFrom("0x1")
      .withPhloLimit(0)
      .withPhloPrice(0)
      .withNonce(0)
    client.doDeploy(d)
  }
}

class RNodeRequestAction(
    val actionName: String,
    val request: Session => DeployServiceBlockingClient => DeployServiceResponse,
    val statsEngine: StatsEngine,
    val next: Action,
    val rnodeProtocol: RNodeProtocol,
    val client: DeployServiceBlockingClient)
    extends ExitableAction
    with NameGen {
  override def name: String = s"req-$actionName-${rnodeProtocol.host}"

  override def execute(session: Session): Unit = recover(session) {
    val (contractName, contract): (String, String) =
      session("contract").as[(String, String)]
    val start = System.currentTimeMillis()
    io.gatling.commons.validation.Success("").map { _ =>
      val r = Try { request(session)(client) }
      val timings = ResponseTimings(start, System.currentTimeMillis())

      r match {
        case Failure(exception) =>
          exception.printStackTrace()
          statsEngine.logResponse(session,
                                  s"$name-$contractName",
                                  timings,
                                  KO,
                                  None,
                                  Some(exception.getMessage),
                                  List(contractName))
          next ! session.markAsFailed

        case Success(DeployServiceResponse(false, msg)) =>
          statsEngine.logResponse(session,
                                  s"$name-$contractName",
                                  timings,
                                  KO,
                                  None,
                                  Some(msg),
                                  List(contractName))
          next ! session.markAsFailed

        case Success(DeployServiceResponse(true, msg)) =>
          statsEngine.logResponse(session,
                                  s"$name-$contractName",
                                  timings,
                                  OK,
                                  None,
                                  Some(msg),
                                  List(contractName))
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
  val execute: Session => DeployServiceBlockingClient => DeployServiceResponse
  val actionName: String

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val rnodeComponents =
      protocolComponentsRegistry.components(RNodeProtocol.RNodeProtocolKey)
    new RNodeRequestAction(actionName,
                           execute,
                           coreComponents.statsEngine,
                           next,
                           rnodeComponents.rnodeProtocol,
                           rnodeComponents.client)
  }
}

object RNodeProtocol {
  val RNodeProtocolKey = new ProtocolKey {
    type Protocol = RNodeProtocol
    type Components = RNodeComponents

    def protocolClass: Class[io.gatling.core.protocol.Protocol] =
      classOf[RNodeProtocol]
        .asInstanceOf[Class[io.gatling.core.protocol.Protocol]]

    def defaultProtocolValue(
        configuration: GatlingConfiguration): RNodeProtocol =
      throw new IllegalStateException(
        "Can't provide a default value for RNodeProtocol")

    def newComponents(
        system: ActorSystem,
        coreComponents: CoreComponents): RNodeProtocol => RNodeComponents = {
      rnodeProtocol =>
        {
          val channel: ManagedChannel = ManagedChannelBuilder
            .forAddress(rnodeProtocol.host, rnodeProtocol.port)
            .usePlaintext(true)
            .build
          val client = DeployServiceGrpc.blockingStub(channel)
          RNodeComponents(rnodeProtocol, client)
        }
    }
  }
}

case class RNodeComponents(rnodeProtocol: RNodeProtocol,
                           client: DeployServiceBlockingClient)
    extends ProtocolComponents {

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
