package coop.rchain.perf
import java.nio.file.{Files, Paths}

import collection.JavaConverters._
import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef.{Simulation, atOnceUsers, scenario}
import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.io.Source

class BinaryFileSimulation100kb extends BinaryFileSimulation(1, 100*1024)

//class BinaryFileSimulation1MB extends BinaryFileSimulation(2, 1024*1024)

//class BinaryFileSimulation5MB extends BinaryFileSimulation(3,5*1024*1024)

//class BinaryFileSimulation10MB extends BinaryFileSimulation(4,10*1024*1024)

abstract class BinaryFileSimulation(fileId: Int, fileSizeInBytes: Int)
    extends Simulation {
  import RNodeActionDSL._
  import BinaryFileSimulation._

  val path = Paths.get(Paths.get("").toAbsolutePath().normalize().toString(),
                       "../../simulations/binary/a_binMapStore.rho")

  val conf = ConfigFactory.load()
  val rnodes = conf.getStringList("rnodes").asScala.toList

  val rhoContent = Source.fromFile(path.toUri).mkString
  val binMapInstallContract = (path.getFileName.toString, rhoContent)

  val storeScript = s"""
       | new mySave in {
       |   @"binSave"!($fileId, "${strOfSize(fileSizeInBytes)}".toByteArray())
       | }
     """.stripMargin

  val loadScript = s"""
       | new myLoad in {
       |   @"binLoad"!($fileId, *myLoad)
       | }
     """.stripMargin

  println(s"storing a $fileSizeInBytes bytes on the blockchain on ${rnodes
    .mkString(", ")}")

  println("-------------------------------")

  val protocol = RNodeProtocol.createFor(rnodes)

  val scnInstallToStore = scenario("Create_BinaryFileStore")
    .foreach(List(binMapInstallContract), "contract") {
      exec(deploy()).exec(propose())
    }

  val scnSave = scenario("SaveTo_BinaryFileStore")
    .foreach(List((s"saveToStore_$fileSizeInBytes.rho", storeScript)),
             "contract") {
      exec(deploy()).exec(propose())
    }

    val scnLoad = scenario("LoadFrom_BinaryFileStore")
      .foreach(List((s"loadFromStore_$fileSizeInBytes.rho", loadScript)),
               "contract") {
        exec(deploy()).exec(propose())
      }

  val scnCombined = scenario(s"BinaryFileStore_${fileSizeInBytes}_bytes")
    .exec(scnInstallToStore)
    .exec(scnSave)
    .exec(scnLoad)

  scnInstallToStore.inject(rampUsers(1) over (20 seconds))
  scnSave.inject(rampUsers(1) over (30 seconds))
  scnLoad.inject(rampUsers(1) over (30 seconds))

  setUp(
    scnCombined.inject(rampUsers(1) over (90 seconds))
  ).protocols(protocol)
}

object BinaryFileSimulation {
  def strOfSize(fileSizeInBytes: Int): String = {
    val start = 'a'.toInt
    val range = 'z'.toInt - start
    val sb: StringBuilder = new StringBuilder()
    for (i <- 0 to fileSizeInBytes) {
      sb += (start + (i % range)).toChar
    }
    sb.toString
  }
}
