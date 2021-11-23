package com.example.domain

import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.testkit.EventSourcedResult
import com.example
import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class TurkeySpec extends AnyWordSpec with Matchers {
  "The Turkey" should {
    "have example test that can be removed" in {
      val testKit = TurkeyTestKit(new Turkey(_))
      // use the testkit to execute a command:
      // val result: EventSourcedResult[R] = testKit.someOperation(SomeRequest("id"));
      // verify the emitted events
      // val actualEvent: ExpectedEvent = result.nextEventOfType[ExpectedEvent]
      // actualEvent shouldBe expectedEvent
      // verify the final state after applying the events
      // testKit.state() shouldBe expectedState
      // verify the response
      // result.reply shouldBe expectedReply
      // verify the final state after the command
    }

    "correctly process commands of type StartCooking" in {
      val testKit = TurkeyTestKit(new Turkey(_))
      // val result: EventSourcedResult[Empty] = testKit.startCooking(example.CookingCommand(...))
    }

    "correctly process commands of type EndCooking" in {
      val testKit = TurkeyTestKit(new Turkey(_))
      // val result: EventSourcedResult[Empty] = testKit.endCooking(example.CookingCommand(...))
    }

    "correctly process commands of type IncreaseOvenTemperature" in {
      val testKit = TurkeyTestKit(new Turkey(_))
      // val result: EventSourcedResult[Empty] = testKit.increaseOvenTemperature(example.TemperatureChangeCommand(...))
    }

    "correctly process commands of type DecreaseOvenTemperature" in {
      val testKit = TurkeyTestKit(new Turkey(_))
      // val result: EventSourcedResult[Empty] = testKit.decreaseOvenTemperature(example.TemperatureChangeCommand(...))
    }

    "correctly process commands of type GetCurrentTurkey" in {
      val testKit = TurkeyTestKit(new Turkey(_))
      // val result: EventSourcedResult[TurkeyState] = testKit.getCurrentTurkey(example.GetTurkeyCommand(...))
    }
  }
}
