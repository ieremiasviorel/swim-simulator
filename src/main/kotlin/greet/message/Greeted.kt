package greet.message

import akka.actor.typed.ActorRef

data class Greeted(val whom: String, val from: ActorRef<Greet>)
