# Class Exam Instruction: Building a ZIO Application Backend

## Team

Benjamin BERNARD

François CHARVET

Stanley DELLON

## Requirements
To run the Sudoku solver, you will need the following:

Scala 3.3.0

sbt

## Topic

Building a REST API using the "Major League Baseball Dataset" from [Kaggle](https://www.kaggle.com/datasets/saurabhshahane/major-league-baseball-dataset).

### Dataset Description
The "Major League Baseball Dataset" from Kaggle is a comprehensive collection of data related to Major League Baseball (MLB) games, players, teams, and statistics. The dataset contains information about game-by-game Elo ratings and forecasts back to 1871. You can visit the Kaggle page for a more detailed description of the dataset.

The dataset is available in CSV format: `mlb_elo.csv` contains all data: `mlb_elo_latest.csv` contains data for only the latest season. No need to register and download the files from Kaggle, they are available in Teams group's files tab.

### Ratings Systems: ELO and MLB Predictions
The dataset includes two ratings systems, ELO and MLB Predictions, which are used to evaluate teams' performance and predict game outcomes:

1. **ELO**: The ELO rating system is a method for calculating the relative skill levels of teams in two-player games, such as chess. In the context of MLB, the ELO rating system assigns a numerical rating to each team, which reflects their relative strength. The rating is updated based on game outcomes, with teams gaining or losing points depending on the result of the match.

2. **MLB Predictions**: The MLB Predictions rating system utilizes various statistical models and algorithms to predict game outcomes. It takes into account factors such as team performance, player statistics, historical data, and other relevant factors to generate predictions for upcoming games.

## Getting Started
Follow these steps to run the Sudoku solver:

Clone the Git repository: git clone <repository-url>

Change into the project directory: cd sudokusolver

Build the project: sbt compile

Run the solver: sbt run

## Data Structure
To get content of each important columns, we used Game's case class which is composed of several objects (columns) such as SeasonYear, HomeTeam, AwayTeam, Elo / Mlb predictions...

# MlbApi file Structure

Endpoints part which contains each endpoint and calls required functions

ApiService which controls responses and calculation

DataService which contains everything related to the database => Initialization, Requests...

## Endpoints 

/init => To initialize the database

/game/latest/homeTeam/awayTeam => To get the last game between two teams

/game/predict/homeTeam/awayTeam => To predict the probability of winning using ELO and MB rating systems

/games/count => To get the total of games from the database

/games/history/homeTeam => To get all games of a specific teams

/games/season/year => To get all games for a specific season

## Error Handling
The MlbApi handles various error scenarios using ZIO's error handling capabilities.

## Libraries Used
The MlbApi uses the following external libraries:
zio - Provides the core functionality for effectful programming, error handling, and console interaction.
zio-jdbc - ZIO integration for JDBC, providing a type-safe and composable way to work with relational databases in a purely functional manner.
zio-streams - Functional streaming library built on top of ZIO. It provides a composable and high-performance stream processing API that allows you to process and transform large streams of data in a purely functional way.
zio-http - A type-safe and purely functional HTTP client and server library based on ZIO
tototoshi/scala-csv - https://github.com/tototoshi/scala-csv - To process CSV
