scalaVersion := "2.10.2"

// organization := "org.scala-lang"

name := "IbScala"

// <classpathentry kind="lib" path="E:/Projects/Scala/ScalaIB/lib/IB_Java.jar" sourcepath="E:/Projects/IB_API_9_68/Java/IbClientSrc.jar"/>
// <classpathentry kind="lib" path="E:/Users/cork/.ivy2/cache/org.scalatest/scalatest_2.10/jars/scalatest_2.10-2.0.jar"/>
// <classpathentry kind="lib" path="E:/Users/cork/.ivy2/cache/junit/junit/jars/junit-4.11.jar"/>
// E:\Users\cork\.ivy2\cache\com.netflix.rxjava\rxjava-scala\jars\rxjava-scala-0.15.1.jar  E:/Users/cork/.ivy2/cache/com.netflix.rxjava/rxjava-scala/srcs/rxjava-scala-0.15.0-sources.jar
// E:\Users\cork\.ivy2\cache\com.netflix.rxjava\rxjava-core\jars\rxjava-core-0.15.1.jar
// E:\Users\cork\.ivy2\cache\org.joda\joda-convert\jars\joda-convert-1.2.jar
// E:\Users\cork\.ivy2\cache\joda-time\joda-time\jars\joda-time-2.2.jar

libraryDependencies += "junit" % "junit-dep" % "4.11" % "test"

libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"

libraryDependencies ++= Seq(
    "joda-time" % "joda-time" % "2.1",
    "org.joda" % "joda-convert" % "1.2",
    "com.netflix.rxjava" % "rxjava-core" % "0.15.1",
    "com.netflix.rxjava" % "rxjava-scala" % "0.15.1",
    "org.scala-lang" % "scala-swing" % "2.10.2"
) 