import sbt.Keys.{libraryDependencies, _}

Compile / unmanagedJars ++= {
  val base = baseDirectory.value
  val customJars = (unmanagedBase.value ** "*.jar") +++ (base ** "*.jar")
  customJars.classpath
}

lazy val root = (project in file("."))
  .enablePlugins(PlayService, PlayLayoutPlugin, Common)
  .settings(
    name := "analytics-rest-play",
    version := "2.7.x",
    maintainer := "ZHANG, Meng <mzhang2@paypal.com>",
    scalaVersion := "2.11.12",
    libraryDependencies ++= Seq(
      guice,
      "com.typesafe.play" %% "play-slick" % "4.0.2",
      "com.typesafe.play" %% "play-slick-evolutions" % "4.0.2",
      "com.h2database" % "h2" % "1.4.200",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.8",
      "org.joda" % "joda-convert" % "2.1.2",
      "net.logstash.logback" % "logstash-logback-encoder" % "5.2",
      "io.lemonlabs" %% "scala-uri" % "1.4.10",
      "net.codingwell" %% "scala-guice" % "4.2.5",
      "com.pauldijou" %% "jwt-play-json" % "4.2.0",
      "com.opencsv" % "opencsv" % "5.0",
      "org.apache.spark" %% "spark-core" % "2.4.0",
      "org.apache.spark" %% "spark-sql" % "2.4.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
    ),
    libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-log4j12")) }
  )
