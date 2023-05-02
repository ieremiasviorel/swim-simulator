package device.message

import akka.actor.typed.ActorRef

class RegisterDeviceResponse(val device: ActorRef<Command>): Command
