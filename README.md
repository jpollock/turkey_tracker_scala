# Distributed Real-Time Turkey Cooking With Scala, Akka Serverless and Kafka&reg;
Right now, my mind turns toward the wonderful feast awaiting me this coming Thanksgiving Thursday. But before I dig into the turkey, stuffing and pumpkin pie, I thought that I would whet my appetite with an antipasto of Scala, Akka Serverless and Kafka.

Thanksgiving is a big holiday here in the United States. It is a day when family and friends gather together to celebrate life and give thanks to the bounty that we enjoy. Also, we like to eat and drink on this day. A lot. And the foods we tend to enjoy are most often long observed traditions such as stuffing, pumpkin pies, cranberry sauce, mashed potatoes with the gravy made from the juices and nicely browned bits of the piece de resistance, the Thanksgiving Turkey.

Gluttonous consumption seems rather distasteful these days so I thought that it would be great to see how we could leverage technology to make sure that the food that we do being to the table is done so most efficiently and with maximum great taste. What better tools to bring into the picture than Akka - in the form of the new serverless offering - and Kafka?

There is plenty of content and data out there supporting the assertion that Akka is a highly efficient distributed data and streaming platform. These are some good articles and posts: [Towards a Reliable Device Management Platform](https://netflixtechblog.com/towards-a-reliable-device-management-platform-4f86230ca623), [Streaming a Million Likes/Second: Real-Time Interactions on Live Video](https://www.infoq.com/presentations/linkedin-play-akka-distributed-systems/), [Actors for Akka Stream Throughput](https://medium.com/expedia-group-tech/actors-for-akka-stream-throughput-65d97837b34b). But let's talk turkey!

Wait. Is this another blog post about the perfect recipe or most effective cooking method for that large bird you've procured from the local supermarket?  It is not. In fact, this blog post is intended really to let us play a bit with an interesting new developer platform product: [Akka Serverless](https://akkaserverless.com). And to do so with a fun, funny and utterly useless use case: how to enable a distributed team of cooks, experts, and industry, to create the perfect turkey! 

In order to not stuff ourselves too much, we'll keep our focus tight:

1. Standup a general turkey management API, using the event sourcing capabilities of Akka Serverless;
2. Integrate Kafka into the API, simulating an industry-hosted Machine Learning-based recommender system.

At the end of this article, you will have prepared yourself fully for a real Thanksgiving Feast!

Let's get going!

For this exercise, we will start with a partially cooked project. Go ahead and get your environment setup by cloning: `git clone git@github.com:jpollock/turkey_tracker_python.git`. The `main` brach is our starter, the partially cooked turkey, if you will. There are other branches that you can pull to get further along the cooking process. Oops. I mean "coding process"!

1. The `schema_change` branch has the modifications we will do in the first part of this exercise;
2. The `kafka_integration` branch has the modifications we will do in the latter part, i.e. when we hook up Kafka to Akka Serverless!

## What The Turkey Expects
In order to walk through the code in this post, you're going to want the following set up in your environment:
1. [Docker 20.10.8 or higher] (https://docs.docker.com/engine/install/);
2. [Python3](https://www.python.org/downloads/);
3. [Curl](https://curl.se/download.html).

## Initial State

To kick things off, let's walk through the initial part of the exercise..

1. In a terminal window, get into the root of the project directory, cloned above, e.g.  `cd turkey_tracker_python`;
2. You will want to create a [Virtual Environment](https://docs.python.org/3/library/venv.html): `python3 -m venv akkasls_env` (`akkasls_env` can be named whatever you want and located anywhere though);
3. Initiate the virtual environment: `source akkasls_env/bin/activate`;
4. Install the needed Python libraries: `pip install -r requirements.txt`;
5. In the same terminal window (or another with the virtual environment activated), start the partially cooked Turkey API: `start.sh`.

### Make Some API Calls
Let's start cooking!

	curl -X POST -H 'Content-Type: application/json' http://localhost:9000/com.example.TurkeyService/StartCooking -d '{"turkey_id": "myfirstturkey"}'

	curl -X POST -H 'Content-Type: application/json' http://localhost:9000/com.example.TurkeyService/GetCurrentTurkey -d '{"turkey_id": "myfirstturkey"}'

The response should be:

	{"inOven":true,"done":"RAW","externalTemperature":0.0}%

Let's stop cooking!

	curl -X POST -H 'Content-Type: application/json' http://localhost:9000/com.example.TurkeyService/EndCooking -d '{"turkey_id": "myfirstturkey"}'

	curl -X POST -H 'Content-Type: application/json' http://localhost:9000/com.example.TurkeyService/GetCurrentTurkey -d '{"turkey_id": "myfirstturkey"}'

The response should be:

	{"inOven":false,"done":"RAW","externalTemperature":0.0}%

But the question remains: **Was the turkey tasty?**

## Modifying the API to prevent salmonella poisoning

> **NOTE:** You can `git clone schema_changes` to get all of the code changes below.

Before determining taste though, the more important question is **Has the turkey reached an internal temperature such that salmonella bacteria have been killed?** Because, you know, having folks visit the hospital on account of Thanksgiving eating is not desirable.

Let's look at the current data model for our Turkey. Akka Serverless uses [Protocol Buffers](https://developers.google.com/protocol-buffers) as the specification format for the data and event model as well as the API interfaces. The below is the contents from the `turkey_domain.proto` file.

	syntax = "proto3";

	package com.example.domain;
	
	import "akkaserverless/annotations.proto";
	
	option (akkaserverless.file).event_sourced_entity = {
		name: "Turkey"
		entity_type: "turkey"
		state: "TurkeyState",
		events: ["InOven", "OutOfOven", "TemperatureChange"]
	};

	message TurkeyState {
		bool in_oven = 1;
		enum DoneStatus {
			RAW = 0;
			SALMONELLA = 1;
			STILL_SALMONELLA = 2;
			ALMOST_THERE = 3;
			PERFECT = 4;
		}
		DoneStatus done = 2;
		float external_temperature = 3;
	}
	
	message InOven {
		string turkey_id = 1 [(akkaserverless.field).entity_key = true];
	}
	message OutOfOven {
		string turkey_id = 1 [(akkaserverless.field).entity_key = true];
	}

	message TemperatureChange {
		string turkey_id = 1 [(akkaserverless.field).entity_key = true];
		float new_temperature = 3;
	}

There is much to unpack here but for now, let's focus on the mission at hand: preventing salmonella poisoning by fully cooking our bird! In the above, we see that we're tracking the external temperature. That's the oven. And we can imagine increasing or decreasing throughout the cooking process in order to ensure optimal results. But we won't know that unless we start tracking what the [USDA recommends](https://www.usda.gov/media/blog/2020/11/12/countdown-food-safe-thanksgiving-day-faqs#:~:text=A%3A%20The%20turkey%20is%20ready,innermost%20part%20of%20the%20wing.): internal temperature.

We can do that by adding a new attribute, `internal_temperature` to the `TurkeyState`:

	message TurkeyState {
		bool in_oven = 1;
		enum DoneStatus {
			RAW = 0;
			SALMONELLA = 1;
			STILL_SALMONELLA = 2;
			ALMOST_THERE = 3;
			PERFECT = 4;
		}
		DoneStatus done = 2;
		float external_temperature = 3;
		float internal_temperature = 4;
	}

That takes care of the domain data that we will manage. But how to communicate to the bird - or anyone else - that the internal temperature has indeed changed? In the above proto file, we see that we already have a `TemperatureChange` event defined. This is used in the `IncreaseOvenTemperature` and `DecreaseOvenTemperature` API signatures in the `turkey_api.proto`.  Let's try to re-use this approach by modifying the `TemperatureChange` message schema.

	message TemperatureChange {
    	string turkey_id = 1 [(akkaserverless.field).entity_key = true];
	    enum Type {
    	    INTERNAL = 0;
        	EXTERNAL = 1;
	    }
    	Type type = 2;
	    float new_temperature = 3;    
	}

Now we can identify the type of change and, as we will see in just a moment, store the data in the right place. In order to store data though we have to receive it. Let's move over to our API specification in the `turkey_api.proto` file.

	// This is the public API offered by your entity.
	syntax = "proto3";

  	import "google/protobuf/empty.proto";
	import "akkaserverless/annotations.proto";
	import "google/api/annotations.proto";
	import "turkey_domain.proto";
	
	package com.example;

  	message CookingCommand {
		string turkey_id = 1 [(akkaserverless.field).entity_key = true];
	}
	message TemperatureChangeCommand {
		string turkey_id = 1 [(akkaserverless.field).entity_key = true];
		float temperature_change = 2;
	}

	message GetTurkeyCommand {
		string turkey_id = 1 [(akkaserverless.field).entity_key = true];
	}

	service TurkeyService {
		option (akkaserverless.service) = {
			type : SERVICE_TYPE_ENTITY
			component : "com.example.domain.Turkey"
		};
		rpc StartCooking(CookingCommand) returns (google.protobuf.Empty);
		rpc EndCooking(CookingCommand) returns (google.protobuf.Empty);
		rpc IncreaseOvenTemperature(TemperatureChangeCommand) returns (google.protobuf.Empty);
		rpc DecreaseOvenTemperature(TemperatureChangeCommand) returns (google.protobuf.Empty);

		rpc GetCurrentTurkey(GetTurkeyCommand) returns (domain.TurkeyState);
	}

We see that we have API signatures - `Commands` - for increasing and decreasing oven temperature. Let's support increasing and decreasing turkey temperature. We can add the following to the above protobuf file.

		rpc IncreaseTurkeyTemperature(TemperatureChangeCommand) returns (google.protobuf.Empty);
		rpc DecreaseTurkeyTemperature(TemperatureChangeCommand) returns (google.protobuf.Empty);

So far, we have modified our data schema and API specification to account for this need to prevent salmonella poisoining. Now we have to add the appropriate logic in our Python code to account for this change. Let's look at our `turkey_eventsourced_entity.py`.

As mentioned earlier in this post, we are using [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html) as the state model for Akka Serverless. For the file that we're inspecting now - the logic behind getting data in and out of Akka - that means that we need to have code for handling both `commands` and `events`. We see both for our oven temperature changes; snippets for the `IncreaseOvenTemperature` flow below.

	@entity.command_handler("IncreaseOvenTemperature")
	def increase_oven_temperature(state: TurkeyState, command: TemperatureChangeCommand, context: EventSourcedCommandContext):
		np = TemperatureChange(turkey_id= command.turkey_id, new_temperature=(state.external_temperature + command.temperature_change))
		context.emit(np)
		return Empty()

	@entity.event_handler(TemperatureChange)
	def temperature_changed(state: TurkeyState, event: TemperatureChange ):
		state.external_temperature = event.new_temperature
		return state

When an API request is issued to Akka Serverless, to increase the temperature of the oven, that request is handled by the command handler above, which creates the `TemperatureChange` event and emits it. That event is subsequently, and **asynchronously**, handled by the event handler, which updates the actual state of the turkey with the new `external_temperature`. We need to make some modifications, to account for the `type` attribute of the `TemperatureChange` message schema, so we can reuse that, distinguishing between `internal` and `external` updates.

Let's first update the existing code, adding in the `type` attribute:

	@entity.command_handler("IncreaseOvenTemperature")
	def increase_oven_temperature(state: TurkeyState, command: TemperatureChangeCommand, context: EventSourcedCommandContext):
		np = TemperatureChange(turkey_id= command.turkey_id, type=TemperatureChange.Type.EXTERNAL, new_temperature=(state.external_temperature + command.temperature_change))
		context.emit(np)
		return Empty()

	@entity.command_handler("DecreaseOvenTemperature")
	def decrease_oven_temperature(state: TurkeyState, command: TemperatureChangeCommand, context: EventSourcedCommandContext):
		np = TemperatureChange(turkey_id= command.turkey_id, type=TemperatureChange.Type.EXTERNAL, new_temperature=(state.external_temperature - command.temperature_change))
		context.emit(np)
		return Empty()
		
Let's add the new command handlers for the increasing and decreasing of turkey temperature.

	@entity.command_handler("IncreaseTurkeyTemperature")
	def increase_turkey_temperature(state: TurkeyState, command: TemperatureChangeCommand, context: EventSourcedCommandContext):
		np = TemperatureChange(turkey_id= command.turkey_id, type=TemperatureChange.Type.INTERNAL, new_temperature=(state.internal_temperature + command.temperature_change))
		context.emit(np)
		return Empty()
	
	@entity.command_handler("DecreaseTurkeyTemperature")
	def decrease_turkey_temperature(state: TurkeyState, command: TemperatureChangeCommand, context: EventSourcedCommandContext):
		np = TemperatureChange(turkey_id= command.turkey_id, type=TemperatureChange.Type.INTERNAL, new_temperature=(state.internal_temperature - command.temperature_change))
		context.emit(np)
		return Empty()
		
And finally, let's update the event handler to account for the different types of temperature changes.

	@entity.event_handler(TemperatureChange)
	def temperature_changed(state: TurkeyState, event: TemperatureChange ):
		if event.type == TemperatureChange.Type.EXTERNAL:
			state.external_temperature = event.new_temperature
		elif event.type == TemperatureChange.Type.INTERNAL:
			state.internal_temperature = event.new_temperature
		return state

Smell that? That's some turkey cooking! Let's see how far along it is.

### Re-start the local dev environment and make some API calls

In that same terminal command window, you can `CTRL+C` to stop the current running service. If not already running, simply type `start.sh`, which will recompile all of the protobufs, stop any running associated Docker containers and start the updated Akka Serverless service.

And now for some cooking! I mean...curling!

	curl -X POST -H 'Content-Type: application/json' http://localhost:9000/com.example.TurkeyService/StartCooking -d '{"turkey_id": "myfirstturkey"}'
	
	curl -X POST -H 'Content-Type: application/json' http://localhost:9000/com.example.TurkeyService/IncreaseOvenTemperature -d '{"turkey_id": "myfirstturkey", "temperature_change": 100.0}'
	
	curl -X POST -H 'Content-Type: application/json' http://localhost:9000/com.example.TurkeyService/GetCurrentTurkey -d '{"turkey_id": "myfirstturkey"}'

The response should be:
	
	{"inOven":true,"done":"RAW","externalTemperature":100.0}%
	
	
		curl -i -v -X POST -H 'Content-Type: application/json' http://localhost:9000/com.example.TurkeyService/IncreaseTurkeyTemperature -d '{"turkey_id": "myfirstturkey", "temperature_change": 10.0}'

		curl -X POST -H 'Content-Type: application/json' http://localhost:9000/com.example.TurkeyService/GetCurrentTurkey -d '{"turkey_id": "myfirstturkey"}'
	
The response should be:

	{"inOven":true,"done":"RAW","externalTemperature":100.0,"internalTemperature":10.0}%

Before moving on to the next section of this post, please shut down the services. `CTRL+C` should work but it would be good to ensure proper shutdown of the Docker containers spun up: `docker-compose -f docker-compose-proxy.yml down`.
## Integrating Kafka events into our service

> **NOTE:** You can `git clone kafka_integration` to get all of the code changes below.

One of the amazing features in Akka Serverless is the ability to ingest (and egress if so desired) events from external messaging systems, e.g. Google Pubsub and Kafka, as well as use events to communicate between services running in the Akka Serverless Platform-as-a-Service (PaaS). You can read more about this feature [here](https://jpollock.github.io/akkaserverless-python-sdk/python/topic-eventing.html), although it is recommended to visit the main Akka Serverless [doc site](https://developer.lightbend.com/docs/akka-serverless/index.html) for all information, as the Python SDK is community managed and may lag behind the production offering.

Eventing easy to add though! And actually won't require any more Python coding beyond the already implemented change for tracking a turkey's internal temperature.

First, let's define that events will be supported, and of what type, for our APIs. For our prevent-salmonella-poisoning scenario, let's imagine that industry - a company perhaps - has delivered to the market a product that enables for remote recommendation generation based on machine-learning models being run in a cloud distant from the homes in which the turkeys are being cooked. We're not going to actually create an ML model and run it but we will simulate the process by which those events could be published on to a company's Kafka broker and sent through the pipes into Akka Serverless. So instead of making `gRPC` or `HTTP` API calls to increase the temperature of the oven, for example, we can have those commands issued from deep within the bowels of a company's infrastructure and sent over Kafka to the turkey entities managed in the Akka Serverless cloud. Neat, huh?

To do that, we will update the `commands` in the `turkey_api.proto`: `IncreaseOvenTemperature` and `DecreaseOvenTemperature`. We can add the protobuf annotations supplied by Akka Serverless, to say "when `TemperatureChangeCommands` are received on the `increase_temp` topic, execute the `IncreaseOvenTemperature` command." We do that for both commands.

	rpc IncreaseOvenTemperature(TemperatureChangeCommand) returns (google.protobuf.Empty) {
		option (akkaserverless.method).eventing.in = {
			topic: "increase_temp"
		};
	}
	rpc DecreaseOvenTemperature(TemperatureChangeCommand) returns (google.protobuf.Empty) {
		option (akkaserverless.method).eventing.in = {
			topic: "decrease_temp"
		};	
	}

That is it. I did say it was neat, didn't I?

To run this locally, we take advantage of a great product, [Redpanda](https://vectorized.io/redpanda/) from Vectorized.io. They have a Kafka API compatible streaming platform for mission-critical workloads, and a great way to run that locally (Docker or not).

From a terminal window, in the root directory of the project, with the virtual environment initialized, run the `start.sh` command. You will notice some error messages that indicate that the topics, `increase_temp` and `decrease_temp` need to be created. Ignore for now, given that we are running locally. When moving to a hosted offering, like Confluent Cloud you will need to create topics [manually](https://developer.lightbend.com/docs/akka-serverless/projects/message-brokers.html#_confluent_cloud).

### Make some API calls and issue Kafka events
First, let's get the initial state of our turkey.

	curl -X GET -H 'Content-Type: application/json' http://localhost:9000/turkeys/myfirstturkey
	
The result should look like.
	
	{"inOven":false,"done":"RAW","internalTemperature":0.0,"externalTemperature":0.0}%
	
From a new terminal window, in the root directory of the project, with the virtual environment initialized, run the following command: `python3 kafka_message_generator.py --id myfirstturkey --increase --amount 100`. This will fire a `TemperatureChangeCommand` event on the `increase_temp` topic, sent to the locally running Redpanda cluster.

Now if we make that same `GET` call again, from above.

	curl -X GET -H 'Content-Type: application/json' http://localhost:9000/turkeys/myfirstturkey
	
The result should look like.
	
	{"inOven":false,"done":"RAW","internalTemperature":0.0,"externalTemperature":100.0}%
	
We have successfully delivered event data from an external source, via Kafka (Redpanda in this local case), into Akka Serverless. Profit!

## Where to next?
1. [Install the Akka Serverless CLI](https://developer.lightbend.com/docs/akka-serverless/akkasls/install-akkasls.html);
2. Once installed, you can sign-up for a new account via `akkasls auth signup` (or visit the [Console Sign-up page](https://console.akkaserverless.lightbend.com/p/register);
3. Login to your account via the CLI: `akkasls auth login`;
4. In a terminal window, in the root directory of the project, with the virtual environment initialized, set some Docker environment variables: `export DOCKER_REGISTRY=docker.io` and `export DOCKER_USER=<insert your docker hub username>`.
5. In the same terminal window, run the command `docker_build.sh 0.0.1` and then `docker_push.sh 0.0.1`.
6. Configure your Kakfa broker per the [documentation](https://developer.lightbend.com/docs/akka-serverless/projects/message-brokers.html#_confluent_cloud. **Do not pick the Python client, since the actual Kafka integration is running in the Akka Serverless proxy.**
7. Per the same documentation, create the `increase_temp` and `decrease_temp` topics in Confluent Cloud.
9. In the same terminal window, run the command `akkasls services deploy turkey-tracker $DOCKER_REGISTRY/$DOCKER_USER/turkey_tracker_python:0.0.1`
10. In the same terminal window, run the command `akkasls services proxy turkey-tracker --grpcui`. You can use this to explore the API running in the cloud without [exposing](https://developer.lightbend.com/docs/akka-serverless/akkasls/akkasls_services_expose.html) over the internet.

## Conclusion
Hopefully you now have a better idea on how some of the moving parts work in Akka Serverless and how you can build event driven APIs and services, including those that can be integrated with Kafka. While we're not really cooking turkeys, perhaps one day, we can leverage technologies like this to improve all of our Thanksgiving food-eating experiences!

And don't forget: get your free Akka Serverless account [here](https://akkaserverless.com).