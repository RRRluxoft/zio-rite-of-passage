ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.4"

ThisBuild / scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature"
)

ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

val Versions = new {
  val zio        = "2.0.19"
  val tapir      = "1.2.6"
  val zioLogging = "2.1.8"
  val zioConfig  = "3.0.7"
  val sttp       = "3.8.8"
  val javaMail   = "1.6.2"
  val stripe     = "24.3.0"
}

val dependencies = new {
  val all = Seq(
    "com.softwaremill.sttp.tapir"   %% "tapir-sttp-client"                 % Versions.tapir,
    "com.softwaremill.sttp.tapir"   %% "tapir-json-zio"                    % Versions.tapir,
    "com.softwaremill.sttp.client3" %% "zio"                               % Versions.sttp,
    "dev.zio"                       %% "zio-json"                          % "0.4.2",
    "com.softwaremill.sttp.tapir"   %% "tapir-zio"                         % Versions.tapir,
    "com.softwaremill.sttp.tapir"   %% "tapir-zio-http-server"             % Versions.tapir,
    "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle"           % Versions.tapir,

    "ch.qos.logback"                 % "logback-classic"                   % "1.4.13",
    "dev.zio"                       %% "zio-test"                          % Versions.zio,

    "dev.zio"                       %% "zio-config"                        % Versions.zioConfig,
    "dev.zio"                       %% "zio-config-magnolia"               % Versions.zioConfig,
    "dev.zio"                       %% "zio-config-typesafe"               % Versions.zioConfig,
    "io.getquill"                   %% "quill-jdbc-zio"                    % "4.7.3",
    "org.postgresql"                 % "postgresql"                        % "42.5.0",
    "org.flywaydb"                   % "flyway-core"                       % "9.7.0",
    "io.github.scottweaver"         %% "zio-2-0-testcontainers-postgresql" % "0.9.0",
    "dev.zio"                       %% "zio-prelude"                       % "1.0.0-RC16",
    "com.auth0"                      % "java-jwt"                          % "4.2.1",
    "com.sun.mail"                   % "javax.mail"                        % Versions.javaMail,
    "com.stripe"                     % "stripe-java"                       % Versions.stripe
  )

  val test = Seq(
    "com.softwaremill.sttp.tapir"   %% "tapir-sttp-stub-server"            % Versions.tapir,
    "dev.zio"                       %% "zio-test-junit"                    % Versions.zio,
    "dev.zio"                       %% "zio-test-sbt"                      % Versions.zio,
    "dev.zio"                       %% "zio-test-magnolia"                 % Versions.zio,
    "dev.zio"                       %% "zio-mock"                          % "1.0.0-RC9"
  ).map(_ % "test")

  val logs = Seq(
    "dev.zio"                       %% "zio-logging"                       % Versions.zioLogging,
    "dev.zio"                       %% "zio-logging-slf4j"                 % Versions.zioLogging
  )
}

lazy val foundations = (project in file("modules/foundations"))
  .settings(
    libraryDependencies ++= dependencies.all ++ dependencies.logs ++ dependencies.test
  )

lazy val root = (project in file("."))
  .settings(
    name := "zio-rite-of-passage"
  )
  .aggregate(foundations)
  .dependsOn(foundations)
