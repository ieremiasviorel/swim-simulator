package greet.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import greet.message.Greet
import greet.message.Greeted
import greet.message.SayHello

class GreeterGuardian private constructor(context: ActorContext<SayHello>) : AbstractBehavior<SayHello>(context) {

    private val greeter: ActorRef<Greet> = context.spawn(Greeter.create(), "greeter")

    override fun createReceive(): Receive<SayHello> {
        return newReceiveBuilder()
            .onMessage(SayHello::class.java, this::onSayHello)
            .build()
    }

    private fun onSayHello(request: SayHello): Behavior<SayHello> {
        val replyTo: ActorRef<Greeted> = context.spawn(GreeterBot.create(3), request.name)
        greeter.tell(Greet(request.name, replyTo))
        return this
    }

    companion object {
        fun create(): Behavior<SayHello> {
            return Behaviors.setup { context: ActorContext<SayHello> -> GreeterGuardian(context) }
        }
    }
}
