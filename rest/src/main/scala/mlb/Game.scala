package mlb

import zio.json._
import zio.jdbc._

import java.time.LocalDate

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

object EloProbs1{
  opaque type EloProb1 = Double

  object EloProb1 {

    def apply(value: Double): EloProb1 = value

    def unapply(eloProb1: EloProb1): Double = eloProb1
  }

  given CanEqual[EloProb1, EloProb1] = CanEqual.derived
  implicit val eloProb1Encoder: JsonEncoder[EloProb1] = JsonEncoder.double
  implicit val eloProb1Decoder: JsonDecoder[EloProb1] = JsonDecoder.double
}


object EloProbs2{
  opaque type EloProb2 = Double
  object EloProb2 {

    def apply(value: Double): EloProb2 = value

    def unapply(eloProb2: EloProb2): Double = eloProb2
  }

  given CanEqual[EloProb2, EloProb2] = CanEqual.derived
  implicit val eloProb2Encoder: JsonEncoder[EloProb2] = JsonEncoder.double
  implicit val eloProb2Decoder: JsonDecoder[EloProb2] = JsonDecoder.double
}


object MlbProbs1{
  opaque type MlbProb1 = Double
  object MlbProb1 {

    def apply(value: Double): MlbProb1 = value

    def unapply(mlbProb1: MlbProb1): Double = mlbProb1
  }

  given CanEqual[MlbProb1, MlbProb1] = CanEqual.derived
  implicit val mlbProb1Encoder: JsonEncoder[MlbProb1] = JsonEncoder.double
  implicit val mlbProb1Decoder: JsonDecoder[MlbProb1] = JsonDecoder.double
}

object MlbProbs2{
  opaque type MlbProb2 = Double
  object MlbProb2 {

    def apply(value: Double): MlbProb2 = value

    def unapply(mlbProb2: MlbProb2): Double = mlbProb2
  }

  given CanEqual[MlbProb2, MlbProb2] = CanEqual.derived
  implicit val mlbProb2Encoder: JsonEncoder[MlbProb2] = JsonEncoder.double
  implicit val mlbProb2Decoder: JsonDecoder[MlbProb2] = JsonDecoder.double
}
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
import EloProbs1.*
import EloProbs2.*
import MlbProbs1.*
import MlbProbs2.*

final case class Game(
                       date: GameDate,
                       season: SeasonYear,
                       playoffRound: PlayoffRound,
                       homeTeam: HomeTeam,
                       awayTeam: AwayTeam,
                       eloProb1: EloProb1,
                       eloProb2: EloProb2,
                       mlbProb1: MlbProb1,
                       mlbProb2: MlbProb2

                     )

object Game {

  given CanEqual[Game, Game] = CanEqual.derived
  implicit val gameEncoder: JsonEncoder[Game] = DeriveJsonEncoder.gen[Game]
  implicit val gameDecoder: JsonDecoder[Game] = DeriveJsonDecoder.gen[Game]

  def unapply(game: Game): (GameDate, SeasonYear, PlayoffRound, HomeTeam, AwayTeam, EloProb1, EloProb2, MlbProb1, MlbProb2) =
    (game.date, game.season, game.playoffRound, game.homeTeam, game.awayTeam, game.eloProb1, game.eloProb2, game.mlbProb1, game.mlbProb2)

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
        EloProb1.unapply(e1),
        EloProb2.unapply(e2),
        MlbProb1.unapply(m1),
        MlbProb2.unapply(m2)
      )

  implicit val jdbcDecoder: JdbcDecoder[Game] = JdbcDecoder[Row]().map[Game] { t =>
    val (date, season, maybePlayoff, home, away, elo_prob1, elo_prob2, mlb_prob1, mlb_prob2) = t
    Game(
      GameDate(LocalDate.parse(date)),
      SeasonYear(season),
      PlayoffRound(maybePlayoff),
      HomeTeam(home),
      AwayTeam(away),
      EloProb1(elo_prob1),
      EloProb2(elo_prob2),
      MlbProb1(mlb_prob1),
      MlbProb2(mlb_prob2)
    )
  }
}

val games: List[Game] = List(
  //Game(GameDate(LocalDate.parse("2021-10-03")), SeasonYear(2023), PlayoffRound(0), HomeTeam("ATL"), AwayTeam("NYM")),
  //Game(GameDate(LocalDate.parse("2021-10-03")), SeasonYear(2023), PlayoffRound(0), HomeTeam("STL"), AwayTeam("CHC"))
)