package device.actor

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import device.message.*

class Device private constructor(
    context: ActorContext<Command>,
    private val groupId: String,
    private val deviceId: String
) : AbstractBehavior<Command>(context) {
    private var lastTemperatureReading: Double? = null

    init {
        context.log.info("$groupId-$deviceId started")
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(RecordTemperatureRequest::class.java, this::onRecordTemperature)
            .onMessage(ReadTemperatureRequest::class.java, this::onReadTemperature)
            .onMessage(PassivateDeviceRequest::class.java) { Behaviors.stopped() }
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()
    }

    private fun onRecordTemperature(request: RecordTemperatureRequest): Device {
        context.log.info("Recorded temperature reading ${request.value} with ${request.requestId}")
        lastTemperatureReading = request.value
        request.replyTo.tell(RecordTemperatureResponse(request.requestId))
        return this
    }

    private fun onReadTemperature(request: ReadTemperatureRequest): Device {
        request.replyTo.tell(
            ReadTemperatureResponse(
                request.requestId,
                deviceId,
                lastTemperatureReading
            )
        )
        return this
    }

    private fun onPostStop(): Device {
        context.log.info("$groupId-$deviceId stopped")
        return this
    }

    companion object {
        fun create(groupId: String, deviceId: String): Behavior<Command> {
            return Behaviors.setup { context -> Device(context, groupId, deviceId) }
        }
    }
}
