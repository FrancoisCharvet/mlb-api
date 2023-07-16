package mlb

import zio.json._
import zio.jdbc._

import java.time.LocalDate

//Object to map values from homeTeams column
object HomeTeams {

  opaque type HomeTeam = String

  object HomeTeam {

    def apply(value: String): HomeTeam = value

    def unapply(homeTeam: HomeTeam): String = homeTeam
  }

  given CanEqual[HomeTeam, HomeTeam] = CanEqual.derived
  implicit val homeTeamEncoder: JsonEncoder[HomeTeam] = JsonEncoder.string
  implicit val homeTeamDecoder: JsonDecoder[HomeTeam] = JsonDecoder.string
}

//Object to map values from ELO prediction column 1
object EloPres1{
  opaque type EloPre1 = Double

  object EloPre1 {

    def apply(value: Double): EloPre1 = value

    def unapply(eloPre1: EloPre1): Double = eloPre1
  }

  given CanEqual[EloPre1, EloPre1] = CanEqual.derived
  implicit val eloPre1Encoder: JsonEncoder[EloPre1] = JsonEncoder.double
  implicit val eloPre1Decoder: JsonDecoder[EloPre1] = JsonDecoder.double
}

//Object to map values from ELO column 2
object EloPres2{
  opaque type EloPre2 = Double
  object EloPre2 {

    def apply(value: Double): EloPre2 = value

    def unapply(eloPre2: EloPre2): Double = eloPre2
  }

  given CanEqual[EloPre2, EloPre2] = CanEqual.derived
  implicit val eloPre2Encoder: JsonEncoder[EloPre2] = JsonEncoder.double
  implicit val eloPre2Decoder: JsonDecoder[EloPre2] = JsonDecoder.double
}

//Object to map values from MLB column1
object MlbPres1{
  opaque type MlbPre1 = Double
  object MlbPre1 {

    def apply(value: Double): MlbPre1 = value

    def unapply(mlbPre1: MlbPre1): Double = mlbPre1
  }

  given CanEqual[MlbPre1, MlbPre1] = CanEqual.derived
  implicit val mlbPre1Encoder: JsonEncoder[MlbPre1] = JsonEncoder.double
  implicit val mlbPre1Decoder: JsonDecoder[MlbPre1] = JsonDecoder.double
}

//Object to map values from MLB column 2
object MlbPres2{
  opaque type MlbPre2 = Double
  object MlbPre2 {

    def apply(value: Double): MlbPre2 = value

    def unapply(mlbPre2: MlbPre2): Double = mlbPre2
  }

  given CanEqual[MlbPre2, MlbPre2] = CanEqual.derived
  implicit val mlbPre2Encoder: JsonEncoder[MlbPre2] = JsonEncoder.double
  implicit val mlbPre2Decoder: JsonDecoder[MlbPre2] = JsonDecoder.double
}

//Object to map values from AwayTeams column
object AwayTeams {

  opaque type AwayTeam = String

  object AwayTeam {

    def apply(value: String): AwayTeam = value

    def unapply(awayTeam: AwayTeam): String = awayTeam
  }

  given CanEqual[AwayTeam, AwayTeam] = CanEqual.derived
  implicit val awayTeamEncoder: JsonEncoder[AwayTeam] = JsonEncoder.string
  implicit val awayTeamDecoder: JsonDecoder[AwayTeam] = JsonDecoder.string
}

//Object to map values from dates column
object GameDates {

  opaque type GameDate = LocalDate

  object GameDate {

    def apply(value: LocalDate): GameDate = value

    def unapply(gameDate: GameDate): LocalDate = gameDate
  }

  given CanEqual[GameDate, GameDate] = CanEqual.derived
  implicit val gameDateEncoder: JsonEncoder[GameDate] = JsonEncoder.localDate
  implicit val gameDateDecoder: JsonDecoder[GameDate] = JsonDecoder.localDate
}

//Object to map values from season column
object SeasonYears {

  opaque type SeasonYear <: Int = Int

  object SeasonYear {

    def apply(year: Int): SeasonYear = year

