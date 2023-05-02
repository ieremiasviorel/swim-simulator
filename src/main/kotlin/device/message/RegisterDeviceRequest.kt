package device.message

import akka.actor.typed.ActorRef

class RegisterDeviceRequest(
    val groupId: String,
    val deviceId: String,
    val replyTo: ActorRef<Command>
) : Command
