package mlb

import zio._
import zio.jdbc._
import zio.http._
import com.github.tototoshi.csv._
import zio.stream.ZStream
import mlb.GameDates.GameDate
import java.time.LocalDate
import mlb.SeasonYears.SeasonYear
import mlb.PlayoffRounds.PlayoffRound
import mlb.HomeTeams.HomeTeam
import mlb.AwayTeams.AwayTeam
import mlb.DataService.getEloRatings
import mlb.DataService.getMlbRatings

object MlbApi extends ZIOAppDefault {

  import DataService._
  import ApiService._
  import HomeTeams._
  import AwayTeams._

  val static: App[Any] = Http.collect[Request] {
    case Method.GET -> Root / "text" => Response.text("Hello MLB Fans!")
    case Method.GET -> Root / "json" => Response.json("""{"greetings": "Hello MLB Fans!"}""")
  }.withDefaultErrorResponse
  
  // Contains each endpoints and the call to multiple functions
  val endpoints: App[ZConnectionPool] = Http.collectZIO[Request] {
    case Method.GET -> Root / "init" => init
    case Method.GET -> Root / "game" / "latest" / homeTeam / awayTeam =>
      for {
        game: Option[Game] <- latest(HomeTeam(homeTeam), AwayTeam(awayTeam))
        res: Response = latestGameResponse(game)
      } yield res
   case Method.GET -> Root / "game" / "predict" / homeTeam / awayTeam =>
    //We use the flatMap to combine ZIO effect of predictNextGame and predictResponse
    //So we can get eloPrediction and mlbPrediction values and pass them to predictResponse
    predictNextGame(HomeTeam(homeTeam), AwayTeam(awayTeam)).flatMap { (eloPrediction,mlbPrediction) =>
        predictResponse(HomeTeam(homeTeam), AwayTeam(awayTeam), eloPrediction, mlbPrediction).map { response =>
        response
        }
    }    
    case Method.GET -> Root / "games" / "count" =>
      for {
        count: Option[Int] <- count
        res: Response = countResponse(count)
      } yield res
    case Method.GET -> Root / "games" / "history" / homeTeam =>
      import zio.json.EncoderOps
      import Game._
      for {
        games: List[Game] <- history(HomeTeam(homeTeam))
        res: Response = gamesHistoryResponse(games)
      } yield res
    case Method.GET -> Root / "games" / "season" / year =>
      import zio.json.EncoderOps
      import Game._
      for {
        games: List[Game] <- season(SeasonYear(year.toInt))
        res: Response = gamesSeasonResponse(games)
      } yield res
    case _ =>
      ZIO.succeed(Response.text("Not Found").withStatus(Status.NotFound))
  }.withDefaultErrorResponse


  val appLogic: ZIO[ZConnectionPool & Server, Throwable, Unit] = for {
    _ <- Server.serve[ZConnectionPool](static ++ endpoints)
  } yield ()

  override def run: ZIO[Any, Throwable, Unit] =
    appLogic.provide(createZIOPoolConfig >>> connectionPool, Server.default)
}

// Contains functions which handle responses
object ApiService {

  import zio.json.EncoderOps
  import Game._
  
  //Build response for the total number of games stored in the database
  def countResponse(count: Option[Int]): Response = {
    count match
      case Some(c) => Response.text(s"$c game(s) in historical data").withStatus(Status.Ok)
      case None => Response.text("No game in historical data").withStatus(Status.NotFound)
  }

  //Build the response for the lastGame between two teams
  def latestGameResponse(game: Option[Game]): Response = {
    println(game)
    game match
      case Some(g) => Response.json(g.toJson).withStatus(Status.Ok)
      case None => Response.text("No game found in historical data").withStatus(Status.NotFound)
  }

