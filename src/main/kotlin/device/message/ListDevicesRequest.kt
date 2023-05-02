package device.message

import akka.actor.typed.ActorRef

class ListDevicesRequest(
    val requestId: Long,
    val groupId: String,
    val replyTo: ActorRef<ListDevicesResponse>
): Command
