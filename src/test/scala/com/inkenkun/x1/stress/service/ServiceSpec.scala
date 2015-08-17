package com.inkenkun.x1.stress.service

import akka.event.NoLogging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._

class ServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with Service {
  override def testConfigSource = "akka.loglevel = WARNING"
  override def config = testConfig
  override val logger = NoLogging

  "Service" should "respond to /strong and /weak" in {
    Get( "/strong/hogehoge%3A2015-08-17" ) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val res = responseAs[Message]
      res.message shouldBe "find date: 2015-08-17"
    }

    Get( "/strong/2015%2F8%2F7" ) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val res = responseAs[Message]
      res.message shouldBe "find date: 2015-8-7"
    }

    Get( "/weak/2015%e5%b9%b408%e6%9c%8817%e6%97%a5:foobar" ) ~> routes ~> check { // 2015年08月17日:foobar
      status shouldBe OK
      contentType shouldBe `application/json`
      val res = responseAs[Message]
      res.message shouldBe "find date: 2015-08-17"
    }
  }

  it should "respond with bad request for query without date-string when /strong" in {
    Get( "/strong/hogehoge" ) ~> routes ~> check {
      status shouldBe BadRequest
      val res = responseAs[Message]
      res.message shouldBe "cannot find date string"
    }
  }

  it should "respond with bad request for query without date-string when /weak" in {
    Get( "/weak/hogehoge" ) ~> routes ~> check {
      status shouldBe BadRequest
      val res = responseAs[Message]
      res.message shouldBe "cannot find date string"
    }
  }

}
