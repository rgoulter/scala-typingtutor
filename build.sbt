name              := "scala-typingtutor"

version           := "0.1-SNAPSHOT"

organization      := "com.rgoulter"

scalaVersion      := "2.11.8"

libraryDependencies += "com.fifesoft" % "rsyntaxtextarea" % "2.5.8" withSources() withJavadoc()

libraryDependencies += "org.apache.directory.studio" % "org.apache.commons.io" % "2.4"

libraryDependencies += "nz.sodium" % "sodium" % "1.1.0" withSources() withJavadoc()
