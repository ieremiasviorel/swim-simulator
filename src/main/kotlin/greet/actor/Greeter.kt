package greet.actor

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import greet.message.Greet
import greet.message.Greeted

class Greeter private constructor(context: ActorContext<Greet>) : AbstractBehavior<Greet>(context) {

    override fun createReceive(): Receive<Greet> {
        return newReceiveBuilder()
            .onMessage(Greet::class.java, this::onGreet)
            .build()
    }

    private fun onGreet(request: Greet): Behavior<Greet> {
        println("Hello ${request.whom}!")
        request.replyTo.tell(Greeted(request.whom, context.self))
        return this
    }

    companion object {
        fun create(): Behavior<Greet> {
            return Behaviors.setup { context: ActorContext<Greet> -> Greeter(context) }
        }
    }
}
