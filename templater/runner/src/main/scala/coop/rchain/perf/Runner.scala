package coop.rchain.perf

import akka.actor.ActorSystem
import com.google.protobuf.empty.Empty
import coop.rchain.casper.protocol.DeployServiceGrpc.DeployServiceBlockingClient
import coop.rchain.casper.protocol.{
  DeployData,
  DeployServiceGrpc,
  DeployServiceResponse
}
import io.gatling.commons.util.RoundRobin
import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.CoreComponents
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{Protocol, ProtocolComponents, ProtocolKey}
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.grpc.ManagedChannelBuilder

import scala.util.{Failure, Success, Try}

object Runner {}

import io.gatling.core.Predef._

object Propose {
  def propose(session: Session)(
      client: DeployServiceBlockingClient): (Session, DeployServiceResponse) = {
    (session.remove("client"), client.createBlock(Empty()))
  }
}

object Deploy {
  def deploy(session: Session)(
      client: DeployServiceBlockingClient): (Session, DeployServiceResponse) = {
    val (contractName, contract): (String, String) = session("contract")
      .as[(String, String)]
    val d = DeployData()
      .withTimestamp(System.currentTimeMillis())
      .withTerm(contract)
      .withFrom("0x1")
      .withPhloLimit(0)
      .withPhloPrice(0)
      .withNonce(0)
    (session.set("client", client), client.doDeploy(d))
  }
}

class RNodeRequestAction(
    val actionName: String,
    val request: Session => DeployServiceBlockingClient => (Session,
                                                            DeployServiceResponse),
    val statsEngine: StatsEngine,
    val next: Action,
    val clients: List[DeployServiceBlockingClient],
    val pool: Iterator[DeployServiceBlockingClient])
    extends ExitableAction
    with NameGen {

  override def name: String = actionName

  private def requestName(cn: String) = s"$cn-$name"

  def logResponse(timings: ResponseTimings,
                  ns: Session,
                  msg: String,
                  ok: Status,
                  contractName: String) = {
    statsEngine.logResponse(ns,
                            requestName(contractName),
                            timings,
                            ok,
                            None,
                            Some(msg),
                            List(contractName))
    ns
  }

  override def execute(session: Session): Unit = recover(session) {
    val (contractName, contract): (String, String) =
      session("contract").as[(String, String)]
    val client =
      session("client").asOption[DeployServiceBlockingClient].getOrElse {
        pool.next()
      }
    val start = System.currentTimeMillis()
    io.gatling.commons.validation.Success("").map { _ =>
      val r = Try {
        request(session)(client)
      }
      val timings = ResponseTimings(start, System.currentTimeMillis())

      r match {
        case Failure(exception) =>
          exception.printStackTrace()
          next ! logResponse(timings,
                             session.markAsFailed,
                             exception.getMessage,
                             KO,
                             contractName)
        case Success((ns, DeployServiceResponse(false, msg))) =>
          next ! logResponse(timings, ns.markAsFailed, msg, KO, contractName)
        case Success((ns, DeployServiceResponse(true, msg))) =>
          next ! logResponse(timings, ns.markAsSucceeded, msg, OK, contractName)
      }
    }
  }
}

object RNodeActionDSL {
  // Note that these two actions work in tandem. a deploy will request a client and propose will utilise it.
  // if proposes and deploys don't work in a balanced way weird behaviour might be observed on rnode.
  def propose(): RNodeActionBuilder = {
    new RNodeActionBuilder {
      override val execute = Propose.propose
      override val actionName: String = "propose"
    }
  }

  def deploy(): RNodeActionBuilder = {
    new RNodeActionBuilder {
      override val execute = Deploy.deploy
      override val actionName: String = "deploy"
    }
  }
}
abstract class RNodeActionBuilder extends ActionBuilder {
  val execute: Session => DeployServiceBlockingClient => (Session,
                                                          DeployServiceResponse)
  val actionName: String

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val rnodeComponents =
      protocolComponentsRegistry.components(RNodeProtocol.RNodeProtocolKey)
    new RNodeRequestAction(actionName,
                           execute,
                           coreComponents.statsEngine,
                           next,
                           rnodeComponents.clients,
                           rnodeComponents.pool)
  }
}

case class RNodeProtocol(hosts: List[(String, Int)]) extends Protocol {}

object RNodeProtocol {

  def createFor(hostStrings: List[String]): RNodeProtocol = {
    val mapped = hostStrings.map { host =>
      val s = host.split(":")
      assert(s.size == 2,
             s"Invalid host string $s, expected format is address:port")
      val address = s(0)
      val port = s(1).toInt
      (address, port)

    }
    RNodeProtocol(mapped)
  }

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
          val clients: List[DeployServiceBlockingClient] =
            rnodeProtocol.hosts.map {
              case (host, port) =>
                val channel = ManagedChannelBuilder
                  .forAddress(host, port)
                  .usePlaintext(true)
                  .build
                DeployServiceGrpc.blockingStub(channel)
            }
          val pool = RoundRobin(clients.toIndexedSeq)
          RNodeComponents(rnodeProtocol, clients, pool)
        }
    }
  }
}

case class RNodeComponents(rnodeProtocol: RNodeProtocol,
                           clients: List[DeployServiceBlockingClient],
                           pool: Iterator[DeployServiceBlockingClient])
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
