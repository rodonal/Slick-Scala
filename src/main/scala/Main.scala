package org.slick.tutorial

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


  val shawShankRedemption = Movie(1, "ShawSHank Redemption", LocalDate.of(1994, 9, 23), 162)
  val theMatrix = Movie(2, "THe Matrix", LocalDate.of(1999, 12, 1), 150)


  def demoInsertMovie(): Unit = {
    val queryDescription = SlickTables.movieTable += theMatrix
    val futureId: Future[Int] = Connection.db.run(queryDescription)

    futureId.onComplete {
      case Success(newMovieID) => println(s"QUery was successful , $newMovieID")
      case Failure(ex) => println(s"Faile with error $ex")

    }
    Thread.sleep(10000)
  }


  def demoReallAllMovies(): Unit = {

    val resultFuture: Future[Seq[Movie]] = Connection.db.run(SlickTables.movieTable.result) //select * from movieTable
    resultFuture.onComplete {
      case Success(movies) => println(s"Fetched ${movies.mkString(", ")}")
      case Failure(ex) => println(s"Error fetching:  $ex")
    }
    Thread.sleep(10000)
  }

  def demoReadSomeMovies(): Unit = {
     //select * from movies where name like "Matrix"
    val resultFuture: Future[Seq[Movie]] = Connection.db.run(SlickTables.movieTable.filter(movie => movie.name.like("%Matrix%")).result) //select * from movieTable
    resultFuture.onComplete {
      case Success(movies) => println(s"Fetched ${movies.mkString(", ")}")
      case Failure(ex) => println(s"Error fetching:  $ex")
    }
    Thread.sleep(10000)
  }


  def demoUpdateMovie() :  Unit = {

    val queryDescriptor = SlickTables.movieTable.filter(movie => movie.id === 1L).update(shawShankRedemption.copy(lengthInMin = 150))
    val futureId: Future[Int] = Connection.db.run(queryDescriptor)

    futureId.onComplete {
      case Success(newMovieID) => println(s"QUery was successful , $newMovieID")
      case Failure(ex) => println(s"Faile with error $ex")

    }
    Thread.sleep(10000)

  }

  def demoDeleteMovie() : Unit = {
    val queryDescriptor = SlickTables.movieTable.filter(movie => movie.id === 2L).delete
    Connection.db.run(queryDescriptor)
    Thread.sleep(10000)

  }

  def main(args: Array[String]): Unit = {
    //demoInsertMovie()
    //demoReallAllMovies()
    //demoReadSomeMovies()
    //demoUpdateMovie()
    demoDeleteMovie()
  }


}
