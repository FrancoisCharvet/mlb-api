# Class Exam Instruction: Building a ZIO Application Backend

## Topic

Building a REST API using the "Major League Baseball Dataset" from [Kaggle](https://www.kaggle.com/datasets/saurabhshahane/major-league-baseball-dataset).

### Dataset Description
The "Major League Baseball Dataset" from Kaggle is a comprehensive collection of data related to Major League Baseball (MLB) games, players, teams, and statistics. The dataset contains information about game-by-game Elo ratings and forecasts back to 1871. You can visit the Kaggle page for a more detailed description of the dataset.

The dataset is available in CSV format: `mlb_elo.csv` contains all data: `mlb_elo_latest.csv` contains data for only the latest season. No need to register and download the files from Kaggle, they are available in Teams group's files tab.

### Ratings Systems: ELO and MLB Predictions
The dataset includes two ratings systems, ELO and MLB Predictions, which are used to evaluate teams' performance and predict game outcomes:

1. **ELO**: The ELO rating system is a method for calculating the relative skill levels of teams in two-player games, such as chess. In the context of MLB, the ELO rating system assigns a numerical rating to each team, which reflects their relative strength. The rating is updated based on game outcomes, with teams gaining or losing points depending on the result of the match.

2. **MLB Predictions**: The MLB Predictions rating system utilizes various statistical models and algorithms to predict game outcomes. It takes into account factors such as team performance, player statistics, historical data, and other relevant factors to generate predictions for upcoming games.

## Expectations
1. Design and implement data structures: You should design appropriate data structures to represent games, teams, players, and the two ratings systems (ELO and MLB Predictions). Consider using functional programming principles and immutable data structures when possible.

1. Use ZIO and related libraries: Build your application backend using Scala 3 and leverage the power of ZIO. Utilize libraries such as `zio-jdbc`, `zio-streams`, `zio-json`, or `zio-http` to handle database operations, stream processing, JSON parsing, and HTTP application, respectively.

1. Database initialization at startup: Implement a mechanism to initialize the H2 database engine at application startup. You can use ZIO for managing the initialization process and setting up the required database schema. To process CSV, you can use the [tototoshi/scala-csv](https://github.com/tototoshi/scala-csv) library.

1. Dedicated endpoint for database initialization: Alternatively create a dedicated endpoint in your REST API that triggers the database initialization process. This endpoint should be used to initialize the database and ensure it is ready for use.

1. Endpoints for accessing game history and making predictions: Implement additional endpoints that allow users to query and retrieve game history and make predictions for future games. These endpoints should be designed to provide relevant information and facilitate interaction with the MLB dataset. Be creative and explain your motivation in your project README file.

1. Git repository and documentation quality: Set up a Git repository to manage your application's source code. Ensure that your repository is well-organized, contains appropriate commits, and has a clear README file. Document your code, including class and method-level comments, explaining the purpose and functionality of each component.

1. Implement tests: Write test cases to validate the functionality of your application. Consider using frameworks like ScalaTest or ZIO Test to write unit tests that cover critical components of your codebase.

1. Consider functional properties: Wherever applicable, emphasize functional programming principles such as immutability, referential transparency, and composability. Use appropriate abstractions and design patterns to enhance code modularity and maintainability.

## Additional Requirements

1. Group Size: Form groups of up to 4 students. You are encouraged to collaborate and discuss ideas within your group but ensure that each member actively contributes to the project.

1. Due Date: The project is expected to be completed within one week after the class. Submit your project by the specified due date and time. Late submissions may incur penalties unless prior arrangements have been made with the instructor.

1. Language: Use English for code, comments and documentation.

## Deliverables

1. Scala 3 code implementing the ZIO application, adhering to the given requirements and expectations.

1. Git repository containing your code with appropriate commits and a README file providing instructions on how to run and test your application and the decisions made (libraries, data structure(s), algorithm and its performance, ...).

1. Documentation explaining the purpose, functionality, and usage of your application, along with any external libraries used.

## Grading

Your solution will be graded based on the following criteria, with equal distribution of the number of points on **criteria 1 to 5**:

1. Correctness and functionality of the application implementation.

1. Quality the data model and adherence to functional programming principles.

1. Effective usage of ZIO, including ecosystem libraries.

1. Testing completeness and effectiveness, covering various scenarios.

1. Quality and clarity of code organization and documentation, including the README file.

1. Collaboration within the group and active participation of each member.

1. Timely submission of the project by the specified due date.

## Additional Information

Certainly! Here's a skeleton code structure for a Scala 3 application backend using ZIO, `zio-jdbc`, `zio-http`, and the H2 database:

```scala
package mlb

import zio._
import zio.jdbc._
import zio.http._

object MlbApi extends ZIOAppDefault {

  val createZIOPoolConfig: ULayer[ZConnectionPoolConfig] =
    ZLayer.succeed(ZConnectionPoolConfig.default)

  val properties: Map[String, String] = Map(
    "user" -> "postgres",
    "password" -> "postgres"
  )

  val connectionPool : ZLayer[ZConnectionPoolConfig, Throwable, ZConnectionPool] =
    ZConnectionPool.h2mem(
      database = "testdb",
      props = properties
    )

  val create: ZIO[ZConnectionPool, Throwable, Unit] = transaction {
    execute(
      sql"CREATE TABLE IF NOT EXISTS ..."
    )
  }

  val insertRows: ZIO[ZConnectionPool, Throwable, UpdateResult] = transaction {
    insert(
      sql"INSERT INTO ...".values((value1, value2, value2))
    )
  }

  val endpoints: App[Any] =
    Http
      .collect[Request] {
        case Method.GET -> Root / "init" => ???
        case Method.GET -> Root / "games" => ???
        case Method.GET -> Root / "predict" / "game" / gameId => ???
      }
      .withDefaultErrorResponse

  val app: ZIO[ZConnectionPool & Server, Throwable, Unit] = for {
    conn <- create *> insertRows
    _ <- Server.serve(endpoints)
  } yield ()

  override def run: ZIO[Any, Throwable, Unit] =
    app.provide(createZIOPoolConfig >>> connectionPool, Server.default)
}
```

This skeleton code provides a basic structure for your application. You can start by fill in the placeholders (`???`) with the appropriate code to handle database operations, retrieve game history, make predictions, and handle other functionalities as per your requirements. Feel free to make any modification to this skeleton. Take your time discovering the ZIO ecosystem by reading the official document.

Here's an example of the associated `build.sbt` file for the skeleton code provided above:

```scala
val scala3Version = "3.3.0"
val h2Version = "2.1.214"
val scalaCsvVersion = "1.3.10"
val zioVersion = "2.0.6"
val zioSchemaVersion = "0.4.8"
val zioJdbcVersion = "0.0.2"
val zioJsonVersion = "0.5.0"
val zioHtppVersion = "3.0.0-RC2"

lazy val root = (project in file("."))
  .settings(
    name := "mlb-api",
    version := "1.0",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "com.h2database" % "h2" % h2Version,
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-schema" % zioSchemaVersion,
      "dev.zio" %% "zio-jdbc" % zioJdbcVersion,
      "dev.zio" %% "zio-json" % zioJsonVersion,
      "dev.zio" %% "zio-http" % zioHtppVersion,
      "com.github.tototoshi" %% "scala-csv" % scalaCsvVersion,
    ).map(_ % Compile),
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29"
    ).map(_ % Test)
  )
```

Make sure to place this `build.sbt` file in the root directory of your project. Adjust the dependencies' versions as necessary, and add any additional dependencies required for your project.

This is a basic `build.sbt` configuration. Depending on your project's requirements, you may need to add more settings, such as resolvers, additional libraries, plugins or configurations for code formatting, coverage, and more.

In the example above, `insertRows` is very simple and takes no parameter. You may want to make it a function use a stream to batch insert your data at initialization time.

```scala
for {
  conn <- create
  source <- ZIO.succeed(CSVReader.open(???))
  stream <- ZStream
    .fromIterator[Seq[String]](source.iterator)
    .map[???](???)
    .grouped(???)
    .foreach(chunk => insertRows(???))
  _ <- ZIO.succeed(source.close())
  res <- select
} yield res
```

Finally, it is encouraged to use [sbt-revolver](https://github.com/spray/sbt-revolver) in your workflow. This is a plugin for SBT enabling a super-fast development turnaround for your Scala applications. It supports the following features:
* Starting and stopping your application in the background of your interactive SBT shell (in a forked JVM).
* Triggered restart: automatically restart your application as soon as some of its sources have been changed.

Add the following dependency to your `project/plugins.sbt`:

```scala
addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")
```

You can then use `~reStart` to go into "triggered restart" mode. Your application starts up and SBT watches for changes in your source (or resource) files. If a change is detected SBT recompiles the required classes and sbt-revolver automatically restarts your application. When you press `<ENTER>` SBT leaves "triggered restart" and returns to the normal prompt keeping your application running.

Good luck with your exam, and feel free to ask any further questions!