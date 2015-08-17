package com.inkenkun.x1.stress.service

import java.util.UUID

import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import spray.json.DefaultJsonProtocol


case class Message( message: String, interval: Long = 0L )


trait Protocols extends DefaultJsonProtocol {
  implicit val messageFormat = jsonFormat2(Message.apply)
}


trait Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  val normalStore: mutable.HashMap[String, BigDecimal]     = mutable.HashMap.empty[String, BigDecimal]
  val weakStore  : mutable.WeakHashMap[String, BigDecimal] = mutable.WeakHashMap.empty[String, BigDecimal]

  def config: Config
  val logger: LoggingAdapter

  def process( segment: String, isNormal: Boolean = true ): Future[Either[Message, Message]] = {
    val start = System.currentTimeMillis()

    /** シグモイド計算 */
    val sigs = ( 1 to 10000 ).map { x =>
      BigDecimal( ( tanh( x / 20000d ) + 1 ) / 2 )
    }

    /** Mapにデータを格納する */
    val s = if ( isNormal ) normalStore else weakStore

    sigs.foldLeft( s ) { ( acc, n ) =>
      acc += ( UUID.randomUUID().toString -> n )
    }

    /** 正規表現を使ってごにょごにょする */
    Future.successful( rex( segment ) match {
      case Right( message ) => Right( Message( message, System.currentTimeMillis() - start ) )
      case Left( error ) => Left( Message( error, System.currentTimeMillis() - start ) )
    } )
  }

  def rex( segment: String ): Either[String, String] = {

    // 日付っぽい文字列を見つける
    val rx = """.*(\d\d\d\d)[-/年](\d\d?)[-/月](\d\d?)日?.*""".r

    val r = segment match {
      case rx( year, month, day ) => Right( s"find date: $year-$month-$day" )
      case _ => Left( "cannot find date string" )
    }
    r
  }

  val routes = {
    logRequestResult( "stress-service" ) {
      pathPrefix( "strong" ) {
        /** 強参照オブジェクトにデータをためる */
        ( get & path( Segment ) ) { segment =>
          complete {
            process( segment ).map[ToResponseMarshallable] {
              case Right( message ) => message
              case Left( message ) => BadRequest -> message
            }
          }
        }
      } ~
      pathPrefix( "weak" ) {
        /** 弱参照オブジェクトにデータをためる */
        ( get & path( Segment ) ) { segment =>
          complete {
            process( segment, false ).map[ToResponseMarshallable] {
              case Right( message ) => message
              case Left( message ) => BadRequest -> message
            }
          }
        }
      }
    }
  }

}

object StressService extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle( routes, config.getString("http.interface"), config.getInt("http.port") )
}
