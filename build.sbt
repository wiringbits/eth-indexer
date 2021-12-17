ThisBuild / scalaVersion := "2.13.3"
ThisBuild / organization := "io.stakenet"

val consoleDisabledOptions = Seq("-Xfatal-warnings", "-Ywarn-unused", "-Ywarn-unused-import")

lazy val baseSettings: Project => Project = {
  _.settings(
    scalacOptions ++= Seq(
      "-Werror",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-target:jvm-1.8",
      "-encoding",
      "UTF-8",
      "-Xsource:3",
      "-Wconf:src=src_managed/.*:silent",
      "-Xlint:missing-interpolator",
      "-Xlint:adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Ywarn-unused"
    ),
    (Compile / doc / scalacOptions) ++= Seq(
      "-no-link-warnings"
    ),
    // Some options are very noisy when using the console and prevent us using it smoothly, let's disable them
    (Compile / console / scalacOptions) ~= (_ filterNot consoleDisabledOptions.contains)
  )
}

lazy val playSettings: Project => Project = {
  _.enablePlugins(PlayScala)
    .disablePlugins(PlayLayoutPlugin)
    .settings(
      // remove play noisy warnings
      play.sbt.routes.RoutesKeys.routesImport := Seq.empty,
      libraryDependencies ++= Seq(
        guice,
        evolutions,
        jdbc,
        ws,
        "com.google.inject" % "guice" % "5.0.1",
        "org.playframework.anorm" %% "anorm" % "2.6.10",
        "org.postgresql" % "postgresql" % "42.3.1"
      )
    )
}

lazy val testSettings: Project => Project = {
  _.enablePlugins(PlayScala)
    .settings(
      libraryDependencies ++= Seq(
        "com.spotify" % "docker-client" % "8.16.0" % "test",
        "com.whisk" %% "docker-testkit-scalatest" % "0.9.9" % "test",
        "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.9" % "test",
        "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
        "org.mockito" %% "mockito-scala" % "1.16.49" % Test,
        "org.mockito" %% "mockito-scala-scalatest" % "1.16.49" % Test
      )
    )
}

lazy val root = (project in file("."))
  .configure(baseSettings, playSettings, testSettings)
  .settings(
    name := "eth-indexer",
    libraryDependencies ++= Seq("org.web3j" % "core" % "5.0.0"),
    libraryDependencies ++= Seq("com.beachape" %% "enumeratum" % "1.7.0")
  )
