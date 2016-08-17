# Typing Tutor

A typing tutor inspired by <typing.io>, a typing tutor for programmers.
This provides syntax-highlighting for the typing-practice documents, like the
development environments programmers are used to.

## Building

This project's uses Scala Built Tool (SBT) for its build, so:

* compiling can be done using `sbt compile`; the program can be run using
  `sbt run`.

* a JAR can be produced using `sbt packageBin`.

* documentation can be produced using `sbt doc`.

For other tasks SBT can do, see `sbt tasks`. These commands should be called
from the commandline in the root directory of the project. (The tasks can also
be run in the SBT shell, by running `sbt` then calling the tasks from that).

### Build Dependency: SodiumFRP (Scala)

SBT can take care of most of the dependencies; however, the Scala
implementation of the Sodium FRP library isn't published centrally,
so publish a local version by running `sbt publishLocal`
from the `scala/` subfolder of <SodiumFRP/sodium>
repository. i.e.:

```
git clone https://github.com/SodiumFRP/sodium.git
cd sodium/scala
sbt publishLocal
```

