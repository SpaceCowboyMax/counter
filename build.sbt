name := "counter"

version := "0.1"

scalaVersion := "2.12.1"

libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.0.2"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1",
  "org.scalacheck" %% "scalacheck" % "1.13.4")
