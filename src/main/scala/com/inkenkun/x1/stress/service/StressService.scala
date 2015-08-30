package com.inkenkun.x1.stress.service

import java.util.UUID
import java.util.concurrent.{TimeoutException, TimeUnit}

import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._
import scala.util.Random

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.pattern.after
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

  val normalStore1: mutable.HashMap[String, String]     = mutable.HashMap.empty[String, String]
  val normalStore2: mutable.HashMap[String, String]     = mutable.HashMap.empty[String, String]
  val normalStore3: mutable.HashMap[String, String]     = mutable.HashMap.empty[String, String]
  val normalStore4: mutable.HashMap[String, String]     = mutable.HashMap.empty[String, String]
  val normalStore5: mutable.HashMap[String, String]     = mutable.HashMap.empty[String, String]

  val weakStore1  : mutable.WeakHashMap[String, String] = mutable.WeakHashMap.empty[String, String]
  val weakStore2  : mutable.WeakHashMap[String, String] = mutable.WeakHashMap.empty[String, String]
  val weakStore3  : mutable.WeakHashMap[String, String] = mutable.WeakHashMap.empty[String, String]
  val weakStore4  : mutable.WeakHashMap[String, String] = mutable.WeakHashMap.empty[String, String]
  val weakStore5  : mutable.WeakHashMap[String, String] = mutable.WeakHashMap.empty[String, String]

  val normalRxStore: mutable.HashMap[String, Either[String, String]]     = mutable.HashMap.empty[String, Either[String, String]]
  val weakRxStore  : mutable.WeakHashMap[String, Either[String, String]] = mutable.WeakHashMap.empty[String, Either[String, String]]

  lazy val random    = new Random
  lazy val timeout   = config.getDuration( "service.timeout", TimeUnit.SECONDS ) second
  lazy val dummyData = config.getStringList( "service.dummy.data" )

  def config: Config
  val logger: LoggingAdapter

  def process( segment: String, isNormal: Boolean = true ): Future[Either[Message, Message]] = {
    val start = System.currentTimeMillis()

    val n = random.nextInt( 4 ) + 1

    /** データを格納するMapを選択する */
    val ss = if ( isNormal ) {
      n match {
        case 1 => normalStore1
        case 2 => normalStore2
        case 3 => normalStore3
        case 4 => normalStore4
        case 5 => normalStore5
      }
    } else {
      n match {
        case 1 => weakStore1
        case 2 => weakStore2
        case 3 => weakStore3
        case 4 => weakStore4
        case 5 => weakStore5
      }
    }

    // 100byte * 1000 = 100kbの文字列を生成
    val dummyText = ( 1 to 1000 ).foldLeft( new StringBuilder ) { ( acc, n ) =>
      val rn = random.nextInt( 10 )
      acc ++= dummyData( rn )
    } toString

    ss += ( UUID.randomUUID().toString -> dummyText )

    /** 正規表現を使ってごにょごにょする */
    val rexs = ( 1 to 10 ).map { x =>
      rex( segment )
    }
    val rs = if ( isNormal ) normalRxStore else weakRxStore
    rexs.foldLeft( rs ) { ( acc, n ) =>
      acc += ( UUID.randomUUID().toString -> n )
    }

    Future.successful( rexs.head match {
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
            val future = process( segment ).map[ToResponseMarshallable] {
              case Right( message ) => message
              case Left( message ) => BadRequest -> message
            }
            Future.firstCompletedOf(
              future ::
                after( timeout, system.scheduler )( Future.failed(new TimeoutException) ) ::
                Nil
            )
          }
        }
      } ~
      pathPrefix( "weak" ) {
        /** 弱参照オブジェクトにデータをためる */
        ( get & path( Segment ) ) { segment =>
          complete {
            val future = process( segment, false ).map[ToResponseMarshallable] {
              case Right( message ) => message
              case Left( message ) => BadRequest -> message
            }
            Future.firstCompletedOf(
              future ::
                after( timeout, system.scheduler )( Future.failed(new TimeoutException) ) ::
                Nil
            )
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
