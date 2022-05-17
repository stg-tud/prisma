ThisBuild / version        := "0.0.6"
ThisBuild / scalaVersion   := "2.13.8"
ThisBuild / scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-Xlint",
                                  "-Ymacro-annotations") //, "-Ypatmat-exhaust-depth", "off")
ThisBuild / resolvers     ++= Seq(Resolver.bintrayRepo("ethereum", "maven"),
                                  Resolver.sonatypeRepo("public"))

ThisBuild / libraryDependencies ++= Seq(
  "org.scalameta" %% "munit" % "0.7.22",
  "org.slf4j" % "slf4j-simple" % "1.6.4",
  "org.web3j" % "core" % "5.0.0", // Apache 2.0
  "com.esaulpaugh" % "headlong" % "4.4.2", // Apache 2.0
  "io.github.scala-loci" %% "retypecheck" % "0.10.0", // Apache 2.0
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.thoughtworks.q" %% "q" % "1.0.4",
)

lazy val commonSettings = Seq(
  testFrameworks += new TestFramework("munit.Framework"),
  Test / parallelExecution := false,
)

lazy val prisma  = project.in(file(".")).settings(commonSettings)

lazy val testCalc = project.dependsOn(prisma).settings(commonSettings)

lazy val testTTT = project.dependsOn(prisma).settings(commonSettings)
lazy val testHangman = project.dependsOn(prisma).settings(commonSettings)
lazy val testRPS = project.dependsOn(prisma).settings(commonSettings)
lazy val testChineseCheckers = project.dependsOn(prisma).settings(commonSettings)

lazy val testCrowdfunding = project.dependsOn(prisma).settings(commonSettings)
lazy val testEscrow = project.dependsOn(prisma).settings(commonSettings)
lazy val testMultiSig = project.dependsOn(prisma).settings(commonSettings)
lazy val testToken = project.dependsOn(prisma).settings(commonSettings)
lazy val testNotary = project.dependsOn(prisma).settings(commonSettings)

lazy val testTTTChannel = project.dependsOn(prisma).settings(commonSettings)
lazy val testTTTLibrary = project.dependsOn(prisma).settings(commonSettings)
lazy val testTTTViaLib = project.dependsOn(prisma).settings(commonSettings)

lazy val all = project.dependsOn(
  testCalc,
  testTTT, testHangman, testChineseCheckers, testRPS,
  testTTTChannel, testCrowdfunding, testEscrow, testMultiSig, testToken, testNotary, testTTTLibrary, testTTTViaLib,
).settings(commonSettings)
