package greet.actor

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import greet.message.Greet
import greet.message.Greeted

class GreeterBot private constructor(context: ActorContext<Greeted>, private val maxGreetingCount: Int) :
    AbstractBehavior<Greeted>(context) {
    private var greetingCount = 0

    override fun createReceive(): Receive<Greeted> {
        return newReceiveBuilder()
            .onMessage(Greeted::class.java, this::onGreeted)
            .build()
    }

    private fun onGreeted(request: Greeted): Behavior<Greeted> {
        greetingCount++
        println("Greeting $greetingCount for ${request.whom}")
        return if (greetingCount == maxGreetingCount) {
            Behaviors.stopped()
        } else {
            request.from.tell(Greet(request.whom, context.self))
            this
        }
    }

    companion object {
        fun create(max: Int): Behavior<Greeted> {
            return Behaviors.setup { context -> GreeterBot(context, max) }
        }
    }
}
