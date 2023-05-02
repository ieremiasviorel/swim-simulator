package greet.message

import akka.actor.typed.ActorRef

data class Greet(val whom: String, val replyTo: ActorRef<Greeted>)
