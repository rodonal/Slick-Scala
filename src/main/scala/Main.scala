package org.slick.tutorial

import slick.jdbc.GetResult

import java.time.LocalDate
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object PrivateExecutionContext {

  //onComplete requires a THreadPool on which to run, create an implicit execution context
  val executor = Executors.newFixedThreadPool(4)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(executor)

}

object Main {

  import slick.jdbc.PostgresProfile.api._
  import PrivateExecutionContext._

  val shawShankRedemption = Movie(1L, "ShawSHank Redemption", LocalDate.of(1994, 9, 23), 162)
  val theMatrix = Movie(2L, "THe Matrix", LocalDate.of(1999, 12, 1), 140)
  val redionXhepa = Actor(1L, "Redion Xhepa")

  val theMatrix2 = Movie(2L, "THe Matrix 2", LocalDate.of(2002, 11, 1), 100)
  val redionXhepa2 = Actor(1L, "Redion Xhepa 2")

  def demoInsertMovie(): Unit = {
    val queryDescription = SlickTables.movieTable += theMatrix
    val futureId: Future[Int] = Connection.db.run(queryDescription)

    futureId.onComplete {
      case Success(newMovieID) => println(s"QUery was successful , $newMovieID")
      case Failure(ex) => println(s"Faile with error $ex")

    }
    Thread.sleep(5000)
  }

  def demoInsertActor(): Unit = {
    val queryDescription = SlickTables.actorTable ++= Seq(redionXhepa)
    val futureId = Connection.db.run(queryDescription)

    futureId.onComplete {
      case Success(_) => println(s"QUery was successful to add actors")
      case Failure(ex) => println(s"Faile with error $ex")

    }
    Thread.sleep(5000)
  }

  def demoReallAllMovies(): Unit = {

    val resultFuture: Future[Seq[Movie]] =
      Connection.db.run(SlickTables.movieTable.result) //select * from movieTable
    resultFuture.onComplete {
      case Success(movies) => println(s"Fetched ${movies.mkString(", ")}")
      case Failure(ex) => println(s"Error fetching:  $ex")
    }
    Thread.sleep(5000)
  }

  def demoReadSomeMovies(): Unit = {
    //select * from movies where name like "Matrix"
    val resultFuture: Future[Seq[Movie]] = Connection.db.run(
      SlickTables.movieTable.filter(movie => movie.name.like("%Matrix%")).result
    ) //select * from movieTable
    resultFuture.onComplete {
      case Success(movies) => println(s"Fetched ${movies.mkString(", ")}")
      case Failure(ex) => println(s"Error fetching:  $ex")
    }
    Thread.sleep(5000)
  }

  def demoUpdateMovie(): Unit = {

    val queryDescriptor = SlickTables.movieTable
      .filter(movie => movie.id === 1L)
      .update(shawShankRedemption.copy(lengthInMin = 150))
    val futureId: Future[Int] = Connection.db.run(queryDescriptor)

    futureId.onComplete {
      case Success(newMovieID) => println(s"QUery was successful , $newMovieID")
      case Failure(ex) => println(s"Faile with error $ex")

    }
    Thread.sleep(5000)

  }

  def demoDeleteMovie(): Unit = {
    val queryDescriptor = SlickTables.movieTable.filter(movie => movie.id === 2L).delete
    Connection.db.run(queryDescriptor)
    Thread.sleep(5000)

  }

  def demoReadMovieBySqlQuery(): Future[Vector[Movie]] = {

    implicit val getResultMovie: GetResult[Movie] = {
      //<< will parse the properties of Movie
      GetResult(positionedResult =>
        Movie(
          positionedResult.<<,
          positionedResult.<<,
          LocalDate.parse(positionedResult.nextString()), //There is no custom parser by <<
          positionedResult.<<))
    }
    val query = sql"""select * from movies."Movie"""".as[Movie]
    Connection.db.run(query)

  }

  def multipleQueriesSingleTransaction(): Unit = {

    val insertMovie = SlickTables.movieTable += theMatrix2
    val insertActor = SlickTables.actorTable += redionXhepa2

    val finalQuery = DBIO.seq(insertMovie, insertActor)

    Connection.db.run(finalQuery.transactionally) //transactionally if one fails all rolls back

  }

  def findAllActorsByMovie(movieId: Long): Future[Seq[Actor]] = {
    val joinQuery = SlickTables.actorMovieMappingTable
      .filter(_.movieId === movieId)
      .join(SlickTables.actorTable)
      .on(_.actorId === _.id)
      .map(_._2)
    //select * from movieActorMapping m join actorTable a   on m.actorId == a.id
    Connection.db.run(joinQuery.result)
  }

  def main(args: Array[String]): Unit = {
    //demoInsertMovie()
    //demoReallAllMovies()
    //demoReadSomeMovies()
    //demoUpdateMovie()
    //demoDeleteMovie()
    //demoInsertActor()

    findAllActorsByMovie(1L).onComplete {
      case Success(result) => println(result)
      case Failure(ex) => println((s"Error happened: $ex"))
    }

    Thread.sleep(5000)
    PrivateExecutionContext.executor.shutdown()

  }

}
