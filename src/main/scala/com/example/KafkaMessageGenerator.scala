package com.example

import java.util.UUID
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}

import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.header.Header
import java.nio.charset.StandardCharsets
import java.util.Properties
import java.io.FileReader

import com.example.TemperatureChangeCommand

object KafkaMessageGenerator { 
  lazy private val config = ConfigFactory.load()
  private var id = UUID.randomUUID().toString
  private var direction = "increase"
  private var amount = "0"

  def main(args: Array[String]): Unit = {
    require(args.length == 3, "usage: path foo whatever")
    val Array(id: String, direction: String, amount: String) = if (args.length == 3) args 
    this.id = id
    this.direction = direction
    this.amount = amount

    implicit val system: ActorSystem = ActorSystem("system")
    implicit val executionContext: ExecutionContext = system.dispatcher

    val done: Future[Done] = source.runWith(sink)

    done.onComplete(_ => system.terminate())
  }

  private def source: Source[ProducerRecord[String, String], NotUsed] = {
    val topic = direction match {
      case "increase" => "increase_temp"
      case "decrease" => "decrease_temp"
    }
    val headers = Map("ce-type" -> "com.example.TemperatureChangeCommand", "ce-specversion" -> "1.0", "ce-datacontenttype" -> "application/protobuf")
    val kafkaHeaders: java.lang.Iterable[Header] = headers.map {
      case (headerKey, headerValue) =>
        new RecordHeader(headerKey, headerValue.getBytes(StandardCharsets.UTF_8)): Header
    }.asJava

    val tc = TemperatureChangeCommand(turkeyId=id, temperatureChange=amount.toFloat)
    
    Source(1 to 1)
      .map { i =>
        new ProducerRecord[String, String](topic, null, null, null, tc.toByteString.toString("UTF-8"), kafkaHeaders)
      }
  }

  private def sink: Sink[ProducerRecord[String, String], Future[Done]] = {
    val producerConfig = config.getConfig("akka.kafka.producer")
    val props = buildProperties("./conf/kafka.properties")
    val bootstrapServers = props.getProperty("bootstrap.servers.local")

    val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)

    Producer.plainSink(producerSettings)
  }

  private def buildProperties(configFileName: String): Properties = {
    val properties: Properties = new Properties
    properties.load(new FileReader(configFileName))
    properties
  }  
}