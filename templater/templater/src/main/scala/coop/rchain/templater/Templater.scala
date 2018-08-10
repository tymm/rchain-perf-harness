package coop.rchain.templater

import java.nio.file.{Files, Path, Paths}

import org.clapper.scalasti.ST
import org.rogach.scallop.ScallopConf

object Consts {
  val javaRuntime =
    """
      |export RCHAIN_RNODE="java -jar -Xmx2G -Xms2G -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=heap.bin -XX:+CMSClassUnloadingEnabled -XX:+UseG1GC -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps `pwd`/rnode.jar"
    """.stripMargin
}

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
      |map-size = 1000000000
      |data-dir = "./data"
      |
      |[grpc-server]
      |host = "grpc"
      |port = <c.grpcPort>
      |
      |[validators]
      |public-key = "118eb0ae2193c83e960b46c42026bd6e21d5cc02854881baedd296764d5cd743"
      |private-key = "<privateKey>"
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
      |map-size = 1000000000
      |data-dir = "./data"
      |bootstrap = "rnode://7119ad2a26cdcde63dca675c4f2a4df85f404726@localhost:<bootstrapPort>"
      |
      |[grpc-server]
      |host = "grpc"
      |port = <c.grpcPort>
      |
      |[validators]
      |private-key = "<privateKey>"
    """.stripMargin

  val start =
    """
      |#!/bin/bash
      |
      |$RCHAIN_RNODE -c ./config.toml run --http-port <c.httpPort>
    """.stripMargin

  val deploy =
    """
      |#!/bin/bash
      |
      |$RCHAIN_RNODE --grpc-port <grpcPort> deploy --from "0x1" --phlo-limit 0 --phlo-price 0 --nonce 0 ../contracts/performance/loop_recursive.rho
    """.stripMargin

  val propose =
    """
      |#!/bin/bash
      |
      |$RCHAIN_RNODE --grpc-port <grpcPort> propose
    """.stripMargin

  val loop =
    """
      |#!/bin/bash
      |./deploy.sh;
      |./propose.sh;
      |
    """.stripMargin

  val runTests =
    """
      |#!/bin/bash
      |
      |export RCHAIN_RNODE="java -jar `pwd`/rnode.jar"
      |
      |pushd 3001
      |./loop.sh &
      |pid1=$!
      |popd
   """.stripMargin

  val runEnv =
    """
      |#!/bin/bash
      |
      |./run-bootstrap.sh
      |sleep 5
      |./run-network.sh
    """.stripMargin

  val runBootstrap =
    s"""
      |#!/bin/bash
      |
      |${Consts.javaRuntime}
      |
      |pushd bootstrap
      |./start > output.log 2>&1 &
      |pid0=$$!
      |popd
      |
      |echo $$pid0 > bootstrap.pid
    """.stripMargin

  val runNetwork =
    s"""
      |#!/bin/bash
      |
      |${Consts.javaRuntime}
      |
      |pushd 3001
      |./start > output.log 2>&1 &
      |pid1=$$!
      |popd
    """.stripMargin

  val killNetwork =
    """
        |killall java -jar `pwd`/rnode.jar
      """.stripMargin
}

object Store {
  val keys = Array(
    "e8408a1444e4347cd5585c955053440f9c40479d69acfccf7f8662316acb6e3e",
    "be0154f6a292b692d7dd506c4923dda178a419f8d99f5c7c941a8383c278e3a6",
    "7d8e4576b131492bdb442f510ab76a506cdcf5e610a69dc1163a674f47a80448",
    "c380558b21ed34779f1c29fec947353bb944d9d50c2338b899d6e2faed8849fa"
  )
}

case class NodeConfig(
    name: String,
    host: String = "localhost",
    port: Int,
    metricsPort: Int,
    httpPort: Int,
    grpcPort: Int
)

class Config(arguments: Seq[String]) extends ScallopConf(arguments) {
//  val validators = opt[Int](required = true)
  val out = opt[Path](required = true)
}

object Templater {
  def main(args: Array[String]): Unit = {
    val conf = new Config(args)
    conf.verify
    val amount = 4
    val out = conf.out()

    val bsc = genVCon(999).copy(name = "bootstrap")
    genConfig(
      out,
      bsc,
      Templates.bootstrap,
      bsc.port,
      "6a22ddc9300ef7658c67cfcf8da358c6e7ffa75257361261ca47409d0c7ea6ee")
    genBSData(out)
    genTests(out, bsc)

    (1 to amount).map(genVCon).zipWithIndex.foreach {
      case (c, i) =>
        genConfig(out, c, Templates.validator, bsc.port, Store.keys(i))
        genVData(out, c.name)
        genTests(out, c)
    }

    genSetupBS(out)
    genSetupEnv(out)
    genRunEnv(out)
    genRunTests(out)
    genKillNetwork(out)
  }

  def genBSData(dir: Path): Unit = {
    val d = dir.resolve("bootstrap/data")
    d.toFile.mkdirs()
    Files.createSymbolicLink(d.resolve("genesis"),
                             Paths.get("../../store/bootstrap/genesis"))
    Files.createSymbolicLink(d.resolve("node.key.pem"),
                             Paths.get("../../store/bootstrap/node.key.pem"))
    Files.createSymbolicLink(
      d.resolve("node.certificate.pem"),
      Paths.get("../../store/bootstrap/node.certificate.pem"))
  }

  def genVData(dir: Path, name: String): Unit = {
    val d = dir.resolve(s"$name/data")
    d.toFile.mkdirs()
    Files.createSymbolicLink(d.resolve("genesis"),
                             Paths.get("../../store/bootstrap/genesis"))
  }

  def genVCon(i: Int): NodeConfig = {
    val prefix = f"3$i%03d"
    NodeConfig(name = prefix,
               port = (prefix + 1).toInt,
               metricsPort = (prefix + 2).toInt,
               httpPort = (prefix + 3).toInt,
               grpcPort = (prefix + 5).toInt)
  }

  def genConfig(out: Path,
                nc: NodeConfig,
                templ: String,
                bootstrapPort: Int,
                privateKey: String): Unit = {
    val bs = ST(templ)
      .add("c", nc)
      .add("bootstrapPort", bootstrapPort)
      .add("privateKey", privateKey)
      .render()
    val bsp = out.resolve(nc.name)
    bsp.toFile.mkdirs()
    val bscp = bsp.resolve("config.toml")
    Files.write(bscp, bs.get.getBytes)
    genStart(bsp, nc)
  }

  def genDeploy(out: Path, grpcPort: Int): Unit = {
    val d = ST(Templates.deploy).add("grpcPort", grpcPort).render()
    Files
      .write(out.resolve("deploy.sh"), d.get.getBytes)
      .toFile
      .setExecutable(true)
  }

  def genPropose(out: Path, grpcPort: Int): Unit = {
    val d = ST(Templates.propose).add("grpcPort", grpcPort).render()
    Files
      .write(out.resolve("propose.sh"), d.get.getBytes)
      .toFile
      .setExecutable(true)
  }

  def genLoop(out: Path): Unit = {
    val d = ST(Templates.loop).render()
    Files
      .write(out.resolve("loop.sh"), d.get.getBytes)
      .toFile
      .setExecutable(true)
  }

  def genTests(out: Path, c: NodeConfig): Unit = {
    val path = out.resolve(c.name)
    genDeploy(path, c.grpcPort)
    genPropose(path, c.grpcPort)
    genLoop(path)
  }

  def genSetupBS(out: Path): Unit = {
    val d = ST(Templates.runBootstrap).render()
    Files
      .write(out.resolve("run-bootstrap.sh"), d.get.getBytes)
      .toFile
      .setExecutable(true)
  }

  def genSetupEnv(out: Path): Unit = {
    val d = ST(Templates.runNetwork).render()
    Files
      .write(out.resolve("run-network.sh"), d.get.getBytes)
      .toFile
      .setExecutable(true)
  }

  def genRunEnv(out: Path): Unit = {
    val d = ST(Templates.runEnv).render()
    Files
      .write(out.resolve("run-env.sh"), d.get.getBytes)
      .toFile
      .setExecutable(true)
  }

  def genRunTests(out: Path): Unit = {
    val d = ST(Templates.runTests).render()
    Files
      .write(out.resolve("test.sh"), d.get.getBytes)
      .toFile
      .setExecutable(true)
  }

  def genKillNetwork(out: Path): Unit = {
    val d = ST(Templates.killNetwork).render()
    Files
      .write(out.resolve("kill-network.sh"), d.get.getBytes)
      .toFile
      .setExecutable(true)
  }

  def genStart(dir: Path, nc: NodeConfig): Unit = {
    val path = dir.resolve("start")
    val s = ST(Templates.start).add("c", nc).render()
    val start = Files.write(path, s.get.getBytes)
    start.toFile.setExecutable(true)
  }
}
