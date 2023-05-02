package device.message

import akka.actor.typed.ActorRef

class ReadGroupTemperatureRequest(
    val requestId: Long,
    val groupId: String,
    val replyTo: ActorRef<Command>
): Command