  //Function to predict probabilities for the next match (ELO & MLB)
  def predictNextGame(homeTeam: HomeTeam, awayTeam: AwayTeam): ZIO[ZConnectionPool, Throwable, (Double, Double)] = {
    for {
        eloRatings <- getEloRatings(homeTeam, awayTeam)
        mlbRatings <- getMlbRatings(homeTeam, awayTeam)
    } yield {
        val (homeElo, awayElo) = eloRatings
        val (homeMlb, awayMlb) = mlbRatings

        val eloDifference = homeElo - awayElo
        val mlbDifference = homeMlb - awayMlb
        
        val winProbabilityElo = 1.0 / (1.0 + math.pow(10.0, eloDifference / 400.0))
        val winProbabilityMlb = 1.0 / (1.0 + math.pow(10.0, mlbDifference / 400.0))

        (winProbabilityElo, winProbabilityMlb)
    }
  }

  //Build the response for the next game's prediction
  def predictResponse(homeTeam: HomeTeam, awayTeam: AwayTeam, eloPrediction: Double, mlbPrediction: Double): ZIO[ZConnectionPool, Throwable, Response] = {
    //We use flatMap to get eloPrediction and mlbPrediction values
    predictNextGame(homeTeam, awayTeam).flatMap {
        case (eloPrediction,mlbPrediction) if eloPrediction >= 0 && mlbPrediction >= 0 =>
        ZIO.succeed(Response.text(s"${homeTeam} has ${eloPrediction * 100} % probability to win according to ELO.\n${homeTeam} has ${mlbPrediction * 100} % probability to win according to MLB.").withStatus(Status.Ok))
        case _ =>
        ZIO.succeed(Response.text("Failed to predict the match.").withStatus(Status.NotFound))
    }

  }

  //Build the game history response
  def gamesHistoryResponse(games: List[Game]): Response = {
    games match
      case Nil => Response.text("No games found in historical data").withStatus(Status.NotFound)
      case _ => Response.json(games.toJson).withStatus(Status.Ok)
  }

  //Build the response which shows all games for a specific season
  def gamesSeasonResponse(games: List[Game]): Response = {
    games match
      case Nil => Response.text("No games found in historical data").withStatus(Status.NotFound)
      case _ => Response.json(games.toJson).withStatus(Status.Ok)
  }

}

