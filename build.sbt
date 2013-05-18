name := "Spear"

version := "0.1"

scalaVersion := "2.8.0"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-library" % "2.8.0",
  "net.sourceforge.parallelcolt" % "parallelcolt" % "0.10.0"
)

mainClass in (Compile, packageBin) := Some("orc.Main")

mainClass in (Compile, run) := Some("orc.Main")

