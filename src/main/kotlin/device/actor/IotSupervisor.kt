package device.actor

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class IotSupervisor private constructor(context: ActorContext<Void>) : AbstractBehavior<Void>(context) {
    init {
        context.log.info("Started")
    }

    override fun createReceive(): Receive<Void> {
        return newReceiveBuilder()
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()
    }

    private fun onPostStop(): IotSupervisor {
        context.log.info("Stopped")
        return this
    }

    companion object {
        fun create(): Behavior<Void> {
            return Behaviors.setup { context: ActorContext<Void> -> IotSupervisor(context) }
        }
    }
}
