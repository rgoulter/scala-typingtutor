// to use scala directory in sodium subrepo as sub-project,
// project id is 'root', so we need to use it like that.
lazy val root = (project in file("sodium/scala"))

lazy val tutor = (project in file("."))
  .settings(
    name := "scala-typingtutor",
    version := "0.2-SNAPSHOT",
    organization := "com.rgoulter",
    scalaVersion := "2.12.8",
  )
  .dependsOn(root)

libraryDependencies += "com.fifesoft" % "rsyntaxtextarea" % "3.0.2" withSources () withJavadoc ()

libraryDependencies += "org.apache.directory.studio" % "org.apache.commons.io" % "2.6"

libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.25.2"

libraryDependencies ++= Seq(
  "org.scalatest"   %% "scalatest"       % "3.0.4" % "test",
  "org.assertj" % "assertj-swing-junit" % "3.8.0" % "test"
)

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")
