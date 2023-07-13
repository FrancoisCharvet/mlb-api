package mlb

import zio._
import zio.jdbc._
import zio.http._
import com.github.tototoshi.csv._
import java.sql.Date
import zio.stream.ZStream
import mlb.GameDates.GameDate
import java.time.LocalDate
import mlb.SeasonYears.SeasonYear
import mlb.PlayoffRounds.PlayoffRound

object MlbApi extends ZIOAppDefault {

  import DataService._
  import ApiService._
  import HomeTeams._
  import AwayTeams._

  val static: App[Any] = Http.collect[Request] {
    case Method.GET -> Root / "text" => Response.text("Hello MLB Fans!")
    case Method.GET -> Root / "json" => Response.json("""{"greetings": "Hello MLB Fans!"}""")
  }.withDefaultErrorResponse

  val endpoints: App[ZConnectionPool] = Http.collectZIO[Request] {
    case Method.GET -> Root / "init" =>
      ZIO.succeed(Response.text("Not Implemented").withStatus(Status.NotImplemented))
    case Method.GET -> Root / "game" / "latest" / homeTeam / awayTeam =>
      for {
        game: Option[Game] <- latest(HomeTeam(homeTeam), AwayTeam(awayTeam))
        res: Response = latestGameResponse(game)
      } yield res
    case Method.GET -> Root / "game" / "predict" / homeTeam / awayTeam =>
      // FIXME : implement correct logic and response
      ZIO.succeed(Response.text(s"$homeTeam vs $awayTeam win probability: 0.0"))
    case Method.GET -> Root / "games" / "count" =>
      for {
        count: Option[Int] <- count
        res: Response = countResponse(count)
      } yield res
    case Method.GET -> Root / "games" / "history" / homeTeam =>
      import zio.json.EncoderOps
      import Game._
      // FIXME: implement correct database request
      ZIO.succeed(Response.json(games.toJson).withStatus(Status.Ok))
    case _ =>
      ZIO.succeed(Response.text("Not Found").withStatus(Status.NotFound))
  }.withDefaultErrorResponse

  val columnNames: List[String] = List("date","season", "neutral", "playoff", "team1", "team2", "elo1_pre",
   "elo2_pre", "elo_prob1", "elo_prob2", "elo1_post", "elo2_post", "rating1_pre", "rating2_pre", "pitcher1",
   "pitcher2", "pitcher1_rgs", "pitcher2_rgs", "pitcher1_adj", "pitcher2_adj", "rating_prob1", "rating_prob2",
   "rating1_post", "rating2_post", "score1", "score2")

  val appLogic: ZIO[ZConnectionPool & Server, Throwable, Unit] = for {
    conn <- create
    source <- ZIO.succeed(CSVReader.open("C:/Users/barto/Desktop/EFREI/M1/S8/functional_programming/mlb-elo/mlb-elo/mlb_elo.csv"))
    stream <- ZStream
        .fromIterator[Seq[String]](source.iterator)
        .drop(1)//we delete the first row
        .foreach(chunk => {
            val rowMap = columnNames.zip(chunk).toMap //We link the columnNamesList with chunk and convert it to a Map [key: columnName,value: chunkValue]
            insertRows2(
            createGame(rowMap("date"), rowMap("season"), rowMap("playoff"), rowMap("team1"), rowMap("team2")))
        })
    _ <- ZIO.succeed(source.close())
  //  _ <- create *> insertRows
    _ <- Server.serve[ZConnectionPool](static ++ endpoints)
  } yield ()

  override def run: ZIO[Any, Throwable, Unit] =
    appLogic.provide(createZIOPoolConfig >>> connectionPool, Server.default)
}

object ApiService {

  import zio.json.EncoderOps
  import Game._

  def countResponse(count: Option[Int]): Response = {
    count match
      case Some(c) => Response.text(s"$c game(s) in historical data").withStatus(Status.Ok)
      case None => Response.text("No game in historical data").withStatus(Status.NotFound)
  }

  def latestGameResponse(game: Option[Game]): Response = {
    println(game)
    game match
      case Some(g) => Response.json(g.toJson).withStatus(Status.Ok)
      case None => Response.text("No game found in historical data").withStatus(Status.NotFound)
  }
}

object DataService {

  val createZIOPoolConfig: ULayer[ZConnectionPoolConfig] =
    ZLayer.succeed(ZConnectionPoolConfig.default)

  val properties: Map[String, String] = Map(
    "user" -> "postgres",
    "password" -> "postgres"
  )

  val connectionPool: ZLayer[ZConnectionPoolConfig, Throwable, ZConnectionPool] =
    ZConnectionPool.h2mem(
      database = "mlb",
      props = properties
    )

  val create: ZIO[ZConnectionPool, Throwable, Unit] = transaction {
    execute(
      sql"CREATE TABLE IF NOT EXISTS games(date DATE NOT NULL, season_year INT NOT NULL, playoff_round INT, home_team VARCHAR(3), away_team VARCHAR(3))"
    )
  }

  
  import HomeTeams.*
  import AwayTeams.*

  val insertRows: ZIO[ZConnectionPool, Throwable, UpdateResult] = {
    val rows: List[Game.Row] = games.map(_.toRow)
    transaction {
      insert(
        sql"INSERT INTO games(date, season_year, playoff_round, home_team, away_team)".values[Game.Row](rows)
      )
    }
  }

  // Should be implemented to replace the `val insertRows` example above. Replace `Any` by the proper case class.
  def insertRows2(games: Game): ZIO[ZConnectionPool, Throwable, UpdateResult] = {
    val rows: List[Game.Row] = List(games.toRow)
     transaction {
      insert(
        sql"INSERT INTO games(date, season_year, playoff_round, home_team, away_team)".values[Game.Row](rows)
      )
    }
  }

  val count: ZIO[ZConnectionPool, Throwable, Option[Int]] = transaction {
    selectOne(
      sql"SELECT COUNT(*) FROM games".as[Int]
    )
  }

  def latest(homeTeam: HomeTeam, awayTeam: AwayTeam): ZIO[ZConnectionPool, Throwable, Option[Game]] = {
    transaction {
      selectOne(
        sql"SELECT date, season_year, playoff_round, home_team, away_team FROM games WHERE home_team = ${HomeTeam.unapply(homeTeam)} AND away_team = ${AwayTeam.unapply(awayTeam)} ORDER BY date DESC LIMIT 1".as[Game]
      )
    }
  }
}
