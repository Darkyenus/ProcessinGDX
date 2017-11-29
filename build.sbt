import sbt.file
import com.darkyen.resourcepacker.PackingOperation

import scala.collection.mutable.ArrayBuffer

name := "PV259 Programování generativního designu"
version := "0.0"

kotlinLib("stdlib")

kotlinLib("reflect")

crossPaths := false

autoScalaLibrary := false

val gdxVersion = "1.9.5"

resolvers += "jitpack" at "https://jitpack.io"

// Core
libraryDependencies ++= Seq(
  "com.badlogicgames.gdx" % "gdx" % gdxVersion,
  "com.badlogicgames.gdx" % "gdx-box2d" % gdxVersion
)

//Desktop
libraryDependencies ++= Seq(
  "com.badlogicgames.gdx" % "gdx-backend-lwjgl3" % gdxVersion,
  "com.badlogicgames.gdx" % "gdx-platform" % gdxVersion classifier "natives-desktop",
  "com.badlogicgames.gdx" % "gdx-box2d-platform" % gdxVersion classifier "natives-desktop"
)

javacOptions ++= Seq("-g", "-Xlint", "-Xlint:-rawtypes", "-Xlint:-unchecked")

javaOptions ++= Seq("-ea")

TaskKey[Unit]("packResources") := {
  ResourcePacker.resourcePack(new PackingOperation(file("./resources"), file("./src/main/resources")))
}

TaskKey[Unit]("dist") := {
  val resultZip = target.value / (name.value+"-"+version.value+".zip")
  val basePrefix = "BackyardRockets/"
  //
  val files = new ArrayBuffer[(File, String)]()
  files += ((assembly.value, basePrefix + "BackyardRockets.jar"))
  //
  def appendContent(directory:File, prefix:String): Unit ={
    for(file <- directory.listFiles() if !file.getName.startsWith(".")) {
      if(file.isFile){
        files += ((file, prefix+file.getName))
      }else if(file.isDirectory){
        appendContent(file, s"$prefix${file.getName}/")
      }
    }
  }
  //
  appendContent((baseDirectory in(Compile, run)).value, basePrefix)
  //
  IO.zip(files, resultZip)
  println("Packed to "+resultZip.getCanonicalPath)
}
