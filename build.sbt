enablePlugins(JavaAppPackaging)

name         := "stressservice"
organization := "com.inkenkun.x1"
version      := "1.0"
scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV       = "2.3.11"
  val akkaStreamV = "1.0"
  val scalaTestV  = "2.2.5"
  val logbackV    = "1.1.3"
  Seq(
    "com.typesafe.akka" %% "akka-actor"                           % akkaV,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-testkit-experimental"       % akkaStreamV,
    "ch.qos.logback"    %  "logback-classic"                      % logbackV,
    "org.scalatest"     %% "scalatest"                            % scalaTestV % "test"
  )
}

Revolver.settings
