name              := "scala-typingtutor"

version           := "0.1-SNAPSHOT"

organization      := "com.rgoulter"

scalaVersion      := "2.11.8"

libraryDependencies += "com.fifesoft" % "rsyntaxtextarea" % "2.5.8" withSources() withJavadoc()

libraryDependencies += "org.apache.directory.studio" % "org.apache.commons.io" % "2.4"

// libraryDependencies += "nz.sodium" % "sodium" % "1.1.0" withSources() withJavadoc()

libraryDependencies += "sodium" % "sodium_2.11" % "1.0" withSources() withJavadoc()

libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.8.11.2"

libraryDependencies ++= Seq (
        "info.cukes" % "cucumber-core" % "1.2.4" % "test",
        "info.cukes" %% "cucumber-scala" % "1.2.4" % "test",
        "info.cukes" % "cucumber-jvm" % "1.2.4" % "test",
        "info.cukes" % "cucumber-junit" % "1.2.4" % "test",
        "org.scalatest" %% "scalatest" % "2.2.4" % "test")

enablePlugins(CucumberPlugin)

CucumberPlugin.glue := "com/rgoulter/cuke/"

def beforeAll() : Unit = { println("** hello **") }
def afterAll() : Unit = { println("** goodbye **") }

CucumberPlugin.beforeAll := beforeAll
CucumberPlugin.afterAll := afterAll

