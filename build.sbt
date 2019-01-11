// to use scala directory in sodium subrepo as sub-project,
// project id is 'root', so we need to use it like that.
lazy val root = (project in file("sodium/scala"))

lazy val tutor = (project in file("."))
  .settings(
    name := "scala-typingtutor",
    version := "0.1-SNAPSHOT",
    organization := "com.rgoulter",
    scalaVersion := "2.11.8",
  )
  .dependsOn(root)

libraryDependencies += "com.fifesoft" % "rsyntaxtextarea" % "2.5.8" withSources () withJavadoc ()

libraryDependencies += "org.apache.directory.studio" % "org.apache.commons.io" % "2.4"

// libraryDependencies += "nz.sodium" % "sodium" % "1.1.0" withSources() withJavadoc()

libraryDependencies += "sodium" % "sodium_2.11" % "1.0" withSources () withJavadoc ()

libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.8.11.2"

libraryDependencies ++= Seq(
  "org.scalatest"   %% "scalatest"       % "2.2.4" % "test",
  "org.assertj" % "assertj-swing-junit" % "3.8.0" % "test"
)

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")
