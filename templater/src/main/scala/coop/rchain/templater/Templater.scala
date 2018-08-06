package coop.rchain.templater

import java.nio.file.{Files, Path, Paths}

import org.clapper.scalasti.ST
import org.rogach.scallop.ScallopConf

object Templates {
  val bootstrap =
    """
      |[server]
      |host = "<c.host>"
      |port = <c.port>
      |metrics-port = <c.metricsPort>
      |http-port = <c.httpPort>
      |no-upnp = true
      |default-timeout = 1000
      |standalone = true
      |map-size = 200000000
      |data-dir = "./<c.name>/data"
      |
      |[grpc-server]
      |host = "grpc"
      |port = <c.grpcPort>
      |
      |[validators]
      |public-key = "118eb0ae2193c83e960b46c42026bd6e21d5cc02854881baedd296764d5cd743"
      |private-key = "6a22ddc9300ef7658c67cfcf8da358c6e7ffa75257361261ca47409d0c7ea6ee"
    """.stripMargin

  val validator =
    """
      |[server]
      |host = "<c.host>"
      |port = <c.port>
      |metrics-port = <c.metricsPort>
      |http-port = <c.httpPort>
      |no-upnp = true
      |default-timeout = 1000
      |map-size = 200000000
      |data-dir = "./<c.name>/data"
      |bootstrap = "rnode://7119ad2a26cdcde63dca675c4f2a4df85f404726@localhost:<bootstrapPort>"
      |
      |[grpc-server]
      |host = "grpc"
      |port = <c.grpcPort>
    """.stripMargin

  val start =
    """
      |./rnode -c <c.name>/config.toml run --http-port <c.httpPort>
    """.stripMargin

  val deploy =
    """
      |#!/bin/bash
      |
      |./rnode --grpc-port <grpcPort> deploy --from "0x1" --phlo-limit 0 --phlo-price 0 --nonce 0 ./contracts/tut-philosophers.rho
    """.stripMargin

  val propose =
    """
      |#!/bin/bash
      |
      |./rnode --grpc-port <grpcPort> propose
    """.stripMargin

  val loop =
    """
      |#!/bin/bash
      |
      |for i in `seq 1 10`;
      |do
      |    ./deploy.sh; ./propose.sh
      |done
    """.stripMargin
}

case class NodeConfig (
                      name: String,
                      host: String = "localhost",
                      port: Int,
                      metricsPort: Int,
                      httpPort: Int,
                      grpcPort: Int
                      )



class Config(arguments: Seq[String]) extends ScallopConf(arguments) {
  val validators = opt[Int](required = true)
  val out = opt[Path](required = true)
}

object Templater {
  def main(args: Array[String]): Unit = {
    val conf = new Config(args)
    val amount = 1
    val out = Paths.get("./envs/builder/envs/test001")

    val bsc = genVCon(999).copy(name = "bootstrap")
    genConfig(out, bsc, Templates.bootstrap, bsc.port)
    genBSData(out)

    1 to amount map genVCon foreach(genConfig(out, _, Templates.validator, bsc.port))

    genDeploy(out, bsc.grpcPort)
    genPropose(out, bsc.grpcPort)
    genTests(out)
  }

  def genBSData(dir: Path): Unit = {
    val d = dir.resolve("bootstrap/data")
    d.toFile.mkdirs()
    Files.createSymbolicLink(d.resolve("genesis"), Paths.get("../../store/bootstrap/genesis"))
    Files.createSymbolicLink(d.resolve("node.key.pem"), Paths.get("../../store/bootstrap/node.key.pem"))
    Files.createSymbolicLink(d.resolve("node.certificate.pem"), Paths.get("../../store/bootstrap/node.certificate.pem"))
  }

  def genVCon(i: Int): NodeConfig = {
    val prefix = f"3$i%03d"
    NodeConfig(name = prefix, port = (prefix + 1).toInt, metricsPort = (prefix + 2).toInt, httpPort = (prefix + 3).toInt, grpcPort = (prefix + 5).toInt)
  }

  def genConfig(out: Path, nc: NodeConfig, templ: String, bootstrapPort: Int): Unit = {
    def genStart(dir: Path): Unit = {
      val path = dir.resolve("start")
      val s = ST(Templates.start).add("c", nc).render()
      val start = Files.write(path, s.get.getBytes)
      start.toFile.setExecutable(true)
    }
    val bs = ST(templ).add("c", nc).add("bootstrapPort", bootstrapPort).render()
    val bsp = out.resolve(nc.name)
    bsp.toFile.mkdirs()
    val bscp = bsp.resolve("config.toml")
    Files.write(bscp, bs.get.getBytes)
    genStart(bsp)
  }

  def genDeploy(out: Path, grpcPort: Int): Unit = {
    val d = ST(Templates.deploy).add("grpcPort", grpcPort).render()
    Files.write(out.resolve("deploy.sh"), d.get.getBytes).toFile.setExecutable(true)
  }

  def genPropose(out: Path, grpcPort: Int): Unit = {
    val d = ST(Templates.propose).add("grpcPort", grpcPort).render()
    Files.write(out.resolve("propose.sh"), d.get.getBytes).toFile.setExecutable(true)
  }

  def genTests(out: Path): Unit = {
    val d = ST(Templates.loop).render()
    Files.write(out.resolve("test.sh"), d.get.getBytes).toFile.setExecutable(true)
  }
}