package device.message

import akka.actor.typed.ActorRef

class ReadTemperatureRequest(
    val requestId: Long,
    val replyTo: ActorRef<ReadTemperatureResponse>
) : Command
