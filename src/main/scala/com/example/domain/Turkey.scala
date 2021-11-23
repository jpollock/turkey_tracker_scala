package com.example.domain

import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
import com.example
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** An event sourced entity. */
class Turkey(context: EventSourcedEntityContext) extends AbstractTurkey {
  override def emptyState: TurkeyState = TurkeyState()

  override def startCooking(currentState: TurkeyState, cookingCommand: example.CookingCommand): EventSourcedEntity.Effect[Empty] = {
    effects
      .emitEvent(InOven(turkeyId=cookingCommand.turkeyId)) 
      .thenReply(_ => Empty.defaultInstance) 
  }
 
  override def endCooking(currentState: TurkeyState, cookingCommand: example.CookingCommand): EventSourcedEntity.Effect[Empty] = {
    effects
      .emitEvent(OutOfOven(turkeyId=cookingCommand.turkeyId)) 
      .thenReply(_ => Empty.defaultInstance) 
  }

  override def increaseOvenTemperature(currentState: TurkeyState, temperatureChangeCommand: example.TemperatureChangeCommand): EventSourcedEntity.Effect[Empty] = {
    effects
      .emitEvent(TemperatureChange(turkeyId=temperatureChangeCommand.turkeyId, newTemperature=(currentState.externalTemperature + temperatureChangeCommand.temperatureChange))) 
      .thenReply(_ => Empty.defaultInstance) 
  }

  override def decreaseOvenTemperature(currentState: TurkeyState, temperatureChangeCommand: example.TemperatureChangeCommand): EventSourcedEntity.Effect[Empty]= {
    effects
      .emitEvent(TemperatureChange(turkeyId=temperatureChangeCommand.turkeyId, newTemperature=(currentState.externalTemperature - temperatureChangeCommand.temperatureChange))) 
      .thenReply(_ => Empty.defaultInstance) 
  }

  override def getCurrentTurkey(currentState: TurkeyState, getTurkeyCommand: example.GetTurkeyCommand): EventSourcedEntity.Effect[TurkeyState]= {
    effects.reply(currentState)
  }

  override def inOven(currentState: TurkeyState, inOven: InOven): TurkeyState = {
    currentState.copy(inOven = true)
  }

  override def outOfOven(currentState: TurkeyState, outOfOven: OutOfOven): TurkeyState = {
    currentState.copy(inOven = false)
  }

  override def temperatureChange(currentState: TurkeyState, temperatureChange: TemperatureChange): TurkeyState = {
    currentState.copy(externalTemperature = temperatureChange.newTemperature)
  }
}