//Contains functions which handle database management
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
      sql"CREATE TABLE IF NOT EXISTS games(date DATE NOT NULL, season_year INT NOT NULL, playoff_round INT, home_team VARCHAR(3), away_team VARCHAR(3), elo1_pre DOUBLE, elo2_pre DOUBLE,  rating1_pre DOUBLE, rating2_pre DOUBLE)"
    )
  }

  //Initialization of the database using CSVReader from tototoshi/scala-csv
  //And map to a Game object
  def init: ZIO[ZConnectionPool, Throwable, Response] = {
    val initLogic = for{
        conn <- create
        source <- ZIO.succeed(CSVReader.open(("rest\\src\\data\\mlb_elo.csv")))
        stream <- ZStream
        .fromIterator[Seq[String]](source.iterator)
        .drop(1) // drop header row
        .map { values =>
            val date = GameDates.GameDate(LocalDate.parse(values(0)))
            val season = SeasonYears.SeasonYear(values(1).toInt)
            val playoffRound = PlayoffRounds.PlayoffRound(values(2).toInt)
            val homeTeam = HomeTeams.HomeTeam(values(4))
            val awayTeam = AwayTeams.AwayTeam(values(5))
            val eloPre1 = EloPres1.EloPre1(values(6).toDouble)
            val eloPre2 = EloPres2.EloPre2(values(7).toDouble)
            val mlbPre1 = MlbPres1.MlbPre1(values(12).toDouble)
            val mlbPre2 = MlbPres2.MlbPre2(values(13).toDouble)
            Game(date, season, playoffRound, homeTeam, awayTeam, eloPre1, eloPre2, mlbPre1, mlbPre2)
        }
        .grouped(1000)
        .foreach(chunk => insertRows(chunk.toList))
        _ <- ZIO.succeed(source.close())
    }yield Response.text("Database initialized")

    initLogic.catchAll(t => ZIO.succeed(Response.text(s"Error initializing database: ${t.getMessage}")))
  }

  import HomeTeams.*
  import AwayTeams.*

  //Insert data inside games table
  def insertRows(games: List[Game]): ZIO[ZConnectionPool, Throwable, UpdateResult] = {
    val rows: List[Game.Row] = games.map(_.toRow)
    transaction {
      insert(
        sql"INSERT INTO games(date, season_year, playoff_round, home_team, away_team, elo1_pre, elo2_pre, rating1_pre, rating2_pre)".values[Game.Row](rows)
      )
    }
  }

  //Select every games stored
  val count: ZIO[ZConnectionPool, Throwable, Option[Int]] = transaction {
    selectOne(
      sql"SELECT COUNT(*) FROM games".as[Int]
    )
  }

  //Retrieve the last game between two teams
  def latest(homeTeam: HomeTeam, awayTeam: AwayTeam): ZIO[ZConnectionPool, Throwable, Option[Game]] = {
    transaction {
      selectOne(
        sql"SELECT date, season_year, playoff_round, home_team, away_team, elo1_pre, elo2_pre, rating1_pre, rating2_pre FROM games WHERE home_team = ${HomeTeam.unapply(homeTeam)} AND away_team = ${AwayTeam.unapply(awayTeam)} ORDER BY date DESC LIMIT 1".as[Game]
      )
    }
  }

  //Retrieve ELO predictions
  def getEloRatings(homeTeam: HomeTeam, awayTeam: AwayTeam): ZIO[ZConnectionPool, Throwable, (Double, Double)] = {
    transaction {
        for {
        homeElo <- selectOne(sql"SELECT elo1_pre FROM games WHERE home_team = ${HomeTeam.unapply(homeTeam)} AND away_team = ${AwayTeam.unapply(awayTeam)}".as[Double]).map(_.getOrElse(1500.0))
        awayElo <- selectOne(sql"SELECT elo2_pre FROM games WHERE home_team = ${HomeTeam.unapply(homeTeam)} AND away_team = ${AwayTeam.unapply(awayTeam)}".as[Double]).map(_.getOrElse(1500.0))
        } yield (homeElo, awayElo)
    }
  }

  //Retrieve MLB predictions
  def getMlbRatings(homeTeam: HomeTeam, awayTeam: AwayTeam): ZIO[ZConnectionPool, Throwable, (Double, Double)] = {
    transaction {
        for {
        homeElo <- selectOne(sql"SELECT rating1_pre FROM games WHERE home_team = ${HomeTeam.unapply(homeTeam)} AND away_team = ${AwayTeam.unapply(awayTeam)}".as[Double]).map(_.getOrElse(1500.0))
        awayElo <- selectOne(sql"SELECT rating2_pre FROM games WHERE home_team = ${HomeTeam.unapply(homeTeam)} AND away_team = ${AwayTeam.unapply(awayTeam)}".as[Double]).map(_.getOrElse(1500.0))
        } yield (homeElo, awayElo)
    }
  }

  //Retrieve games from a specific season
  def season(year: SeasonYear): ZIO[ZConnectionPool, Throwable, List[Game]] = {
    transaction {
        selectAll(
            sql"SELECT date, season_year, playoff_round, home_team, away_team, elo1_pre, elo2_pre, rating1_pre, rating2_pre FROM games WHERE season_year = ${SeasonYear.unapply(year)} ORDER BY date LIMIT 20".as[Game]
        ).map(_.toList)
    }
  }

  //Retrieve every games of a specific team
  def history(homeTeam: HomeTeam): ZIO[ZConnectionPool, Throwable, List[Game]] = {
    transaction {
      selectAll(
        sql"SELECT  date, season_year, playoff_round, home_team, away_team, elo1_pre, elo2_pre, rating1_pre, rating2_pre FROM games WHERE home_team = ${HomeTeam.unapply(homeTeam)} ORDER BY date DESC LIMIT 20".as[Game]
      ).map(_.toList)
    }
  }
}