    def safe(value: Int): Option[SeasonYear] =
      Option.when(value >= 1876 && value <= LocalDate.now.getYear)(value)

    def unapply(seasonYear: SeasonYear): Int = seasonYear
  }

  given CanEqual[SeasonYear, SeasonYear] = CanEqual.derived
  implicit val seasonYearEncoder: JsonEncoder[SeasonYear] = JsonEncoder.int
  implicit val seasonYearDecoder: JsonDecoder[SeasonYear] = JsonDecoder.int
}

//Object to map values from playoff column
object PlayoffRounds {

  opaque type PlayoffRound <: Int = Int

  object PlayoffRound {

    def apply(round: Int): PlayoffRound = round

    def safe(value: Int): Option[PlayoffRound] =
      Option.when(value >= 1 && value <= 4)(value)

    def unapply(playoffRound: PlayoffRound): Int = playoffRound
  }

  given CanEqual[PlayoffRound, PlayoffRound] = CanEqual.derived
  implicit val playoffRoundEncoder: JsonEncoder[PlayoffRound] = JsonEncoder.int
  implicit val playoffRoundDEncoder: JsonDecoder[PlayoffRound] = JsonDecoder.int
}


import GameDates.*
import PlayoffRounds.*
import SeasonYears.*
import HomeTeams.*
import AwayTeams.*
import EloPres1.*
import EloPres2.*
import MlbPres1.*
import MlbPres2.*

//Case class which represents a Game with all of its attributes
final case class Game(
                       date: GameDate,
                       season: SeasonYear,
                       playoffRound: PlayoffRound,
                       homeTeam: HomeTeam,
                       awayTeam: AwayTeam,
                       eloPre1: EloPre1,
                       eloPre2: EloPre2,
                       mlbPre1: MlbPre1,
                       mlbPre2: MlbPre2

                     )

object Game {

  given CanEqual[Game, Game] = CanEqual.derived
  implicit val gameEncoder: JsonEncoder[Game] = DeriveJsonEncoder.gen[Game]
  implicit val gameDecoder: JsonDecoder[Game] = DeriveJsonDecoder.gen[Game]

  def unapply(game: Game): (GameDate, SeasonYear, PlayoffRound, HomeTeam, AwayTeam, EloPre1, EloPre2, MlbPre1, MlbPre2) =
    (game.date, game.season, game.playoffRound, game.homeTeam, game.awayTeam, game.eloPre1, game.eloPre2, game.mlbPre1, game.mlbPre2)

  // a custom decoder from a tuple
  type Row = (String, Int, Int, String, String, Double, Double, Double, Double)

  extension (g:Game)
    def toRow: Row =
      val (d, y, p, h, a, e1, e2, m1, m2) = Game.unapply(g)
      (
        GameDate.unapply(d).toString,
        SeasonYear.unapply(y),
        PlayoffRound.unapply(p),
        HomeTeam.unapply(h),
        AwayTeam.unapply(a),
        EloPre1.unapply(e1),
        EloPre2.unapply(e2),
        MlbPre1.unapply(m1),
        MlbPre2.unapply(m2)
      )

  implicit val jdbcDecoder: JdbcDecoder[Game] = JdbcDecoder[Row]().map[Game] { t =>
    val (date, season, maybePlayoff, home, away, elo1_pre, elo2_pre, rating1_pre, rating2_pre) = t
    Game(
      GameDate(LocalDate.parse(date)),
      SeasonYear(season),
      PlayoffRound(maybePlayoff),
      HomeTeam(home),
      AwayTeam(away),
      EloPre1(elo1_pre),
      EloPre2(elo2_pre),
      MlbPre1(rating1_pre),
      MlbPre2(rating2_pre)
    )
  }
}

val games: List[Game] = List(
  //Game(GameDate(LocalDate.parse("2021-10-03")), SeasonYear(2023), PlayoffRound(0), HomeTeam("ATL"), AwayTeam("NYM")),
  //Game(GameDate(LocalDate.parse("2021-10-03")), SeasonYear(2023), PlayoffRound(0), HomeTeam("STL"), AwayTeam("CHC"))
)