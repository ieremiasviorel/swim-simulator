package device.message

import akka.actor.typed.ActorRef

class RecordTemperatureRequest(
    val requestId: Long,
    val value: Double,
    val replyTo: ActorRef<RecordTemperatureResponse>
) : Command
